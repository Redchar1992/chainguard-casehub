INSERT INTO roles(name) VALUES ('ADMIN'), ('ANALYST'), ('REVIEWER') ON CONFLICT DO NOTHING;

INSERT INTO aml_rules(code, name, severity, threshold, enabled, version) VALUES
('BLACKLIST_EXPOSURE', 'Blacklist Exposure', 'CRITICAL', '{"counterpartyTag":"blacklist"}', TRUE, 1),
('HIGH_FREQUENCY_TRANSFER', 'High Frequency Transfer', 'HIGH', '{"count":20,"windowMinutes":30}', TRUE, 1),
('NEW_ADDRESS_LARGE_WITHDRAWAL', 'New Address Large Withdrawal', 'HIGH', '{"addressAgeDays":7,"amountUsd":10000}', TRUE, 1),
('MULTI_HOP_OBFUSCATION', 'Multi-hop Obfuscation', 'MEDIUM', '{"hops":4,"windowMinutes":60}', FALSE, 1)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    severity = EXCLUDED.severity,
    threshold = EXCLUDED.threshold,
    enabled = EXCLUDED.enabled,
    version = aml_rules.version,
    updated_at = NOW();
