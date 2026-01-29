package com.pl.shugo.gsolog.api.dto;

/**
 * Statistics for a single mode.
 */
public record ModeStats(
        String mode,
        Long countAll,
        Long countConfirmed
) {
}
