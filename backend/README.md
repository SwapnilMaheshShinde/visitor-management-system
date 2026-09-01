# Visitor Management System (VMS) - Production Backend (Render)

Production Node.js/Express REST API powering the Visitor Management System Android application with Neon PostgreSQL and Firebase Cloud Messaging (FCM).

---

## Deployment on Render Web Service

### Service Settings
- **Service Type**: Web Service
- **Runtime**: `Node`
- **Root Directory**: `backend`
- **Build Command**: `npm install --omit=dev`
- **Start Command**: `node server.js`
- **Health Check Path**: `/health`

---

## Required Environment Variables (in Render Dashboard)

| Variable | Value / Format | Description |
| :--- | :--- | :--- |
| `NODE_ENV` | `production` | Enables production mode |
| `DATABASE_URL` | `postgresql://neondb_owner:npg_QO2k8azUHcvy@ep-wandering-water-aeamwfm9-pooler.c-2.us-east-2.aws.neon.tech/neondb?sslmode=require` | Neon PostgreSQL pooled connection string |
| `JWT_SECRET` | `vms_enterprise_production_jwt_secret_2026` (or random string) | Secret key for signing JWT tokens |
| `FIREBASE_SERVICE_ACCOUNT` | `{"type":"service_account","project_id":"vmapp-7eb8a",...}` | Full raw JSON string of Firebase Admin Service Account key |

---

## Verification Endpoints
- `GET /health` -> `{"status":"HEALTHY","database":{"provider":"PostgreSQL (Neon)","status":"CONNECTED"},"fcm":{"initialized":true}}`
- `GET /api/health` -> Same health status under API path
