package com.pl.shugo.gsolog.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Resolves authenticated userId (UUID) from Spring Security Authentication.
 * Defensive against different principal/credentials shapes.
 */
@Component
public class AuthenticatedUserIdResolver {

    private final JwtUtil jwtUtil;

    public AuthenticatedUserIdResolver(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public UUID resolve(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID uuid) {
            return uuid;
        }
        if (principal instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
                // fallback below
            }
        }

        // Fallback: try credentials as raw JWT token
        Object credentials = authentication.getCredentials();
        if (credentials instanceof String token && jwtUtil.validateToken(token)) {
            return jwtUtil.extractUserId(token);
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}

