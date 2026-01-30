package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.api.dto.AiReportResponse;
import com.pl.shugo.gsolog.api.dto.AiTextResponse;
import com.pl.shugo.gsolog.api.dto.QsoDescriptionRequest;
import com.pl.shugo.gsolog.application.service.AiService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * AI helper REST controller.
 * Provides AI-generated suggestions for QSO descriptions and period reports.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Generate AI description for a QSO.
     * Returns suggestion only - does NOT auto-write to QSO notes.
     *
     * @param request QSO details and language preference
     * @return AI-generated description text
     */
    @PostMapping("/qso-description")
    public Mono<AiTextResponse> generateQsoDescription(@RequestBody QsoDescriptionRequest request) {
        return aiService.generateQsoDescription(request);
    }

    /**
     * Generate AI report for a period.
     * Computes stats and generates narrative text.
     * Saves report to history table.
     *
     * @param userId User ID from JWT token principal
     * @param from   Start date (optional)
     * @param to     End date (optional)
     * @param lang   Language ("PL" or "EN")
     * @return AI-generated report
     */
    @PostMapping("/period-report")
    public Mono<AiReportResponse> generatePeriodReport(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "EN") String lang) {

        return aiService.generatePeriodReport(userId, from, to, lang);
    }

    /**
     * Get list of AI-generated reports.
     * Optionally filtered by date range.
     *
     * @param userId User ID from JWT token principal
     * @param from   Start date filter (optional)
     * @param to     End date filter (optional)
     * @return List of reports
     */
    @GetMapping("/reports")
    public Flux<AiReportResponse> getReports(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {

        return aiService.getReports(userId, from, to);
    }

    /**
     * Get a specific AI report by ID.
     *
     * @param userId User ID from JWT token principal
     * @param id     Report ID
     * @return Report if found and owned by user
     */
    @GetMapping("/reports/{id}")
    public Mono<AiReportResponse> getReport(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {

        return aiService.getReport(userId, id);
    }
}
