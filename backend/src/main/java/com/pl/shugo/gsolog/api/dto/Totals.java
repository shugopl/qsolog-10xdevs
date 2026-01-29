package com.pl.shugo.gsolog.api.dto;

/**
 * Total QSO counts.
 */
public record Totals(
        Long all,
        Long confirmed
) {
}
