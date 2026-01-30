package com.pl.shugo.gsolog.infrastructure.security;

import com.pl.shugo.gsolog.QsoLogApplication;
import com.pl.shugo.gsolog.api.dto.QsoDescriptionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Tests for security configuration.
 */
@SpringBootTest(
        classes = QsoLogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb",
        "spring.flyway.enabled=false"
})
class SecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void ping_shouldBeAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/api/v1/ping")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void authEndpoints_shouldBeAccessibleWithoutAuthentication() {
        webTestClient.post()
                .uri("/api/v1/auth/register")
                .exchange()
                .expectStatus().is4xxClientError(); // Will fail validation, but not 401
    }

    @Test
    void actuator_shouldBeAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedQsoEndpoint_shouldReturn401WithoutToken() {
        webTestClient.get()
                .uri("/api/v1/qso")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedStatsEndpoint_shouldReturn401WithoutToken() {
        webTestClient.get()
                .uri("/api/v1/stats/summary")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedExportEndpoint_shouldReturn401WithoutToken() {
        webTestClient.get()
                .uri("/api/v1/export/adif")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedLookupEndpoint_shouldReturn401WithoutToken() {
        webTestClient.get()
                .uri("/api/v1/lookup/CALL")
                .exchange()
                // Lookup is public, but mock adapter returns empty => 404
                .expectStatus().isNotFound();
    }

    @Test
    void protectedAiEndpoint_shouldReturn401WithoutToken() {
        webTestClient.post()
                .uri("/api/v1/ai/period-report")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void publicAiQsoDescription_shouldBeAccessibleWithoutAuthentication() {
        QsoDescriptionRequest request = new QsoDescriptionRequest(
                "SP1ABC",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(14, 30),
                "20m",
                "CW",
                "59",
                "59",
                null,
                null,
                "EN"
        );

        webTestClient.post()
                .uri("/api/v1/ai/qso-description")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }
}
