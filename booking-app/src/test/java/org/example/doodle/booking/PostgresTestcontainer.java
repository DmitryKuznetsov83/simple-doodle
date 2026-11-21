package org.example.doodle.booking;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * PostgreSQL of docker-compose with the production schema. The Spring test context cache shares
 * the container between test classes importing this configuration.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainer {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../db/01-schema.sql"),
                        "/docker-entrypoint-initdb.d/01-schema.sql");
    }
}
