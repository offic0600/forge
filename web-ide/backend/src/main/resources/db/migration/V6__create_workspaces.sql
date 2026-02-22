-- =============================================================================
-- V6: Create workspaces table for workspace persistence
-- Workspace metadata persisted to DB, files stored on disk
-- =============================================================================

CREATE TABLE workspaces (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    owner VARCHAR(255) NOT NULL DEFAULT '',
    repository VARCHAR(1000),
    branch VARCHAR(255),
    local_path VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workspaces_owner ON workspaces(owner);
CREATE INDEX idx_workspaces_status ON workspaces(status);
