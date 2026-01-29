package com.pl.shugo.gsolog.application.service;

import com.pl.shugo.gsolog.domain.entity.User;
import com.pl.shugo.gsolog.domain.enums.Role;
import com.pl.shugo.gsolog.domain.repository.UserRepository;
import com.pl.shugo.gsolog.infrastructure.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

/**
 * Authentication service (use cases).
 * Handles user registration, login, and authentication.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user.
     */
    public Mono<User> register(String email, String username, String password, Role role) {
        // Validate email and username uniqueness
        return userRepository.existsByEmail(email)
                .flatMap(emailExists -> {
                    if (emailExists) {
                        return Mono.error(new ResponseStatusException(CONFLICT, "Email already exists"));
                    }
                    return userRepository.existsByUsername(username);
                })
                .flatMap(usernameExists -> {
                    if (usernameExists) {
                        return Mono.error(new ResponseStatusException(CONFLICT, "Username already exists"));
                    }
                    // Create user with hashed password
                    String hashedPassword = passwordEncoder.encode(password);
                    User user = User.create(email, username, hashedPassword, role);
                    return userRepository.save(user);
                });
    }

    /**
     * Login user and generate JWT token.
     */
    public Mono<String> login(String usernameOrEmail, String password) {
        // Find user by username or email
        Mono<User> userMono = userRepository.findByUsername(usernameOrEmail)
                .switchIfEmpty(userRepository.findByEmail(usernameOrEmail));

        return userMono
                .switchIfEmpty(Mono.error(new ResponseStatusException(UNAUTHORIZED, "Invalid credentials")))
                .flatMap(user -> {
                    // Verify password
                    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                        return Mono.error(new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));
                    }
                    // Generate JWT token
                    String token = jwtUtil.generateToken(user);
                    return Mono.just(token);
                });
    }

    /**
     * Get current user by ID.
     */
    public Mono<User> getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(NOT_FOUND, "User not found")));
    }
}
