// =========================================================================
// Firebase Cloud Messaging (FCM) Production Service
// Sends real push notifications to Android devices
// =========================================================================

const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

let fcmInitialized = false;

function initFirebase() {
    if (admin.apps.length > 0) {
        fcmInitialized = true;
        return;
    }

    try {
        // Option 1: FIREBASE_SERVICE_ACCOUNT environment variable (raw JSON string)
        if (process.env.FIREBASE_SERVICE_ACCOUNT) {
            let serviceAccount;
            try {
                serviceAccount = typeof process.env.FIREBASE_SERVICE_ACCOUNT === 'string'
                    ? JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)
                    : process.env.FIREBASE_SERVICE_ACCOUNT;
            } catch (jsonErr) {
                console.error('[FCM] Error parsing FIREBASE_SERVICE_ACCOUNT JSON:', jsonErr.message);
            }

            if (serviceAccount && serviceAccount.project_id) {
                admin.initializeApp({
                    credential: admin.credential.cert(serviceAccount)
                });
                fcmInitialized = true;
                console.log(`[FCM] Firebase Admin SDK initialized via FIREBASE_SERVICE_ACCOUNT for project: ${serviceAccount.project_id}`);
                return;
            }
        }

        // Option 2: Service Account JSON File on disk
        const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || path.join(__dirname, 'serviceAccountKey.json');
        if (fs.existsSync(serviceAccountPath)) {
            const raw = fs.readFileSync(serviceAccountPath, 'utf8');
            const serviceAccount = JSON.parse(raw);
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
            fcmInitialized = true;
            console.log(`[FCM] Firebase Admin SDK initialized from file: ${serviceAccountPath}`);
            return;
        }

        // Option 3: Default application credentials or FIREBASE_CONFIG
        if (process.env.GOOGLE_APPLICATION_CREDENTIALS || process.env.FIREBASE_CONFIG) {
            admin.initializeApp();
            fcmInitialized = true;
            console.log('[FCM] Firebase Admin SDK initialized with Application Default Credentials');
            return;
        }

        console.warn('[FCM] No Firebase Service Account configuration detected. Push notifications will be disabled until credentials are provided via FIREBASE_SERVICE_ACCOUNT or serviceAccountKey.json.');
    } catch (err) {
        console.error('[FCM] Failed to initialize Firebase Admin SDK:', err.message);
    }
}

initFirebase();

/**
 * Send a push notification to an individual user's device FCM token
 * @param {string} token - FCM device registration token
 * @param {string} title - Notification title
 * @param {string} body - Notification body text
 * @param {object} data - Custom key-value string payload for Android deep links/actions
 */
async function sendPushNotification(token, title, body, data = {}) {
    if (!token) {
        return { success: false, reason: 'NO_TOKEN' };
    }

    if (!fcmInitialized) {
        console.warn(`[FCM] Push skipped for "${title}": Firebase Admin not configured on server.`);
        return { success: false, reason: 'FCM_NOT_CONFIGURED' };
    }

    const stringifiedData = {};
    for (const [key, value] of Object.entries(data || {})) {
        stringifiedData[key] = typeof value === 'object' ? JSON.stringify(value) : String(value);
    }

    try {
        const message = {
            token: token,
            notification: {
                title: title,
                body: body
            },
            data: stringifiedData,
            android: {
                priority: 'high',
                notification: {
                    channelId: 'vms_visitor_alerts',
                    sound: 'default',
                    priority: 'high'
                }
            }
        };

        const response = await admin.messaging().send(message);
        console.log(`[FCM] Push sent to token ${token.substring(0, 15)}... Message ID: ${response}`);
        return { success: true, messageId: response };
    } catch (error) {
        console.error('[FCM] Error sending push notification:', error.message);
        return { success: false, error: error.message };
    }
}

/**
 * Send notification to a topic (e.g., "admins", "guards", "hosts")
 */
async function sendTopicNotification(topic, title, body, data = {}) {
    if (!fcmInitialized) {
        console.warn(`[FCM] Topic "${topic}" push skipped: Firebase Admin not configured on server.`);
        return { success: false, reason: 'FCM_NOT_CONFIGURED' };
    }

    const stringifiedData = {};
    for (const [key, value] of Object.entries(data || {})) {
        stringifiedData[key] = typeof value === 'object' ? JSON.stringify(value) : String(value);
    }

    try {
        const message = {
            topic: topic,
            notification: { title, body },
            data: stringifiedData,
            android: {
                priority: 'high',
                notification: {
                    channelId: 'vms_visitor_alerts',
                    sound: 'default',
                    priority: 'high'
                }
            }
        };
        const response = await admin.messaging().send(message);
        console.log(`[FCM] Topic "${topic}" message sent. Message ID: ${response}`);
        return { success: true, messageId: response };
    } catch (err) {
        console.error(`[FCM] Error sending topic message to "${topic}":`, err.message);
        return { success: false, error: err.message };
    }
}

/**
 * Send High-Priority Professional Visitor Arrival Push Notification
 * Delivers strong visitor arrival alerts to Employee host devices with high priority
 * across all states (foreground, background, closed/swiped-away).
 */
async function sendVisitorArrivalPush(tokens, arrivalData = {}) {
    if (!tokens || (Array.isArray(tokens) && tokens.length === 0)) {
        return { success: false, reason: 'NO_TOKENS' };
    }

    if (!fcmInitialized) {
        console.warn(`[FCM] Visitor arrival push skipped: Firebase Admin not configured.`);
        return { success: false, reason: 'FCM_NOT_CONFIGURED' };
    }

    const tokenList = Array.isArray(tokens) ? tokens.filter(Boolean) : [tokens].filter(Boolean);
    if (tokenList.length === 0) return { success: false, reason: 'NO_VALID_TOKENS' };

    const stringifiedData = {};
    for (const [key, value] of Object.entries(arrivalData || {})) {
        stringifiedData[key] = typeof value === 'object' ? JSON.stringify(value) : String(value);
    }
    stringifiedData['type'] = 'VISITOR_ARRIVAL';
    stringifiedData['urgent'] = 'true';
    stringifiedData['timestamp'] = String(Date.now());

    const notifTitle = 'Visitor Arrival Request';
    const notifBody = `${arrivalData.visitorName || 'Visitor'} (${arrivalData.visitorCompany || 'Visitor'}) arrived at ${arrivalData.gateName || 'Gate'}. Purpose: ${arrivalData.purpose || 'Official Visit'}`;

    const results = [];
    for (const token of tokenList) {
        try {
            const message = {
                token: token,
                notification: {
                    title: notifTitle,
                    body: notifBody
                },
                data: stringifiedData,
                android: {
                    priority: 'high',
                    notification: {
                        channelId: 'vms_visitor_arrivals',
                        sound: 'default',
                        priority: 'high'
                    }
                }
            };
            const response = await admin.messaging().send(message);
            console.log(`[FCM] High-priority visitor arrival notification sent to token ${token.substring(0, 15)}... Message ID: ${response}`);
            results.push({ token, success: true, messageId: response });
        } catch (err) {
            console.error(`[FCM] Error sending visitor arrival notification to token ${token.substring(0, 15)}...:`, err.message);
            results.push({ token, success: false, error: err.message });
        }
    }

    return { success: results.some(r => r.success), results };
}

module.exports = {
    sendPushNotification,
    sendTopicNotification,
    sendVisitorArrivalPush,
    sendIncomingCallPush: sendVisitorArrivalPush,
    isFcmInitialized: () => fcmInitialized
};

