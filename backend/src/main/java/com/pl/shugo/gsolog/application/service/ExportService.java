package com.pl.shugo.gsolog.application.service;

import com.pl.shugo.gsolog.domain.entity.Qso;
import com.pl.shugo.gsolog.domain.repository.QsoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Export service for generating ADIF and CSV files.
 * Provides reactive streaming of QSO data in various formats.
 */
@Service
public class ExportService {

    private static final DateTimeFormatter ADIF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ADIF_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final String ADIF_HEADER = """
            ADIF Export
            <ADIF_VER:5>3.1.4
            <PROGRAMID:6>QSOLOG
            <EOH>

            """;

    private final QsoRepository qsoRepository;

    public ExportService(QsoRepository qsoRepository) {
        this.qsoRepository = qsoRepository;
    }

    /**
     * Generate ADIF format export for user's QSOs.
     * Properly maps mode/submode/customMode according to ADIF specification.
     *
     * @param userId User ID
     * @param from   Start date (inclusive, null for no limit)
     * @param to     End date (inclusive, null for no limit)
     * @return Flux of ADIF formatted strings
     */
    public Flux<String> generateAdif(UUID userId, LocalDate from, LocalDate to) {
        return Flux.concat(
                Flux.just(ADIF_HEADER),
                fetchQsos(userId, from, to).map(this::formatQsoAsAdif)
        );
    }

    /**
     * Generate CSV format export for user's QSOs.
     *
     * @param userId User ID
     * @param from   Start date (inclusive, null for no limit)
     * @param to     End date (inclusive, null for no limit)
     * @return Flux of CSV formatted strings
     */
    public Flux<String> generateCsv(UUID userId, LocalDate from, LocalDate to) {
        String header = "Callsign,Date,Time,Band,Frequency (kHz),Mode,Submode,Custom Mode," +
                "RST Sent,RST Recv,QTH,Grid Square,Notes," +
                "QSL Status,LoTW Status,eQSL Status\n";

        return Flux.concat(
                Flux.just(header),
                fetchQsos(userId, from, to).map(this::formatQsoAsCsv)
        );
    }

    /**
     * Fetch QSOs for export with proper ordering.
     */
    private Flux<Qso> fetchQsos(UUID userId, LocalDate from, LocalDate to) {
        PageRequest pageRequest = PageRequest.of(0, Integer.MAX_VALUE,
                Sort.by(Sort.Direction.ASC, "qsoDate", "timeOn"));

        if (from != null && to != null) {
            return qsoRepository.findByUserIdAndDateRange(userId, from, to, Integer.MAX_VALUE, 0);
        } else if (from != null) {
            return qsoRepository.findByUserIdAndDateRange(userId, from, LocalDate.now().plusYears(100), Integer.MAX_VALUE, 0);
        } else if (to != null) {
            return qsoRepository.findByUserIdAndDateRange(userId, LocalDate.now().minusYears(100), to, Integer.MAX_VALUE, 0);
        } else {
            return qsoRepository.findByUserId(userId, pageRequest);
        }
    }

    /**
     * Format a single QSO as ADIF record.
     * Handles mode/submode/customMode mapping according to ADIF specification.
     */
    private String formatQsoAsAdif(Qso qso) {
        StringBuilder sb = new StringBuilder();

        // Required fields
        appendAdifField(sb, "CALL", qso.getTheirCallsign());
        appendAdifField(sb, "QSO_DATE", qso.getQsoDate().format(ADIF_DATE_FORMAT));
        appendAdifField(sb, "TIME_ON", qso.getTimeOn().format(ADIF_TIME_FORMAT));
        appendAdifField(sb, "BAND", qso.getBand());

        // Mode handling according to ADIF specification
        if (qso.getCustomMode() != null && !qso.getCustomMode().isBlank()) {
            // Custom mode: emit MODE=DATA and vendor field
            appendAdifField(sb, "MODE", "DATA");
            appendAdifField(sb, "APP_QSOLOG_CUSTOMMODE", qso.getCustomMode());
        } else {
            // Standard mode
            appendAdifField(sb, "MODE", qso.getMode().name());
            if (qso.getSubmode() != null) {
                appendAdifField(sb, "SUBMODE", qso.getSubmode().name());
            }
        }

        // Optional fields
        if (qso.getFrequencyKhz() != null) {
            BigDecimal freqMhz = qso.getFrequencyKhz().divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
            appendAdifField(sb, "FREQ", freqMhz.stripTrailingZeros().toPlainString());
        }
        if (qso.getRstSent() != null && !qso.getRstSent().isBlank()) {
            appendAdifField(sb, "RST_SENT", qso.getRstSent());
        }
        if (qso.getRstRecv() != null && !qso.getRstRecv().isBlank()) {
            appendAdifField(sb, "RST_RCVD", qso.getRstRecv());
        }
        if (qso.getQth() != null && !qso.getQth().isBlank()) {
            appendAdifField(sb, "QTH", qso.getQth());
        }
        if (qso.getGridSquare() != null && !qso.getGridSquare().isBlank()) {
            appendAdifField(sb, "GRIDSQUARE", qso.getGridSquare());
        }
        if (qso.getNotes() != null && !qso.getNotes().isBlank()) {
            appendAdifField(sb, "COMMENT", qso.getNotes());
        }

        // QSL status fields
        if (qso.getQslStatus() != null && qso.getQslStatus().name() != null) {
            appendAdifField(sb, "QSL_RCVD", qso.getQslStatus().name());
        }
        if (qso.getLotwStatus() != null && qso.getLotwStatus().name() != null) {
            appendAdifField(sb, "LOTW_QSLRDATE", qso.getLotwStatus().name());
        }
        if (qso.getEqslStatus() != null && qso.getEqslStatus().name() != null) {
            appendAdifField(sb, "EQSL_QSLRDATE", qso.getEqslStatus().name());
        }

        sb.append("<EOR>\n");
        return sb.toString();
    }

    /**
     * Append an ADIF field in the format: <FIELD:length>value
     */
    private void appendAdifField(StringBuilder sb, String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("<").append(fieldName).append(":").append(value.length()).append(">").append(value).append(" ");
        }
    }

    /**
     * Format a single QSO as CSV record.
     */
    private String formatQsoAsCsv(Qso qso) {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                escapeCsv(qso.getTheirCallsign()),
                qso.getQsoDate(),
                qso.getTimeOn(),
                escapeCsv(qso.getBand()),
                qso.getFrequencyKhz() != null ? qso.getFrequencyKhz() : "",
                qso.getMode() != null ? qso.getMode().name() : "",
                qso.getSubmode() != null ? qso.getSubmode().name() : "",
                escapeCsv(qso.getCustomMode()),
                escapeCsv(qso.getRstSent()),
                escapeCsv(qso.getRstRecv()),
                escapeCsv(qso.getQth()),
                escapeCsv(qso.getGridSquare()),
                escapeCsv(qso.getNotes()),
                qso.getQslStatus() != null ? qso.getQslStatus().name() : "",
                qso.getLotwStatus() != null ? qso.getLotwStatus().name() : "",
                qso.getEqslStatus() != null ? qso.getEqslStatus().name() : ""
        );
    }

    /**
     * Escape CSV value (handle quotes and commas).
     */
    private String escapeCsv(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
