-- =============================================================================
-- V3: Create execution records table
-- Stores SuperAgent execution telemetry for learning loop
-- =============================================================================

CREATE TABLE execution_records (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    profile VARCHAR(255) NOT NULL,
    skills_loaded INT DEFAULT 0,
    ooda_durations TEXT DEFAULT '{}',
    tool_calls TEXT DEFAULT '[]',
    baseline_results TEXT,
    hitl_result VARCHAR(255),
    total_duration_ms BIGINT DEFAULT 0,
    total_turns INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_execution_records_session ON execution_records(session_id);
CREATE INDEX idx_execution_records_profile ON execution_records(profile);
CREATE INDEX idx_execution_records_created_at ON execution_records(created_at);
