CREATE TABLE stored_file (
    id            UUID                     PRIMARY KEY,
    original_name VARCHAR(255)             NOT NULL,
    content_type  VARCHAR(100)             NOT NULL,
    size_bytes    BIGINT                   NOT NULL,
    uploaded_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_stored_file_uploaded_at ON stored_file (uploaded_at);
