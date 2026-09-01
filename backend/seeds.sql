-- =========================================================================
-- VISITOR MANAGEMENT SYSTEM - SEED DATA
-- Default sample data for instant deployment and multi-phone testing
-- =========================================================================

-- 1. Plants
INSERT INTO plants (id, name, code, location, address) VALUES
(1, 'Cyber City Technology Park - HQ', 'HQ-01', 'Sector 5, Silicon Valley', '100 Innovation Parkway, Suite 400')
ON CONFLICT (id) DO NOTHING;

-- 2. Departments
INSERT INTO departments (id, code, name, description) VALUES
(1, 'ENG', 'Software & Technology', 'Engineering, Architecture, and DevOps teams'),
(2, 'HR', 'People Operations', 'Human Resources, Talent Acquisition & Facilities'),
(3, 'OPS', 'Plant & Logistics Operations', 'Supply chain, warehouse and site logistics'),
(4, 'EXEC', 'Executive Suite & Board', 'Leadership, Investor Relations & Legal')
ON CONFLICT (id) DO NOTHING;

-- 3. Gates
INSERT INTO gates (id, plant_id, code, name, gate_type, active) VALUES
(1, 1, 'GATE-01', 'Main Security Gate (North)', 'PEDESTRIAN_VEHICLE', true),
(2, 1, 'GATE-02', 'South Visitor & VIP Gate', 'VIP', true),
(3, 1, 'GATE-03', 'Logistics & Cargo Gate', 'LOGISTICS', true)
ON CONFLICT (id) DO NOTHING;

-- 4. Users (Bootstrap Admin Only - all guards and employees must register)
-- Master Admin: swapnilshinde538@gmail.com (Password: 12345678@Ss)

INSERT INTO users (id, email, mobile, password_hash, full_name, role, active) VALUES
(1, 'swapnilshinde538@gmail.com', '9800000000', '$2a$10$aC/LncIBcYOrb5il8A8R9u2R4Dyu88cZXLIsL36JXulYPTp5koqQu', 'Swapnil Shinde (Bootstrap Admin)', 'ADMIN', true)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    mobile = EXCLUDED.mobile,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    active = EXCLUDED.active;

-- Reset sequence counters
SELECT setval('plants_id_seq', (SELECT MAX(id) FROM plants));
SELECT setval('departments_id_seq', (SELECT MAX(id) FROM departments));
SELECT setval('gates_id_seq', (SELECT MAX(id) FROM gates));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('departments_id_seq', (SELECT MAX(id) FROM departments));
SELECT setval('gates_id_seq', (SELECT MAX(id) FROM gates));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('employees_id_seq', (SELECT MAX(id) FROM employees));
SELECT setval('guards_id_seq', (SELECT MAX(id) FROM guards));
SELECT setval('appointments_id_seq', (SELECT MAX(id) FROM appointments));
