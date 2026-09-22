# Bright Bulk 2.0 Backend

Local/dev backend for Bright Bulk. It is intentionally dependency-light: Node.js built-ins only.

## Start

```bash
cd ~/BrightBulk/backend
cp .env.example .env
# export values in the shell or use your preferred env loader
node server.js
```

The Android app can use:

`http://127.0.0.1:8787`

## Channel rules

- Telegram targets use a Telegram `chat_id`. A phone number by itself is not a Telegram delivery target.
- WhatsApp automatic sending uses the official WhatsApp Business Cloud API and requires valid Meta credentials.
- WhatsApp app UI automation is not used as the campaign engine.
- Instagram/Messenger adapters are intentionally not faked. They can be added once the corresponding official Meta permissions and account connections are available.

## Core API

- `GET /api/health`
- `GET /api/stats`
- `GET /api/contacts`
- `POST /api/contacts`
- `POST /api/contacts/bulk`
- `GET /api/templates`
- `POST /api/templates`
- `GET /api/campaigns`
- `POST /api/campaigns`
- `POST /api/campaigns/:id/start`
- `POST /api/campaigns/:id/pause`
- `POST /api/campaigns/:id/resume`
- `POST /api/campaigns/:id/stop`
- `GET /api/logs`
- `POST /api/webhooks/inbound`

Data is stored in `backend/data/store.json` for the first mobile/dev milestone. For production, replace the JSON store with PostgreSQL/SQLite plus authentication, encrypted secrets, webhook verification, and durable job locking.
