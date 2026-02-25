-- Knowledge extraction logs: tracks AI-driven knowledge extraction jobs
CREATE TABLE knowledge_extraction_logs (
    id VARCHAR(50) PRIMARY KEY,
    job_id VARCHAR(50) NOT NULL,
    workspace_id VARCHAR(100) NOT NULL DEFAULT '',
    tag_id VARCHAR(50) NOT NULL,
    tag_name VARCHAR(200) NOT NULL,
    phase VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    applicable BOOLEAN NOT NULL DEFAULT TRUE,
    reason TEXT,
    content_length INT NOT NULL DEFAULT 0,
    tokens_used INT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    model_used VARCHAR(100),
    source_files TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ext_logs_job ON knowledge_extraction_logs(job_id);
CREATE INDEX idx_ext_logs_tag ON knowledge_extraction_logs(tag_id);
CREATE INDEX idx_ext_logs_created ON knowledge_extraction_logs(created_at DESC);

-- Initialize empty tags to 'empty' status
UPDATE knowledge_tags SET status = 'empty' WHERE content = '' OR content IS NULL;
