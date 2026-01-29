package com.example.qsolog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic application startup test.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb",
        "spring.flyway.enabled=false"
})
class QsoLogApplicationTests {

    @Test
    void contextLoads() {
        // Application context loads successfully
    }
}
