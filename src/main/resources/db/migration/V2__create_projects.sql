-- =============================================================
-- V2: Projects table
-- =============================================================

CREATE TABLE projects (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id              UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name                  VARCHAR(100) NOT NULL,
    slug                  VARCHAR(100) NOT NULL UNIQUE,
    github_repo_url       VARCHAR(500) NOT NULL,
    github_webhook_secret VARCHAR(255) NOT NULL,
    branch                VARCHAR(100) NOT NULL DEFAULT 'main',
    subdomain             VARCHAR(200) NOT NULL UNIQUE,
    custom_domain         VARCHAR(255),
    env_variables         JSONB                 DEFAULT '{}',
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_owner ON projects (owner_id);
CREATE INDEX idx_projects_slug  ON projects (slug);
