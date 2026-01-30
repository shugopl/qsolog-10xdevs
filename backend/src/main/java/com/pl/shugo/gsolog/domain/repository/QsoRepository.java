package com.pl.shugo.gsolog.domain.repository;

import com.pl.shugo.gsolog.domain.entity.Qso;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * QSO repository interface (port in Clean Architecture).
 * R2DBC provides the implementation automatically.
 */
@Repository
public interface QsoRepository extends R2dbcRepository<Qso, UUID> {

    /**
     * Find all QSOs for a specific user.
     */
    Flux<Qso> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find QSO by ID and user ID (for ownership enforcement).
     */
    Mono<Qso> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Delete QSO by ID and user ID (for ownership enforcement).
     */
    Mono<Void> deleteByIdAndUserId(UUID id, UUID userId);

    /**
     * Find potential duplicates for duplicate detection.
     * Matches: same userId, theirCallsign, qsoDate, band, mode
     */
    @Query("""
        SELECT * FROM qso
        WHERE user_id = :userId
        AND their_callsign = :callsign
        AND qso_date = :qsoDate
        AND band = :band
        AND mode = :mode
        """)
    Flux<Qso> findPotentialDuplicates(
            @Param("userId") UUID userId,
            @Param("callsign") String callsign,
            @Param("qsoDate") LocalDate qsoDate,
            @Param("band") String band,
            @Param("mode") String mode
    );

    /**
     * Count QSOs for a user.
     */
    Mono<Long> countByUserId(UUID userId);

    /**
     * Find QSOs by user and callsign filter (contains).
     */
    @Query("""
        SELECT * FROM qso
        WHERE user_id = :userId
        AND UPPER(their_callsign) LIKE UPPER(:callsignPattern)
        ORDER BY qso_date DESC, time_on DESC
        LIMIT :limit OFFSET :offset
        """)
    Flux<Qso> findByUserIdAndCallsignContaining(
            @Param("userId") UUID userId,
            @Param("callsignPattern") String callsignPattern,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * Find QSOs by user and band.
     */
    @Query("""
        SELECT * FROM qso
        WHERE user_id = :userId
        AND band = :band
        ORDER BY qso_date DESC, time_on DESC
        LIMIT :limit OFFSET :offset
        """)
    Flux<Qso> findByUserIdAndBand(
            @Param("userId") UUID userId,
            @Param("band") String band,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * Find QSOs by user and date range.
     */
    @Query("""
        SELECT * FROM qso
        WHERE user_id = :userId
        AND qso_date >= :from
        AND qso_date <= :to
        ORDER BY qso_date DESC, time_on DESC
        LIMIT :limit OFFSET :offset
        """)
    Flux<Qso> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * Find QSOs with all filters combined.
     */
    @Query("""
        SELECT * FROM qso
        WHERE user_id = :userId
        AND (:callsignPattern IS NULL OR UPPER(their_callsign) LIKE UPPER(:callsignPattern))
        AND (:band IS NULL OR band = :band)
        AND (:from IS NULL OR qso_date >= :from)
        AND (:to IS NULL OR qso_date <= :to)
        ORDER BY qso_date DESC, time_on DESC
        LIMIT :limit OFFSET :offset
        """)
    Flux<Qso> findByUserIdWithFilters(
            @Param("userId") UUID userId,
            @Param("callsignPattern") String callsignPattern,
            @Param("band") String band,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    // Statistics queries

    /**
     * Get QSO counts by band.
     * Returns band, count_all, count_confirmed.
     */
    @Query("""
        SELECT
            band,
            COUNT(*) as count_all,
            COUNT(CASE WHEN qsl_status = 'CONFIRMED' OR lotw_status = 'CONFIRMED' OR eqsl_status = 'CONFIRMED'
                       THEN 1 END) as count_confirmed
        FROM qso
        WHERE user_id = :userId
        AND (:from IS NULL OR qso_date >= :from)
        AND (:to IS NULL OR qso_date <= :to)
        GROUP BY band
        ORDER BY band
        """)
    Flux<java.util.Map<String, Object>> getStatsByBand(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Get QSO counts by mode.
     * Returns mode, count_all, count_confirmed.
     */
    @Query("""
        SELECT
            mode,
            COUNT(*) as count_all,
            COUNT(CASE WHEN qsl_status = 'CONFIRMED' OR lotw_status = 'CONFIRMED' OR eqsl_status = 'CONFIRMED'
                       THEN 1 END) as count_confirmed
        FROM qso
        WHERE user_id = :userId
        AND (:from IS NULL OR qso_date >= :from)
        AND (:to IS NULL OR qso_date <= :to)
        GROUP BY mode
        ORDER BY mode
        """)
    Flux<java.util.Map<String, Object>> getStatsByMode(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Get QSO counts by day.
     * Returns qso_date, count_all, count_confirmed.
     */
    @Query("""
        SELECT
            qso_date,
            COUNT(*) as count_all,
            COUNT(CASE WHEN qsl_status = 'CONFIRMED' OR lotw_status = 'CONFIRMED' OR eqsl_status = 'CONFIRMED'
                       THEN 1 END) as count_confirmed
        FROM qso
        WHERE user_id = :userId
        AND (:from IS NULL OR qso_date >= :from)
        AND (:to IS NULL OR qso_date <= :to)
        GROUP BY qso_date
        ORDER BY qso_date
        """)
    Flux<java.util.Map<String, Object>> getStatsByDay(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Get total QSO counts.
     * Returns count_all, count_confirmed.
     */
    @Query("""
        SELECT
            COUNT(*) as count_all,
            COUNT(CASE WHEN qsl_status = 'CONFIRMED' OR lotw_status = 'CONFIRMED' OR eqsl_status = 'CONFIRMED'
                       THEN 1 END) as count_confirmed
        FROM qso
        WHERE user_id = :userId
        AND (:from IS NULL OR qso_date >= :from)
        AND (:to IS NULL OR qso_date <= :to)
        """)
    Mono<java.util.Map<String, Object>> getTotals(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Find most recent QSO for a callsign (for suggestions).
     */
    @Query("""
        SELECT * FROM qso
        WHERE user_id = :userId
        AND UPPER(their_callsign) = UPPER(:callsign)
        ORDER BY qso_date DESC, time_on DESC
        LIMIT 1
        """)
    Mono<Qso> findMostRecentByCallsign(
            @Param("userId") UUID userId,
            @Param("callsign") String callsign
    );

    /**
     * Get most common band for a callsign (for suggestions).
     */
    @Query("""
        SELECT band, COUNT(*) as count
        FROM qso
        WHERE user_id = :userId
        AND UPPER(their_callsign) = UPPER(:callsign)
        GROUP BY band
        ORDER BY count DESC
        LIMIT 1
        """)
    Mono<java.util.Map<String, Object>> getMostCommonBandForCallsign(
            @Param("userId") UUID userId,
            @Param("callsign") String callsign
    );

    /**
     * Get most common mode for a callsign (for suggestions).
     */
    @Query("""
        SELECT mode, COUNT(*) as count
        FROM qso
        WHERE user_id = :userId
        AND UPPER(their_callsign) = UPPER(:callsign)
        GROUP BY mode
        ORDER BY count DESC
        LIMIT 1
        """)
    Mono<java.util.Map<String, Object>> getMostCommonModeForCallsign(
            @Param("userId") UUID userId,
            @Param("callsign") String callsign
    );
}
