package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.QsoLogApplication;
import com.pl.shugo.gsolog.api.dto.CreateQsoRequest;
import com.pl.shugo.gsolog.api.dto.LoginRequest;
import com.pl.shugo.gsolog.api.dto.RegisterRequest;
import com.pl.shugo.gsolog.domain.enums.AdifMode;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Integration tests for suggestions endpoint.
 */
@SpringBootTest(
        classes = QsoLogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers(disabledWithoutDocker = true)
class SuggestionsControllerTest {

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

    private String userToken;

    @BeforeEach
    void setUp() {
        // Register and login user
        RegisterRequest user = new RegisterRequest("suggestuser@test.com", "suggestuser", "password123");
        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isCreated();

        byte[] tokenBytes = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("suggestuser", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        userToken = extractToken(new String(tokenBytes));
    }

    @Test
    void getSuggestions_shouldReturnSuggestionsFromHistory() {
        // Create QSOs with SP1ABC
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW, "Nice CW QSO. Name: John");
        createQso("SP1ABC", LocalDate.of(2024, 1, 20), "20m", AdifMode.SSB, "Another contact");
        createQso("SP1ABC", LocalDate.of(2024, 2, 1), "40m", AdifMode.CW, "Third contact");

        // Get suggestions
        webTestClient.get()
                .uri("/api/v1/suggestions/callsign/SP1ABC")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.callsign").isEqualTo("SP1ABC")
                .jsonPath("$.lastKnownName").isEqualTo("John")
                .jsonPath("$.mostCommonBand").isEqualTo("20m")  // 2 QSOs on 20m
                .jsonPath("$.mostCommonMode").isEqualTo("CW");   // 2 QSOs in CW
    }

    @Test
    void getSuggestions_shouldReturnMostRecentData() {
        // Create QSOs with different QTH
        createQsoWithQth("DL1XYZ", LocalDate.of(2024, 1, 15), "Berlin", "20m", AdifMode.SSB);
        createQsoWithQth("DL1XYZ", LocalDate.of(2024, 2, 1), "Munich", "40m", AdifMode.CW);

        // Get suggestions - should return Munich (most recent)
        webTestClient.get()
                .uri("/api/v1/suggestions/callsign/DL1XYZ")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.callsign").isEqualTo("DL1XYZ")
                .jsonPath("$.lastKnownQth").isEqualTo("Munich");
    }

    @Test
    void getSuggestions_shouldReturnNotFoundForUnknownCallsign() {
        webTestClient.get()
                .uri("/api/v1/suggestions/callsign/UNKNOWN")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getSuggestions_shouldRequireAuthentication() {
        webTestClient.get()
                .uri("/api/v1/suggestions/callsign/SP1ABC")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getSuggestions_shouldTruncateLongNotes() {
        // Create QSO with very long notes
        String longNotes = "This is a very long note that should be truncated because it exceeds the maximum length allowed for the snippet in the suggestions response. It contains a lot of information about the contact.";
        createQso("G4ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW, longNotes);

        // Get suggestions
        webTestClient.get()
                .uri("/api/v1/suggestions/callsign/G4ABC")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.lastNotesSnippet").value((String snippet) -> {
                    assert snippet.length() <= 100;
                    assert snippet.endsWith("...");
                });
    }

    private void createQso(String callsign, LocalDate date, String band, AdifMode mode, String notes) {
        CreateQsoRequest request = new CreateQsoRequest(
                callsign,
                date,
                LocalTime.of(14, 30),
                band,
                null,
                mode,
                null,
                null,
                "59", "59", null, null, notes,
                null
        );
        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
    }

    private void createQsoWithQth(String callsign, LocalDate date, String qth, String band, AdifMode mode) {
        CreateQsoRequest request = new CreateQsoRequest(
                callsign,
                date,
                LocalTime.of(14, 30),
                band,
                null,
                mode,
                null,
                null,
                "59", "59", qth, null, null,
                null
        );
        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
    }

    private String extractToken(String responseBody) {
        String[] parts = responseBody.split("\"accessToken\":\"");
        if (parts.length < 2) return "";
        return parts[1].split("\"")[0];
    }
}
