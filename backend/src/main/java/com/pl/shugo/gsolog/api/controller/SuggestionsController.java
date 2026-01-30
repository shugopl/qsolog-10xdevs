package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.api.dto.CallsignSuggestionResponse;
import com.pl.shugo.gsolog.application.service.SuggestionsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Suggestions REST controller.
 * Provides callsign suggestions from user's QSO history.
 */
@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionsController {

    private final SuggestionsService suggestionsService;

    public SuggestionsController(SuggestionsService suggestionsService) {
        this.suggestionsService = suggestionsService;
    }

    /**
     * Get suggestions for a callsign based on user's QSO history.
     * Returns information from past QSOs: name, QTH, notes, common band/mode.
     *
     * @param callsign       Callsign to get suggestions for
     * @param authentication JWT authentication
     * @return Suggestion response if history found, empty otherwise
     */
    @GetMapping("/callsign/{callsign}")
    public Mono<CallsignSuggestionResponse> getCallsignSuggestions(
            @PathVariable String callsign,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        return suggestionsService.getSuggestions(userId, callsign);
    }
}
