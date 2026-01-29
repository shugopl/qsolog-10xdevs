package com.pl.shugo.gsolog.infrastructure.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * RFC7807-like error response structure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        Map<String, Object> errors
) {
    public ErrorResponse(String type, String title, int status, String detail, String instance) {
        this(type, title, status, detail, instance, Instant.now(), null);
    }

    public ErrorResponse(String type, String title, int status, String detail, String instance, Map<String, Object> errors) {
        this(type, title, status, detail, instance, Instant.now(), errors);
    }
}
