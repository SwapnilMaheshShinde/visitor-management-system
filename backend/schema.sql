-- =========================================================================
-- VISITOR MANAGEMENT SYSTEM (VMS) - POSTGRESQL DATABASE SCHEMA
-- Production Schema for Multi-Role Real-Time Access & Visitor Control
-- =========================================================================

-- Enable UUID extension if available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. DEPARTMENTS & PLANTS
CREATE TABLE IF NOT EXISTS departments (
    id SERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS plants (
    id SERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(32) NOT NULL UNIQUE,
    location VARCHAR(256),
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. GATES / ACCESS CHECKPOINTS
CREATE TABLE IF NOT EXISTS gates (
    id SERIAL PRIMARY KEY,
    plant_id INT REFERENCES plants(id) ON DELETE SET NULL,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    gate_type VARCHAR(32) DEFAULT 'PEDESTRIAN_VEHICLE', -- PEDESTRIAN, VEHICLE, VIP, LOGISTICS
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. USERS & ROLES
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(128) NOT NULL UNIQUE,
    mobile VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL, -- 'GUARD', 'EMPLOYEE', 'ADMIN'
    fcm_token TEXT,
    avatar_url TEXT,
    active BOOLEAN DEFAULT FALSE, -- Requires Admin approval for all new registrations
    approved_by_id INT REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. EMPLOYEE EXTENSION TABLE
CREATE TABLE IF NOT EXISTS employees (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department_id INT REFERENCES departments(id) ON DELETE SET NULL,
    employee_code VARCHAR(64) NOT NULL UNIQUE,
    designation VARCHAR(128),
    cabin_location VARCHAR(128),
    backup_approver_id INT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. GUARD EXTENSION TABLE
CREATE TABLE IF NOT EXISTS guards (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    badge_number VARCHAR(64) NOT NULL UNIQUE,
    assigned_gate_id INT REFERENCES gates(id) ON DELETE SET NULL,
    shift_type VARCHAR(32) DEFAULT 'DAY', -- DAY, NIGHT, ROTATIONAL
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. VISITORS REGISTRY (Re-usable visitor profiles)
CREATE TABLE IF NOT EXISTS visitors (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(128) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    company VARCHAR(128),
    email VARCHAR(128),
    id_proof_type VARCHAR(64) DEFAULT 'National ID', -- National ID, Driving License, Passport, Corporate ID
    id_proof_number VARCHAR(128),
    photo_url TEXT,
    vehicle_number VARCHAR(64),
    is_blacklisted BOOLEAN DEFAULT FALSE,
    blacklist_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visitors_mobile ON visitors(mobile);

-- 7. APPOINTMENTS (Pre-Registered Visits)
CREATE TABLE IF NOT EXISTS appointments (
    id SERIAL PRIMARY KEY,
    visitor_name VARCHAR(128) NOT NULL,
    visitor_mobile VARCHAR(20) NOT NULL,
    visitor_company VARCHAR(128),
    visitor_email VARCHAR(128),
    host_employee_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    department_id INT REFERENCES departments(id) ON DELETE SET NULL,
    purpose VARCHAR(256) NOT NULL,
    expected_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) DEFAULT 'SCHEDULED', -- 'SCHEDULED', 'ARRIVED', 'COMPLETED', 'CANCELLED', 'EXPIRED'
    otp_code VARCHAR(10) NOT NULL,
    qr_token VARCHAR(64) NOT NULL UNIQUE,
    otp_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    otp_used BOOLEAN DEFAULT FALSE,
    created_by_user_id INT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_appointments_otp ON appointments(otp_code);
CREATE INDEX IF NOT EXISTS idx_appointments_qr ON appointments(qr_token);
CREATE INDEX IF NOT EXISTS idx_appointments_host ON appointments(host_employee_id);

-- 8. VISIT REQUESTS (Walk-In Real-time Approval Flow)
CREATE TABLE IF NOT EXISTS visit_requests (
    id SERIAL PRIMARY KEY,
    visitor_name VARCHAR(128) NOT NULL,
    visitor_mobile VARCHAR(20) NOT NULL,
    visitor_company VARCHAR(128),
    purpose VARCHAR(256) NOT NULL,
    id_proof_type VARCHAR(64) DEFAULT 'National ID',
    id_proof_number VARCHAR(128),
    vehicle_number VARCHAR(64),
    host_employee_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    gate_id INT REFERENCES gates(id) ON DELETE SET NULL,
    guard_user_id INT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(32) DEFAULT 'PENDING', -- 'PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED'
    decision_time TIMESTAMP WITH TIME ZONE,
    decision_reason TEXT,
    meeting_room VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visit_requests_host ON visit_requests(host_employee_id);
CREATE INDEX IF NOT EXISTS idx_visit_requests_status ON visit_requests(status);

-- 9. VISITS (Active & Historic Completed Visits)
CREATE TABLE IF NOT EXISTS visits (
    id SERIAL PRIMARY KEY,
    request_id INT REFERENCES visit_requests(id) ON DELETE SET NULL,
    appointment_id INT REFERENCES appointments(id) ON DELETE SET NULL,
    visit_type VARCHAR(32) DEFAULT 'WALK_IN', -- 'WALK_IN', 'PRE_REGISTERED'
    visitor_name VARCHAR(128) NOT NULL,
    visitor_mobile VARCHAR(20) NOT NULL,
    visitor_company VARCHAR(128),
    purpose VARCHAR(256) NOT NULL,
    id_proof_type VARCHAR(64),
    id_proof_number VARCHAR(128),
    vehicle_number VARCHAR(64),
    host_employee_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    gate_in_id INT REFERENCES gates(id) ON DELETE SET NULL,
    gate_out_id INT REFERENCES gates(id) ON DELETE SET NULL,
    guard_in_id INT REFERENCES users(id) ON DELETE SET NULL,
    guard_out_id INT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(32) DEFAULT 'INSIDE', -- 'INSIDE', 'COMPLETED', 'OVERSTAY'
    entry_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exit_time TIMESTAMP WITH TIME ZONE,
    total_duration_minutes INT,
    employee_verified BOOLEAN DEFAULT FALSE,
    employee_verified_time TIMESTAMP WITH TIME ZONE,
    employee_signature_data TEXT, -- Base64 vector points or SVG signature
    verification_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visits_status ON visits(status);
CREATE INDEX IF NOT EXISTS idx_visits_host ON visits(host_employee_id);
CREATE INDEX IF NOT EXISTS idx_visits_entry_time ON visits(entry_time);

-- 10. ACCESS EVENTS & GATE AUDIT TRAIL
CREATE TABLE IF NOT EXISTS access_events (
    id SERIAL PRIMARY KEY,
    visit_id INT REFERENCES visits(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL, -- 'WALK_IN_REQUESTED', 'REQUEST_ACCEPTED', 'REQUEST_DECLINED', 'ENTRY_GRANTED', 'OTP_VERIFIED', 'QR_SCANNED', 'MEETING_VERIFIED', 'EXIT_MARKED'
    actor_user_id INT REFERENCES users(id) ON DELETE SET NULL,
    gate_id INT REFERENCES gates(id) ON DELETE SET NULL,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 11. IN-APP & PUSH NOTIFICATIONS
CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(256) NOT NULL,
    body TEXT NOT NULL,
    notification_type VARCHAR(64) NOT NULL, -- 'VISITOR_REQUEST', 'REQUEST_DECISION', 'ENTRY_CONFIRMED', 'MEETING_ALERT', 'APPOINTMENT_REMINDER'
    data_payload JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    fcm_message_id VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);

-- 12. AUDIT LOGS (Security & Compliance)
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64),
    entity_id VARCHAR(64),
    ip_address VARCHAR(64),
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
