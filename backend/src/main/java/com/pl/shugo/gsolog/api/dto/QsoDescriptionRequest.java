package com.pl.shugo.gsolog.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request for AI-generated QSO description.
 */
public record QsoDescriptionRequest(
        String theirCallsign,
        LocalDate qsoDate,
        LocalTime timeOn,
        String band,
        String mode,
        String rstSent,
        String rstRecv,
        String qth,
        String notes,
        String language  // "PL" or "EN"
) {
}
