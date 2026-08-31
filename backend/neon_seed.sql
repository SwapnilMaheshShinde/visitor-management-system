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

-- 4. USERS
-- Admin: swapnilshinde538@gmail.com (Password: 12345678@Ss)
-- Guard: guard@vms.com (Password: Guard@123456)
-- Employee 1: amit.verma@vms.com (Password: Emp@123456)
-- Employee 2: priya.nair@vms.com (Password: Emp@123456)

INSERT INTO users (id, email, mobile, password_hash, full_name, role, active, approved_at) VALUES
(1, 'swapnilshinde538@gmail.com', '9800000000', '$2a$10$WGqrCZDhHEzQMgVcRovT3.8F.nQSZJ7NXWBGSa1kFiNT7pgW./wpS', 'Swapnil Shinde (Bootstrap Admin)', 'ADMIN', true, CURRENT_TIMESTAMP),
(2, 'guard@vms.com', '9800000002', '$2a$10$6cDBd5IhO79mhmVuhZTS6uFwJcJUmTtt4FbHorPbvZDwoNXDru3q6', 'Officer Vikram Singh', 'GUARD', true, CURRENT_TIMESTAMP),
(3, 'amit.verma@vms.com', '9876543210', '$2a$10$zUvvtbPUk4DEw9UndL0Oh.6oURi/yYSEefgWJS5KwOm4DQmN27Y2q', 'Amit Verma', 'EMPLOYEE', true, CURRENT_TIMESTAMP),
(4, 'priya.nair@vms.com', '9876500001', '$2a$10$zUvvtbPUk4DEw9UndL0Oh.6oURi/yYSEefgWJS5KwOm4DQmN27Y2q', 'Priya Nair', 'EMPLOYEE', true, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    mobile = EXCLUDED.mobile,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    active = EXCLUDED.active,
    approved_at = EXCLUDED.approved_at;

-- 5. EMPLOYEES EXTENSION
INSERT INTO employees (id, user_id, department_id, employee_code, designation, cabin_location) VALUES
(1, 3, 1, 'EMP-1042', 'Principal Tech Lead', 'Tower A - Floor 4, Suite 402'),
(2, 4, 2, 'EMP-1088', 'Head of Human Resources', 'Tower B - Floor 2, Room 210')
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    department_id = EXCLUDED.department_id,
    employee_code = EXCLUDED.employee_code,
    designation = EXCLUDED.designation,
    cabin_location = EXCLUDED.cabin_location;

-- 6. GUARDS EXTENSION
INSERT INTO guards (id, user_id, badge_number, assigned_gate_id, shift_type) VALUES
(1, 2, 'SEC-8821', 1, 'DAY')
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    badge_number = EXCLUDED.badge_number,
    assigned_gate_id = EXCLUDED.assigned_gate_id,
    shift_type = EXCLUDED.shift_type;

-- 7. RESET POSTGRESQL SEQUENCES SAFELY
SELECT setval('plants_id_seq', COALESCE((SELECT MAX(id) FROM plants), 1));
SELECT setval('departments_id_seq', COALESCE((SELECT MAX(id) FROM departments), 1));
SELECT setval('gates_id_seq', COALESCE((SELECT MAX(id) FROM gates), 1));
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval('employees_id_seq', COALESCE((SELECT MAX(id) FROM employees), 1));
SELECT setval('guards_id_seq', COALESCE((SELECT MAX(id) FROM guards), 1));
