package com.pl.shugo.gsolog.infrastructure.adapter;

import com.pl.shugo.gsolog.api.dto.CallsignLookupResponse;
import com.pl.shugo.gsolog.domain.port.CallsignLookupPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HamQTH API implementation of CallsignLookupPort.
 * Includes in-memory caching with TTL and rate limiting backoff.
 */
public class HamQthCallsignLookupAdapter implements CallsignLookupPort {

    private static final Logger logger = LoggerFactory.getLogger(HamQthCallsignLookupAdapter.class);
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration RETRY_MIN_BACKOFF = Duration.ofSeconds(1);
    private static final Duration RETRY_MAX_BACKOFF = Duration.ofSeconds(10);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final WebClient webClient;
    private final String username;
    private final String password;
    private final Map<String, CachedLookup> cache = new ConcurrentHashMap<>();

    private volatile String sessionId;
    private volatile long sessionExpiry = 0;

    public HamQthCallsignLookupAdapter(String baseUrl, String username, String password) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.username = username;
        this.password = password;
    }

    @Override
    public Mono<CallsignLookupResponse> lookup(String callsign) {
        String normalizedCallsign = callsign.toUpperCase();

        // Check cache first
        CachedLookup cached = cache.get(normalizedCallsign);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Cache hit for callsign: {}", normalizedCallsign);
            return Mono.justOrEmpty(cached.response);
        }

        // Perform lookup with retry and backoff
        return ensureSession()
                .flatMap(sid -> performLookup(normalizedCallsign, sid))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_MIN_BACKOFF)
                        .maxBackoff(RETRY_MAX_BACKOFF)
                        .filter(throwable -> !(throwable instanceof SessionExpiredException)))
                .doOnNext(response -> cacheResponse(normalizedCallsign, response))
                .doOnError(error -> logger.warn("Lookup failed for callsign: {}", normalizedCallsign, error))
                .onErrorResume(error -> Mono.empty()); // Return empty on error
    }

    private Mono<String> ensureSession() {
        long now = System.currentTimeMillis();
        if (sessionId != null && now < sessionExpiry) {
            return Mono.just(sessionId);
        }

        // Session expired or not present, login
        return login();
    }

    private Mono<String> login() {
        logger.debug("Logging in to HamQTH");

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/xml.php")
                        .queryParam("u", username)
                        .queryParam("p", password)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseSessionId)
                .doOnNext(sid -> {
                    this.sessionId = sid;
                    this.sessionExpiry = System.currentTimeMillis() + Duration.ofHours(1).toMillis();
                    logger.debug("HamQTH session established");
                })
                .onErrorResume(error -> {
                    logger.error("HamQTH login failed", error);
                    return Mono.empty();
                });
    }

    private Mono<CallsignLookupResponse> performLookup(String callsign, String sid) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/xml.php")
                        .queryParam("id", sid)
                        .queryParam("callsign", callsign)
                        .queryParam("prg", "QSOLOG")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(xml -> parseCallsignData(callsign, xml))
                .onErrorResume(error -> {
                    logger.warn("HamQTH lookup failed for {}", callsign, error);
                    return Mono.empty();
                });
    }

    private String parseSessionId(String xml) {
        // Simple XML parsing for session_id
        int start = xml.indexOf("<session_id>");
        int end = xml.indexOf("</session_id>");
        if (start >= 0 && end > start) {
            return xml.substring(start + 12, end);
        }
        throw new SessionExpiredException("Failed to parse session ID from HamQTH response");
    }

    private CallsignLookupResponse parseCallsignData(String callsign, String xml) {
        // Simple XML parsing for callsign data
        String name = extractXmlValue(xml, "nick");
        if (name == null || name.isEmpty()) {
            name = extractXmlValue(xml, "adr_name");
        }
        String qth = extractXmlValue(xml, "adr_city");
        String grid = extractXmlValue(xml, "grid");
        String country = extractXmlValue(xml, "country");

        // Check if we got any data
        if (name == null && qth == null && grid == null && country == null) {
            // No data found, check for error
            String error = extractXmlValue(xml, "error");
            if (error != null) {
                logger.warn("HamQTH error for {}: {}", callsign, error);
            }
            return null;
        }

        return new CallsignLookupResponse(callsign, name, qth, grid, country);
    }

    private String extractXmlValue(String xml, String tag) {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);
        if (start >= 0 && end > start) {
            return xml.substring(start + startTag.length(), end).trim();
        }
        return null;
    }

    private void cacheResponse(String callsign, CallsignLookupResponse response) {
        if (response != null) {
            cache.put(callsign, new CachedLookup(response, System.currentTimeMillis() + CACHE_TTL.toMillis()));
            logger.debug("Cached lookup result for: {}", callsign);
        }
    }

    private static class CachedLookup {
        final CallsignLookupResponse response;
        final long expiryTime;

        CachedLookup(CallsignLookupResponse response, long expiryTime) {
            this.response = response;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private static class SessionExpiredException extends RuntimeException {
        public SessionExpiredException(String message) {
            super(message);
        }
    }
}
