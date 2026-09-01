// =========================================================================
// VISITOR MANAGEMENT SYSTEM (VMS) - PRODUCTION BACKEND SERVER
// PostgreSQL First-Class Cloud Database + JWT Auth + FCM Push Engine
// =========================================================================

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { sendPushNotification, sendTopicNotification, sendVisitorArrivalPush, isFcmInitialized } = require('./fcmService');

const app = express();
const PORT = process.env.PORT || 5000;
const JWT_SECRET = process.env.JWT_SECRET || 'vms_enterprise_production_jwt_secret_2026';

app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Bootstrap Admin credentials as mandated
const BOOTSTRAP_ADMIN = {
    email: 'swapnilshinde538@gmail.com',
    mobile: '9800000000',
    password: '12345678@Ss',
    name: 'Swapnil Shinde (Bootstrap Admin)',
    role: 'ADMIN',
    active: true
};

// =========================================================================
// POSTGRESQL CONNECTION POOL (NEON / MANAGED CLOUD POSTGRES)
// =========================================================================

const dbConnectionString = process.env.DATABASE_URL || process.env.DATABASE_URL_POOLED;

if (!dbConnectionString && !process.env.DB_HOST) {
    console.error('[DB FATAL] DATABASE_URL is not set. Please provide a valid PostgreSQL connection string in the environment.');
}

const poolConfig = dbConnectionString
    ? {
        connectionString: dbConnectionString,
        ssl: {
            rejectUnauthorized: false
        },
        max: 20,
        idleTimeoutMillis: 30000,
        connectionTimeoutMillis: 10000
    }
    : {
        host: process.env.DB_HOST || 'localhost',
        port: parseInt(process.env.DB_PORT || '5432', 10),
        user: process.env.DB_USER || 'postgres',
        password: process.env.DB_PASSWORD || 'postgres',
        database: process.env.DB_NAME || 'vms_db',
        ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false
    };

const pgPool = new Pool(poolConfig);

pgPool.on('error', (err) => {
    console.error('[DB] Unexpected error on idle PostgreSQL client:', err.message);
});

// Auto-verify database connectivity and Bootstrap Admin + Master Seed Data on startup
async function initDatabaseBootstrap() {
    let client;
    try {
        client = await pgPool.connect();
        console.log('[DB] PostgreSQL connected successfully to Neon/Cloud cluster.');

        // 1. Schema Migrations: Ensure all core tables exist
        await client.query(`
            CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

            CREATE TABLE IF NOT EXISTS plants (
                id SERIAL PRIMARY KEY,
                name VARCHAR(128) NOT NULL,
                code VARCHAR(32) NOT NULL UNIQUE,
                location VARCHAR(256),
                address TEXT,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS departments (
                id SERIAL PRIMARY KEY,
                code VARCHAR(32) NOT NULL UNIQUE,
                name VARCHAR(128) NOT NULL,
                description TEXT,
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS gates (
                id SERIAL PRIMARY KEY,
                plant_id INT REFERENCES plants(id) ON DELETE SET NULL,
                code VARCHAR(32) NOT NULL UNIQUE,
                name VARCHAR(128) NOT NULL,
                gate_type VARCHAR(32) DEFAULT 'PEDESTRIAN_VEHICLE',
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                email VARCHAR(128) NOT NULL UNIQUE,
                mobile VARCHAR(20) NOT NULL UNIQUE,
                password_hash VARCHAR(256) NOT NULL,
                full_name VARCHAR(128) NOT NULL,
                role VARCHAR(32) NOT NULL,
                fcm_token TEXT,
                avatar_url TEXT,
                active BOOLEAN DEFAULT FALSE,
                approved_by_id INT REFERENCES users(id) ON DELETE SET NULL,
                approved_at TIMESTAMP WITH TIME ZONE,
                last_login TIMESTAMP WITH TIME ZONE,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS employees (
                id SERIAL PRIMARY KEY,
                user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                department_id INT REFERENCES departments(id) ON DELETE SET NULL,
                employee_code VARCHAR(64) NOT NULL UNIQUE,
                designation VARCHAR(128),
                cabin_location VARCHAR(128),
                backup_approver_id INT REFERENCES users(id) ON DELETE SET NULL,
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS guards (
                id SERIAL PRIMARY KEY,
                user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                badge_number VARCHAR(64) NOT NULL UNIQUE,
                assigned_gate_id INT REFERENCES gates(id) ON DELETE SET NULL,
                shift_type VARCHAR(32) DEFAULT 'DAY',
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS visitors (
                id SERIAL PRIMARY KEY,
                full_name VARCHAR(128) NOT NULL,
                mobile VARCHAR(20) NOT NULL,
                company VARCHAR(128),
                email VARCHAR(128),
                id_proof_type VARCHAR(64) DEFAULT 'National ID',
                id_proof_number VARCHAR(128),
                photo_url TEXT,
                vehicle_number VARCHAR(64),
                is_blacklisted BOOLEAN DEFAULT FALSE,
                blacklist_reason TEXT,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

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
                status VARCHAR(32) DEFAULT 'SCHEDULED',
                otp_code VARCHAR(10) NOT NULL,
                qr_token VARCHAR(64) NOT NULL UNIQUE,
                otp_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                otp_used BOOLEAN DEFAULT FALSE,
                created_by_user_id INT REFERENCES users(id) ON DELETE SET NULL,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

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
                status VARCHAR(32) DEFAULT 'PENDING',
                decision_time TIMESTAMP WITH TIME ZONE,
                decision_reason TEXT,
                meeting_room VARCHAR(128),
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS visits (
                id SERIAL PRIMARY KEY,
                request_id INT REFERENCES visit_requests(id) ON DELETE SET NULL,
                appointment_id INT REFERENCES appointments(id) ON DELETE SET NULL,
                visit_type VARCHAR(32) DEFAULT 'WALK_IN',
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
                status VARCHAR(32) DEFAULT 'INSIDE',
                entry_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                exit_time TIMESTAMP WITH TIME ZONE,
                total_duration_minutes INT,
                employee_verified BOOLEAN DEFAULT FALSE,
                employee_verified_time TIMESTAMP WITH TIME ZONE,
                employee_signature_data TEXT,
                verification_notes TEXT,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS access_events (
                id SERIAL PRIMARY KEY,
                visit_id INT REFERENCES visits(id) ON DELETE CASCADE,
                event_type VARCHAR(64) NOT NULL,
                actor_user_id INT REFERENCES users(id) ON DELETE SET NULL,
                gate_id INT REFERENCES gates(id) ON DELETE SET NULL,
                description TEXT,
                metadata JSONB,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS notifications (
                id SERIAL PRIMARY KEY,
                user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                title VARCHAR(256) NOT NULL,
                body TEXT NOT NULL,
                notification_type VARCHAR(64) NOT NULL,
                data_payload JSONB,
                is_read BOOLEAN DEFAULT FALSE,
                fcm_message_id VARCHAR(128),
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

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

            CREATE TABLE IF NOT EXISTS user_fcm_tokens (
                id SERIAL PRIMARY KEY,
                user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                fcm_token TEXT NOT NULL UNIQUE,
                device_name VARCHAR(128),
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );
            CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_user ON user_fcm_tokens(user_id);
        `);

        // Migration safety for missing columns on existing tables
        await client.query(`
            ALTER TABLE departments ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
            ALTER TABLE gates ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
        `).catch(() => {});

        // 2. Plants seed
        await client.query(`
            INSERT INTO plants (id, name, code, location, address)
            VALUES (1, 'Cyber City Technology Park - HQ', 'HQ-01', 'Sector 5, Silicon Valley', '100 Innovation Parkway, Suite 400')
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                code = EXCLUDED.code,
                location = EXCLUDED.location,
                address = EXCLUDED.address
        `);

        // 3. Departments seed
        await client.query(`
            INSERT INTO departments (id, code, name, description, active) VALUES
            (1, 'ENG', 'Software & Technology', 'Engineering, Architecture, and DevOps teams', true),
            (2, 'HR', 'People Operations', 'Human Resources, Talent Acquisition & Facilities', true),
            (3, 'OPS', 'Plant & Logistics Operations', 'Supply chain, warehouse and site logistics', true),
            (4, 'EXEC', 'Executive Suite & Board', 'Leadership, Investor Relations & Legal', true)
            ON CONFLICT (id) DO UPDATE SET
                code = EXCLUDED.code,
                name = EXCLUDED.name,
                description = EXCLUDED.description,
                active = EXCLUDED.active
        `);

        // 4. Gates seed
        await client.query(`
            INSERT INTO gates (id, plant_id, code, name, gate_type, active) VALUES
            (1, 1, 'GATE-01', 'Main Security Gate (North)', 'PEDESTRIAN_VEHICLE', true),
            (2, 1, 'GATE-02', 'South Visitor & VIP Gate', 'VIP', true),
            (3, 1, 'GATE-03', 'Logistics & Cargo Gate', 'LOGISTICS', true)
            ON CONFLICT (id) DO UPDATE SET
                plant_id = EXCLUDED.plant_id,
                code = EXCLUDED.code,
                name = EXCLUDED.name,
                gate_type = EXCLUDED.gate_type,
                active = EXCLUDED.active
        `);

        // 5. Seed ONLY Bootstrap Admin (Swapnil Shinde)
        const adminSpecialHash = bcrypt.hashSync(BOOTSTRAP_ADMIN.password, 10);

        await client.query(
            `INSERT INTO users (email, mobile, password_hash, full_name, role, active, approved_at)
             VALUES ($1, $2, $3, $4, $5, true, NOW())
             ON CONFLICT (email) DO UPDATE SET
                mobile = EXCLUDED.mobile,
                password_hash = EXCLUDED.password_hash,
                full_name = EXCLUDED.full_name,
                role = 'ADMIN',
                active = true,
                approved_at = NOW()`,
            [BOOTSTRAP_ADMIN.email, BOOTSTRAP_ADMIN.mobile, adminSpecialHash, BOOTSTRAP_ADMIN.name, 'ADMIN']
        );

        // Reset sequence counters safely
        await client.query(`
            SELECT setval('plants_id_seq', COALESCE((SELECT MAX(id) FROM plants), 1));
            SELECT setval('departments_id_seq', COALESCE((SELECT MAX(id) FROM departments), 1));
            SELECT setval('gates_id_seq', COALESCE((SELECT MAX(id) FROM gates), 1));
            SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1));
            SELECT setval('employees_id_seq', COALESCE((SELECT MAX(id) FROM employees), 1));
            SELECT setval('guards_id_seq', COALESCE((SELECT MAX(id) FROM guards), 1));
        `).catch(() => {});

        console.log('[DB] Bootstrap Admin (' + BOOTSTRAP_ADMIN.email + ') & Master Metadata initialized. All other users must register.');
    } catch (err) {
        console.error('[DB FATAL] Error connecting to PostgreSQL or running seed bootstrap:', err.message);
    } finally {
        if (client) client.release();
    }
}

initDatabaseBootstrap();

// Audit log helper
async function logAudit(userId, action, entityType, entityId, details, ip = '0.0.0.0') {
    try {
        await pgPool.query(
            `INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address)
             VALUES ($1, $2, $3, $4, $5, $6)`,
            [userId || null, action, entityType, String(entityId), details, ip]
        );
    } catch (e) {
        console.error('[AUDIT] Failed to persist audit log:', e.message);
    }
}

// =========================================================================
// AUTHENTICATION & AUTHORIZATION MIDDLEWARE
// =========================================================================

function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ error: 'Access denied: Bearer Authorization token required' });
    }

    jwt.verify(token, JWT_SECRET, (err, decodedUser) => {
        if (err) {
            return res.status(403).json({ error: 'Invalid or expired session token. Please log in again.' });
        }
        req.user = decodedUser;
        next();
    });
}

function requireAdmin(req, res, next) {
    if (!req.user || req.user.role !== 'ADMIN') {
        return res.status(403).json({ error: 'Administrative privileges required for this action.' });
    }
    next();
}

// =========================================================================
// HEALTH CHECKS & DIAGNOSTICS
// =========================================================================

const handleHealthCheck = async (req, res) => {
    let dbStatus = 'DISCONNECTED';
    let dbTime = null;
    let dbError = null;

    try {
        const result = await pgPool.query('SELECT NOW() as db_time');
        dbStatus = 'CONNECTED';
        dbTime = result.rows[0].db_time;
    } catch (err) {
        dbError = err.message;
    }

    const statusCode = dbStatus === 'CONNECTED' ? 200 : 503;

    res.status(statusCode).json({
        status: dbStatus === 'CONNECTED' ? 'HEALTHY' : 'UNHEALTHY',
        serverTime: new Date().toISOString(),
        database: {
            provider: 'PostgreSQL (Neon)',
            status: dbStatus,
            time: dbTime,
            error: dbError
        },
        fcm: {
            initialized: isFcmInitialized()
        },
        bootstrapAdmin: BOOTSTRAP_ADMIN.email,
        version: '2.0.0-PROD'
    });
};

app.get('/health', handleHealthCheck);
app.get('/api/health', handleHealthCheck);

// =========================================================================
// 1. AUTHENTICATION & REGISTRATION
// =========================================================================

// User Registration (Pending Admin Approval)
app.post('/api/auth/register', async (req, res) => {
    try {
        const { email, mobile, password, name, role, employeeCode, departmentId, designation, badgeNumber, gateId } = req.body;

        if (!email || !mobile || !password || !name || !role) {
            return res.status(400).json({ error: 'Email, Mobile, Password, Name, and Role are mandatory.' });
        }

        const cleanEmail = email.trim().toLowerCase();
        const cleanMobile = mobile.trim();
        const upperRole = role.trim().toUpperCase();

        if (!['GUARD', 'EMPLOYEE', 'ADMIN'].includes(upperRole)) {
            return res.status(400).json({ error: 'Invalid role. Must be GUARD, EMPLOYEE, or ADMIN.' });
        }

        // Check if user already exists
        const existing = await pgPool.query(
            `SELECT id, email, mobile FROM users WHERE email = $1 OR mobile = $2`,
            [cleanEmail, cleanMobile]
        );

        if (existing.rows.length > 0) {
            return res.status(409).json({ error: 'An account with this email or mobile number already exists in the central database.' });
        }

        const passwordHash = bcrypt.hashSync(password, 10);
        const isActive = false; // New registrations MUST require Admin approval

        const insertUser = await pgPool.query(
            `INSERT INTO users (email, mobile, password_hash, full_name, role, active, created_at)
             VALUES ($1, $2, $3, $4, $5, $6, NOW()) RETURNING id`,
            [cleanEmail, cleanMobile, passwordHash, name.trim(), upperRole, isActive]
        );

        const newUserId = insertUser.rows[0].id;

        // Insert role-specific profile data if applicable
        if (upperRole === 'EMPLOYEE') {
            await pgPool.query(
                `INSERT INTO employees (user_id, department_id, employee_code, designation, cabin_location)
                 VALUES ($1, $2, $3, $4, $5)`,
                [newUserId, Number(departmentId || 1), employeeCode || `EMP-${newUserId + 1000}`, designation || 'Staff Member', 'Main Facility']
            );
        } else if (upperRole === 'GUARD') {
            await pgPool.query(
                `INSERT INTO guards (user_id, badge_number, assigned_gate_id, shift_type)
                 VALUES ($1, $2, $3, $4)`,
                [newUserId, badgeNumber || `SEC-${newUserId + 8000}`, Number(gateId || 1), 'GENERAL']
            );
        }

        await logAudit(
            newUserId,
            'USER_REGISTERED_PENDING_APPROVAL',
            'users',
            newUserId,
            `New ${upperRole} registration submitted for ${name.trim()} (${cleanEmail}). Pending Admin approval.`,
            req.ip
        );

        // Notify active Admins via topic
        await sendTopicNotification(
            'admins',
            `New ${upperRole} Registration Pending`,
            `${name.trim()} (${cleanEmail}) has requested ${upperRole} access. Please review and approve in Admin Portal.`,
            { userId: String(newUserId), role: upperRole, type: 'PENDING_REGISTRATION' }
        );

        res.status(201).json({
            success: true,
            active: false,
            message: 'Registration submitted successfully! Your account is pending Administrator approval before you can sign in.',
            userId: newUserId
        });
    } catch (err) {
        console.error('Registration error:', err);
        res.status(500).json({ error: 'Database error processing registration: ' + err.message });
    }
});

// User Login
app.post('/api/auth/login', async (req, res) => {
    try {
        const { identifier, password, role } = req.body;
        if (!identifier) {
            return res.status(400).json({ error: 'Email or Mobile is required.' });
        }

        const cleanIdent = identifier.trim().toLowerCase();
        const enteredPassword = password || '';

        // 1. BOOTSTRAP ADMIN (Swapnil Shinde)
        const isBootstrapAdmin = (
            cleanIdent === BOOTSTRAP_ADMIN.email.toLowerCase() ||
            cleanIdent === BOOTSTRAP_ADMIN.mobile
        );

        let user;

        if (isBootstrapAdmin) {
            let result = await pgPool.query(
                `SELECT * FROM users WHERE (LOWER(email) = $1 OR mobile = $1)`,
                [BOOTSTRAP_ADMIN.email.toLowerCase()]
            );

            if (result.rows.length === 0) {
                const adminHash = bcrypt.hashSync(BOOTSTRAP_ADMIN.password, 10);
                const insRes = await pgPool.query(
                    `INSERT INTO users (email, mobile, password_hash, full_name, role, active, approved_at)
                     VALUES ($1, $2, $3, $4, 'ADMIN', true, NOW())
                     RETURNING *`,
                    [BOOTSTRAP_ADMIN.email, BOOTSTRAP_ADMIN.mobile, adminHash, BOOTSTRAP_ADMIN.name]
                );
                user = insRes.rows[0];
            } else {
                user = result.rows[0];
            }

            // Verify Bootstrap Admin Password
            const isPasswordValid = (
                enteredPassword === BOOTSTRAP_ADMIN.password ||
                (user.password_hash && bcrypt.compareSync(enteredPassword, user.password_hash))
            );

            if (!isPasswordValid) {
                return res.status(401).json({
                    error: 'Invalid password for Bootstrap Admin. Please enter the correct master password.'
                });
            }

            // Ensure active & admin status + sync hash
            const currentHash = bcrypt.hashSync(enteredPassword, 10);
            await pgPool.query(
                `UPDATE users SET password_hash = $1, active = true, role = 'ADMIN', last_login = NOW() WHERE id = $2`,
                [currentHash, user.id]
            );
            user.active = true;
            user.role = 'ADMIN';
        } else {
            // 2. ALL OTHER USERS (Must register & be approved)
            const result = await pgPool.query(
                `SELECT * FROM users WHERE (LOWER(email) = $1 OR mobile = $1)`,
                [cleanIdent]
            );

            if (result.rows.length === 0) {
                return res.status(404).json({
                    error: 'No account found with this email or mobile. All guards and employees must register first.'
                });
            }

            user = result.rows[0];

            // Check if approved by Administrator
            if (!user.active) {
                return res.status(403).json({
                    error: 'Your registration is pending Administrator approval. Please contact Admin Swapnil Shinde to approve your access in the Admin Portal.'
                });
            }

            // Verify password
            const isValid = user.password_hash ? bcrypt.compareSync(enteredPassword, user.password_hash) : false;

            if (!isValid) {
                return res.status(401).json({ error: 'Invalid password. Please verify your credentials.' });
            }

            await pgPool.query(`UPDATE users SET last_login = NOW() WHERE id = $1`, [user.id]);
        }

        const token = jwt.sign(
            { id: user.id, email: user.email, role: user.role, name: user.full_name },
            JWT_SECRET,
            { expiresIn: '7d' }
        );

        await logAudit(user.id, 'USER_LOGIN', 'users', user.id, `User logged in as ${user.full_name} (${user.role})`, req.ip);

        // Fetch additional role metadata
        let extraInfo = {};
        if (user.role === 'EMPLOYEE') {
            const empRes = await pgPool.query(
                `SELECT e.employee_code, e.designation, d.name as department
                 FROM employees e
                 LEFT JOIN departments d ON e.department_id = d.id
                 WHERE e.user_id = $1`,
                [user.id]
            );
            if (empRes.rows.length > 0) {
                extraInfo = {
                    employeeCode: empRes.rows[0].employee_code,
                    designation: empRes.rows[0].designation,
                    department: empRes.rows[0].department || 'General'
                };
            }
        } else if (user.role === 'GUARD') {
            const grdRes = await pgPool.query(
                `SELECT g.badge_number, gt.name as assigned_gate
                 FROM guards g
                 LEFT JOIN gates gt ON g.assigned_gate_id = gt.id
                 WHERE g.user_id = $1`,
                [user.id]
            );
            if (grdRes.rows.length > 0) {
                extraInfo = {
                    badgeNumber: grdRes.rows[0].badge_number,
                    assignedGate: grdRes.rows[0].assigned_gate || 'Main Gate'
                };
            }
        }

        res.json({
            token,
            user: {
                id: user.id,
                email: user.email,
                mobile: user.mobile,
                name: user.full_name,
                role: user.role,
                avatar: user.avatar_url,
                ...extraInfo
            }
        });
    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ error: 'Database authentication failure: ' + err.message });
    }
});

// Register FCM Device Token
app.post('/api/auth/register-fcm', authenticateToken, async (req, res) => {
    try {
        const { fcmToken, deviceName } = req.body;
        const userId = req.user.id;

        if (!fcmToken) {
            return res.status(400).json({ error: 'fcmToken is required' });
        }

        await pgPool.query(`UPDATE users SET fcm_token = $1 WHERE id = $2`, [fcmToken, userId]);
        await pgPool.query(`
            INSERT INTO user_fcm_tokens (user_id, fcm_token, device_name, updated_at)
            VALUES ($1, $2, $3, NOW())
            ON CONFLICT (fcm_token) DO UPDATE SET user_id = $1, device_name = $3, updated_at = NOW()
        `, [userId, fcmToken, deviceName || 'Android Device']);

        console.log(`[FCM] Registered token for user ${userId}: ${fcmToken.substring(0, 15)}...`);
        res.json({ success: true, message: 'FCM Token registered successfully' });
    } catch (err) {
        console.error('FCM register error:', err);
        res.status(500).json({ error: 'Failed to update FCM token in database' });
    }
});

// =========================================================================
// 2. ADMIN APPROVALS & USER MANAGEMENT
// =========================================================================

// Get Pending Registrations
app.get('/api/admin/pending-users', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const result = await pgPool.query(`
            SELECT id, email, mobile, full_name as "fullName", role, created_at as "createdAt"
            FROM users
            WHERE active = false
            ORDER BY created_at DESC
        `);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch pending users error:', err);
        res.status(500).json({ error: 'Failed to fetch pending users from database' });
    }
});

// Approve Pending User
app.post('/api/admin/users/:id/approve', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const targetUserId = Number(req.params.id);
        const adminId = req.user.id;

        const updateRes = await pgPool.query(
            `UPDATE users
             SET active = true, approved_by_id = $1, approved_at = NOW()
             WHERE id = $2
             RETURNING id, email, full_name, role, fcm_token`,
            [adminId, targetUserId]
        );

        if (updateRes.rows.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }

        const approvedUser = updateRes.rows[0];

        await logAudit(
            adminId,
            'USER_APPROVED',
            'users',
            targetUserId,
            `Admin ${req.user.name} approved registration for ${approvedUser.full_name} (${approvedUser.role})`,
            req.ip
        );

        // Send push notification to user device if FCM token exists
        if (approvedUser.fcm_token) {
            await sendPushNotification(
                approvedUser.fcm_token,
                'Account Approved!',
                `Your ${approvedUser.role} access request has been approved by the Administrator. You can now log in.`,
                { type: 'ACCOUNT_APPROVED' }
            );
        }

        res.json({
            success: true,
            message: `Account for ${approvedUser.full_name} approved and activated successfully.`
        });
    } catch (err) {
        console.error('Approve user error:', err);
        res.status(500).json({ error: 'Failed to approve user: ' + err.message });
    }
});

// Reject / Delete Pending User
app.post('/api/admin/users/:id/reject', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const targetUserId = Number(req.params.id);
        const adminId = req.user.id;

        // Clean up role extensions
        await pgPool.query(`DELETE FROM employees WHERE user_id = $1`, [targetUserId]);
        await pgPool.query(`DELETE FROM guards WHERE user_id = $1`, [targetUserId]);

        const delRes = await pgPool.query(`DELETE FROM users WHERE id = $1 RETURNING full_name, email`, [targetUserId]);

        if (delRes.rows.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }

        const deletedUser = delRes.rows[0];

        await logAudit(
            adminId,
            'USER_REJECTED',
            'users',
            targetUserId,
            `Admin ${req.user.name} rejected registration for ${deletedUser.full_name} (${deletedUser.email})`,
            req.ip
        );

        res.json({ success: true, message: `Registration for ${deletedUser.full_name} has been rejected.` });
    } catch (err) {
        console.error('Reject user error:', err);
        res.status(500).json({ error: 'Failed to reject user from database: ' + err.message });
    }
});

// Get All Users (Admin Master List)
app.get('/api/admin/users', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const result = await pgPool.query(`
            SELECT id, email, mobile, full_name as "fullName", role, active, created_at as "createdAt", approved_at as "approvedAt"
            FROM users
            ORDER BY id ASC
        `);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch admin users error:', err);
        res.status(500).json({ error: 'Failed to fetch users list from database' });
    }
});

// =========================================================================
// 3. MASTER METADATA (DEPARTMENTS, GATES, EMPLOYEES)
// =========================================================================

app.get('/api/meta/departments', async (req, res) => {
    try {
        const result = await pgPool.query(`SELECT id, code, name, description, active FROM departments ORDER BY id ASC`);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch departments error:', err);
        res.status(500).json({ error: 'Failed to fetch departments' });
    }
});

app.get('/api/meta/gates', async (req, res) => {
    try {
        const result = await pgPool.query(`SELECT id, code, name, gate_type, active FROM gates ORDER BY id ASC`);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch gates error:', err);
        res.status(500).json({ error: 'Failed to fetch gates' });
    }
});

app.get('/api/meta/employees', async (req, res) => {
    try {
        const result = await pgPool.query(`
            SELECT u.id, u.full_name as name, u.email, u.mobile,
                   COALESCE(e.employee_code, 'EMP-' || u.id) as "employeeCode",
                   COALESCE(e.designation, 'Staff Host') as designation,
                   COALESCE(d.name, 'General') as department
            FROM users u
            LEFT JOIN employees e ON u.id = e.user_id
            LEFT JOIN departments d ON e.department_id = d.id
            WHERE u.role = 'EMPLOYEE' AND u.active = true
            ORDER BY u.full_name ASC
        `);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch employees error:', err);
        res.status(500).json({ error: 'Failed to fetch employees list' });
    }
});

// Trigger master seeds on demand
app.all('/api/meta/seed', async (req, res) => {
    try {
        await initDatabaseBootstrap();
        res.json({
            success: true,
            message: 'Database bootstrap & master seeds (Guard, Employees, Admin, Metadata) successfully synchronized.'
        });
    } catch (err) {
        res.status(500).json({ error: 'Failed to run bootstrap seed: ' + err.message });
    }
});

// =========================================================================
// 4. WALK-IN VISITOR REQUESTS WORKFLOW (GUARD -> HTTPS -> FCM -> EMPLOYEE)
// =========================================================================

// Submit Walk-in Request
app.post('/api/requests', authenticateToken, async (req, res) => {
    try {
        const { visitorName, visitorMobile, visitorCompany, purpose, idProofType, idProofNumber, vehicleNumber, hostEmployeeId, gateId } = req.body;

        if (!visitorName || !visitorMobile || !hostEmployeeId) {
            return res.status(400).json({ error: 'Visitor name, mobile, and host employee are required' });
        }

        const insertReq = await pgPool.query(`
            INSERT INTO visit_requests (
                visitor_name, visitor_mobile, visitor_company, purpose,
                id_proof_type, id_proof_number, vehicle_number,
                host_employee_id, gate_id, guard_user_id, status
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'PENDING')
            RETURNING id, created_at
        `, [
            visitorName.trim(),
            visitorMobile.trim(),
            visitorCompany ? visitorCompany.trim() : 'Guest Visitor',
            purpose.trim(),
            idProofType || 'National ID',
            idProofNumber ? idProofNumber.trim() : 'ID-VERIFIED',
            vehicleNumber ? vehicleNumber.trim() : null,
            Number(hostEmployeeId),
            Number(gateId || 1),
            req.user.id
        ]);

        const newRequestId = insertReq.rows[0].id;
        const createdAt = insertReq.rows[0].created_at;

        // Fetch gate and host info for push notification
        const hostQuery = await pgPool.query(`SELECT id, full_name, email, fcm_token FROM users WHERE id = $1`, [Number(hostEmployeeId)]);
        const gateQuery = await pgPool.query(`SELECT id, name FROM gates WHERE id = $1`, [Number(gateId || 1)]);

        const hostUser = hostQuery.rows[0];
        const gateName = gateQuery.rows.length > 0 ? gateQuery.rows[0].name : 'Main Security Gate';
        const guardName = req.user.name || 'Security Officer';

        const arrivalPayload = {
            type: 'VISITOR_ARRIVAL',
            requestId: String(newRequestId),
            callId: `CALL_${newRequestId}_${Date.now()}`,
            visitorName: visitorName.trim(),
            visitorMobile: visitorMobile.trim(),
            visitorCompany: visitorCompany ? visitorCompany.trim() : 'Guest Visitor',
            purpose: purpose.trim(),
            gateName,
            guardName,
            gateId: String(gateId || 1),
            hostEmployeeId: String(hostEmployeeId),
            hostName: hostUser ? hostUser.full_name : 'Host Employee',
            idProofType: idProofType || 'National ID',
            idProofNumber: idProofNumber ? idProofNumber.trim() : '',
            vehicleNumber: vehicleNumber ? vehicleNumber.trim() : '',
            createdAt: new Date(createdAt).toISOString(),
            urgent: 'true'
        };

        // Query all registered FCM tokens for the host employee
        const tokenQuery = await pgPool.query(`
            SELECT DISTINCT fcm_token FROM (
                SELECT fcm_token FROM user_fcm_tokens WHERE user_id = $1 AND fcm_token IS NOT NULL
                UNION
                SELECT fcm_token FROM users WHERE id = $1 AND fcm_token IS NOT NULL
            ) t
        `, [Number(hostEmployeeId)]);

        const hostTokens = tokenQuery.rows.map(r => r.fcm_token).filter(Boolean);
        console.log(`[FCM] Dispatching High-Priority Visitor Arrival Notification to host ${hostEmployeeId} (${hostTokens.length} devices)`);

        if (hostTokens.length > 0) {
            await sendVisitorArrivalPush(hostTokens, arrivalPayload);
        }

        // Store in notifications table for in-app history
        await pgPool.query(`
            INSERT INTO notifications (user_id, title, body, notification_type, data_payload)
            VALUES ($1, $2, $3, 'VISITOR_REQUEST', $4)
        `, [
            Number(hostEmployeeId),
            `Visitor Arrival Request`,
            `Visitor: ${visitorName.trim()}\nCompany: ${visitorCompany ? visitorCompany.trim() : 'Guest'}\nPurpose: ${purpose.trim()}\nGate: ${gateName}\nGuard: ${guardName}`,
            JSON.stringify(arrivalPayload)
        ]);

        await logAudit(
            req.user.id,
            'WALK_IN_REQUEST_CREATED',
            'visit_requests',
            newRequestId,
            `Walk-in requested for ${visitorName} to host ${hostUser ? hostUser.full_name : hostEmployeeId} at ${gateName}`,
            req.ip
        );

        res.status(201).json({
            success: true,
            message: 'Visitor arrival request submitted. High-priority notification dispatched to host employee.',
            requestId: newRequestId,
            createdAt
        });
    } catch (err) {
        console.error('Error creating visit request:', err);
        res.status(500).json({ error: 'Database error creating visit request: ' + err.message });
    }
});

// Get Visit Requests
app.get('/api/requests', authenticateToken, async (req, res) => {
    try {
        const { status, hostEmployeeId, hostId } = req.query;
        const targetHostId = hostEmployeeId || hostId;

        let query = `
            SELECT vr.id, vr.visitor_name as "visitorName", vr.visitor_mobile as "visitorMobile",
                   vr.visitor_company as "visitorCompany", vr.purpose, vr.id_proof_type as "idProofType",
                   vr.id_proof_number as "idProofNumber", vr.vehicle_number as "vehicleNumber",
                   vr.host_employee_id as "hostEmployeeId", vr.gate_id as "gateId",
                   vr.status, vr.decision_time as "decisionTime", vr.decision_reason as "decisionReason",
                   vr.meeting_room as "meetingRoom", vr.created_at as "createdAt",
                   u.full_name as "hostName", g.name as "gateName"
            FROM visit_requests vr
            LEFT JOIN users u ON vr.host_employee_id = u.id
            LEFT JOIN gates g ON vr.gate_id = g.id
            WHERE 1=1
        `;
        const params = [];

        if (status) {
            params.push(status.toUpperCase());
            query += ` AND vr.status = $${params.length}`;
        }

        if (targetHostId) {
            params.push(Number(targetHostId));
            query += ` AND vr.host_employee_id = $${params.length}`;
        } else if (req.user && req.user.role === 'EMPLOYEE') {
            params.push(req.user.id);
            query += ` AND vr.host_employee_id = $${params.length}`;
        }

        query += ` ORDER BY vr.created_at DESC`;

        const result = await pgPool.query(query, params);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch requests error:', err);
        res.status(500).json({ error: 'Failed to fetch visit requests from database' });
    }
});

// Check Call Status for a Request
app.get('/api/requests/:id/call-status', authenticateToken, async (req, res) => {
    try {
        const requestId = Number(req.params.id);
        const result = await pgPool.query(`
            SELECT id, status, decision_time as "decisionTime", decision_reason as "decisionReason",
                   meeting_room as "meetingRoom", visitor_name as "visitorName"
            FROM visit_requests WHERE id = $1
        `, [requestId]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Request not found' });
        }

        res.json({ success: true, request: result.rows[0] });
    } catch (err) {
        console.error('Call status error:', err);
        res.status(500).json({ error: 'Failed to fetch call status' });
    }
});

// Employee Decision Handler (Shared by PUT /api/requests/:id/decision and POST /api/employee/requests/:id/respond)
async function handleRequestDecision(req, res) {
    try {
        const requestId = Number(req.params.id);
        const { decision, reason, meetingRoom } = req.body;

        if (!['ACCEPTED', 'DECLINED'].includes(decision?.toUpperCase())) {
            return res.status(400).json({ error: 'Decision must be ACCEPTED or DECLINED' });
        }

        const upperDecision = decision.toUpperCase();
        const room = meetingRoom || (upperDecision === 'ACCEPTED' ? 'Conference Room A' : null);

        // Check request existence and current state
        const existingReq = await pgPool.query(`SELECT * FROM visit_requests WHERE id = $1`, [requestId]);
        if (existingReq.rows.length === 0) {
            return res.status(404).json({ error: 'Visit request not found' });
        }

        const request = existingReq.rows[0];

        // Authorization check: Only host employee or Admin
        if (req.user.role !== 'ADMIN' && req.user.id !== request.host_employee_id) {
            return res.status(403).json({ error: 'Unauthorized: Only the designated host employee can respond to this visitor request' });
        }

        // Idempotency: If already decided, return current state
        if (request.status !== 'PENDING') {
            return res.json({
                success: true,
                alreadyDecided: true,
                status: request.status,
                message: `Visitor request was already ${request.status.toLowerCase()}`
            });
        }

        const updateRes = await pgPool.query(`
            UPDATE visit_requests
            SET status = $1, decision_time = NOW(), decision_reason = $2, meeting_room = $3
            WHERE id = $4
            RETURNING id, visitor_name, guard_user_id, host_employee_id, status
        `, [upperDecision, reason || null, room, requestId]);

        const updatedRequest = updateRes.rows[0];
        const visitorName = updatedRequest.visitor_name;
        const title = `Visit ${upperDecision}: ${visitorName}`;
        const body = upperDecision === 'ACCEPTED'
            ? `Host approved entry for ${visitorName}. Room: ${room || 'Assigned Office'}. You may grant entry.`
            : `Host DECLINED visit for ${visitorName}. Reason: ${reason || 'Host unavailable'}.`;

        // 1. Notify Guard's devices
        if (updatedRequest.guard_user_id) {
            const guardTokensQuery = await pgPool.query(`
                SELECT DISTINCT fcm_token FROM (
                    SELECT fcm_token FROM user_fcm_tokens WHERE user_id = $1 AND fcm_token IS NOT NULL
                    UNION
                    SELECT fcm_token FROM users WHERE id = $1 AND fcm_token IS NOT NULL
                ) t
            `, [updatedRequest.guard_user_id]);

            const guardTokens = guardTokensQuery.rows.map(r => r.fcm_token).filter(Boolean);
            if (guardTokens.length > 0) {
                await sendIncomingCallPush(guardTokens, {
                    type: 'REQUEST_DECISION',
                    requestId: String(requestId),
                    status: upperDecision,
                    visitorName,
                    meetingRoom: room || '',
                    reason: reason || ''
                });
            }
        }

        // 2. Broadcast to guards topic
        await sendTopicNotification('guards', title, body, {
            requestId: String(requestId),
            status: upperDecision,
            type: 'REQUEST_DECISION'
        });

        // 3. Dismiss call on all of host's devices
        const hostTokensQuery = await pgPool.query(`
            SELECT DISTINCT fcm_token FROM (
                SELECT fcm_token FROM user_fcm_tokens WHERE user_id = $1 AND fcm_token IS NOT NULL
                UNION
                SELECT fcm_token FROM users WHERE id = $1 AND fcm_token IS NOT NULL
            ) t
        `, [updatedRequest.host_employee_id]);

        const hostTokens = hostTokensQuery.rows.map(r => r.fcm_token).filter(Boolean);
        if (hostTokens.length > 0) {
            await sendIncomingCallPush(hostTokens, {
                type: 'VISITOR_CALL_DISMISSED',
                requestId: String(requestId),
                decision: upperDecision
            });
        }

        await logAudit(
            req.user.id,
            `VISIT_REQUEST_${upperDecision}`,
            'visit_requests',
            requestId,
            `Visit request ${requestId} for ${visitorName} marked as ${upperDecision} by host ${req.user.name}`,
            req.ip
        );

        res.json({
            success: true,
            status: upperDecision,
            message: `Visitor request has been ${upperDecision.toLowerCase()}`
        });
    } catch (err) {
        console.error('Error updating request decision:', err);
        res.status(500).json({ error: 'Database error updating visit request: ' + err.message });
    }
}

// Employee Decision: Accept / Decline Walk-in Request
app.put('/api/requests/:id/decision', authenticateToken, handleRequestDecision);
app.post('/api/employee/requests/:id/respond', authenticateToken, handleRequestDecision);

// Guard Grants Entry for Accepted Request
app.post('/api/requests/:id/grant-entry', authenticateToken, async (req, res) => {
    try {
        const requestId = Number(req.params.id);

        const reqQuery = await pgPool.query(`SELECT * FROM visit_requests WHERE id = $1`, [requestId]);
        if (reqQuery.rows.length === 0) {
            return res.status(404).json({ error: 'Visit request not found' });
        }

        const request = reqQuery.rows[0];
        if (request.status !== 'ACCEPTED') {
            return res.status(400).json({ error: `Cannot grant entry. Current request status is ${request.status}` });
        }

        const insertVisit = await pgPool.query(`
            INSERT INTO visits (
                request_id, visit_type, visitor_name, visitor_mobile, visitor_company,
                purpose, id_proof_type, id_proof_number, vehicle_number,
                host_employee_id, gate_in_id, guard_in_id, status, entry_time, verification_notes
            )
            VALUES ($1, 'WALK_IN', $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, 'INSIDE', NOW(), $12)
            RETURNING id, entry_time
        `, [
            requestId,
            request.visitor_name,
            request.visitor_mobile,
            request.visitor_company,
            request.purpose,
            request.id_proof_type,
            request.id_proof_number,
            request.vehicle_number,
            request.host_employee_id,
            request.gate_id,
            req.user.id,
            `Meeting Room: ${request.meeting_room || 'Assigned Office'}`
        ]);

        const newVisitId = insertVisit.rows[0].id;
        await pgPool.query(`UPDATE visit_requests SET status = 'ENTRY_GRANTED' WHERE id = $1`, [requestId]);

        // Notify Host that visitor has physically entered
        const hostQuery = await pgPool.query(`SELECT fcm_token FROM users WHERE id = $1`, [request.host_employee_id]);
        if (hostQuery.rows.length > 0 && hostQuery.rows[0].fcm_token) {
            await sendPushNotification(
                hostQuery.rows[0].fcm_token,
                `Visitor Inside: ${request.visitor_name}`,
                `${request.visitor_name} has entered through the gate and is proceeding to meet you.`,
                { visitId: String(newVisitId), type: 'ENTRY_CONFIRMED' }
            );
        }

        await logAudit(req.user.id, 'VISITOR_ENTRY_GRANTED', 'visits', newVisitId, `Entry granted for ${request.visitor_name}`, req.ip);

        res.json({
            success: true,
            message: 'Entry granted. Visitor is now marked as INSIDE.',
            visitId: newVisitId
        });
    } catch (err) {
        console.error('Grant entry error:', err);
        res.status(500).json({ error: 'Database error granting entry: ' + err.message });
    }
});

// =========================================================================
// 5. PRE-REGISTERED APPOINTMENTS WORKFLOW (EMPLOYEE -> SERVER -> OTP/QR)
// =========================================================================

// Create Appointment / Pre-registered Pass
app.post('/api/appointments', authenticateToken, async (req, res) => {
    try {
        const { visitorName, visitorMobile, visitorCompany, visitorEmail, purpose, expectedDateTime, departmentId } = req.body;

        if (!visitorName || !visitorMobile) {
            return res.status(400).json({ error: 'Visitor name and mobile are required' });
        }

        // Safely parse and validate the appointment date & time
        let parsedDate = null;
        if (expectedDateTime && typeof expectedDateTime === 'string' && expectedDateTime.trim() !== '') {
            const trimmed = expectedDateTime.trim();
            // Direct Date parse (handles ISO-8601 strings e.g. "2026-09-01T14:30:00Z" or "2026-09-01T14:30:00+05:30")
            const d = new Date(trimmed);
            if (!isNaN(d.getTime())) {
                parsedDate = d;
            } else {
                // Support natural phrases like "Today, 02:30 PM" or "Tomorrow, 10:00 AM"
                try {
                    const now = new Date();
                    const isTomorrow = /tomorrow/i.test(trimmed);
                    const timeMatch = trimmed.match(/(\d{1,2}):(\d{2})\s*(AM|PM)?/i);
                    if (timeMatch) {
                        let hours = parseInt(timeMatch[1], 10);
                        const minutes = parseInt(timeMatch[2], 10);
                        const meridiem = timeMatch[3] ? timeMatch[3].toUpperCase() : null;
                        if (meridiem === 'PM' && hours < 12) hours += 12;
                        if (meridiem === 'AM' && hours === 12) hours = 0;
                        const tempDate = new Date(now);
                        if (isTomorrow) tempDate.setDate(tempDate.getDate() + 1);
                        tempDate.setHours(hours, minutes, 0, 0);
                        if (!isNaN(tempDate.getTime())) {
                            parsedDate = tempDate;
                        }
                    }
                } catch (parseErr) {
                    console.warn('[Appointments] Fallback parsing failed:', parseErr.message);
                }
            }
        }

        // If not provided or unparseable, default gracefully to 1 hour from now
        if (!parsedDate || isNaN(parsedDate.getTime())) {
            parsedDate = new Date(Date.now() + 3600000);
        }

        const validIsoTimestamp = parsedDate.toISOString();

        // Generate genuine 6-digit OTP and QR token
        const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
        const qrToken = `VMS-PASS-${Date.now()}-${otpCode.substring(0, 3)}`;

        const insertAppt = await pgPool.query(`
            INSERT INTO appointments (
                visitor_name, visitor_mobile, visitor_company, visitor_email,
                host_employee_id, department_id, purpose, expected_date_time,
                status, otp_code, qr_token, otp_expires_at, otp_used
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'SCHEDULED', $9, $10, NOW() + INTERVAL '1 day', false)
            RETURNING id, otp_code, qr_token, created_at
        `, [
            visitorName.trim(),
            visitorMobile.trim(),
            visitorCompany ? visitorCompany.trim() : 'Guest Partner',
            visitorEmail ? visitorEmail.trim() : null,
            req.user.id,
            Number(departmentId || 1),
            purpose ? purpose.trim() : 'Official Meeting',
            validIsoTimestamp,
            otpCode,
            qrToken
        ]);

        const newApptId = insertAppt.rows[0].id;

        await logAudit(
            req.user.id,
            'APPOINTMENT_CREATED',
            'appointments',
            newApptId,
            `Host ${req.user.name} created pass for ${visitorName.trim()}. Pass OTP: ${otpCode}`,
            req.ip
        );

        res.status(201).json({
            success: true,
            message: 'Appointment scheduled! Pass OTP: ' + otpCode,
            appointment: {
                id: newApptId,
                visitorName: visitorName.trim(),
                visitorMobile: visitorMobile.trim(),
                visitorCompany: visitorCompany || 'Guest Partner',
                visitorEmail: visitorEmail || null,
                hostEmployeeId: req.user.id,
                hostName: req.user.name,
                purpose: purpose || 'Official Meeting',
                expectedDateTime: validIsoTimestamp,
                status: 'SCHEDULED',
                otpCode,
                qrToken,
                otpExpiresAt: 'Tomorrow, 11:59 PM',
                otpUsed: false
            }
        });
    } catch (err) {
        console.error('Appointment creation error:', err);
        res.status(500).json({ error: 'Database error creating appointment: ' + err.message });
    }
});

// Get Appointments
app.get('/api/appointments', authenticateToken, async (req, res) => {
    try {
        const { hostEmployeeId, hostId } = req.query;
        const targetHostId = hostEmployeeId || hostId;

        let query = `
            SELECT a.id, a.visitor_name as "visitorName", a.visitor_mobile as "visitorMobile",
                   a.visitor_company as "visitorCompany", a.visitor_email as "visitorEmail",
                   a.host_employee_id as "hostEmployeeId", a.purpose,
                   a.expected_date_time as "expectedDateTime", a.status,
                   a.otp_code as "otpCode", a.qr_token as "qrToken",
                   a.otp_expires_at as "otpExpiresAt", a.otp_used as "otpUsed",
                   a.created_at as "createdAt", u.full_name as "hostName"
            FROM appointments a
            LEFT JOIN users u ON a.host_employee_id = u.id
            WHERE 1=1
        `;
        const params = [];

        if (targetHostId) {
            params.push(Number(targetHostId));
            query += ` AND a.host_employee_id = $${params.length}`;
        } else if (req.user && req.user.role === 'EMPLOYEE') {
            params.push(req.user.id);
            query += ` AND a.host_employee_id = $${params.length}`;
        }

        query += ` ORDER BY a.created_at DESC`;

        const result = await pgPool.query(query, params);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch appointments error:', err);
        res.status(500).json({ error: 'Failed to fetch appointments from database' });
    }
});

// =========================================================================
// 6. OTP & QR CHECKPOINT VALIDATION AT GUARD POST
// =========================================================================

// Verify Pass OTP
app.post('/api/verify/otp', authenticateToken, async (req, res) => {
    try {
        const { otp, gateId } = req.body;
        if (!otp) return res.status(400).json({ error: 'OTP code is required' });

        const cleanOtp = otp.trim();

        const apptQuery = await pgPool.query(`SELECT * FROM appointments WHERE otp_code = $1`, [cleanOtp]);
        if (apptQuery.rows.length === 0) {
            return res.status(404).json({ error: 'Invalid or unknown Pass OTP. Please verify the code.' });
        }

        const appt = apptQuery.rows[0];

        if (appt.otp_used) {
            return res.status(400).json({ error: 'This OTP pass has already been used for entry.' });
        }

        await pgPool.query(`UPDATE appointments SET otp_used = true, status = 'ARRIVED' WHERE id = $1`, [appt.id]);

        const insertVisit = await pgPool.query(`
            INSERT INTO visits (
                appointment_id, visit_type, visitor_name, visitor_mobile, visitor_company,
                purpose, id_proof_type, id_proof_number, host_employee_id,
                gate_in_id, guard_in_id, status, entry_time, verification_notes
            )
            VALUES ($1, 'PRE_REGISTERED', $2, $3, $4, $5, 'Verified Pass OTP', $6, $7, $8, $9, 'INSIDE', NOW(), 'Pre-registered Entry via OTP')
            RETURNING id, entry_time
        `, [
            appt.id,
            appt.visitor_name,
            appt.visitor_mobile,
            appt.visitor_company,
            appt.purpose,
            appt.qr_token,
            appt.host_employee_id,
            Number(gateId || 1),
            req.user.id
        ]);

        const newVisitId = insertVisit.rows[0].id;

        await logAudit(req.user.id, 'OTP_VERIFIED_ENTRY', 'visits', newVisitId, `Pass OTP ${cleanOtp} verified for ${appt.visitor_name}`, req.ip);

        res.json({
            success: true,
            message: `Pass OTP verified! Entry granted for ${appt.visitor_name}.`,
            visit: {
                id: newVisitId,
                visitorName: appt.visitor_name,
                status: 'INSIDE'
            }
        });
    } catch (err) {
        console.error('OTP verification error:', err);
        res.status(500).json({ error: 'Database error verifying OTP: ' + err.message });
    }
});

// Verify QR Pass
app.post('/api/verify/qr', authenticateToken, async (req, res) => {
    try {
        const { qrToken, gateId } = req.body;
        if (!qrToken) return res.status(400).json({ error: 'QR Token is required' });

        const cleanToken = qrToken.trim();

        const apptQuery = await pgPool.query(
            `SELECT * FROM appointments WHERE qr_token = $1 OR otp_code = $1`,
            [cleanToken]
        );

        if (apptQuery.rows.length === 0) {
            return res.status(404).json({ error: 'Invalid or unrecognized QR pass.' });
        }

        const appt = apptQuery.rows[0];

        if (appt.otp_used) {
            return res.status(400).json({ error: 'This QR Pass has already been used for entry.' });
        }

        await pgPool.query(`UPDATE appointments SET otp_used = true, status = 'ARRIVED' WHERE id = $1`, [appt.id]);

        const insertVisit = await pgPool.query(`
            INSERT INTO visits (
                appointment_id, visit_type, visitor_name, visitor_mobile, visitor_company,
                purpose, id_proof_type, id_proof_number, host_employee_id,
                gate_in_id, guard_in_id, status, entry_time, verification_notes
            )
            VALUES ($1, 'PRE_REGISTERED', $2, $3, $4, $5, 'Digital QR Pass', $6, $7, $8, $9, 'INSIDE', NOW(), 'Pre-registered Entry via QR')
            RETURNING id, entry_time
        `, [
            appt.id,
            appt.visitor_name,
            appt.visitor_mobile,
            appt.visitor_company,
            appt.purpose,
            appt.qr_token,
            appt.host_employee_id,
            Number(gateId || 1),
            req.user.id
        ]);

        const newVisitId = insertVisit.rows[0].id;

        await logAudit(req.user.id, 'QR_SCANNED_ENTRY', 'visits', newVisitId, `QR Pass validated for ${appt.visitor_name}`, req.ip);

        res.json({
            success: true,
            message: `QR Pass validated! Entry granted for ${appt.visitor_name}.`,
            visit: {
                id: newVisitId,
                visitorName: appt.visitor_name,
                status: 'INSIDE'
            }
        });
    } catch (err) {
        console.error('QR verification error:', err);
        res.status(500).json({ error: 'Database error validating QR pass: ' + err.message });
    }
});

// =========================================================================
// 7. ACTIVE VISITS & CHECKOUT
// =========================================================================

// Get Active Visitors Inside Premises
app.get('/api/visits/inside', authenticateToken, async (req, res) => {
    try {
        const { hostEmployeeId, hostId } = req.query;
        const targetHostId = hostEmployeeId || hostId;

        let query = `
            SELECT v.id, v.request_id as "requestId", v.appointment_id as "appointmentId",
                   v.visit_type as "visitType", v.visitor_name as "visitorName",
                   v.visitor_mobile as "visitorMobile", v.visitor_company as "visitorCompany",
                   v.purpose, v.host_employee_id as "hostEmployeeId", v.gate_in_id as "gateInId",
                   v.status, v.entry_time as "entryTime", v.exit_time as "exitTime",
                   v.total_duration_minutes as "totalDurationMinutes",
                   v.employee_verified as "employeeVerified",
                   v.employee_verified_time as "employeeVerifiedTime",
                   v.employee_signature_data as "employeeSignatureData",
                   v.verification_notes as "verificationNotes",
                   v.verification_notes as "notes", u.full_name as "hostName", g.name as "gateInName"
            FROM visits v
            LEFT JOIN users u ON v.host_employee_id = u.id
            LEFT JOIN gates g ON v.gate_in_id = g.id
            WHERE v.status = 'INSIDE'
        `;
        const params = [];

        if (targetHostId) {
            params.push(Number(targetHostId));
            query += ` AND v.host_employee_id = $${params.length}`;
        } else if (req.user && req.user.role === 'EMPLOYEE') {
            params.push(req.user.id);
            query += ` AND v.host_employee_id = $${params.length}`;
        }

        query += ` ORDER BY v.entry_time DESC`;

        const result = await pgPool.query(query, params);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch inside visits error:', err);
        res.status(500).json({ error: 'Failed to fetch active visits from database' });
    }
});

// Get Historical Completed Visits
app.get('/api/visits/history', authenticateToken, async (req, res) => {
    try {
        const { hostEmployeeId, hostId } = req.query;
        const targetHostId = hostEmployeeId || hostId;

        let query = `
            SELECT v.id, v.request_id as "requestId", v.appointment_id as "appointmentId",
                   v.visit_type as "visitType", v.visitor_name as "visitorName",
                   v.visitor_mobile as "visitorMobile", v.visitor_company as "visitorCompany",
                   v.purpose, v.host_employee_id as "hostEmployeeId", v.gate_in_id as "gateInId",
                   v.status, v.entry_time as "entryTime", v.exit_time as "exitTime",
                   v.total_duration_minutes as "totalDurationMinutes",
                   v.employee_verified as "employeeVerified",
                   v.employee_verified_time as "employeeVerifiedTime",
                   v.employee_signature_data as "employeeSignatureData",
                   v.verification_notes as "verificationNotes",
                   v.verification_notes as "notes", u.full_name as "hostName", g1.name as "gateInName", g2.name as "gateOutName"
            FROM visits v
            LEFT JOIN users u ON v.host_employee_id = u.id
            LEFT JOIN gates g1 ON v.gate_in_id = g1.id
            LEFT JOIN gates g2 ON v.gate_out_id = g2.id
            WHERE v.status = 'COMPLETED'
        `;
        const params = [];

        if (targetHostId) {
            params.push(Number(targetHostId));
            query += ` AND v.host_employee_id = $${params.length}`;
        } else if (req.user && req.user.role === 'EMPLOYEE') {
            params.push(req.user.id);
            query += ` AND v.host_employee_id = $${params.length}`;
        }

        query += ` ORDER BY v.exit_time DESC`;

        const result = await pgPool.query(query, params);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch history error:', err);
        res.status(500).json({ error: 'Failed to fetch visit history from database' });
    }
});

// Employee Digital Sign-off for meeting
app.put('/api/visits/:id/verify-met', authenticateToken, async (req, res) => {
    try {
        const visitId = Number(req.params.id);
        const { signatureData, notes } = req.body;

        const updateRes = await pgPool.query(`
            UPDATE visits
            SET employee_verified = true, employee_verified_time = NOW(),
                employee_signature_data = $1, verification_notes = $2
            WHERE id = $3
            RETURNING id, visitor_name
        `, [signatureData || 'DIGITAL_SIGNATURE_OK', notes || 'Meeting verified', visitId]);

        if (updateRes.rows.length === 0) {
            return res.status(404).json({ error: 'Visit record not found' });
        }

        await logAudit(req.user.id, 'MEETING_VERIFIED', 'visits', visitId, `Host ${req.user.name} signed off on meeting with ${updateRes.rows[0].visitor_name}`, req.ip);

        res.json({ success: true, message: 'Meeting verified & digitally signed successfully.' });
    } catch (err) {
        console.error('Verify meeting error:', err);
        res.status(500).json({ error: 'Database error signing meeting: ' + err.message });
    }
});

// Guard Marks Exit (Visitor Checkout)
app.put('/api/visits/:id/exit', authenticateToken, async (req, res) => {
    try {
        const visitId = Number(req.params.id);
        const { gateOutId } = req.body;

        const visitRes = await pgPool.query(`SELECT entry_time, visitor_name FROM visits WHERE id = $1`, [visitId]);
        if (visitRes.rows.length === 0) {
            return res.status(404).json({ error: 'Visit record not found' });
        }

        const visit = visitRes.rows[0];
        const entryTime = new Date(visit.entry_time);
        const exitTime = new Date();
        const durationMinutes = Math.max(1, Math.round((exitTime - entryTime) / 60000));

        await pgPool.query(`
            UPDATE visits
            SET status = 'COMPLETED', exit_time = NOW(), gate_out_id = $1, guard_out_id = $2, total_duration_minutes = $3
            WHERE id = $4
        `, [Number(gateOutId || 1), req.user.id, durationMinutes, visitId]);

        await logAudit(req.user.id, 'VISITOR_EXIT_MARKED', 'visits', visitId, `Visitor ${visit.visitor_name} checkout recorded (${durationMinutes} min)`, req.ip);

        res.json({
            success: true,
            message: `Checkout complete. Total visit duration: ${durationMinutes} minutes.`
        });
    } catch (err) {
        console.error('Mark exit error:', err);
        res.status(500).json({ error: 'Database error marking exit: ' + err.message });
    }
});

// =========================================================================
// 8. ADMIN DASHBOARD & AUDIT LOGS
// =========================================================================

app.get('/api/admin/stats', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const qInside = await pgPool.query(`SELECT count(*) FROM visits WHERE status = 'INSIDE'`);
        const insideCount = parseInt(qInside.rows[0].count, 10);

        const qPending = await pgPool.query(`SELECT count(*) FROM visit_requests WHERE status = 'PENDING'`);
        const pendingCount = parseInt(qPending.rows[0].count, 10);

        const qAppts = await pgPool.query(`SELECT count(*) FROM appointments WHERE status = 'SCHEDULED'`);
        const scheduledCount = parseInt(qAppts.rows[0].count, 10);

        const qComp = await pgPool.query(`SELECT count(*) FROM visits WHERE status = 'COMPLETED'`);
        const completedCount = parseInt(qComp.rows[0].count, 10);

        const qTotAppts = await pgPool.query(`SELECT count(*) FROM appointments`);
        const totalAppts = parseInt(qTotAppts.rows[0].count, 10);

        const qGuards = await pgPool.query(`SELECT count(*) FROM users WHERE role = 'GUARD' AND active = true`);
        const guardsCount = parseInt(qGuards.rows[0].count, 10);

        const qEmps = await pgPool.query(`SELECT count(*) FROM users WHERE role = 'EMPLOYEE' AND active = true`);
        const employeesCount = parseInt(qEmps.rows[0].count, 10);

        res.json({
            visitorsToday: insideCount + completedCount,
            currentlyInside: insideCount,
            pendingApprovals: pendingCount,
            totalAppointments: totalAppts,
            scheduledAppointments: scheduledCount,
            completedVisits: completedCount,
            activeGuardsCount: Math.max(guardsCount, 1),
            totalEmployeesCount: Math.max(employeesCount, 1),
            gatesCount: 3
        });
    } catch (err) {
        console.error('Admin stats error:', err);
        res.status(500).json({ error: 'Failed to compute statistics from database' });
    }
});

app.get('/api/admin/audit-logs', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const result = await pgPool.query(`
            SELECT id, action, entity_type as "entityType", entity_id as "entityId", details, created_at as "createdAt"
            FROM audit_logs
            ORDER BY id DESC
            LIMIT 100
        `);
        res.json(result.rows);
    } catch (err) {
        console.error('Fetch audit logs error:', err);
        res.status(500).json({ error: 'Failed to fetch audit logs from database' });
    }
});

// =========================================================================
// SERVER INITIALIZATION
// =========================================================================

app.listen(PORT, '0.0.0.0', () => {
    console.log(`========================================================`);
    console.log(`🚀 VISITOR MANAGEMENT SYSTEM ENTERPRISE PRODUCTION BACKEND`);
    console.log(`📡 Listening on: http://0.0.0.0:${PORT}`);
    console.log(`👑 Bootstrap Admin: ${BOOTSTRAP_ADMIN.email}`);
    console.log(`========================================================`);
});
