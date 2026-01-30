package com.pl.shugo.gsolog.api.dto;

/**
 * Response for callsign lookup from external service.
 */
public record CallsignLookupResponse(
        String callsign,
        String name,
        String qth,
        String grid,
        String country
) {
}
