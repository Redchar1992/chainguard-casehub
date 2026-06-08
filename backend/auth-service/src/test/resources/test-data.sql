-- Seed an analyst user. Password hash is BCrypt for 'Analyst123!'.
INSERT INTO users(id, password_hash, username, display_name) VALUES
('22222222-2222-2222-2222-222222222222',
 '$2a$10$nVrqhWrcJ11/cELZKQj47OMamrCTn.jl80dRFWImngvlZah0JvDDW',
 'analyst@chainguard.demo', 'Demo Analyst');

INSERT INTO roles(id, name) VALUES
('33333333-3333-3333-3333-333333333333', 'ANALYST');

INSERT INTO user_roles(user_id, role_id) VALUES
('22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333');
