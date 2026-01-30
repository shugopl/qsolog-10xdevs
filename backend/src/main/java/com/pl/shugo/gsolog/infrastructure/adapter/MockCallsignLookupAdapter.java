package com.pl.shugo.gsolog.infrastructure.adapter;

import com.pl.shugo.gsolog.api.dto.CallsignLookupResponse;
import com.pl.shugo.gsolog.domain.port.CallsignLookupPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Mock implementation of CallsignLookupPort.
 * Used when HamQTH credentials are not configured.
 * Returns empty results to allow app to run without external API.
 */
public class MockCallsignLookupAdapter implements CallsignLookupPort {

    private static final Logger logger = LoggerFactory.getLogger(MockCallsignLookupAdapter.class);

    @Override
    public Mono<CallsignLookupResponse> lookup(String callsign) {
        logger.debug("Mock lookup for callsign: {} (HamQTH credentials not configured)", callsign);

        // Return empty result - mock adapter doesn't provide data
        return Mono.empty();
    }
}
