# Bright Bulk Production

This replaces the V3 mock UI with a real local data layer and a production-oriented API foundation.

## What is real
- Android local SQLite contacts/campaigns/messages.
- CSV/TXT import.
- Contact creation and campaign creation.
- Backend URL + health check.
- PostgreSQL schema.
- Password hashing + JWT auth on backend.
- WhatsApp Cloud API adapter (server-side secret only).
- Telegram Bot API adapter (server-side secret only).
- Campaign queue with DB row locking and retry.
- Inbox storage and inbound webhook endpoint.
- Templates and audit-ready database structure.

## Required external setup
No software can send through WhatsApp/Telegram without valid provider accounts and credentials. Credentials belong on the server, never in the APK.

For a real deployment:
1. Create PostgreSQL.
2. Set backend environment variables.
3. Run `npm ci` in backend.
4. Run `npm run db:migrate`.
5. Run `npm start` or Docker Compose.
6. Create an account using `/api/auth/register`.
7. Connect official Meta/Telegram webhooks to the public HTTPS backend.

The app intentionally does not automate clicks inside WhatsApp and does not bypass provider protections.
