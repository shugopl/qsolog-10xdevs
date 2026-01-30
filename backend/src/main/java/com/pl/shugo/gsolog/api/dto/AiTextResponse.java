package com.pl.shugo.gsolog.api.dto;

/**
 * Response from AI text generation.
 */
public record AiTextResponse(
        String language,  // "PL" or "EN"
        String text
) {
}
