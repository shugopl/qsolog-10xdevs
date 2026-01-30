package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.QsoLogApplication;
import com.pl.shugo.gsolog.api.dto.CreateQsoRequest;
import com.pl.shugo.gsolog.api.dto.LoginRequest;
import com.pl.shugo.gsolog.api.dto.RegisterRequest;
import com.pl.shugo.gsolog.api.dto.UpdateQsoRequest;
import com.pl.shugo.gsolog.domain.enums.AdifMode;
import com.pl.shugo.gsolog.domain.enums.AdifSubmode;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Integration tests for QSO CRUD operations.
 */
@SpringBootTest(
        classes = QsoLogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers(disabledWithoutDocker = true)
class QsoControllerTest {

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

    private String user1Token;
    private String user2Token;
    private String user1Username;
    private String user2Username;

    @BeforeEach
    void setUp() {
        // Register and login user 1
        String suffix = UUID.randomUUID().toString().replace("-", "");
        user1Username = "user1_" + suffix;
        user2Username = "user2_" + suffix;

        RegisterRequest user1 = new RegisterRequest("user1+" + suffix + "@test.com", user1Username, "password123");
        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user1)
                .exchange()
                .expectStatus().isCreated();

        byte[] tokenBytes1 = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(user1Username, "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        user1Token = extractToken(new String(tokenBytes1));

        // Register and login user 2
        RegisterRequest user2 = new RegisterRequest("user2+" + suffix + "@test.com", user2Username, "password123");
        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user2)
                .exchange()
                .expectStatus().isCreated();

        byte[] tokenBytes2 = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(user2Username, "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        user2Token = extractToken(new String(tokenBytes2));
    }

    @Test
    void createQso_shouldSucceed() {
        CreateQsoRequest request = new CreateQsoRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "20m",
                new BigDecimal("14.250"),
                AdifMode.SSB,
                null,
                null,
                "59",
                "59",
                "Warsaw",
                "KO02",
                "Nice contact",
                null
        );

        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.theirCallsign").isEqualTo("SP1ABC")
                .jsonPath("$.band").isEqualTo("20m")
                .jsonPath("$.mode").isEqualTo("SSB");
    }

    @Test
    void createQso_shouldValidateBand() {
        CreateQsoRequest request = new CreateQsoRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "invalid-band",
                null,
                AdifMode.SSB,
                null,
                null,
                null, null, null, null, null, null
        );

        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createQso_shouldValidateModeSubmodeRules() {
        // FT8 requires MFSK mode
        CreateQsoRequest invalidRequest = new CreateQsoRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "20m",
                null,
                AdifMode.SSB, // Wrong! Should be MFSK
                AdifSubmode.FT8,
                null,
                null, null, null, null, null, null
        );

        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();

        // FT8 with MFSK should succeed
        CreateQsoRequest validRequest = new CreateQsoRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "20m",
                null,
                AdifMode.MFSK,
                AdifSubmode.FT8,
                null,
                null, null, null, null, null, null
        );

        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void createQso_shouldValidateCustomModeRules() {
        // customMode requires DATA mode and null submode
        CreateQsoRequest invalidRequest = new CreateQsoRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "20m",
                null,
                AdifMode.SSB, // Wrong! Should be DATA
                null,
                "VARAC",
                null, null, null, null, null, null
        );

        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();

        // customMode with DATA and null submode should succeed
        CreateQsoRequest validRequest = new CreateQsoRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "20m",
                null,
                AdifMode.DATA,
                null,
                "VARAC",
                null, null, null, null, null, null
        );

        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void createQso_shouldDetectDuplicates() {
        CreateQsoRequest request = new CreateQsoRequest(
                "DL1XYZ",
                LocalDate.of(2024, 1, 20),
                LocalTime.of(16, 0),
                "40m",
                null,
                AdifMode.CW,
                null,
                null,
                null, null, null, null, null, null
        );

        // First QSO should succeed
        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Second identical QSO should return 409 Conflict
        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.type").isEqualTo("duplicate_detected")
                .jsonPath("$.existingIds").isNotEmpty();
    }

    @Test
    void createQso_shouldAllowDuplicateWithConfirmFlag() {
        CreateQsoRequest request = new CreateQsoRequest(
                "G4ABC",
                LocalDate.of(2024, 1, 25),
                LocalTime.of(18, 0),
                "80m",
                null,
                AdifMode.SSB,
                null,
                null,
                null, null, null, null, null, false
        );

        // First QSO
        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Second QSO with confirmDuplicate=true should succeed
        CreateQsoRequest confirmedRequest = new CreateQsoRequest(
                request.theirCallsign(),
                request.qsoDate(),
                request.timeOn(),
                request.band(),
                request.frequencyKhz(),
                request.mode(),
                request.submode(),
                request.customMode(),
                request.rstSent(),
                request.rstRecv(),
                request.qth(),
                request.gridSquare(),
                request.notes(),
                true // confirmDuplicate
        );

        webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(confirmedRequest)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void getQso_shouldReturnQsoForOwner() {
        // Create QSO
        String qsoId = createQsoAndGetId(user1Token);

        // Get QSO
        webTestClient.get()
                .uri("/api/v1/qso/" + qsoId)
                .header("Authorization", "Bearer " + user1Token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(qsoId)
                .jsonPath("$.theirCallsign").isEqualTo("OM1TEST");
    }

    @Test
    void getQso_shouldReturn404ForNonOwner() {
        // User 1 creates QSO
        String qsoId = createQsoAndGetId(user1Token);

        // User 2 tries to access it
        webTestClient.get()
                .uri("/api/v1/qso/" + qsoId)
                .header("Authorization", "Bearer " + user2Token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateQso_shouldSucceedForOwner() {
        String qsoId = createQsoAndGetId(user1Token);

        UpdateQsoRequest updateRequest = new UpdateQsoRequest(
                "OM1UPDATED",
                LocalDate.of(2024, 2, 1),
                LocalTime.of(12, 0),
                "15m",
                null,
                AdifMode.FM,
                null,
                null,
                "57", "58", "Bratislava", "JN88", "Updated notes",
                null, null, null
        );

        webTestClient.put()
                .uri("/api/v1/qso/" + qsoId)
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.theirCallsign").isEqualTo("OM1UPDATED")
                .jsonPath("$.band").isEqualTo("15m");
    }

    @Test
    void updateQso_shouldReturn404ForNonOwner() {
        String qsoId = createQsoAndGetId(user1Token);

        UpdateQsoRequest updateRequest = new UpdateQsoRequest(
                "OM1HACK",
                LocalDate.of(2024, 2, 1),
                LocalTime.of(12, 0),
                "15m",
                null,
                AdifMode.FM,
                null,
                null,
                null, null, null, null, null,
                null, null, null
        );

        webTestClient.put()
                .uri("/api/v1/qso/" + qsoId)
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteQso_shouldSucceedForOwner() {
        String qsoId = createQsoAndGetId(user1Token);

        webTestClient.delete()
                .uri("/api/v1/qso/" + qsoId)
                .header("Authorization", "Bearer " + user1Token)
                .exchange()
                .expectStatus().isNoContent();

        // Verify deleted
        webTestClient.get()
                .uri("/api/v1/qso/" + qsoId)
                .header("Authorization", "Bearer " + user1Token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteQso_shouldReturn404ForNonOwner() {
        String qsoId = createQsoAndGetId(user1Token);

        webTestClient.delete()
                .uri("/api/v1/qso/" + qsoId)
                .header("Authorization", "Bearer " + user2Token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getAllQsos_shouldReturnOnlyUserQsos() {
        // User 1 creates 2 QSOs
        createQsoAndGetId(user1Token);
        createQsoAndGetId(user1Token, "OM1TEST2");

        // User 2 creates 1 QSO
        createQsoAndGetId(user2Token, "OM2TEST");

        // User 1 should see only their 2 QSOs
        webTestClient.get()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + user1Token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    private String createQsoAndGetId(String token) {
        return createQsoAndGetId(token, "OM1TEST");
    }

    private String createQsoAndGetId(String token, String callsign) {
        CreateQsoRequest request = new CreateQsoRequest(
                callsign,
                LocalDate.of(2024, 1, 30),
                LocalTime.of(10, 0),
                "20m",
                null,
                AdifMode.CW,
                null,
                null,
                null, null, null, null, null, null
        );

        byte[] responseBytes = webTestClient.post()
                .uri("/api/v1/qso")
                .header("Authorization", "Bearer " + token)
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
