package com.pl.shugo.gsolog.api.dto;

/**
 * Statistics for a single band.
 */
public record BandStats(
        String band,
        Long countAll,
        Long countConfirmed
) {
}
