CREATE TABLE org_invitations (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(128) NOT NULL UNIQUE,
    org_id      VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role        VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    created_by  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    used_by     VARCHAR(255),
    used_at     TIMESTAMP
);
CREATE INDEX idx_org_invitations_token ON org_invitations(token);
CREATE INDEX idx_org_invitations_org ON org_invitations(org_id);
