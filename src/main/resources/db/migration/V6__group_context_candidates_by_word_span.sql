ALTER TABLE context_analyses
    DROP CONSTRAINT ck_context_analyses_candidate_count;

ALTER TABLE context_analyses
    RENAME COLUMN candidate_count TO requested_candidate_count;

ALTER TABLE context_analyses
    ADD CONSTRAINT ck_context_analyses_requested_candidate_count
        CHECK (requested_candidate_count BETWEEN 2 AND 5);

ALTER TABLE context_analyses
    ADD COLUMN ambiguity_count INT NOT NULL DEFAULT 0;

CREATE TABLE context_ambiguities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL,
    ambiguity_order INT NOT NULL,
    excerpt LONGTEXT NOT NULL,
    start_word_id BIGINT,
    end_word_id BIGINT,
    start_word_order INT,
    end_word_order INT,
    candidate_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_context_ambiguities PRIMARY KEY (id),
    CONSTRAINT ck_context_ambiguities_order CHECK (ambiguity_order >= 1),
    CONSTRAINT ck_context_ambiguities_candidate_count CHECK (candidate_count BETWEEN 2 AND 5),
    CONSTRAINT ck_context_ambiguities_word_order CHECK (
        (start_word_order IS NULL AND end_word_order IS NULL)
        OR (start_word_order >= 0 AND end_word_order >= start_word_order)
    ),
    CONSTRAINT uk_context_ambiguities_order UNIQUE (analysis_id, ambiguity_order),
    CONSTRAINT fk_context_ambiguities_analysis
        FOREIGN KEY (analysis_id) REFERENCES context_analyses (id),
    CONSTRAINT fk_context_ambiguities_start_word
        FOREIGN KEY (start_word_id) REFERENCES transcript_words (id),
    CONSTRAINT fk_context_ambiguities_end_word
        FOREIGN KEY (end_word_id) REFERENCES transcript_words (id)
);

CREATE INDEX idx_context_ambiguities_analysis
    ON context_ambiguities (analysis_id);

INSERT INTO context_ambiguities (
    analysis_id,
    ambiguity_order,
    excerpt,
    candidate_count,
    created_at
)
SELECT
    id,
    1,
    source_current_text,
    requested_candidate_count,
    created_at
FROM context_analyses;

UPDATE context_analyses
SET ambiguity_count = 1;

ALTER TABLE context_analysis_selections
    RENAME TO legacy_context_analysis_selections;

ALTER TABLE context_candidates
    RENAME TO legacy_context_candidates;

CREATE TABLE context_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ambiguity_id BIGINT NOT NULL,
    candidate_rank INT NOT NULL,
    interpretation LONGTEXT NOT NULL,
    inferred_intent LONGTEXT NOT NULL,
    rationale LONGTEXT NOT NULL,
    intent_similarity_score DECIMAL(5, 4) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_context_candidates_v2 PRIMARY KEY (id),
    CONSTRAINT ck_context_candidates_rank_v2 CHECK (candidate_rank >= 1),
    CONSTRAINT ck_context_candidates_score_v2 CHECK (intent_similarity_score BETWEEN 0 AND 1),
    CONSTRAINT uk_context_candidates_rank_v2 UNIQUE (ambiguity_id, candidate_rank),
    CONSTRAINT fk_context_candidates_ambiguity_v2
        FOREIGN KEY (ambiguity_id) REFERENCES context_ambiguities (id)
);

INSERT INTO context_candidates (
    id,
    ambiguity_id,
    candidate_rank,
    interpretation,
    inferred_intent,
    rationale,
    intent_similarity_score,
    created_at
)
SELECT
    candidate.id,
    ambiguity.id,
    candidate.candidate_rank,
    candidate.interpretation,
    candidate.inferred_intent,
    candidate.rationale,
    candidate.intent_similarity_score,
    candidate.created_at
FROM legacy_context_candidates candidate
JOIN context_ambiguities ambiguity
    ON ambiguity.analysis_id = candidate.analysis_id;

CREATE TABLE context_analysis_selections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ambiguity_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    original_candidate_text LONGTEXT NOT NULL,
    final_text LONGTEXT NOT NULL,
    selected_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_context_analysis_selections_v2 PRIMARY KEY (id),
    CONSTRAINT uk_context_analysis_selections_ambiguity_v2 UNIQUE (ambiguity_id),
    CONSTRAINT fk_context_analysis_selections_ambiguity_v2
        FOREIGN KEY (ambiguity_id) REFERENCES context_ambiguities (id),
    CONSTRAINT fk_context_analysis_selections_candidate_v2
        FOREIGN KEY (candidate_id) REFERENCES context_candidates (id)
);

INSERT INTO context_analysis_selections (
    id,
    ambiguity_id,
    candidate_id,
    original_candidate_text,
    final_text,
    selected_at,
    updated_at,
    version
)
SELECT
    selection.id,
    ambiguity.id,
    selection.candidate_id,
    selection.original_candidate_text,
    selection.final_text,
    selection.selected_at,
    selection.updated_at,
    selection.version
FROM legacy_context_analysis_selections selection
JOIN context_ambiguities ambiguity
    ON ambiguity.analysis_id = selection.analysis_id;

DROP TABLE legacy_context_analysis_selections;
DROP TABLE legacy_context_candidates;
