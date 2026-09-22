const http = require("http");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const PORT = Number(process.env.PORT || 8787);
const HOST = process.env.HOST || "0.0.0.0";
const DATA_DIR = path.join(__dirname, "data");
const DATA_FILE = path.join(DATA_DIR, "store.json");

fs.mkdirSync(DATA_DIR, { recursive: true });

const emptyStore = {
  contacts: [],
  campaigns: [],
  campaignMessages: [],
  templates: [],
  logs: []
};

function loadStore() {
  try {
    return { ...emptyStore, ...JSON.parse(fs.readFileSync(DATA_FILE, "utf8")) };
  } catch (_) {
    return JSON.parse(JSON.stringify(emptyStore));
  }
}

let db = loadStore();
let workerBusy = false;

function saveStore() {
  const tmp = DATA_FILE + ".tmp";
  fs.writeFileSync(tmp, JSON.stringify(db, null, 2));
  fs.renameSync(tmp, DATA_FILE);
}

function id(prefix) {
  return `${prefix}_${Date.now().toString(36)}_${crypto.randomBytes(3).toString("hex")}`;
}

function now() {
  return new Date().toISOString();
}

function json(res, code, body) {
  const out = JSON.stringify(body);
  res.writeHead(code, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(out),
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS"
  });
  res.end(out);
}

function noContent(res) {
  res.writeHead(204, {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS"
  });
  res.end();
}

function body(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", chunk => {
      raw += chunk;
      if (raw.length > 2_000_000) {
        reject(new Error("payload too large"));
        req.destroy();
      }
    });
    req.on("end", () => {
      if (!raw) return resolve({});
      try { resolve(JSON.parse(raw)); }
      catch (e) { reject(new Error("invalid json")); }
    });
    req.on("error", reject);
  });
}

function normalizeContact(input) {
  return {
    id: input.id || id("cnt"),
    name: String(input.name || "عميل").trim(),
    phone: String(input.phone || "").trim(),
    tags: Array.isArray(input.tags) ? input.tags : [],
    channels: input.channels && typeof input.channels === "object" ? input.channels : {},
    status: input.status || "active",
    createdAt: input.createdAt || now(),
    updatedAt: now()
  };
}

function channelTarget(contact, channel) {
  const c = contact.channels || {};
  const direct = c[channel];
  if (direct !== undefined && direct !== null && String(direct).trim()) {
    return String(direct).trim();
  }
  if (channel === "whatsapp") {
    return String(c.whatsapp_wa_id || contact.phone || "").replace(/[^\d]/g, "");
  }
  return "";
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const text = await response.text();
  let data = {};
  try { data = text ? JSON.parse(text) : {}; } catch (_) { data = { raw: text }; }
  return { ok: response.ok, status: response.status, data };
}

async function sendTelegram(chatId, text) {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  if (!token) return { ok: false, error: "TELEGRAM_BOT_TOKEN is not configured" };

  const r = await requestJson(
    `https://api.telegram.org/bot${token}/sendMessage`,
    {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({ chat_id: chatId, text })
    }
  );
  return r.ok && r.data.ok !== false
    ? { ok: true, providerId: String(r.data.result?.message_id || "") }
    : { ok: false, error: r.data?.description || `HTTP ${r.status}` };
}

async function sendWhatsApp(to, text) {
  const token = process.env.WA_ACCESS_TOKEN;
  const phoneId = process.env.WA_PHONE_NUMBER_ID;
  const apiVersion = process.env.WA_API_VERSION || "v23.0";
  if (!token || !phoneId) {
    return { ok: false, error: "WA_ACCESS_TOKEN / WA_PHONE_NUMBER_ID is not configured" };
  }

  const r = await requestJson(
    `https://graph.facebook.com/${apiVersion}/${phoneId}/messages`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
      },
      body: JSON.stringify({
        messaging_product: "whatsapp",
        to,
        type: "text",
        text: { body: text, preview_url: false }
      })
    }
  );

  const providerId = r.data?.messages?.[0]?.id || "";
  return r.ok
    ? { ok: true, providerId }
    : { ok: false, error: r.data?.error?.message || r.data?.message || `HTTP ${r.status}` };
}

async function deliver(message) {
  const contact = db.contacts.find(c => c.id === message.contactId);
  if (!contact) return { ok: false, error: "contact not found" };

  const rendered = String(message.body || "")
    .replace(/\{\{name\}\}/gi, contact.name || "عميل")
    .replace(/\{\{phone\}\}/gi, contact.phone || "");

  const target = channelTarget(contact, message.channel);
  if (!target) return { ok: false, error: `missing ${message.channel} target for contact` };

  if (message.channel === "telegram") {
    return sendTelegram(target, rendered);
  }
  if (message.channel === "whatsapp") {
    return sendWhatsApp(target, rendered);
  }
  return {
    ok: false,
    error: `${message.channel} adapter is not enabled yet. Use an official channel adapter.`
  };
}

function createCampaignMessages(campaign) {
  db.campaignMessages = db.campaignMessages.filter(x => x.campaignId !== campaign.id);
  const contacts = db.contacts.filter(c => (campaign.contactIds || []).includes(c.id));
  let order = 0;
  for (const contact of contacts) {
    db.campaignMessages.push({
      id: id("msg"),
      campaignId: campaign.id,
      contactId: contact.id,
      channel: campaign.channel,
      body: campaign.body,
      status: "queued",
      order: order++,
      attempts: 0,
      createdAt: now()
    });
  }
}

function stats() {
  const activeCampaigns = db.campaigns.filter(c => c.status === "running").length;
  const queued = db.campaignMessages.filter(m => m.status === "queued").length;
  const sent = db.campaignMessages.filter(m => m.status === "sent").length;
  const failed = db.campaignMessages.filter(m => m.status === "failed").length;
  return {
    contacts: db.contacts.length,
    campaigns: db.campaigns.length,
    activeCampaigns,
    queued,
    sent,
    failed
  };
}

async function workerTick() {
  if (workerBusy) return;
  workerBusy = true;
  try {
    const runningIds = new Set(
      db.campaigns.filter(c => c.status === "running").map(c => c.id)
    );
    const next = db.campaignMessages.find(m =>
      m.status === "queued" && runningIds.has(m.campaignId)
    );
    if (!next) return;

    const campaign = db.campaigns.find(c => c.id === next.campaignId);
    next.attempts = Number(next.attempts || 0) + 1;
    next.updatedAt = now();

    const result = await deliver(next);
    if (result.ok) {
      next.status = "sent";
      next.providerId = result.providerId || "";
      next.sentAt = now();
      db.logs.unshift({
        id: id("log"),
        level: "info",
        event: "sent",
        campaignId: next.campaignId,
        contactId: next.contactId,
        channel: next.channel,
        detail: next.providerId || "sent",
        createdAt: now()
      });
    } else {
      next.status = next.attempts < 3 ? "queued" : "failed";
      next.error = result.error;
      db.logs.unshift({
        id: id("log"),
        level: "error",
        event: "send_failed",
        campaignId: next.campaignId,
        contactId: next.contactId,
        channel: next.channel,
        detail: result.error,
        createdAt: now()
      });
    }

    const pending = db.campaignMessages.filter(
      m => m.campaignId === next.campaignId && (m.status === "queued" || m.status === "sending")
    ).length;
    if (campaign && pending === 0) {
      campaign.status = "completed";
      campaign.completedAt = now();
      db.logs.unshift({
        id: id("log"),
        level: "info",
        event: "campaign_completed",
        campaignId: campaign.id,
        detail: campaign.name,
        createdAt: now()
      });
    }

    saveStore();

    const delay = Math.max(500, Number(campaign?.delayMs || 2000));
    setTimeout(() => { workerBusy = false; }, delay);
    return;
  } finally {
    if (workerBusy) workerBusy = false;
  }
}

setInterval(() => { workerTick().catch(() => {}); }, 750);

async function handler(req, res) {
  if (req.method === "OPTIONS") return noContent(res);

  const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);
  const route = url.pathname;

  try {
    if (req.method === "GET" && route === "/api/health") {
      return json(res, 200, { ok: true, service: "bright-bulk-backend", time: now() });
    }

    if (req.method === "GET" && route === "/api/stats") {
      return json(res, 200, stats());
    }

    if (req.method === "GET" && route === "/api/contacts") {
      const q = String(url.searchParams.get("q") || "").trim().toLowerCase();
      const list = q
        ? db.contacts.filter(c =>
            [c.name, c.phone, JSON.stringify(c.channels)].join(" ").toLowerCase().includes(q)
          )
        : db.contacts;
      return json(res, 200, { contacts: list });
    }

    if (req.method === "POST" && route === "/api/contacts") {
      const input = await body(req);
      const contact = normalizeContact(input);
      db.contacts.push(contact);
      saveStore();
      return json(res, 201, contact);
    }

    if (req.method === "POST" && route === "/api/contacts/bulk") {
      const input = await body(req);
      const items = Array.isArray(input.contacts) ? input.contacts : [];
      const created = [];
      const seen = new Set(db.contacts.map(c => `${c.phone}|${c.name}`));
      for (const item of items) {
        const c = normalizeContact(item);
        const key = `${c.phone}|${c.name}`;
        if (seen.has(key)) continue;
        seen.add(key);
        db.contacts.push(c);
        created.push(c);
      }
      saveStore();
      return json(res, 201, { created: created.length, contacts: created });
    }

    if (req.method === "GET" && route === "/api/templates") {
      return json(res, 200, { templates: db.templates });
    }

    if (req.method === "POST" && route === "/api/templates") {
      const input = await body(req);
      const template = {
        id: input.id || id("tpl"),
        name: String(input.name || "Template"),
        channel: String(input.channel || "whatsapp"),
        body: String(input.body || ""),
        createdAt: now(),
        updatedAt: now()
      };
      db.templates.push(template);
      saveStore();
      return json(res, 201, template);
    }

    if (req.method === "GET" && route === "/api/campaigns") {
      const campaigns = db.campaigns.map(c => ({
        ...c,
        sent: db.campaignMessages.filter(m => m.campaignId === c.id && m.status === "sent").length,
        failed: db.campaignMessages.filter(m => m.campaignId === c.id && m.status === "failed").length,
        queued: db.campaignMessages.filter(m => m.campaignId === c.id && m.status === "queued").length
      }));
      return json(res, 200, { campaigns });
    }

    if (req.method === "POST" && route === "/api/campaigns") {
      const input = await body(req);
      const campaign = {
        id: id("cmp"),
        name: String(input.name || "New Campaign"),
        channel: String(input.channel || "whatsapp"),
        body: String(input.body || ""),
        contactIds: Array.isArray(input.contactIds) ? input.contactIds : [],
        delayMs: Math.max(500, Number(input.delayMs || 2000)),
        scheduleAt: input.scheduleAt ? new Date(input.scheduleAt).toISOString() : null,
        status: "draft",
        createdAt: now(),
        updatedAt: now()
      };
      db.campaigns.push(campaign);
      createCampaignMessages(campaign);
      saveStore();
      return json(res, 201, campaign);
    }

    const campaignMatch = route.match(/^\/api\/campaigns\/([^/]+)\/(start|pause|resume|stop)$/);
    if (req.method === "POST" && campaignMatch) {
      const [, cid, action] = campaignMatch;
      const campaign = db.campaigns.find(c => c.id === cid);
      if (!campaign) return json(res, 404, { error: "campaign not found" });

      if (action === "start" || action === "resume") {
        if (campaign.scheduleAt && new Date(campaign.scheduleAt).getTime() > Date.now()) {
          campaign.status = "scheduled";
        } else {
          campaign.status = "running";
        }
      } else if (action === "pause") {
        campaign.status = "paused";
      } else if (action === "stop") {
        campaign.status = "stopped";
        db.campaignMessages
          .filter(m => m.campaignId === cid && m.status === "queued")
          .forEach(m => m.status = "cancelled");
      }

      campaign.updatedAt = now();
      saveStore();
      return json(res, 200, campaign);
    }

    if (req.method === "GET" && route === "/api/logs") {
      const limit = Math.min(500, Math.max(1, Number(url.searchParams.get("limit") || 100)));
      return json(res, 200, { logs: db.logs.slice(0, limit) });
    }

    if (req.method === "POST" && route === "/api/webhooks/inbound") {
      const input = await body(req);
      const channel = String(input.channel || "");
      const externalId = String(input.externalId || "");
      const contact = db.contacts.find(c => channelTarget(c, channel) === externalId);
      if (contact) {
        for (const campaign of db.campaigns.filter(c => c.status === "running")) {
          db.campaignMessages
            .filter(m => m.campaignId === campaign.id && m.contactId === contact.id && m.status === "queued")
            .forEach(m => m.status = "cancelled");
        }
        db.logs.unshift({
          id: id("log"),
          level: "info",
          event: "inbound_reply",
          contactId: contact.id,
          channel,
          detail: "Campaign messages cancelled after inbound reply",
          createdAt: now()
        });
        saveStore();
      }
      return json(res, 200, { ok: true, matched: Boolean(contact) });
    }

    return json(res, 404, { error: "not found" });
  } catch (e) {
    return json(res, 500, { error: e.message || "server error" });
  }
}

http.createServer(handler).listen(PORT, HOST, () => {
  console.log(`Bright Bulk backend listening on http://${HOST}:${PORT}`);
  console.log("Providers: Telegram Bot API + WhatsApp Business Cloud API");
});
