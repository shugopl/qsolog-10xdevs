package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.QsoLogApplication;
import com.pl.shugo.gsolog.api.dto.LoginRequest;
import com.pl.shugo.gsolog.api.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for authentication endpoints.
 */
@SpringBootTest(
        classes = QsoLogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers(disabledWithoutDocker = true)
class AuthControllerTest {

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

    @Test
    void register_shouldCreateNewUser() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "testuser",
                "password123"
        );

        webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.email").isEqualTo("test@example.com")
                .jsonPath("$.username").isEqualTo("testuser")
                .jsonPath("$.role").isEqualTo("OPERATOR");
    }

    @Test
    void register_shouldFailWithDuplicateEmail() {
        RegisterRequest request1 = new RegisterRequest(
                "duplicate@example.com",
                "user1",
                "password123"
        );

        RegisterRequest request2 = new RegisterRequest(
                "duplicate@example.com",
                "user2",
                "password123"
        );

        // First registration succeeds
        webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request1)
                .exchange()
                .expectStatus().isCreated();

        // Second registration with same email fails
        webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request2)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Email already exists");
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "login@example.com",
                "loginuser",
                "password123"
        );

        webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated();

        // Login with username
        LoginRequest loginRequest = new LoginRequest("loginuser", "password123");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.expiresInSeconds").isEqualTo(86400);
    }

    @Test
    void login_shouldWorkWithEmail() {
        // Register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "emaillogin@example.com",
                "emailuser",
                "password123"
        );

        webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated();

        // Login with email
        LoginRequest loginRequest = new LoginRequest("emaillogin@example.com", "password123");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty();
    }

    @Test
    void login_shouldFailWithInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest("nonexistent", "wrongpassword");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void me_shouldReturnCurrentUserWithValidToken() {
        // Register and login
        RegisterRequest registerRequest = new RegisterRequest(
                "me@example.com",
                "meuser",
                "password123"
        );

        webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated();

        LoginRequest loginRequest = new LoginRequest("meuser", "password123");

        byte[] tokenBytes = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        String accessToken = extractToken(new String(tokenBytes));

        // Get current user
        webTestClient.get()
                .uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("me@example.com")
                .jsonPath("$.username").isEqualTo("meuser")
                .jsonPath("$.role").isEqualTo("OPERATOR");
    }

    @Test
    void me_shouldReturn401WithoutToken() {
        webTestClient.get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void me_shouldReturn401WithInvalidToken() {
        webTestClient.get()
                .uri("/api/v1/auth/me")
                .header("Authorization", "Bearer invalid.token.here")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String extractToken(String responseBody) {
        // Simple JSON extraction - in real tests, use a JSON library
        String[] parts = responseBody.split("\"accessToken\":\"");
        if (parts.length < 2) return "";
        String token = parts[1].split("\"")[0];
        return token;
    }
}
