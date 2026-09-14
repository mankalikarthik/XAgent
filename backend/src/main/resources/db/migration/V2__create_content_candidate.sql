CREATE TABLE content_candidate
(
    id UUID PRIMARY KEY,

    topic VARCHAR(500) NOT NULL,
    tone VARCHAR(255) NOT NULL,

    content TEXT NOT NULL,

    status VARCHAR(50) NOT NULL,

    technical_accuracy INTEGER NOT NULL,
    clarity INTEGER NOT NULL,
    naturalness INTEGER NOT NULL,
    engagement INTEGER NOT NULL,
    overall_score INTEGER NOT NULL,

    issues TEXT,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_content_candidate_created_at
    ON content_candidate(created_at DESC);

CREATE INDEX idx_content_candidate_status
    ON content_candidate(status);