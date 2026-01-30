package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.QsoLogApplication;
import com.pl.shugo.gsolog.api.dto.CreateQsoRequest;
import com.pl.shugo.gsolog.api.dto.LoginRequest;
import com.pl.shugo.gsolog.api.dto.RegisterRequest;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for export endpoints.
 * Tests ADIF and CSV export with proper mode/submode/customMode mapping.
 */
@SpringBootTest(
        classes = QsoLogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers(disabledWithoutDocker = true)
class ExportControllerTest {

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
    private String username;

    @BeforeEach
    void setUp() {
        // Register and login user
        String suffix = UUID.randomUUID().toString().replace("-", "");
        username = "exportuser_" + suffix;
        RegisterRequest user = new RegisterRequest("exportuser+" + suffix + "@test.com", username, "password123");
        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isCreated();

        byte[] tokenBytes = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(username, "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        userToken = extractToken(new String(tokenBytes));
    }

    /**
     * Test case 1: CW mode
     * stored mode=CW, submode=null, customMode=null
     * ADIF must contain: <MODE:2>CW
     * ADIF must NOT contain SUBMODE for this QSO.
     */
    @Test
    void exportAdif_cwMode_shouldEmitModeOnly() {
        // Create CW QSO
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW, null, null);

        // Export ADIF
        byte[] adifBytes = webTestClient.get()
                .uri("/api/v1/export/adif")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_PLAIN_VALUE)
                .expectBody()
                .returnResult()
                .getResponseBody();

        String adif = new String(adifBytes);

        // Verify ADIF contains MODE:2>CW
        assertThat(adif).contains("<MODE:2>CW");
        // Verify ADIF does NOT contain SUBMODE for this QSO
        assertThat(adif).doesNotContain("<SUBMODE:");
        // Verify required fields
        assertThat(adif).contains("<CALL:6>SP1ABC");
        assertThat(adif).contains("<QSO_DATE:8>20240115");
        assertThat(adif).contains("<BAND:3>20m");
    }

    /**
     * Test case 2: SSB mode
     * stored mode=SSB
     * ADIF must contain: <MODE:3>SSB
     */
    @Test
    void exportAdif_ssbMode_shouldEmitModeOnly() {
        // Create SSB QSO
        createQso("DL1XYZ", LocalDate.of(2024, 1, 16), "40m", AdifMode.SSB, null, null);

        // Export ADIF
        byte[] adifBytes = webTestClient.get()
                .uri("/api/v1/export/adif")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        String adif = new String(adifBytes);

        // Verify ADIF contains MODE:3>SSB
        assertThat(adif).contains("<MODE:3>SSB");
        assertThat(adif).doesNotContain("<SUBMODE:");
        assertThat(adif).contains("<CALL:6>DL1XYZ");
    }

    /**
     * Test case 3: PSK31 mode
     * stored mode=PSK, submode=PSK31
     * ADIF must contain: <MODE:3>PSK and <SUBMODE:5>PSK31
     */
    @Test
    void exportAdif_psk31Mode_shouldEmitModeAndSubmode() {
        // Create PSK31 QSO
        createQso("G4ABC", LocalDate.of(2024, 1, 17), "20m", AdifMode.PSK, AdifSubmode.PSK31, null);

        // Export ADIF
        byte[] adifBytes = webTestClient.get()
                .uri("/api/v1/export/adif")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        String adif = new String(adifBytes);

        // Verify ADIF contains MODE:3>PSK and SUBMODE:5>PSK31
        assertThat(adif).contains("<MODE:3>PSK");
        assertThat(adif).contains("<SUBMODE:5>PSK31");
        assertThat(adif).contains("<CALL:5>G4ABC");
    }

    /**
     * Test case 4: FT8 mode
     * stored mode=MFSK, submode=FT8
     * ADIF must contain: <MODE:4>MFSK and <SUBMODE:3>FT8
     */
    @Test
    void exportAdif_ft8Mode_shouldEmitModeAndSubmode() {
        // Create FT8 QSO
        createQso("OM1TEST", LocalDate.of(2024, 1, 18), "40m", AdifMode.MFSK, AdifSubmode.FT8, null);

        // Export ADIF
        byte[] adifBytes = webTestClient.get()
                .uri("/api/v1/export/adif")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        String adif = new String(adifBytes);

        // Verify ADIF contains MODE:4>MFSK and SUBMODE:3>FT8
        assertThat(adif).contains("<MODE:4>MFSK");
        assertThat(adif).contains("<SUBMODE:3>FT8");
        assertThat(adif).contains("<CALL:7>OM1TEST");
    }

    /**
     * Test case 5: Custom mode (VARAC)
     * stored mode=DATA, submode=null, customMode="VARAC"
     * ADIF must contain: <MODE:4>DATA
     * ADIF must contain vendor field: <APP_QSOLOG_CUSTOMMODE:5>VARAC
     * ADIF must NOT contain SUBMODE for this QSO.
     */
    @Test
    void exportAdif_customMode_shouldEmitModeAndVendorField() {
        // Create custom mode QSO
        createQso("F5XYZ", LocalDate.of(2024, 1, 19), "20m", AdifMode.DATA, null, "VARAC");

        // Export ADIF
        byte[] adifBytes = webTestClient.get()
                .uri("/api/v1/export/adif")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        String adif = new String(adifBytes);

        // Verify ADIF contains MODE:4>DATA
        assertThat(adif).contains("<MODE:4>DATA");
        // Verify ADIF contains vendor field APP_QSOLOG_CUSTOMMODE:5>VARAC
        assertThat(adif).contains("<APP_QSOLOG_CUSTOMMODE:5>VARAC");
        // Verify ADIF does NOT contain SUBMODE for this QSO
        assertThat(adif).doesNotContain("<SUBMODE:");
        assertThat(adif).contains("<CALL:5>F5XYZ");
    }

    @Test
    void exportAdif_shouldIncludeAdifHeader() {
        // Create a QSO
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW, null, null);

        // Export ADIF
        byte[] adifBytes = webTestClient.get()
                .uri("/api/v1/export/adif")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        String adif = new String(adifBytes);

        // Verify ADIF header
        assertThat(adif).contains("<ADIF_VER:5>3.1.4");
        assertThat(adif).contains("<PROGRAMID:6>QSOLOG");
        assertThat(adif).contains("<EOH>");
        assertThat(adif).contains("<EOR>");
    }

    @Test
    void exportAdif_shouldFilterByDateRange() {
        // Create QSOs on different dates
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW, null, null);
        createQso("DL1XYZ", LocalDate.of(2024, 1, 20), "40m", AdifMode.SSB, null, null);
        createQso("G4ABC", LocalDate.of(2024, 2, 1), "20m", AdifMode.PSK, AdifSubmode.PSK31, null);

        // Export ADIF for January only
        byte[] adifBytes = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/export/adif")
                        .queryParam("from", "2024-01-01")
                        .queryParam("to", "2024-01-31")
                        .build())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        String adif = new String(adifBytes);

        // Should include January QSOs
        assertThat(adif).contains("SP1ABC");
        assertThat(adif).contains("DL1XYZ");
        // Should NOT include February QSO
        assertThat(adif).doesNotContain("G4ABC");
    }

    @Test
    void exportCsv_shouldIncludeHeaderAndData() {
        // Create a QSO
        createQso("SP1ABC", LocalDate.of(2024, 1, 15), "20m", AdifMode.CW, null, null);

        // Export CSV
        byte[] csvBytes = webTestClient.get()
                .uri("/api/v1/export/csv")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/csv")
                .expectBody()
                .returnResult()
                .getResponseBody();

        String csv = new String(csvBytes);

        // Verify CSV header
        assertThat(csv).contains("Callsign,Date,Time,Band");
        // Verify CSV data
        assertThat(csv).contains("SP1ABC,2024-01-15");
        assertThat(csv).contains("20m");
        assertThat(csv).contains("CW");
    }

    @Test
    void exportCsv_shouldEscapeCommasAndQuotes() {
        // Create QSO with notes containing comma and quotes
        CreateQsoRequest request = new CreateQsoRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "20m",
                null,
                AdifMode.CW,
                null,
                null,
                "59", "59", null, null,
                "Nice QSO, \"great signal\"",
                null
        );
        createQsoWithRequest(request);

        // Export CSV
        byte[] csvBytes = webTestClient.get()
                .uri("/api/v1/export/csv")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        String csv = new String(csvBytes);

        // Verify CSV escaping
        assertThat(csv).contains("\"Nice QSO, \"\"great signal\"\"\"");
    }

    @Test
    void exportAdif_shouldRequireAuthentication() {
        webTestClient.get()
                .uri("/api/v1/export/adif")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void exportCsv_shouldRequireAuthentication() {
        webTestClient.get()
                .uri("/api/v1/export/csv")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private void createQso(String callsign, LocalDate date, String band, AdifMode mode, AdifSubmode submode, String customMode) {
        CreateQsoRequest request = new CreateQsoRequest(
                callsign,
                date,
                LocalTime.of(14, 30),
                band,
                null,
                mode,
                submode,
                customMode,
                "59", "59", null, null, null,
                null
        );
        createQsoWithRequest(request);
    }

    private void createQsoWithRequest(CreateQsoRequest request) {
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
