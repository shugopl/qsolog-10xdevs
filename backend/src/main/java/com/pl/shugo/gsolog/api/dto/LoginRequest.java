package com.pl.shugo.gsolog.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * User login request DTO.
 */
public record LoginRequest(
        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password
) {
}
