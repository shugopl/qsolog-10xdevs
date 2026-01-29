package com.pl.shugo.gsolog.api.dto;

import java.util.List;

/**
 * Statistics summary response.
 * Contains aggregated QSO counts by band, mode, and day.
 */
public record StatsResponse(
        List<BandStats> countsByBand,
        List<ModeStats> countsByMode,
        List<DayStats> countsByDay,
        Totals totals
) {
}
