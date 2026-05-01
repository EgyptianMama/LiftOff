CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email TEXT UNIQUE NOT NULL,
                       password_hash TEXT NOT NULL,
                       created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                token_hash TEXT UNIQUE NOT NULL,
                                expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE projects (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
                          name TEXT NOT NULL,
                          repo_url TEXT NOT NULL,
                          branch TEXT NOT NULL DEFAULT 'main',
                          subdomain TEXT UNIQUE NOT NULL,
                          webhook_secret_hash TEXT NOT NULL
);

CREATE TABLE deployments (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
                             state TEXT NOT NULL DEFAULT 'QUEUED',
                             artifact_path TEXT,
                             commit_sha TEXT,
                             created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE deployment_logs (
                                 id BIGSERIAL PRIMARY KEY,
                                 deployment_id UUID REFERENCES deployments(id) ON DELETE CASCADE,
                                 line TEXT NOT NULL,
                                 logged_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE webhook_deliveries (
                                    github_delivery_id TEXT PRIMARY KEY,
                                    project_id UUID REFERENCES projects(id),
                                    received_at TIMESTAMPTZ DEFAULT NOW()
);