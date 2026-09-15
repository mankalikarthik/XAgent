ALTER TABLE content_candidate
    ADD COLUMN external_id VARCHAR(255);

ALTER TABLE content_candidate
    ADD COLUMN external_url TEXT;

ALTER TABLE content_candidate
    ADD COLUMN published_at TIMESTAMPTZ;

ALTER TABLE content_candidate
    ADD COLUMN publisher VARCHAR(100);

CREATE INDEX idx_content_candidate_external_id
    ON content_candidate(external_id);

CREATE INDEX idx_content_candidate_published_at
    ON content_candidate(published_at DESC);