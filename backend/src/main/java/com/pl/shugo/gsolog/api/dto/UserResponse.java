package com.pl.shugo.gsolog.api.dto;

import com.pl.shugo.gsolog.domain.entity.User;

import java.util.UUID;

/**
 * User response DTO.
 */
public record UserResponse(
        UUID id,
        String email,
        String username,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name()
        );
    }
}
