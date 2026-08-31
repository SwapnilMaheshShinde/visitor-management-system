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

-- 4. Users (Password: "password123" -> hashed with bcrypt $2a$10$w8uD.Vw7U6L3v.M9C875y.tXw081P1f64K6F35Jb98m79N5k9i6Oa or handled dynamically)
-- Sample Users:
-- Admin: admin@vms.com
-- Guard: guard@vms.com (Vikram Singh)
-- Employee 1: amit.verma@vms.com (Amit Verma)
-- Employee 2: priya.nair@vms.com (Priya Nair)

INSERT INTO users (id, email, mobile, password_hash, full_name, role, active) VALUES
(1, 'swapnilshinde538@gmail.com', '9800000000', '$2a$10$7ZzM8lXF6wE0aJ8S8GkO6.Q1J4zP8K9N3n2B1V0C9X8Z7Y6W5V4U3', 'Swapnil Shinde (Bootstrap Admin)', 'ADMIN', true),
(2, 'guard@vms.com', '9800000002', '$2a$10$nOuIs5kJ7naTuTFkBy1veuKOhs24LYfCvnMpuUFTAk5xqc6V96NOK', 'Officer Vikram Singh', 'GUARD', true),
(3, 'amit.verma@vms.com', '9876543210', '$2a$10$nOuIs5kJ7naTuTFkBy1veuKOhs24LYfCvnMpuUFTAk5xqc6V96NOK', 'Amit Verma', 'EMPLOYEE', true),
(4, 'priya.nair@vms.com', '9876500001', '$2a$10$nOuIs5kJ7naTuTFkBy1veuKOhs24LYfCvnMpuUFTAk5xqc6V96NOK', 'Priya Nair', 'EMPLOYEE', true),
(5, 'admin@vms.com', '9800000001', '$2a$10$nOuIs5kJ7naTuTFkBy1veuKOhs24LYfCvnMpuUFTAk5xqc6V96NOK', 'Administrator Alex Vance', 'ADMIN', true)
ON CONFLICT (id) DO NOTHING;

-- 5. Employee Records
INSERT INTO employees (id, user_id, department_id, employee_code, designation, cabin_location, active) VALUES
(1, 3, 1, 'EMP-1042', 'Principal Tech Lead', 'Tower A - Floor 4, Suite 402', true),
(2, 4, 2, 'EMP-1088', 'Head of Human Resources', 'Tower B - Floor 2, Room 210', true)
ON CONFLICT (id) DO NOTHING;

-- 6. Guard Records
INSERT INTO guards (id, user_id, badge_number, assigned_gate_id, shift_type, active) VALUES
(1, 2, 'SEC-8821', 1, 'DAY', true)
ON CONFLICT (id) DO NOTHING;

-- 7. Sample Pre-registered Appointment (OTP: 482910)
INSERT INTO appointments (id, visitor_name, visitor_mobile, visitor_company, visitor_email, host_employee_id, department_id, purpose, expected_date_time, status, otp_code, qr_token, otp_expires_at, otp_used) VALUES
(1, 'Rohit Sharma', '9876543210', 'Apex Solutions Ltd', 'rohit.sharma@apex.com', 3, 1, 'Quarterly Architecture Review', NOW() + INTERVAL '2 hours', 'SCHEDULED', '482910', 'VMS-APPT-482910-SEC', NOW() + INTERVAL '1 day', false)
ON CONFLICT (id) DO NOTHING;

-- Reset sequence counters
SELECT setval('plants_id_seq', (SELECT MAX(id) FROM plants));
SELECT setval('departments_id_seq', (SELECT MAX(id) FROM departments));
SELECT setval('gates_id_seq', (SELECT MAX(id) FROM gates));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('employees_id_seq', (SELECT MAX(id) FROM employees));
SELECT setval('guards_id_seq', (SELECT MAX(id) FROM guards));
SELECT setval('appointments_id_seq', (SELECT MAX(id) FROM appointments));
