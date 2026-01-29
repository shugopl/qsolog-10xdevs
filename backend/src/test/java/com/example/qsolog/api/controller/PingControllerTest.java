package com.example.qsolog.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests for PingController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb",
        "spring.flyway.enabled=false"
})
class PingControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void ping_shouldReturnOk() {
        webTestClient.get()
                .uri("/api/v1/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");
    }
}
