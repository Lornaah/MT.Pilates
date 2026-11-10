package fr.mtpilates.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;

/** PostgreSQL éphémère partagé par les tests d'intégration (même version que docker-compose), et emails capturés. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainer {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    @Bean
    public CapturingMailSender mailSender() {
        return new CapturingMailSender();
    }

    @Bean
    public TestUsers testUsers(final JdbcTemplate jdbc, final PasswordEncoder encoder) {
        return new TestUsers(jdbc, encoder);
    }
}
