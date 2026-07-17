package com.tracepilot.api.Migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Testcontainers
class FlywayMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("tracepilot_migration_test")
            .withUsername("test")
            .withPassword("test");

    private DataSource newDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMaximumPoolSize(2);
        return new HikariDataSource(config);
    }

    @Test
    void migrateAppliesCleanlyToAFreshDatabase() {
        DataSource dataSource = newDataSource();
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isGreaterThan(0);
    }

    @Test
    void migrateIsIdempotentOnAlreadyMigratedDatabase() {
        DataSource dataSource = newDataSource();
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
        MigrateResult second = flyway.migrate();

        assertThat(second.success).isTrue();
        assertThat(second.migrationsExecuted).isZero();
    }

    @Test
    void expectedTablesExistAfterMigration() throws Exception {
        DataSource dataSource = newDataSource();
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();

        Set<String> expectedTables = Set.of(
                "users", "refresh_tokens", "trace_audits", "agent_reports", "reliability_history");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            Set<String> actualTables = new HashSet<>();
            try (ResultSet rs = metaData.getTables(null, "public", "%", new String[] { "TABLE" })) {
                while (rs.next()) {
                    actualTables.add(rs.getString("TABLE_NAME"));
                }
            }
            assertThat(actualTables).containsAll(expectedTables);
        }
    }
}
