package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.QsoLogApplication;
import com.pl.shugo.gsolog.api.dto.LoginRequest;
import com.pl.shugo.gsolog.api.dto.RegisterRequest;
import com.pl.shugo.gsolog.domain.entity.User;
import com.pl.shugo.gsolog.domain.enums.Role;
import com.pl.shugo.gsolog.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for admin endpoints.
 */
@SpringBootTest(
        classes = QsoLogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
class AdminControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("qsolog_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                String.format("r2dbc:postgresql://%s:%d/%s",
                        postgres.getHost(),
                        postgres.getFirstMappedPort(),
                        postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        // Clean up database
        userRepository.deleteAll().block();

        // Create and save admin user directly to database
        User admin = User.create(
                "admin@test.com",
                "admin",
                passwordEncoder.encode("password123"),
                Role.ADMIN
        );
        userRepository.save(admin).block();

        // Login as admin
        byte[] adminTokenBytes = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("admin", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        adminToken = extractToken(new String(adminTokenBytes));

        // Register and login as operator
        RegisterRequest operatorRequest = new RegisterRequest(
                "operator@test.com",
                "operator",
                "password123"
        );

        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(operatorRequest)
                .exchange()
                .expectStatus().isCreated();

        byte[] operatorTokenBytes = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("operator", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        operatorToken = extractToken(new String(operatorTokenBytes));
    }

    @Test
    void getAllUsers_shouldReturn401WithoutToken() {
        webTestClient.get()
                .uri("/api/v1/admin/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getAllUsers_shouldReturn403ForOperator() {
        webTestClient.get()
                .uri("/api/v1/admin/users")
                .header("Authorization", "Bearer " + operatorToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getAllUsers_shouldReturn200ForAdmin() {
        webTestClient.get()
                .uri("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isNotEmpty()
                .jsonPath("$[0].email").isNotEmpty()
                .jsonPath("$[0].username").isNotEmpty()
                .jsonPath("$[0].role").isNotEmpty()
                .jsonPath("$[0].passwordHash").doesNotExist(); // Ensure password not exposed
    }

    @Test
    void getAllUsers_shouldReturnAllUsersIncludingBothRoles() {
        webTestClient.get()
                .uri("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.role == 'ADMIN')]").exists()
                .jsonPath("$[?(@.role == 'OPERATOR')]").exists();
    }

    private String extractToken(String responseBody) {
        String[] parts = responseBody.split("\"accessToken\":\"");
        if (parts.length < 2) return "";
        return parts[1].split("\"")[0];
    }
}
