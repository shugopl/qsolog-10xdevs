package com.pl.shugo.gsolog.api.dto;

import com.pl.shugo.gsolog.domain.entity.Qso;
import com.pl.shugo.gsolog.domain.enums.AdifMode;
import com.pl.shugo.gsolog.domain.enums.AdifSubmode;
import com.pl.shugo.gsolog.domain.enums.EqslStatus;
import com.pl.shugo.gsolog.domain.enums.LotwStatus;
import com.pl.shugo.gsolog.domain.enums.QslStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO for QSO.
 */
public record QsoResponse(
        UUID id,
        UUID userId,
        String theirCallsign,
        LocalDate qsoDate,
        LocalTime timeOn,
        String band,
        BigDecimal frequencyKhz,
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
        EqslStatus eqslStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public static QsoResponse from(Qso qso) {
        return new QsoResponse(
                qso.getId(),
                qso.getUserId(),
                qso.getTheirCallsign(),
                qso.getQsoDate(),
                qso.getTimeOn(),
                qso.getBand(),
                qso.getFrequencyKhz(),
                qso.getMode(),
                qso.getSubmode(),
                qso.getCustomMode(),
                qso.getRstSent(),
                qso.getRstRecv(),
                qso.getQth(),
                qso.getGridSquare(),
                qso.getNotes(),
                qso.getQslStatus(),
                qso.getLotwStatus(),
                qso.getEqslStatus(),
                qso.getCreatedAt(),
                qso.getUpdatedAt()
        );
    }
}
