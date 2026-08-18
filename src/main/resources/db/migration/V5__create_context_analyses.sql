CREATE TABLE context_analyses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    utterance_id BIGINT NOT NULL,
    transcription_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    source_speaker_name VARCHAR(50),
    conversation_context LONGTEXT,
    source_original_text LONGTEXT NOT NULL,
    source_current_text LONGTEXT NOT NULL,
    candidate_count INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_context_analyses PRIMARY KEY (id),
    CONSTRAINT ck_context_analyses_candidate_count CHECK (candidate_count BETWEEN 2 AND 5),
    CONSTRAINT fk_context_analyses_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversation_sessions (id),
    CONSTRAINT fk_context_analyses_utterance
        FOREIGN KEY (utterance_id) REFERENCES conversation_utterances (id),
    CONSTRAINT fk_context_analyses_transcription
        FOREIGN KEY (transcription_id) REFERENCES transcriptions (id)
);

CREATE INDEX idx_context_analyses_utterance_created_at
    ON context_analyses (utterance_id, created_at);

CREATE TABLE context_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL,
    candidate_rank INT NOT NULL,
    interpretation LONGTEXT NOT NULL,
    inferred_intent LONGTEXT NOT NULL,
    rationale LONGTEXT NOT NULL,
    intent_similarity_score DECIMAL(5, 4) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_context_candidates PRIMARY KEY (id),
    CONSTRAINT ck_context_candidates_rank CHECK (candidate_rank >= 1),
    CONSTRAINT ck_context_candidates_score CHECK (intent_similarity_score BETWEEN 0 AND 1),
    CONSTRAINT uk_context_candidates_rank UNIQUE (analysis_id, candidate_rank),
    CONSTRAINT fk_context_candidates_analysis
        FOREIGN KEY (analysis_id) REFERENCES context_analyses (id)
);

CREATE TABLE context_analysis_selections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    original_candidate_text LONGTEXT NOT NULL,
    final_text LONGTEXT NOT NULL,
    selected_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_context_analysis_selections PRIMARY KEY (id),
    CONSTRAINT uk_context_analysis_selections_analysis UNIQUE (analysis_id),
    CONSTRAINT fk_context_analysis_selections_analysis
        FOREIGN KEY (analysis_id) REFERENCES context_analyses (id),
    CONSTRAINT fk_context_analysis_selections_candidate
        FOREIGN KEY (candidate_id) REFERENCES context_candidates (id)
);
