package com.contextstt.backend.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class ContextAnalysisMigrationTest {

    private static final String DATABASE_URL = "jdbc:h2:mem:context-analysis-migration;MODE=MySQL";

    @Test
    void migrationGroupsExistingCandidatesWithoutLosingSelection() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "")) {
            migrateToVersionFive();
            insertExistingAnalysis(connection);

            Flyway.configure()
                    .dataSource(DATABASE_URL, "sa", "")
                    .load()
                    .migrate();

            assertAnalysisMigrated(connection);
            assertCandidatesAndSelectionMigrated(connection);
        }
    }

    private void migrateToVersionFive() {
        Flyway.configure()
                .dataSource(DATABASE_URL, "sa", "")
                .target("5")
                .load()
                .migrate();
    }

    private void insertExistingAnalysis(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (id, email, password, nickname, role, created_at, updated_at)
                    VALUES (1, 'existing@example.com', 'encoded-password', '기존사용자',
                            'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO transcriptions (
                        id, user_id, provider, model, language_code, original_text, current_text,
                        audio_size_bytes, status, version, created_at, updated_at
                    ) VALUES (
                        1, 1, 'GOOGLE_SPEECH_V2', 'long', 'ko-KR', '그거 해줘', '그거 해줘',
                        100, 'CONFIRMED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO conversation_sessions (
                        id, owner_user_id, title, status, next_utterance_order,
                        version, created_at, updated_at
                    ) VALUES (
                        1, 1, '기존 대화', 'ACTIVE', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO conversation_participants (
                        id, conversation_id, user_id, display_name, participant_type, created_at
                    ) VALUES (1, 1, 1, '기존사용자', 'SELF', CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO conversation_utterances (
                        id, conversation_id, transcription_id, speaker_participant_id,
                        utterance_order, created_at, updated_at
                    ) VALUES (1, 1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO context_analyses (
                        id, conversation_id, utterance_id, transcription_id, provider, model,
                        source_speaker_name, source_original_text, source_current_text,
                        candidate_count, version, created_at, updated_at
                    ) VALUES (
                        1, 1, 1, 1, 'ANTHROPIC', 'claude-sonnet-5', '기존사용자',
                        '그거 해줘', '그거 해줘', 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO context_candidates (
                        id, analysis_id, candidate_rank, interpretation, inferred_intent,
                        rationale, intent_similarity_score, created_at
                    ) VALUES
                        (1, 1, 1, '보고서 작성', '업무 요청', '이전 대화 근거', 0.8000, CURRENT_TIMESTAMP),
                        (2, 1, 2, '예약 진행', '예약 요청', '대화 배경 근거', 0.2000, CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO context_analysis_selections (
                        id, analysis_id, candidate_id, original_candidate_text, final_text,
                        selected_at, updated_at, version
                    ) VALUES (
                        1, 1, 1, '보고서 작성', '주간 보고서 작성',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
                    )
                    """);
        }
    }

    private void assertAnalysisMigrated(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT requested_candidate_count, ambiguity_count
                     FROM context_analyses
                     WHERE id = 1
                     """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("requested_candidate_count")).isEqualTo(2);
            assertThat(resultSet.getInt("ambiguity_count")).isEqualTo(1);
        }

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT id, excerpt, start_word_id, end_word_id, candidate_count
                     FROM context_ambiguities
                     WHERE analysis_id = 1
                     """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("excerpt")).isEqualTo("그거 해줘");
            assertThat(resultSet.getObject("start_word_id")).isNull();
            assertThat(resultSet.getObject("end_word_id")).isNull();
            assertThat(resultSet.getInt("candidate_count")).isEqualTo(2);
        }
    }

    private void assertCandidatesAndSelectionMigrated(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*) AS candidate_count
                     FROM context_candidates candidate
                     JOIN context_ambiguities ambiguity ON ambiguity.id = candidate.ambiguity_id
                     WHERE ambiguity.analysis_id = 1
                     """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("candidate_count")).isEqualTo(2);
        }

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT selection.final_text, selection.resolution_type, ambiguity.analysis_id
                     FROM context_analysis_selections selection
                     JOIN context_ambiguities ambiguity ON ambiguity.id = selection.ambiguity_id
                     WHERE selection.id = 1
                     """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("final_text")).isEqualTo("주간 보고서 작성");
            assertThat(resultSet.getString("resolution_type")).isEqualTo("CANDIDATE");
            assertThat(resultSet.getLong("analysis_id")).isEqualTo(1L);
        }
    }
}
