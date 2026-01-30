package com.pl.shugo.gsolog.infrastructure.persistence;

import com.pl.shugo.gsolog.QsoLogApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying Flyway migrations run successfully.
 * Uses Testcontainers to spin up a PostgreSQL database.
 */
@SpringBootTest(classes = QsoLogApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("qsolog_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure R2DBC connection
        registry.add("spring.r2dbc.url", () ->
                String.format("r2dbc:postgresql://%s:%d/%s",
                        postgres.getHost(),
                        postgres.getFirstMappedPort(),
                        postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        // Configure Flyway (JDBC) connection
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private DatabaseClient databaseClient;

    @Test
    void flywayMigrationsCreateTables() {
        // Verify users table exists
        StepVerifier.create(
                databaseClient.sql("SELECT COUNT(*) FROM users")
                        .fetch()
                        .first()
        ).assertNext(result -> {
            assertThat(result).containsKey("count");
        }).verifyComplete();

        // Verify qso table exists
        StepVerifier.create(
                databaseClient.sql("SELECT COUNT(*) FROM qso")
                        .fetch()
                        .first()
        ).assertNext(result -> {
            assertThat(result).containsKey("count");
        }).verifyComplete();

        // Verify ai_report_history table exists
        StepVerifier.create(
                databaseClient.sql("SELECT COUNT(*) FROM ai_report_history")
                        .fetch()
                        .first()
        ).assertNext(result -> {
            assertThat(result).containsKey("count");
        }).verifyComplete();
    }

    @Test
    void qsoTableHasRequiredColumns() {
        // Verify qso table has mode, submode, and custom_mode columns
        StepVerifier.create(
                databaseClient.sql("""
                    SELECT column_name, data_type, is_nullable
                    FROM information_schema.columns
                    WHERE table_name = 'qso'
                    AND column_name IN ('mode', 'submode', 'custom_mode')
                    ORDER BY column_name
                """)
                .fetch()
                .all()
        ).expectNextMatches(row ->
                "custom_mode".equals(row.get("column_name")) &&
                "YES".equals(row.get("is_nullable"))
        ).expectNextMatches(row ->
                "mode".equals(row.get("column_name")) &&
                "NO".equals(row.get("is_nullable"))
        ).expectNextMatches(row ->
                "submode".equals(row.get("column_name")) &&
                "YES".equals(row.get("is_nullable"))
        ).verifyComplete();
    }

    @Test
    void qsoTableHasRequiredIndexes() {
        // Verify indexes exist
        StepVerifier.create(
                databaseClient.sql("""
                    SELECT indexname
                    FROM pg_indexes
                    WHERE tablename = 'qso'
                    AND indexname IN ('idx_qso_user_date', 'idx_qso_user_callsign', 'idx_qso_user_band')
                    ORDER BY indexname
                """)
                .fetch()
                .all()
        ).expectNextCount(3)
         .verifyComplete();
    }
}
