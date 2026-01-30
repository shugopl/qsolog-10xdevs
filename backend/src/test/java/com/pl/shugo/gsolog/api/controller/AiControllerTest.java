package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.QsoLogApplication;
import com.pl.shugo.gsolog.api.dto.CreateQsoRequest;
import com.pl.shugo.gsolog.api.dto.LoginRequest;
import com.pl.shugo.gsolog.api.dto.QsoDescriptionRequest;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AI helper endpoints.
 */
@SpringBootTest(
        classes = QsoLogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
class AiControllerTest {

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

        // Ensure OpenAI key is not configured (use mock adapter)
        registry.add("openai.api-key", () -> "");
    }

    @Autowired
    private WebTestClient webTestClient;

    private String userToken;

    @BeforeEach
    void setUp() {
        // Register and login user
        RegisterRequest user = new RegisterRequest("aiuser@test.com", "aiuser", "password123");
        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isCreated();

        byte[] tokenBytes = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("aiuser", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        userToken = extractToken(new String(tokenBytes));
    }

    @Test
    void generateQsoDescription_shouldReturnMockTextInEnglish() {
        QsoDescriptionRequest request = new QsoDescriptionRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "20m",
                "CW",
                "599",
                "579",
                "Warsaw",
                "Nice operator",
                "EN"
        );

        webTestClient.post()
                .uri("/api/v1/ai/qso-description")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.language").isEqualTo("EN")
                .jsonPath("$.text").value((String text) -> {
                    assertThat(text).isNotEmpty();
                    assertThat(text).contains("SP1ABC");
                    assertThat(text).contains("20m");
                    assertThat(text).contains("CW");
                });
    }

    @Test
    void generateQsoDescription_shouldReturnMockTextInPolish() {
        QsoDescriptionRequest request = new QsoDescriptionRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "20m",
                "CW",
                "599",
                "579",
                "Warszawa",
                "Miły operator",
                "PL"
        );

        webTestClient.post()
                .uri("/api/v1/ai/qso-description")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.language").isEqualTo("PL")
                .jsonPath("$.text").value((String text) -> {
                    assertThat(text).isNotEmpty();
                    assertThat(text).contains("SP1ABC");
                    assertThat(text).contains("20m");
                });
    }

    @Test
    void generatePeriodReport_shouldCreateReportAndPersistToHistory() {
        // Create some QSOs
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW);
        createQso("DL1XYZ", LocalDate.of(2024, 1, 16), "40m", AdifMode.SSB);
        createQso("G4ABC", LocalDate.of(2024, 1, 17), "20m", AdifMode.CW);

        // Generate report
        byte[] reportBytes = webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/ai/period-report")
                        .queryParam("from", "2024-01-01")
                        .queryParam("to", "2024-01-31")
                        .queryParam("lang", "EN")
                        .build())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.dateFrom").isEqualTo("2024-01-01")
                .jsonPath("$.dateTo").isEqualTo("2024-01-31")
                .jsonPath("$.language").isEqualTo("EN")
                .jsonPath("$.content").value((String content) -> {
                    assertThat(content).isNotEmpty();
                    assertThat(content).containsAnyOf("Total", "contacts", "QSO");
                })
                .returnResult()
                .getResponseBody();

        String reportId = extractReportId(new String(reportBytes));

        // Verify report is in history
        webTestClient.get()
                .uri("/api/v1/ai/reports/" + reportId)
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(reportId)
                .jsonPath("$.language").isEqualTo("EN");
    }

    @Test
    void generatePeriodReport_shouldWorkInPolish() {
        // Create some QSOs
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW);

        // Generate Polish report
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/ai/period-report")
                        .queryParam("lang", "PL")
                        .build())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.language").isEqualTo("PL")
                .jsonPath("$.content").value((String content) -> {
                    assertThat(content).isNotEmpty();
                });
    }

    @Test
    void getReports_shouldReturnUserReports() {
        // Create QSOs and generate report
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/ai/period-report")
                        .queryParam("lang", "EN")
                        .build())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk();

        // Get reports list
        webTestClient.get()
                .uri("/api/v1/ai/reports")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].language").isEqualTo("EN");
    }

    @Test
    void getReports_shouldFilterByDateRange() {
        // Create QSOs and generate reports for different periods
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/ai/period-report")
                        .queryParam("from", "2024-01-01")
                        .queryParam("to", "2024-01-31")
                        .queryParam("lang", "EN")
                        .build())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk();

        // Filter reports
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/ai/reports")
                        .queryParam("from", "2024-01-01")
                        .queryParam("to", "2024-01-31")
                        .build())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }

    @Test
    void aiEndpoints_shouldRequireAuthentication() {
        QsoDescriptionRequest request = new QsoDescriptionRequest(
                "SP1ABC", LocalDate.now(), LocalTime.now(),
                "20m", "CW", "59", "59", null, null, "EN"
        );

        // QSO description doesn't require auth (it's just a text generator)
        webTestClient.post()
                .uri("/api/v1/ai/qso-description")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        // Period report requires auth
        webTestClient.post()
                .uri("/api/v1/ai/period-report")
                .exchange()
                .expectStatus().isUnauthorized();

        // Reports list requires auth
        webTestClient.get()
                .uri("/api/v1/ai/reports")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private void createQso(String callsign, LocalDate date, String band, AdifMode mode) {
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

    private String extractReportId(String responseBody) {
        String[] parts = responseBody.split("\"id\":\"");
        if (parts.length < 2) return "";
        return parts[1].split("\"")[0];
    }
}
