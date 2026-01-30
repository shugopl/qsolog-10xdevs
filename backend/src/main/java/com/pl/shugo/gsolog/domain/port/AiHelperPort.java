package com.pl.shugo.gsolog.domain.port;

import com.pl.shugo.gsolog.api.dto.QsoDescriptionRequest;
import com.pl.shugo.gsolog.api.dto.StatsResponse;
import reactor.core.publisher.Mono;

/**
 * Port for AI text generation.
 * Implementations can be mock or OpenAI adapters.
 */
public interface AiHelperPort {

    /**
     * Generate a description for a QSO.
     *
     * @param request QSO details
     * @return Generated description text
     */
    Mono<String> generateQsoDescription(QsoDescriptionRequest request);

    /**
     * Generate a narrative report from statistics.
     *
     * @param stats    Statistics data
     * @param language Language code ("PL" or "EN")
     * @return Generated report text
     */
    Mono<String> generatePeriodReport(StatsResponse stats, String language);
}
