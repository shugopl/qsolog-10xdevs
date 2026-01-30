package com.pl.shugo.gsolog.application.service;

import com.pl.shugo.gsolog.api.dto.CallsignSuggestionResponse;
import com.pl.shugo.gsolog.domain.entity.Qso;
import com.pl.shugo.gsolog.domain.repository.QsoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service for generating callsign suggestions from user's QSO history.
 */
@Service
public class SuggestionsService {

    private final QsoRepository qsoRepository;

    public SuggestionsService(QsoRepository qsoRepository) {
        this.qsoRepository = qsoRepository;
    }

    /**
     * Get suggestions for a callsign based on user's QSO history.
     * Returns information from the most recent QSO and most common band/mode.
     *
     * @param userId   User ID
     * @param callsign Callsign to get suggestions for
     * @return Suggestion response or empty if no history found
     */
    public Mono<CallsignSuggestionResponse> getSuggestions(UUID userId, String callsign) {
        Mono<Qso> mostRecentQso = qsoRepository.findMostRecentByCallsign(userId, callsign);
        Mono<String> mostCommonBand = qsoRepository.getMostCommonBandForCallsign(userId, callsign)
                .map(row -> (String) row.get("band"));
        Mono<String> mostCommonMode = qsoRepository.getMostCommonModeForCallsign(userId, callsign)
                .map(row -> (String) row.get("mode"));

        return Mono.zip(
                mostRecentQso.defaultIfEmpty(new Qso()),
                mostCommonBand.defaultIfEmpty(""),
                mostCommonMode.defaultIfEmpty("")
        ).flatMap(tuple -> {
            Qso recentQso = tuple.getT1();
            String band = tuple.getT2();
            String mode = tuple.getT3();

            // If no QSO found, return empty
            if (recentQso.getId() == null) {
                return Mono.empty();
            }

            // Extract name from QTH or notes (simple heuristic)
            String name = extractNameFromQso(recentQso);
            String qth = recentQso.getQth();
            String notesSnippet = truncateNotes(recentQso.getNotes());

            CallsignSuggestionResponse response = new CallsignSuggestionResponse(
                    callsign.toUpperCase(),
                    name,
                    qth,
                    notesSnippet,
                    band.isEmpty() ? null : band,
                    mode.isEmpty() ? null : mode
            );

            return Mono.just(response);
        });
    }

    /**
     * Try to extract operator name from QSO data.
     * This is a simple heuristic - in real implementation, name might be a dedicated field.
     */
    private String extractNameFromQso(Qso qso) {
        // For now, we don't have a dedicated name field in QSO
        // Could parse from notes if it follows a pattern like "Name: John"
        String notes = qso.getNotes();
        if (notes != null && notes.toLowerCase().contains("name:")) {
            int startIdx = notes.toLowerCase().indexOf("name:") + 5;
            int endIdx = notes.indexOf('\n', startIdx);
            if (endIdx < 0) endIdx = notes.length();
            return notes.substring(startIdx, endIdx).trim();
        }
        return null;
    }

    /**
     * Truncate notes to a reasonable snippet length.
     */
    private String truncateNotes(String notes) {
        if (notes == null || notes.isEmpty()) {
            return null;
        }
        if (notes.length() <= 100) {
            return notes;
        }
        return notes.substring(0, 97) + "...";
    }
}
