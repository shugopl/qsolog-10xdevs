package com.pl.shugo.gsolog.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response for AI-generated report.
 */
public record AiReportResponse(
        UUID id,
        LocalDate dateFrom,
        LocalDate dateTo,
        String language,
        String content,
        Instant createdAt
) {
}
