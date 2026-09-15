ALTER TABLE content_candidate
    ADD COLUMN research_brief_id UUID;

ALTER TABLE content_candidate
    ADD CONSTRAINT fk_content_candidate_research_brief
        FOREIGN KEY (research_brief_id)
        REFERENCES research_brief(id)
        ON DELETE SET NULL;

CREATE INDEX idx_content_candidate_research_brief
    ON content_candidate(research_brief_id);