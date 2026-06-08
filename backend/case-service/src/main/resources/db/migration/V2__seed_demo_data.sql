INSERT INTO roles(name) VALUES ('ADMIN'), ('ANALYST'), ('REVIEWER') ON CONFLICT DO NOTHING;

INSERT INTO aml_rules(code, name, severity, threshold, enabled, version) VALUES
('BLACKLIST_EXPOSURE', 'Blacklist Exposure', 'CRITICAL', '{"counterpartyTag":"blacklist"}', TRUE, 1),
('HIGH_FREQUENCY_TRANSFER', 'High Frequency Transfer', 'HIGH', '{"count":20,"windowMinutes":30}', TRUE, 1),
('NEW_ADDRESS_LARGE_WITHDRAWAL', 'New Address Large Withdrawal', 'HIGH', '{"addressAgeDays":7,"amountUsd":10000}', TRUE, 1),
('LARGE_AGGREGATE_VOLUME', 'Large Aggregate Volume', 'MEDIUM', '{"totalUsd":100000}', TRUE, 1),
('MULTI_HOP_OBFUSCATION', 'Multi-hop Obfuscation', 'MEDIUM', '{"hops":4,"windowMinutes":60}', FALSE, 1)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    severity = EXCLUDED.severity,
    threshold = EXCLUDED.threshold,
    enabled = EXCLUDED.enabled,
    version = aml_rules.version,
    updated_at = NOW();

-- Demo users with BCrypt password hashes (plaintext documented in the README).
-- admin -> Admin123!  analyst -> Analyst123!  reviewer -> Reviewer123!
INSERT INTO users(username, password_hash, display_name) VALUES
('admin@chainguard.demo', '$2a$10$ww.oC7MIW8zA0hOy254qPODnBFFpuMhwC/BNnFwd/km0B5vpeX2QC', 'Demo Admin'),
('analyst@chainguard.demo', '$2a$10$nVrqhWrcJ11/cELZKQj47OMamrCTn.jl80dRFWImngvlZah0JvDDW', 'Demo Analyst'),
('reviewer@chainguard.demo', '$2a$10$6fWoieDuMjppuhOs0XjJGe7zYyUq.3CdZdEbEb8RMVntYLbQg2tbG', 'Demo Reviewer')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles(user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON TRUE
WHERE (u.username = 'admin@chainguard.demo' AND r.name IN ('ADMIN', 'ANALYST', 'REVIEWER'))
   OR (u.username = 'analyst@chainguard.demo' AND r.name = 'ANALYST')
   OR (u.username = 'reviewer@chainguard.demo' AND r.name = 'REVIEWER')
ON CONFLICT DO NOTHING;
