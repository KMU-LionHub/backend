package com.contextstt.backend.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class ConversationMigrationTest {

    private static final String DATABASE_URL = "jdbc:h2:mem:conversation-migration;MODE=MySQL";

    @Test
    void migrationKeepsExistingTranscriptionsAsDrafts() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "")) {
            migrateToVersionThree();
            insertExistingUserAndTranscription(connection);

            Flyway.configure()
                    .dataSource(DATABASE_URL, "sa", "")
                    .load()
                    .migrate();

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT status, confirmed_at FROM transcriptions WHERE id = 1"
            ); ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("DRAFT");
                assertThat(resultSet.getTimestamp("confirmed_at")).isNull();
            }
        }
    }

    private void migrateToVersionThree() {
        Flyway.configure()
                .dataSource(DATABASE_URL, "sa", "")
                .target("3")
                .load()
                .migrate();
    }

    private void insertExistingUserAndTranscription(Connection connection) throws SQLException {
        try (PreparedStatement userStatement = connection.prepareStatement("""
                INSERT INTO users (id, email, password, nickname, role, created_at, updated_at)
                VALUES (1, 'existing@example.com', 'encoded-password', '기존사용자',
                        'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """); PreparedStatement transcriptionStatement = connection.prepareStatement("""
                INSERT INTO transcriptions (
                    id, user_id, provider, model, language_code, original_text, current_text,
                    audio_size_bytes, version, created_at, updated_at
                ) VALUES (
                    1, 1, 'GOOGLE_SPEECH_V2', 'long', 'ko-KR', '기존 발언', '기존 발언',
                    100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """)) {
            userStatement.executeUpdate();
            transcriptionStatement.executeUpdate();
        }
    }
}
