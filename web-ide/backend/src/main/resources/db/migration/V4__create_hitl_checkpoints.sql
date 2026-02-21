-- =============================================================================
-- V4: Create HITL (Human-In-The-Loop) checkpoints table
-- Stores human review checkpoints for SuperAgent delivery loop
-- =============================================================================

CREATE TABLE hitl_checkpoints (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    profile VARCHAR(255) NOT NULL,
    checkpoint VARCHAR(255) NOT NULL,
    deliverables TEXT DEFAULT '[]',
    baseline_results TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    feedback TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_hitl_checkpoints_session ON hitl_checkpoints(session_id);
CREATE INDEX idx_hitl_checkpoints_status ON hitl_checkpoints(status);
