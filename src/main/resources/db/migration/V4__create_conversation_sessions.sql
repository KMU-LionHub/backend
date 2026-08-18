ALTER TABLE transcriptions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE transcriptions
    ADD COLUMN confirmed_at DATETIME(6);

CREATE TABLE conversation_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    context LONGTEXT,
    status VARCHAR(20) NOT NULL,
    next_utterance_order INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_conversation_sessions PRIMARY KEY (id),
    CONSTRAINT fk_conversation_sessions_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE INDEX idx_conversation_sessions_owner_updated_at
    ON conversation_sessions (owner_user_id, updated_at);

CREATE TABLE conversation_participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT,
    display_name VARCHAR(50) NOT NULL,
    participant_type VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_conversation_participants PRIMARY KEY (id),
    CONSTRAINT uk_conversation_participants_user UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_conversation_participants_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversation_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_participants_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE conversation_utterances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    transcription_id BIGINT NOT NULL,
    speaker_participant_id BIGINT NOT NULL,
    utterance_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_conversation_utterances PRIMARY KEY (id),
    CONSTRAINT uk_conversation_utterances_transcription UNIQUE (transcription_id),
    CONSTRAINT uk_conversation_utterances_order UNIQUE (conversation_id, utterance_order),
    CONSTRAINT fk_conversation_utterances_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversation_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_utterances_transcription
        FOREIGN KEY (transcription_id) REFERENCES transcriptions (id),
    CONSTRAINT fk_conversation_utterances_speaker
        FOREIGN KEY (speaker_participant_id) REFERENCES conversation_participants (id)
);
