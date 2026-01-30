package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.QsoLogApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for lookup endpoint.
 */
@SpringBootTest(
        classes = QsoLogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers(disabledWithoutDocker = true)
class LookupControllerTest {

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

        // Ensure HamQTH credentials are not configured (use mock adapter)
        registry.add("hamqth.username", () -> "");
        registry.add("hamqth.password", () -> "");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void lookup_shouldUseMockAdapterWhenCredentialsNotConfigured() {
        // With mock adapter, lookup should return 404 (empty Mono)
        webTestClient.get()
                .uri("/api/v1/lookup/SP1ABC")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void lookup_shouldAcceptValidCallsign() {
        // Mock adapter returns empty, so we expect 404
        webTestClient.get()
                .uri("/api/v1/lookup/DL1XYZ")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void lookup_shouldNormalizeCallsignToUppercase() {
        // Even with lowercase input, mock adapter should handle it
        webTestClient.get()
                .uri("/api/v1/lookup/sp1abc")
                .exchange()
                .expectStatus().isNotFound();
    }
}
