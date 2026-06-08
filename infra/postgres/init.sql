-- Docker entrypoint bootstrap for the ChainGuard PostgreSQL container.
-- This mirrors the case-service Flyway schema (V1/V2) so that services which
-- only READ shared tables (auth-service users, risk-engine aml_rules) work even
-- before case-service has run. case-service Flyway uses baseline-on-migrate, so
-- when these tables already exist it baselines instead of re-creating them.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS compliance_cases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    wallet_address VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    risk_score INT NOT NULL CHECK (risk_score >= 0 AND risk_score <= 100),
    assignee_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS aml_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    threshold JSONB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id UUID NULL,
    action VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO roles(name) VALUES ('ADMIN'), ('ANALYST'), ('REVIEWER') ON CONFLICT DO NOTHING;

-- MULTI_HOP_OBFUSCATION ships disabled (no evaluator yet); the others are active.
INSERT INTO aml_rules(code, name, severity, threshold, enabled) VALUES
('BLACKLIST_EXPOSURE', 'Blacklist Exposure', 'CRITICAL', '{"counterpartyTag":"blacklist"}', TRUE),
('HIGH_FREQUENCY_TRANSFER', 'High Frequency Transfer', 'HIGH', '{"count":20,"windowMinutes":30}', TRUE),
('NEW_ADDRESS_LARGE_WITHDRAWAL', 'New Address Large Withdrawal', 'HIGH', '{"addressAgeDays":7,"amountUsd":10000}', TRUE),
('LARGE_AGGREGATE_VOLUME', 'Large Aggregate Volume', 'MEDIUM', '{"totalUsd":100000}', TRUE),
('MULTI_HOP_OBFUSCATION', 'Multi-hop Obfuscation', 'MEDIUM', '{"hops":4,"windowMinutes":60}', FALSE)
ON CONFLICT DO NOTHING;

-- Demo users. Password hashes are BCrypt; plaintext is documented in the README.
-- admin    -> Admin123!     analyst -> Analyst123!     reviewer -> Reviewer123!
INSERT INTO users(username, password_hash, display_name) VALUES
('admin@chainguard.demo', '$2a$10$ww.oC7MIW8zA0hOy254qPODnBFFpuMhwC/BNnFwd/km0B5vpeX2QC', 'Demo Admin'),
('analyst@chainguard.demo', '$2a$10$nVrqhWrcJ11/cELZKQj47OMamrCTn.jl80dRFWImngvlZah0JvDDW', 'Demo Analyst'),
('reviewer@chainguard.demo', '$2a$10$6fWoieDuMjppuhOs0XjJGe7zYyUq.3CdZdEbEb8RMVntYLbQg2tbG', 'Demo Reviewer')
ON CONFLICT (username) DO NOTHING;

-- Role assignments: admin gets all three roles, analyst/reviewer get their own.
INSERT INTO user_roles(user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON TRUE
WHERE (u.username = 'admin@chainguard.demo' AND r.name IN ('ADMIN', 'ANALYST', 'REVIEWER'))
   OR (u.username = 'analyst@chainguard.demo' AND r.name = 'ANALYST')
   OR (u.username = 'reviewer@chainguard.demo' AND r.name = 'REVIEWER')
ON CONFLICT DO NOTHING;
