package com.pl.shugo.gsolog.api.dto;

import com.pl.shugo.gsolog.domain.enums.AdifMode;
import com.pl.shugo.gsolog.domain.enums.AdifSubmode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating a new QSO.
 */
public record CreateQsoRequest(
        @NotBlank(message = "Callsign is required")
        String theirCallsign,

        @NotNull(message = "QSO date is required")
        LocalDate qsoDate,

        @NotNull(message = "Time on is required")
        LocalTime timeOn,

        @NotBlank(message = "Band is required")
        String band,

        BigDecimal frequencyKhz,

        @NotNull(message = "Mode is required")
        AdifMode mode,

        AdifSubmode submode,

        String customMode,

        String rstSent,
        String rstRecv,
        String qth,
        String gridSquare,
        String notes,

        // Duplicate detection flag
        Boolean confirmDuplicate
) {
    public boolean isConfirmDuplicate() {
        return confirmDuplicate != null && confirmDuplicate;
    }
}
