CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS compliance_cases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    wallet_address VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    risk_score INT NOT NULL,
    assignee_id UUID NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS aml_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    threshold JSONB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id UUID NULL,
    action VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO roles(name) VALUES ('ADMIN'), ('ANALYST'), ('REVIEWER') ON CONFLICT DO NOTHING;

INSERT INTO aml_rules(code, name, severity, threshold) VALUES
('BLACKLIST_EXPOSURE', 'Blacklist Exposure', 'CRITICAL', '{"counterpartyTag":"blacklist"}'),
('HIGH_FREQUENCY_TRANSFER', 'High Frequency Transfer', 'HIGH', '{"count":20,"windowMinutes":30}'),
('NEW_ADDRESS_LARGE_WITHDRAWAL', 'New Address Large Withdrawal', 'HIGH', '{"addressAgeDays":7,"amountUsd":10000}'),
('MULTI_HOP_OBFUSCATION', 'Multi-hop Obfuscation', 'MEDIUM', '{"hops":4,"windowMinutes":60}')
ON CONFLICT DO NOTHING;
