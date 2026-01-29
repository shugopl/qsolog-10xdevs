package com.pl.shugo.gsolog.api.dto;

/**
 * Login response with JWT token.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
    public static LoginResponse of(String accessToken, long expiresInSeconds) {
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
