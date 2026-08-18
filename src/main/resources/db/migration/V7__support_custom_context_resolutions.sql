ALTER TABLE context_analysis_selections
    ADD COLUMN resolution_type VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE';

ALTER TABLE context_analysis_selections
    MODIFY COLUMN candidate_id BIGINT NULL;

ALTER TABLE context_analysis_selections
    MODIFY COLUMN original_candidate_text LONGTEXT NULL;

ALTER TABLE context_analysis_selections
    MODIFY COLUMN final_text LONGTEXT NULL;

ALTER TABLE context_analysis_selections
    ADD CONSTRAINT ck_context_analysis_selections_resolution CHECK (
        (
            resolution_type = 'CANDIDATE'
            AND candidate_id IS NOT NULL
            AND original_candidate_text IS NOT NULL
            AND final_text IS NOT NULL
        )
        OR (
            resolution_type = 'CUSTOM'
            AND candidate_id IS NULL
            AND original_candidate_text IS NULL
            AND final_text IS NOT NULL
        )
        OR (
            resolution_type = 'DISMISSED'
            AND candidate_id IS NULL
            AND original_candidate_text IS NULL
            AND final_text IS NULL
        )
    );
