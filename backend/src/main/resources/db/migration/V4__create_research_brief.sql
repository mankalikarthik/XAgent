CREATE TABLE research_brief
(
    id UUID PRIMARY KEY,

    source_id UUID NOT NULL
        REFERENCES research_source(id)
        ON DELETE CASCADE,

    topic TEXT NOT NULL,

    summary TEXT NOT NULL,

    key_facts JSONB NOT NULL,

    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_research_brief_source_topic
    ON research_brief(source_id, topic);

CREATE INDEX idx_research_brief_created_at
    ON research_brief(created_at DESC);