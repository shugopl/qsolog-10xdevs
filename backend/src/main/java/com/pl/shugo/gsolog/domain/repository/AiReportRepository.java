package com.pl.shugo.gsolog.domain.repository;

import com.pl.shugo.gsolog.domain.entity.AiReport;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * AI report repository interface.
 */
@Repository
public interface AiReportRepository extends R2dbcRepository<AiReport, UUID> {

    /**
     * Find all reports for a user.
     */
    Flux<AiReport> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find report by ID and user ID (for ownership enforcement).
     */
    Mono<AiReport> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find reports within a date range.
     */
    @Query("""
        SELECT * FROM ai_report_history
        WHERE user_id = :userId
        AND (:from IS NULL OR from_date >= :from)
        AND (:to IS NULL OR to_date <= :to)
        ORDER BY created_at DESC
        """)
    Flux<AiReport> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
