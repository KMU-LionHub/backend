CREATE TABLE analyses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    transcription_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    result_json LONGTEXT,
    error_message VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_analyses PRIMARY KEY (id),
    CONSTRAINT fk_analyses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_analyses_transcription FOREIGN KEY (transcription_id) REFERENCES transcriptions (id)
);

CREATE INDEX idx_analyses_user_created_at
    ON analyses (user_id, created_at);

CREATE INDEX idx_analyses_transcription
    ON analyses (transcription_id);
