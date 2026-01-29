package com.pl.shugo.gsolog.application.service;

import com.pl.shugo.gsolog.api.dto.*;
import com.pl.shugo.gsolog.domain.repository.QsoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Statistics service.
 * Provides aggregated QSO statistics for users.
 */
@Service
public class StatsService {

    private final QsoRepository qsoRepository;

    public StatsService(QsoRepository qsoRepository) {
        this.qsoRepository = qsoRepository;
    }

    /**
     * Get statistics summary for a user within a date range.
     *
     * @param userId User ID
     * @param from   Start date (inclusive, null for no limit)
     * @param to     End date (inclusive, null for no limit)
     * @return Statistics response with counts by band, mode, day, and totals
     */
    public Mono<StatsResponse> getStatsSummary(UUID userId, LocalDate from, LocalDate to) {
        // Fetch all statistics in parallel
        Mono<List<BandStats>> bandStats = qsoRepository.getStatsByBand(userId, from, to)
                .map(this::mapToBandStats)
                .collectList();

        Mono<List<ModeStats>> modeStats = qsoRepository.getStatsByMode(userId, from, to)
                .map(this::mapToModeStats)
                .collectList();

        Mono<List<DayStats>> dayStats = qsoRepository.getStatsByDay(userId, from, to)
                .map(this::mapToDayStats)
                .collectList();

        Mono<Totals> totals = qsoRepository.getTotals(userId, from, to)
                .map(this::mapToTotals);

        // Combine all results into a single response
        return Mono.zip(bandStats, modeStats, dayStats, totals)
                .map(tuple -> new StatsResponse(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3(),
                        tuple.getT4()
                ));
    }

    private BandStats mapToBandStats(Map<String, Object> row) {
        return new BandStats(
                (String) row.get("band"),
                getLongValue(row, "count_all"),
                getLongValue(row, "count_confirmed")
        );
    }

    private ModeStats mapToModeStats(Map<String, Object> row) {
        return new ModeStats(
                (String) row.get("mode"),
                getLongValue(row, "count_all"),
                getLongValue(row, "count_confirmed")
        );
    }

    private DayStats mapToDayStats(Map<String, Object> row) {
        return new DayStats(
                (LocalDate) row.get("qso_date"),
                getLongValue(row, "count_all"),
                getLongValue(row, "count_confirmed")
        );
    }

    private Totals mapToTotals(Map<String, Object> row) {
        return new Totals(
                getLongValue(row, "count_all"),
                getLongValue(row, "count_confirmed")
        );
    }

    private Long getLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}
