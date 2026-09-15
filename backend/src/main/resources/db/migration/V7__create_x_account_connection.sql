CREATE TABLE x_account_connection
(
    id UUID PRIMARY KEY,

    x_user_id VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),

    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT,

    expires_at TIMESTAMPTZ NOT NULL,

    connected_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_x_account_connection_x_user_id
    ON x_account_connection(x_user_id);

CREATE INDEX idx_x_account_connection_username
    ON x_account_connection(username);