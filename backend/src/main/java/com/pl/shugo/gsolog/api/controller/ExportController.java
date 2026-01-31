package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.application.service.ExportService;
import com.pl.shugo.gsolog.infrastructure.security.AuthenticatedUserIdResolver;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Export REST controller.
 * Provides ADIF and CSV export endpoints for QSO data.
 */
@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ExportService exportService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public ExportController(ExportService exportService, AuthenticatedUserIdResolver userIdResolver) {
        this.exportService = exportService;
        this.userIdResolver = userIdResolver;
    }

    /**
     * Export QSOs in ADIF format.
     * Optionally filtered by date range.
     *
     * @param from           Start date (inclusive, optional)
     * @param to             End date (inclusive, optional)
     * @param authentication JWT authentication
     * @return ADIF file as streaming response
     */
    @GetMapping(value = "/adif", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Flux<DataBuffer>> exportAdif(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        var userId = userIdResolver.resolve(authentication);

        Flux<DataBuffer> adifData = exportService.generateAdif(userId, from, to)
                .map(line -> bufferFactory.wrap(line.getBytes(StandardCharsets.UTF_8)));

        String filename = generateFilename("qsolog", from, to, "adi");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(adifData);
    }

    /**
     * Export QSOs in CSV format.
     * Optionally filtered by date range.
     *
     * @param from           Start date (inclusive, optional)
     * @param to             End date (inclusive, optional)
     * @param authentication JWT authentication
     * @return CSV file as streaming response
     */
    @GetMapping(value = "/csv", produces = "text/csv")
    public ResponseEntity<Flux<DataBuffer>> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        var userId = userIdResolver.resolve(authentication);

        Flux<DataBuffer> csvData = exportService.generateCsv(userId, from, to)
                .map(line -> bufferFactory.wrap(line.getBytes(StandardCharsets.UTF_8)));

        String filename = generateFilename("qsolog", from, to, "csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csvData);
    }

    /**
     * Generate filename with optional date range.
     */
    private String generateFilename(String prefix, LocalDate from, LocalDate to, String extension) {
        StringBuilder filename = new StringBuilder(prefix);

        if (from != null && to != null) {
            filename.append("_")
                    .append(from.format(FILE_DATE_FORMAT))
                    .append("-")
                    .append(to.format(FILE_DATE_FORMAT));
        } else if (from != null) {
            filename.append("_from_").append(from.format(FILE_DATE_FORMAT));
        } else if (to != null) {
            filename.append("_to_").append(to.format(FILE_DATE_FORMAT));
        }

        filename.append(".").append(extension);
        return filename.toString();
    }
}
