-- =========================================================================
-- VISITOR MANAGEMENT SYSTEM (VMS) - PRODUCTION NEON SEED SCRIPT
-- Exactly compatible with schema.sql
-- =========================================================================

-- 1. PLANTS
INSERT INTO plants (id, name, code, location, address) VALUES
(1, 'Cyber City Technology Park - HQ', 'HQ-01', 'Sector 5, Silicon Valley', '100 Innovation Parkway, Suite 400')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    location = EXCLUDED.location,
    address = EXCLUDED.address;

-- 2. DEPARTMENTS
INSERT INTO departments (id, code, name, description) VALUES
(1, 'ENG', 'Software & Technology', 'Engineering, Architecture, and DevOps teams'),
(2, 'HR', 'People Operations', 'Human Resources, Talent Acquisition & Facilities'),
(3, 'OPS', 'Plant & Logistics Operations', 'Supply chain, warehouse and site logistics'),
(4, 'EXEC', 'Executive Suite & Board', 'Leadership, Investor Relations & Legal')
ON CONFLICT (id) DO UPDATE SET
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 3. GATES
INSERT INTO gates (id, plant_id, code, name, gate_type, active) VALUES
(1, 1, 'GATE-01', 'Main Security Gate (North)', 'PEDESTRIAN_VEHICLE', true),
(2, 1, 'GATE-02', 'South Visitor & VIP Gate', 'VIP', true),
(3, 1, 'GATE-03', 'Logistics & Cargo Gate', 'LOGISTICS', true)
ON CONFLICT (id) DO UPDATE SET
    plant_id = EXCLUDED.plant_id,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    gate_type = EXCLUDED.gate_type,
    active = EXCLUDED.active;

-- 4. USERS (Bootstrap Admin Only - all guards and employees must register)
-- Admin: swapnilshinde538@gmail.com (Password: 12345678@Ss)

INSERT INTO users (id, email, mobile, password_hash, full_name, role, active, approved_at) VALUES
(1, 'swapnilshinde538@gmail.com', '9800000000', '$2a$10$aC/LncIBcYOrb5il8A8R9u2R4Dyu88cZXLIsL36JXulYPTp5koqQu', 'Swapnil Shinde (Bootstrap Admin)', 'ADMIN', true, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    mobile = EXCLUDED.mobile,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    active = EXCLUDED.active,
    approved_at = EXCLUDED.approved_at;

-- 5. RESET POSTGRESQL SEQUENCES SAFELY
SELECT setval('plants_id_seq', COALESCE((SELECT MAX(id) FROM plants), 1));
SELECT setval('departments_id_seq', COALESCE((SELECT MAX(id) FROM departments), 1));
SELECT setval('gates_id_seq', COALESCE((SELECT MAX(id) FROM gates), 1));
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval('employees_id_seq', COALESCE((SELECT MAX(id) FROM employees), 1));
SELECT setval('guards_id_seq', COALESCE((SELECT MAX(id) FROM guards), 1));
