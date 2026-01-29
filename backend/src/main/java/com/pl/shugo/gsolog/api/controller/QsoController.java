package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.api.dto.CreateQsoRequest;
import com.pl.shugo.gsolog.api.dto.DuplicateWarningResponse;
import com.pl.shugo.gsolog.api.dto.QsoResponse;
import com.pl.shugo.gsolog.api.dto.UpdateQsoRequest;
import com.pl.shugo.gsolog.application.service.QsoService;
import com.pl.shugo.gsolog.domain.entity.Qso;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * QSO REST controller.
 * All endpoints require JWT authentication and enforce user ownership.
 */
@RestController
@RequestMapping("/api/v1/qso")
public class QsoController {

    private final QsoService qsoService;

    public QsoController(QsoService qsoService) {
        this.qsoService = qsoService;
    }

    /**
     * Create a new QSO.
     * Returns 409 Conflict if duplicate detected (unless confirmDuplicate=true).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<Object>> createQso(
            @Valid @RequestBody CreateQsoRequest request,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        return qsoService.createQso(
                userId,
                request.theirCallsign(),
                request.qsoDate(),
                request.timeOn(),
                request.band(),
                request.frequencyKhz(),
                request.mode(),
                request.submode(),
                request.customMode(),
                request.rstSent(),
                request.rstRecv(),
                request.qth(),
                request.gridSquare(),
                request.notes(),
                request.isConfirmDuplicate()
        )
        .map(qso -> ResponseEntity.status(HttpStatus.CREATED).body((Object) QsoResponse.from(qso)))
        .onErrorResume(QsoService.DuplicateQsoException.class, ex -> {
            DuplicateWarningResponse warning = DuplicateWarningResponse.of(ex.getExistingIds());
            return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body((Object) warning));
        });
    }

    /**
     * Get all QSOs for the authenticated user with optional filters.
     */
    @GetMapping
    public Flux<QsoResponse> getAllQsos(
            @RequestParam(required = false) String callsign,
            @RequestParam(required = false) String band,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        return qsoService.findAll(userId, callsign, band, from, to, page, size)
                .map(QsoResponse::from);
    }

    /**
     * Get single QSO by ID.
     * Returns 404 if not found or not owned by user.
     */
    @GetMapping("/{id}")
    public Mono<QsoResponse> getQsoById(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        return qsoService.findById(id, userId)
                .map(QsoResponse::from);
    }

    /**
     * Update existing QSO.
     * Returns 404 if not found or not owned by user.
     */
    @PutMapping("/{id}")
    public Mono<QsoResponse> updateQso(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQsoRequest request,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        return qsoService.updateQso(
                id,
                userId,
                request.theirCallsign(),
                request.qsoDate(),
                request.timeOn(),
                request.band(),
                request.frequencyKhz(),
                request.mode(),
                request.submode(),
                request.customMode(),
                request.rstSent(),
                request.rstRecv(),
                request.qth(),
                request.gridSquare(),
                request.notes()
        )
        .flatMap(qso -> {
            // Update QSL statuses if provided
            if (request.qslStatus() != null) {
                qso.setQslStatus(request.qslStatus());
            }
            if (request.lotwStatus() != null) {
                qso.setLotwStatus(request.lotwStatus());
            }
            if (request.eqslStatus() != null) {
                qso.setEqslStatus(request.eqslStatus());
            }
            return qsoService.updateQso(
                    qso.getId(),
                    userId,
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
                    qso.getNotes()
            );
        })
        .map(QsoResponse::from);
    }

    /**
     * Delete QSO by ID.
     * Returns 404 if not found or not owned by user.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteQso(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        return qsoService.deleteQso(id, userId);
    }
}
