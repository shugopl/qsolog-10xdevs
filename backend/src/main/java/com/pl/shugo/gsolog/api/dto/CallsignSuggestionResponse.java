package com.pl.shugo.gsolog.api.dto;

/**
 * Response for callsign suggestions from user's QSO history.
 */
public record CallsignSuggestionResponse(
        String callsign,
        String lastKnownName,
        String lastKnownQth,
        String lastNotesSnippet,
        String mostCommonBand,
        String mostCommonMode
) {
}
