package com.pl.shugo.gsolog.infrastructure.config;

import com.pl.shugo.gsolog.application.service.AuthService;
import com.pl.shugo.gsolog.domain.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Admin user bootstrap.
 * Creates an admin user on startup if ADMIN_BOOTSTRAP=true and credentials are provided.
 * Idempotent - only creates if admin doesn't already exist.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthService authService;

    @Value("${admin.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${admin.bootstrap.email:}")
    private String adminEmail;

    @Value("${admin.bootstrap.username:}")
    private String adminUsername;

    @Value("${admin.bootstrap.password:}")
    private String adminPassword;

    public AdminBootstrap(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.debug("Admin bootstrap disabled");
            return;
        }

        if (adminEmail.isBlank() || adminUsername.isBlank() || adminPassword.isBlank()) {
            log.warn("Admin bootstrap enabled but credentials not provided. Set admin.bootstrap.email, admin.bootstrap.username, and admin.bootstrap.password");
            return;
        }

        log.info("Admin bootstrap enabled - attempting to create admin user: {}", adminUsername);

        authService.register(adminEmail, adminUsername, adminPassword, Role.ADMIN)
                .doOnSuccess(user -> log.info("Admin user created successfully: {} ({})", user.getUsername(), user.getEmail()))
                .doOnError(error -> log.info("Admin user already exists or creation failed: {}", error.getMessage()))
                .onErrorResume(error -> {
                    // Silently continue if user already exists
                    return null;
                })
                .subscribe();
    }
}
