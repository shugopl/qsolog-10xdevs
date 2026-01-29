package com.pl.shugo.gsolog.api.dto;

import com.pl.shugo.gsolog.domain.enums.AdifMode;
import com.pl.shugo.gsolog.domain.enums.AdifSubmode;
import com.pl.shugo.gsolog.domain.enums.EqslStatus;
import com.pl.shugo.gsolog.domain.enums.LotwStatus;
import com.pl.shugo.gsolog.domain.enums.QslStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for updating an existing QSO.
 */
public record UpdateQsoRequest(
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

        QslStatus qslStatus,
        LotwStatus lotwStatus,
        EqslStatus eqslStatus
) {
}
