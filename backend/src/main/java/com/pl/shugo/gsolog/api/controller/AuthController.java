package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.api.dto.LoginRequest;
import com.pl.shugo.gsolog.api.dto.LoginResponse;
import com.pl.shugo.gsolog.api.dto.RegisterRequest;
import com.pl.shugo.gsolog.api.dto.UserResponse;
import com.pl.shugo.gsolog.application.service.AuthService;
import com.pl.shugo.gsolog.domain.enums.Role;
import com.pl.shugo.gsolog.infrastructure.security.AuthenticatedUserIdResolver;
import com.pl.shugo.gsolog.infrastructure.security.JwtProperties;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Authentication REST controller.
 * Handles user registration, login, and current user retrieval.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final AuthenticatedUserIdResolver userIdResolver;

    public AuthController(AuthService authService, JwtProperties jwtProperties, AuthenticatedUserIdResolver userIdResolver) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
        this.userIdResolver = userIdResolver;
    }

    /**
     * Register a new user.
     * Default role is OPERATOR.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(
                request.email(),
                request.username(),
                request.password(),
                Role.OPERATOR
        ).map(UserResponse::from);
    }

    /**
     * Login with username or email and password.
     * Returns JWT access token.
     */
    @PostMapping("/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.usernameOrEmail(), request.password())
                .map(token -> LoginResponse.of(
                        token,
                        jwtProperties.getExpiration() / 1000 // convert to seconds
                ));
    }

    /**
     * Get current authenticated user.
     */
    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser(Authentication authentication) {
        var userId = userIdResolver.resolve(authentication);
        return authService.getCurrentUser(userId)
                .map(UserResponse::from);
    }
}
