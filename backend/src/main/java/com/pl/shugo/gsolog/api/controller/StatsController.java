package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.api.dto.StatsResponse;
import com.pl.shugo.gsolog.application.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Statistics REST controller.
 * Provides aggregated QSO statistics for authenticated users.
 */
@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * Get statistics summary for the authenticated user.
     * Optionally filtered by date range.
     *
     * @param userId User ID from JWT token principal
     * @param from   Start date (inclusive, optional)
     * @param to     End date (inclusive, optional)
     * @return Statistics response with counts by band, mode, day, and totals
     */
    @GetMapping("/summary")
    public Mono<StatsResponse> getStatsSummary(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return statsService.getStatsSummary(userId, from, to);
    }
}
