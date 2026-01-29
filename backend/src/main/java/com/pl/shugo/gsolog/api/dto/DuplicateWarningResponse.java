package com.pl.shugo.gsolog.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response for duplicate QSO warning.
 * Returned with 409 Conflict when duplicate detected without confirmDuplicate flag.
 */
public record DuplicateWarningResponse(
        String type,
        String message,
        List<UUID> existingIds
) {
    public static DuplicateWarningResponse of(List<UUID> existingIds) {
        return new DuplicateWarningResponse(
                "duplicate_detected",
                "Potential duplicate QSO detected. Pass confirmDuplicate=true to force save.",
                existingIds
        );
    }
}
