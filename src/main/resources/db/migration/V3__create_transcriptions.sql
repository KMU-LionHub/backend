CREATE TABLE transcriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    replaces_transcription_id BIGINT,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    language_code VARCHAR(35) NOT NULL,
    original_text LONGTEXT NOT NULL,
    current_text LONGTEXT NOT NULL,
    confidence FLOAT,
    audio_content_type VARCHAR(100),
    audio_size_bytes BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_transcriptions PRIMARY KEY (id),
    CONSTRAINT fk_transcriptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_transcriptions_replacement FOREIGN KEY (replaces_transcription_id) REFERENCES transcriptions (id)
);

CREATE INDEX idx_transcriptions_user_created_at
    ON transcriptions (user_id, created_at);

CREATE INDEX idx_transcriptions_replacement
    ON transcriptions (replaces_transcription_id);

CREATE TABLE transcript_words (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transcription_id BIGINT NOT NULL,
    word_order INT NOT NULL,
    original_text VARCHAR(2000) NOT NULL,
    corrected_text VARCHAR(2000),
    start_offset_millis BIGINT,
    end_offset_millis BIGINT,
    confidence FLOAT,
    speaker_label VARCHAR(50),
    corrected_at DATETIME(6),
    CONSTRAINT pk_transcript_words PRIMARY KEY (id),
    CONSTRAINT uk_transcript_words_order UNIQUE (transcription_id, word_order),
    CONSTRAINT fk_transcript_words_transcription
        FOREIGN KEY (transcription_id) REFERENCES transcriptions (id) ON DELETE CASCADE
);
