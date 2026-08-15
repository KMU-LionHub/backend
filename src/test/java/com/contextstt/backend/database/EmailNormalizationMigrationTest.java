package com.contextstt.backend.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class EmailNormalizationMigrationTest {

    private static final String DATABASE_URL = "jdbc:h2:mem:email-normalization;MODE=MySQL";

    @Test
    void migrationNormalizesExistingEmail() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "")) {
            migrateToVersionOne();
            insertExistingUser(connection);

            Flyway.configure()
                    .dataSource(DATABASE_URL, "sa", "")
                    .load()
                    .migrate();

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT email FROM users WHERE id = 1"
            ); ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("email")).isEqualTo("mixed.case@example.com");
            }
        }
    }

    private void migrateToVersionOne() {
        Flyway.configure()
                .dataSource(DATABASE_URL, "sa", "")
                .target("1")
                .load()
                .migrate();
    }

    private void insertExistingUser(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO users (email, password, nickname, role, created_at, updated_at)
                VALUES ('  Mixed.Case@Example.COM  ', 'encoded-password', '기존사용자',
                        'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)) {
            statement.executeUpdate();
        }
    }
}