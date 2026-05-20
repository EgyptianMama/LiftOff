-- =============================================================
-- V3: Builds table
-- =============================================================

CREATE TABLE builds (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    commit_sha          VARCHAR(40),
    commit_message      TEXT,
    branch              VARCHAR(100),
    status              VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
    framework_detected  VARCHAR(50),
    build_command       VARCHAR(500),
    output_dir          VARCHAR(200),
    log_text            TEXT,
    github_delivery_id  VARCHAR(100) UNIQUE,
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_builds_project ON builds (project_id);
CREATE INDEX idx_builds_status  ON builds (status);
