package com.hospital.auth.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the whole service against a throwaway PostgreSQL 15 (same major version as docker-compose).
 * Flyway runs every migration on startup, so a broken or empty migration fails these tests.
 * Skipped automatically when Docker is not available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void appProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TestJwt.SECRET);
        registry.add("app.internal-secret", () -> TestJwt.INTERNAL_SECRET);
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
    }
}
