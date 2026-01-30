package com.pl.shugo.gsolog.application.service;

import com.pl.shugo.gsolog.api.dto.AiReportResponse;
import com.pl.shugo.gsolog.api.dto.AiTextResponse;
import com.pl.shugo.gsolog.api.dto.QsoDescriptionRequest;
import com.pl.shugo.gsolog.domain.entity.AiReport;
import com.pl.shugo.gsolog.domain.port.AiHelperPort;
import com.pl.shugo.gsolog.domain.repository.AiReportRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * AI helper service.
 * Provides AI-generated text suggestions and report management.
 */
@Service
public class AiService {

    private final AiHelperPort aiHelperPort;
    private final StatsService statsService;
    private final AiReportRepository aiReportRepository;

    public AiService(AiHelperPort aiHelperPort, StatsService statsService, AiReportRepository aiReportRepository) {
        this.aiHelperPort = aiHelperPort;
        this.statsService = statsService;
        this.aiReportRepository = aiReportRepository;
    }

    /**
     * Generate AI description for a QSO.
     * Returns suggestion only - never auto-writes to QSO.
     *
     * @param request QSO details and language
     * @return AI-generated description
     */
    public Mono<AiTextResponse> generateQsoDescription(QsoDescriptionRequest request) {
        String language = request.language() != null ? request.language() : "EN";

        return aiHelperPort.generateQsoDescription(request)
                .map(text -> new AiTextResponse(language, text));
    }

    /**
     * Generate AI report for a period.
     * Computes stats, generates narrative, and saves to history.
     *
     * @param userId User ID
     * @param from   Start date (optional)
     * @param to     End date (optional)
     * @param lang   Language ("PL" or "EN")
     * @return AI-generated report
     */
    public Mono<AiReportResponse> generatePeriodReport(UUID userId, LocalDate from, LocalDate to, String lang) {
        String language = lang != null ? lang : "EN";

        return statsService.getStatsSummary(userId, from, to)
                .flatMap(stats -> aiHelperPort.generatePeriodReport(stats, language)
                        .map(text -> AiReport.create(userId, from, to, language, text)))
                .flatMap(aiReportRepository::save)
                .map(this::mapToResponse);
    }

    /**
     * Get all reports for a user, optionally filtered by date range.
     *
     * @param userId User ID
     * @param from   Start date filter (optional)
     * @param to     End date filter (optional)
     * @return List of reports
     */
    public Flux<AiReportResponse> getReports(UUID userId, LocalDate from, LocalDate to) {
        if (from != null || to != null) {
            return aiReportRepository.findByUserIdAndDateRange(userId, from, to)
                    .map(this::mapToResponse);
        } else {
            return aiReportRepository.findByUserIdOrderByCreatedAtDesc(userId)
                    .map(this::mapToResponse);
        }
    }

    /**
     * Get a specific report by ID.
     *
     * @param userId   User ID (for ownership check)
     * @param reportId Report ID
     * @return Report if found and owned by user
     */
    public Mono<AiReportResponse> getReport(UUID userId, UUID reportId) {
        return aiReportRepository.findByIdAndUserId(reportId, userId)
                .map(this::mapToResponse);
    }

    private AiReportResponse mapToResponse(AiReport report) {
        return new AiReportResponse(
                report.getId(),
                report.getFromDate(),
                report.getToDate(),
                report.getLanguage(),
                report.getContent(),
                report.getCreatedAt()
        );
    }
}
