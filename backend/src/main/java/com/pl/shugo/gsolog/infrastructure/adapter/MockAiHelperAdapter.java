package com.pl.shugo.gsolog.infrastructure.adapter;

import com.pl.shugo.gsolog.api.dto.QsoDescriptionRequest;
import com.pl.shugo.gsolog.api.dto.StatsResponse;
import com.pl.shugo.gsolog.domain.port.AiHelperPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;

/**
 * Mock implementation of AiHelperPort.
 * Returns deterministic text without calling external APIs.
 * Allows app to run without OpenAI API key.
 */
public class MockAiHelperAdapter implements AiHelperPort {

    private static final Logger logger = LoggerFactory.getLogger(MockAiHelperAdapter.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public Mono<String> generateQsoDescription(QsoDescriptionRequest request) {
        logger.debug("Mock AI generating QSO description for {}", request.theirCallsign());

        String language = request.language() != null ? request.language() : "EN";

        if ("PL".equalsIgnoreCase(language)) {
            return Mono.just(generatePolishQsoDescription(request));
        } else {
            return Mono.just(generateEnglishQsoDescription(request));
        }
    }

    @Override
    public Mono<String> generatePeriodReport(StatsResponse stats, String language) {
        logger.debug("Mock AI generating period report in {}", language);

        if ("PL".equalsIgnoreCase(language)) {
            return Mono.just(generatePolishReport(stats));
        } else {
            return Mono.just(generateEnglishReport(stats));
        }
    }

    private String generateEnglishQsoDescription(QsoDescriptionRequest req) {
        StringBuilder sb = new StringBuilder();

        sb.append("Contact with ").append(req.theirCallsign());

        if (req.qsoDate() != null) {
            sb.append(" on ").append(req.qsoDate().format(DATE_FORMATTER));
        }

        if (req.timeOn() != null) {
            sb.append(" at ").append(req.timeOn().format(TIME_FORMATTER)).append(" UTC");
        }

        if (req.band() != null && req.mode() != null) {
            sb.append(". Worked on ").append(req.band()).append(" using ").append(req.mode()).append(" mode");
        } else if (req.band() != null) {
            sb.append(" on ").append(req.band());
        } else if (req.mode() != null) {
            sb.append(" using ").append(req.mode()).append(" mode");
        }

        if (req.rstSent() != null && req.rstRecv() != null) {
            sb.append(". Signal reports: sent ").append(req.rstSent())
              .append(", received ").append(req.rstRecv());
        }

        if (req.qth() != null && !req.qth().isEmpty()) {
            sb.append(". Operator located in ").append(req.qth());
        }

        sb.append(". Nice contact!");

        return sb.toString();
    }

    private String generatePolishQsoDescription(QsoDescriptionRequest req) {
        StringBuilder sb = new StringBuilder();

        sb.append("Kontakt z ").append(req.theirCallsign());

        if (req.qsoDate() != null) {
            sb.append(" w dniu ").append(req.qsoDate().format(DATE_FORMATTER));
        }

        if (req.timeOn() != null) {
            sb.append(" o godzinie ").append(req.timeOn().format(TIME_FORMATTER)).append(" UTC");
        }

        if (req.band() != null && req.mode() != null) {
            sb.append(". Łączność na paśmie ").append(req.band())
              .append(" w emisji ").append(req.mode());
        } else if (req.band() != null) {
            sb.append(" na paśmie ").append(req.band());
        } else if (req.mode() != null) {
            sb.append(" w emisji ").append(req.mode());
        }

        if (req.rstSent() != null && req.rstRecv() != null) {
            sb.append(". Raporty: nadany ").append(req.rstSent())
              .append(", odebrany ").append(req.rstRecv());
        }

        if (req.qth() != null && !req.qth().isEmpty()) {
            sb.append(". Operator z ").append(req.qth());
        }

        sb.append(". Przyjemna łączność!");

        return sb.toString();
    }

    private String generateEnglishReport(StatsResponse stats) {
        StringBuilder sb = new StringBuilder();

        long totalQsos = stats.totals().all();
        long confirmedQsos = stats.totals().confirmed();

        sb.append("Activity Summary\n\n");
        sb.append("Total contacts: ").append(totalQsos);

        if (confirmedQsos > 0) {
            sb.append(" (").append(confirmedQsos).append(" confirmed)");
        }
        sb.append(".\n\n");

        if (!stats.countsByBand().isEmpty()) {
            sb.append("Band activity:\n");
            stats.countsByBand().forEach(band -> {
                sb.append("- ").append(band.band()).append(": ")
                  .append(band.countAll()).append(" QSOs");
                if (band.countConfirmed() > 0) {
                    sb.append(" (").append(band.countConfirmed()).append(" confirmed)");
                }
                sb.append("\n");
            });
            sb.append("\n");
        }

        if (!stats.countsByMode().isEmpty()) {
            sb.append("Mode distribution:\n");
            stats.countsByMode().forEach(mode -> {
                sb.append("- ").append(mode.mode()).append(": ")
                  .append(mode.countAll()).append(" QSOs\n");
            });
        }

        sb.append("\nKeep up the good work on the air!");

        return sb.toString();
    }

    private String generatePolishReport(StatsResponse stats) {
        StringBuilder sb = new StringBuilder();

        long totalQsos = stats.totals().all();
        long confirmedQsos = stats.totals().confirmed();

        sb.append("Podsumowanie aktywności\n\n");
        sb.append("Łącznie łączności: ").append(totalQsos);

        if (confirmedQsos > 0) {
            sb.append(" (").append(confirmedQsos).append(" potwierdzonych)");
        }
        sb.append(".\n\n");

        if (!stats.countsByBand().isEmpty()) {
            sb.append("Aktywność na pasmach:\n");
            stats.countsByBand().forEach(band -> {
                sb.append("- ").append(band.band()).append(": ")
                  .append(band.countAll()).append(" łączności");
                if (band.countConfirmed() > 0) {
                    sb.append(" (").append(band.countConfirmed()).append(" potwierdzonych)");
                }
                sb.append("\n");
            });
            sb.append("\n");
        }

        if (!stats.countsByMode().isEmpty()) {
            sb.append("Rozkład emisji:\n");
            stats.countsByMode().forEach(mode -> {
                sb.append("- ").append(mode.mode()).append(": ")
                  .append(mode.countAll()).append(" łączności\n");
            });
        }

        sb.append("\nTrzymaj się świetnie w eterze!");

        return sb.toString();
    }
}
