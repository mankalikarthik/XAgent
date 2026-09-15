CREATE TABLE research_source
(
    id UUID PRIMARY KEY,
    requested_url TEXT NOT NULL,
    final_url TEXT NOT NULL,
    title TEXT NOT NULL,
    extracted_text TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_research_source_hash
    ON research_source(content_hash);

CREATE INDEX idx_research_source_fetched_at
    ON research_source(fetched_at DESC);