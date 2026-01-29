package com.pl.shugo.gsolog.api.dto;

import java.time.LocalDate;

/**
 * Statistics for a single day.
 */
public record DayStats(
        LocalDate date,
        Long countAll,
        Long countConfirmed
) {
}
