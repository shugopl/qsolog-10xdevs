package com.pl.shugo.gsolog.domain.repository;

import com.pl.shugo.gsolog.domain.entity.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * User repository interface (port in Clean Architecture).
 * R2DBC provides the implementation automatically.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {

    /**
     * Find user by email.
     */
    Mono<User> findByEmail(String email);

    /**
     * Find user by username.
     */
    Mono<User> findByUsername(String username);

    /**
     * Check if email already exists.
     */
    Mono<Boolean> existsByEmail(String email);

    /**
     * Check if username already exists.
     */
    Mono<Boolean> existsByUsername(String username);
}
