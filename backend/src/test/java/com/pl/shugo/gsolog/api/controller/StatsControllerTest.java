package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.QsoLogApplication;
import com.pl.shugo.gsolog.api.dto.CreateQsoRequest;
import com.pl.shugo.gsolog.api.dto.LoginRequest;
import com.pl.shugo.gsolog.api.dto.RegisterRequest;
import com.pl.shugo.gsolog.domain.enums.AdifMode;
import com.pl.shugo.gsolog.domain.enums.QslStatus;
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
 * Integration tests for statistics endpoint.
 */
@SpringBootTest(
        classes = QsoLogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
class StatsControllerTest {

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
        RegisterRequest user = new RegisterRequest("statsuser@test.com", "statsuser", "password123");
        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isCreated();

        byte[] tokenBytes = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("statsuser", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        userToken = extractToken(new String(tokenBytes));

        // Create sample QSOs for testing
        createSampleQsos();
    }

    private void createSampleQsos() {
        // QSO 1: 20m, SSB, confirmed via QSL
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.SSB, QslStatus.CONFIRMED);

        // QSO 2: 20m, SSB, not confirmed
        createQso("DL1XYZ", LocalDate.of(2024, 1, 16), "20m", AdifMode.SSB, QslStatus.NONE);

        // QSO 3: 40m, CW, confirmed via QSL
        createQso("G4ABC", LocalDate.of(2024, 1, 17), "40m", AdifMode.CW, QslStatus.CONFIRMED);

        // QSO 4: 40m, CW, not confirmed
        createQso("OM1TEST", LocalDate.of(2024, 1, 18), "40m", AdifMode.CW, QslStatus.NONE);

        // QSO 5: 20m, CW, not confirmed
        createQso("F5XYZ", LocalDate.of(2024, 1, 19), "20m", AdifMode.CW, QslStatus.NONE);

        // QSO 6: 80m, SSB, confirmed via QSL (same day as QSO 5)
        createQso("I2ABC", LocalDate.of(2024, 1, 19), "80m", AdifMode.SSB, QslStatus.CONFIRMED);

        // QSO 7: Outside date range (for filter testing)
        createQso("EA1XYZ", LocalDate.of(2024, 2, 1), "20m", AdifMode.SSB, QslStatus.CONFIRMED);
    }

    private void createQso(String callsign, LocalDate date, String band, AdifMode mode, QslStatus qslStatus) {
        CreateQsoRequest request = new CreateQsoRequest(
                callsign,
                date,
                LocalTime.of(14, 30),
                band,
                null,
                mode,
                null,
                null,
                "59", "59", null, null, null,
                null
        );

        // Create QSO
        String qsoId = createQsoAndGetId(request);

        // Update QSL status if needed
        if (qslStatus != QslStatus.NONE) {
            webTestClient.put()
                    .uri("/api/v1/qso/" + qsoId)
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(String.format("""
                            {
                                "theirCallsign": "%s",
                                "qsoDate": "%s",
                                "timeOn": "14:30:00",
                                "band": "%s",
                                "mode": "%s",
                                "qslStatus": "%s"
                            }
                            """, callsign, date, band, mode, qslStatus))
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Test
    void getStatsSummary_shouldReturnAllStats() {
        webTestClient.get()
                .uri("/api/v1/stats/summary")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // Totals: 7 total, 4 confirmed
                .jsonPath("$.totals.all").isEqualTo(7)
                .jsonPath("$.totals.confirmed").isEqualTo(4)
                // Counts by band
                .jsonPath("$.countsByBand.length()").isEqualTo(3)
                // Counts by mode
                .jsonPath("$.countsByMode.length()").isEqualTo(2)
                // Counts by day
                .jsonPath("$.countsByDay.length()").isEqualTo(6);
    }

    @Test
    void getStatsSummary_shouldCountByBandCorrectly() {
        webTestClient.get()
                .uri("/api/v1/stats/summary")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // 20m: 4 total (3 in Jan + 1 in Feb), 2 confirmed
                .jsonPath("$.countsByBand[?(@.band == '20m')].countAll").isEqualTo(4)
                .jsonPath("$.countsByBand[?(@.band == '20m')].countConfirmed").isEqualTo(2)
                // 40m: 2 total, 1 confirmed
                .jsonPath("$.countsByBand[?(@.band == '40m')].countAll").isEqualTo(2)
                .jsonPath("$.countsByBand[?(@.band == '40m')].countConfirmed").isEqualTo(1)
                // 80m: 1 total, 1 confirmed
                .jsonPath("$.countsByBand[?(@.band == '80m')].countAll").isEqualTo(1)
                .jsonPath("$.countsByBand[?(@.band == '80m')].countConfirmed").isEqualTo(1);
    }

    @Test
    void getStatsSummary_shouldCountByModeCorrectly() {
        webTestClient.get()
                .uri("/api/v1/stats/summary")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // SSB: 4 total, 3 confirmed
                .jsonPath("$.countsByMode[?(@.mode == 'SSB')].countAll").isEqualTo(4)
                .jsonPath("$.countsByMode[?(@.mode == 'SSB')].countConfirmed").isEqualTo(3)
                // CW: 3 total, 1 confirmed
                .jsonPath("$.countsByMode[?(@.mode == 'CW')].countAll").isEqualTo(3)
                .jsonPath("$.countsByMode[?(@.mode == 'CW')].countConfirmed").isEqualTo(1);
    }

    @Test
    void getStatsSummary_shouldFilterByDateRange() {
        // Filter to January only (exclude Feb QSO)
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/stats/summary")
                        .queryParam("from", "2024-01-01")
                        .queryParam("to", "2024-01-31")
                        .build())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // Totals: 6 total (7 - 1 Feb QSO), 3 confirmed (4 - 1 Feb QSO)
                .jsonPath("$.totals.all").isEqualTo(6)
                .jsonPath("$.totals.confirmed").isEqualTo(3)
                // 20m: 3 total (4 - 1 Feb QSO), 1 confirmed (2 - 1 Feb QSO)
                .jsonPath("$.countsByBand[?(@.band == '20m')].countAll").isEqualTo(3)
                .jsonPath("$.countsByBand[?(@.band == '20m')].countConfirmed").isEqualTo(1);
    }

    @Test
    void getStatsSummary_shouldCountByDayCorrectly() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/stats/summary")
                        .queryParam("from", "2024-01-01")
                        .queryParam("to", "2024-01-31")
                        .build())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // Jan 19: 2 QSOs (1 CW not confirmed + 1 SSB confirmed)
                .jsonPath("$.countsByDay[?(@.date == '2024-01-19')].countAll").isEqualTo(2)
                .jsonPath("$.countsByDay[?(@.date == '2024-01-19')].countConfirmed").isEqualTo(1);
    }

    @Test
    void getStatsSummary_shouldReturnEmptyForUserWithNoQsos() {
        // Register new user
        RegisterRequest newUser = new RegisterRequest("newuser@test.com", "newuser", "password123");
        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newUser)
                .exchange()
                .expectStatus().isCreated();

        byte[] tokenBytes = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("newuser", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        String newUserToken = extractToken(new String(tokenBytes));

        webTestClient.get()
                .uri("/api/v1/stats/summary")
                .header("Authorization", "Bearer " + newUserToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totals.all").isEqualTo(0)
                .jsonPath("$.totals.confirmed").isEqualTo(0)
                .jsonPath("$.countsByBand.length()").isEqualTo(0)
                .jsonPath("$.countsByMode.length()").isEqualTo(0)
                .jsonPath("$.countsByDay.length()").isEqualTo(0);
    }

    private String createQsoAndGetId(CreateQsoRequest request) {
        byte[] responseBytes = webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .returnResult()
                .getResponseBody();

        String response = new String(responseBytes);
        return extractId(response);
    }

    private String extractToken(String responseBody) {
        String[] parts = responseBody.split("\"accessToken\":\"");
        if (parts.length < 2) return "";
        return parts[1].split("\"")[0];
    }

    private String extractId(String responseBody) {
        String[] parts = responseBody.split("\"id\":\"");
        if (parts.length < 2) return "";
        return parts[1].split("\"")[0];
    }
}
