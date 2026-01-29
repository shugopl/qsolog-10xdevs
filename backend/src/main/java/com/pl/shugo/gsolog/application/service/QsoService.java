package com.pl.shugo.gsolog.application.service;

import com.pl.shugo.gsolog.domain.entity.Qso;
import com.pl.shugo.gsolog.domain.enums.AdifMode;
import com.pl.shugo.gsolog.domain.enums.AdifSubmode;
import com.pl.shugo.gsolog.domain.repository.QsoRepository;
import com.pl.shugo.gsolog.domain.validation.QsoValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * QSO service (use cases).
 * Handles QSO CRUD operations with ownership enforcement and duplicate detection.
 */
@Service
public class QsoService {

    private final QsoRepository qsoRepository;
    private final QsoValidator qsoValidator;

    public QsoService(QsoRepository qsoRepository, QsoValidator qsoValidator) {
        this.qsoRepository = qsoRepository;
        this.qsoValidator = qsoValidator;
    }

    /**
     * Create a new QSO.
     * Validates mode/submode/customMode and band.
     * Checks for duplicates unless confirmDuplicate is true.
     */
    public Mono<Qso> createQso(UUID userId, String theirCallsign, LocalDate qsoDate, LocalTime timeOn,
                               String band, BigDecimal frequencyKhz, AdifMode mode, AdifSubmode submode,
                               String customMode, String rstSent, String rstRecv, String qth,
                               String gridSquare, String notes, boolean confirmDuplicate) {

        // Validate band
        if (!qsoValidator.isValidBand(band)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    qsoValidator.getBandValidationError(band)
            ));
        }

        // Validate mode configuration
        List<String> validationErrors = qsoValidator.validateModeConfiguration(mode, submode, customMode);
        if (!validationErrors.isEmpty()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mode validation failed: " + String.join(", ", validationErrors)
            ));
        }

        // Check for duplicates if not confirmed
        if (!confirmDuplicate) {
            return qsoRepository.findPotentialDuplicates(userId, theirCallsign, qsoDate, band, mode.name())
                    .collectList()
                    .flatMap(duplicates -> {
                        if (!duplicates.isEmpty()) {
                            List<UUID> existingIds = duplicates.stream()
                                    .map(Qso::getId)
                                    .collect(Collectors.toList());

                            // Return 409 Conflict with duplicate information
                            String message = String.format(
                                    "Potential duplicate detected (found %d similar QSO(s)). Pass confirmDuplicate=true to save anyway. Existing IDs: %s",
                                    duplicates.size(),
                                    existingIds
                            );
                            return Mono.error(new DuplicateQsoException(message, existingIds));
                        }
                        // No duplicates, proceed with save
                        return saveQso(userId, theirCallsign, qsoDate, timeOn, band, frequencyKhz, mode, submode, customMode, rstSent, rstRecv, qth, gridSquare, notes);
                    });
        }

        // Duplicate confirmed, save anyway
        return saveQso(userId, theirCallsign, qsoDate, timeOn, band, frequencyKhz, mode, submode, customMode, rstSent, rstRecv, qth, gridSquare, notes);
    }

    private Mono<Qso> saveQso(UUID userId, String theirCallsign, LocalDate qsoDate, LocalTime timeOn,
                              String band, BigDecimal frequencyKhz, AdifMode mode, AdifSubmode submode,
                              String customMode, String rstSent, String rstRecv, String qth,
                              String gridSquare, String notes) {
        Qso qso = Qso.create(userId, theirCallsign, qsoDate, timeOn, band, frequencyKhz, mode, submode,
                customMode, rstSent, rstRecv, qth, gridSquare, notes);
        return qsoRepository.save(qso);
    }

    /**
     * Find QSO by ID for the authenticated user.
     */
    public Mono<Qso> findById(UUID id, UUID userId) {
        return qsoRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "QSO not found")));
    }

    /**
     * Find all QSOs for the authenticated user with filters.
     */
    public Flux<Qso> findAll(UUID userId, String callsignFilter, String band, LocalDate from, LocalDate to, int page, int size) {
        int limit = size;
        long offset = (long) page * size;

        String callsignPattern = callsignFilter != null && !callsignFilter.isBlank() ? "%" + callsignFilter + "%" : null;

        return qsoRepository.findByUserIdWithFilters(userId, callsignPattern, band, from, to, limit, offset);
    }

    /**
     * Update an existing QSO.
     * Validates ownership and mode configuration.
     */
    public Mono<Qso> updateQso(UUID id, UUID userId, String theirCallsign, LocalDate qsoDate, LocalTime timeOn,
                               String band, BigDecimal frequencyKhz, AdifMode mode, AdifSubmode submode,
                               String customMode, String rstSent, String rstRecv, String qth,
                               String gridSquare, String notes) {

        // Validate band
        if (!qsoValidator.isValidBand(band)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    qsoValidator.getBandValidationError(band)
            ));
        }

        // Validate mode configuration
        List<String> validationErrors = qsoValidator.validateModeConfiguration(mode, submode, customMode);
        if (!validationErrors.isEmpty()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mode validation failed: " + String.join(", ", validationErrors)
            ));
        }

        return qsoRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "QSO not found")))
                .flatMap(existingQso -> {
                    // Update fields
                    existingQso.setTheirCallsign(theirCallsign);
                    existingQso.setQsoDate(qsoDate);
                    existingQso.setTimeOn(timeOn);
                    existingQso.setBand(band);
                    existingQso.setFrequencyKhz(frequencyKhz);
                    existingQso.setMode(mode);
                    existingQso.setSubmode(submode);
                    existingQso.setCustomMode(customMode);
                    existingQso.setRstSent(rstSent);
                    existingQso.setRstRecv(rstRecv);
                    existingQso.setQth(qth);
                    existingQso.setGridSquare(gridSquare);
                    existingQso.setNotes(notes);
                    existingQso.setUpdatedAt(java.time.Instant.now());

                    return qsoRepository.save(existingQso);
                });
    }

    /**
     * Delete QSO by ID for the authenticated user.
     */
    public Mono<Void> deleteQso(UUID id, UUID userId) {
        return qsoRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "QSO not found")))
                .flatMap(qso -> qsoRepository.deleteByIdAndUserId(id, userId));
    }

    /**
     * Count QSOs for a user.
     */
    public Mono<Long> countByUserId(UUID userId) {
        return qsoRepository.countByUserId(userId);
    }

    /**
     * Custom exception for duplicate QSO detection.
     */
    public static class DuplicateQsoException extends ResponseStatusException {
        private final List<UUID> existingIds;

        public DuplicateQsoException(String message, List<UUID> existingIds) {
            super(HttpStatus.CONFLICT, message);
            this.existingIds = existingIds;
        }

        public List<UUID> getExistingIds() {
            return existingIds;
        }
    }
}
