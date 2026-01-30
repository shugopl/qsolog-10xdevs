package com.pl.shugo.gsolog.infrastructure.adapter;

import com.pl.shugo.gsolog.api.dto.QsoDescriptionRequest;
import com.pl.shugo.gsolog.api.dto.StatsResponse;
import com.pl.shugo.gsolog.domain.port.AiHelperPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * OpenAI API implementation of AiHelperPort.
 * Requires OPENAI_API_KEY to be configured.
 */
public class OpenAiHelperAdapter implements AiHelperPort {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiHelperAdapter.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-3.5-turbo";

    private final WebClient webClient;
    private final String apiKey;

    public OpenAiHelperAdapter(String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(OPENAI_API_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public Mono<String> generateQsoDescription(QsoDescriptionRequest request) {
        logger.debug("OpenAI generating QSO description for {}", request.theirCallsign());

        String language = request.language() != null ? request.language() : "EN";
        String prompt = buildQsoPrompt(request, language);

        return callOpenAi(prompt)
                .doOnError(error -> logger.error("OpenAI QSO description failed", error))
                .onErrorResume(error -> {
                    // Fallback to mock on error
                    logger.warn("Falling back to mock description due to OpenAI error");
                    MockAiHelperAdapter fallback = new MockAiHelperAdapter();
                    return fallback.generateQsoDescription(request);
                });
    }

    @Override
    public Mono<String> generatePeriodReport(StatsResponse stats, String language) {
        logger.debug("OpenAI generating period report in {}", language);

        String prompt = buildReportPrompt(stats, language);

        return callOpenAi(prompt)
                .doOnError(error -> logger.error("OpenAI report generation failed", error))
                .onErrorResume(error -> {
                    // Fallback to mock on error
                    logger.warn("Falling back to mock report due to OpenAI error");
                    MockAiHelperAdapter fallback = new MockAiHelperAdapter();
                    return fallback.generatePeriodReport(stats, language);
                });
    }

    private String buildQsoPrompt(QsoDescriptionRequest req, String language) {
        StringBuilder prompt = new StringBuilder();

        if ("PL".equalsIgnoreCase(language)) {
            prompt.append("Wygeneruj krótki, przyjazny opis łączności radiowej w języku polskim. ");
            prompt.append("Ton: neutralny, luźny, jak między krótkofalowcami. ");
        } else {
            prompt.append("Generate a brief, friendly description of a ham radio contact in English. ");
            prompt.append("Tone: neutral, casual, as between radio operators. ");
        }

        prompt.append("\n\nDetails:\n");
        prompt.append("Callsign: ").append(req.theirCallsign()).append("\n");

        if (req.qsoDate() != null) {
            prompt.append("Date: ").append(req.qsoDate().format(DATE_FORMATTER)).append("\n");
        }

        if (req.timeOn() != null) {
            prompt.append("Time: ").append(req.timeOn().format(TIME_FORMATTER)).append(" UTC\n");
        }

        if (req.band() != null) {
            prompt.append("Band: ").append(req.band()).append("\n");
        }

        if (req.mode() != null) {
            prompt.append("Mode: ").append(req.mode()).append("\n");
        }

        if (req.rstSent() != null && req.rstRecv() != null) {
            prompt.append("RST: sent ").append(req.rstSent())
                    .append(", received ").append(req.rstRecv()).append("\n");
        }

        if (req.qth() != null && !req.qth().isEmpty()) {
            prompt.append("QTH: ").append(req.qth()).append("\n");
        }

        if (req.notes() != null && !req.notes().isEmpty()) {
            prompt.append("Notes: ").append(req.notes()).append("\n");
        }

        prompt.append("\nGenerate a 2-3 sentence description. Keep it concise and natural.");

        return prompt.toString();
    }

    private String buildReportPrompt(StatsResponse stats, String language) {
        StringBuilder prompt = new StringBuilder();

        if ("PL".equalsIgnoreCase(language)) {
            prompt.append("Wygeneruj raport narracyjny z działalności krótkofalowej w języku polskim. ");
            prompt.append("Ton: neutralny, luźny. ");
        } else {
            prompt.append("Generate a narrative report of ham radio activity in English. ");
            prompt.append("Tone: neutral, casual. ");
        }

        prompt.append("\n\nStatistics:\n");
        prompt.append("Total QSOs: ").append(stats.totals().all());
        prompt.append(" (").append(stats.totals().confirmed()).append(" confirmed)\n\n");

        if (!stats.countsByBand().isEmpty()) {
            prompt.append("Band activity:\n");
            stats.countsByBand().forEach(band -> {
                prompt.append("- ").append(band.band()).append(": ")
                        .append(band.countAll()).append(" QSOs (")
                        .append(band.countConfirmed()).append(" confirmed)\n");
            });
            prompt.append("\n");
        }

        if (!stats.countsByMode().isEmpty()) {
            prompt.append("Mode distribution:\n");
            stats.countsByMode().forEach(mode -> {
                prompt.append("- ").append(mode.mode()).append(": ")
                        .append(mode.countAll()).append(" QSOs\n");
            });
        }

        prompt.append("\nGenerate a friendly narrative summary (3-5 sentences). Don't just list numbers - tell a story.");

        return prompt.toString();
    }

    private Mono<String> callOpenAi(String prompt) {
        Map<String, Object> request = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "max_tokens", 300
        );

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    // Parse OpenAI response
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        if (message != null) {
                            return (String) message.get("content");
                        }
                    }
                    throw new RuntimeException("Invalid OpenAI response format");
                });
    }
}
