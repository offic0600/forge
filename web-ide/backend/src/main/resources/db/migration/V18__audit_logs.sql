CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    org_id      VARCHAR(36),
    actor_id    VARCHAR(255) NOT NULL,
    action      VARCHAR(64) NOT NULL,
    target_type VARCHAR(64),
    target_id   VARCHAR(255),
    detail      TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_org ON audit_logs(org_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
