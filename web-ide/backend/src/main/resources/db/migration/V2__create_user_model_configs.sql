-- =============================================================================
-- V2: Create user model configuration table
-- Users can override system model provider settings with their own API keys
-- =============================================================================

CREATE TABLE user_model_configs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    api_key_encrypted VARCHAR(1024) DEFAULT '',
    base_url VARCHAR(512) DEFAULT '',
    region VARCHAR(50) DEFAULT '',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_user_model_configs_user ON user_model_configs(user_id);
