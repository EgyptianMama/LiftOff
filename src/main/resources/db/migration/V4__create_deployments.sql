-- =============================================================
-- V4: Deployments table
-- =============================================================

CREATE TABLE deployments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    build_id      UUID         NOT NULL REFERENCES builds (id) ON DELETE CASCADE,
    project_id    UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    artifact_path VARCHAR(500) NOT NULL,
    is_live       BOOLEAN      NOT NULL DEFAULT false,
    url           VARCHAR(300),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_deployments_project ON deployments (project_id);

-- Partial index: quickly find the live deployment for a project
CREATE UNIQUE INDEX idx_deployments_live
    ON deployments (project_id)
    WHERE is_live = true;
