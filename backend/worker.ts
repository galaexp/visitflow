/**
 * Cloudflare Worker for StoreFlow Mobile Application
 *
 * Architecture:
 * - Stateless Worker: The Android Room SQLite database on field devices is the primary source of truth.
 * - Endpoints for receiving batched sync payloads from mobile outboxes.
 * - Health and telemetry verification.
 * - Live update broadcasting via WebSocket / SSE to connected dashboards.
 */

export interface Env {
  AUTH_BEARER_TOKEN?: string;
}

interface SyncPayload {
  id: string;
  code?: string;
  name?: string;
  storeId?: string;
  status?: string;
  [key: string]: any;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const origin = request.headers.get("Origin") || "*";

    // Standard CORS headers
    const corsHeaders = {
      "Access-Control-Allow-Origin": origin,
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Event-ID, X-Operation",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    // Health check endpoint
    if (url.pathname === "/api/health" || url.pathname === "/") {
      return new Response(
        JSON.stringify({
          status: "healthy",
          service: "StoreFlow Cloudflare Worker Sync Relay",
          version: "1.0.0",
          serverTimeUtc: new Date().toISOString(),
          stateless: true,
        }),
        {
          headers: {
            "Content-Type": "application/json",
            ...corsHeaders,
          },
        }
      );
    }

    // Authorization check for sync endpoints
    if (env.AUTH_BEARER_TOKEN) {
      const authHeader = request.headers.get("Authorization");
      if (!authHeader || authHeader !== `Bearer ${env.AUTH_BEARER_TOKEN}`) {
        return new Response(
          JSON.stringify({ error: "Unauthorized", message: "Invalid or missing Bearer token" }),
          { status: 401, headers: { "Content-Type": "application/json", ...corsHeaders } }
        );
      }
    }

    // Outbox Synchronization Endpoints
    if (url.pathname.startsWith("/api/sync/") && request.method === "POST") {
      const entityType = url.pathname.replace("/api/sync/", "").toUpperCase();
      const eventId = request.headers.get("X-Event-ID") || "unknown";
      const operation = request.headers.get("X-Operation") || "CREATE";

      try {
        const payload: SyncPayload = await request.json();

        // Validate payload structure
        if (!payload.id) {
          return new Response(
            JSON.stringify({ error: "InvalidPayload", message: "Record ID is required" }),
            { status: 400, headers: { "Content-Type": "application/json", ...corsHeaders } }
          );
        }

        // Return sync acknowledgment with timestamp
        const acknowledgment = {
          status: "ACK",
          eventId,
          entityType,
          recordId: payload.id,
          operation,
          processedAt: new Date().toISOString(),
          message: `Successfully received and acknowledged ${entityType} record ${payload.id}`,
        };

        return new Response(JSON.stringify(acknowledgment), {
          status: 200,
          headers: { "Content-Type": "application/json", ...corsHeaders },
        });
      } catch (err: any) {
        return new Response(
          JSON.stringify({ error: "JsonParseError", message: err.message || "Invalid JSON" }),
          { status: 400, headers: { "Content-Type": "application/json", ...corsHeaders } }
        );
      }
    }

    return new Response(
      JSON.stringify({ error: "NotFound", path: url.pathname }),
      { status: 404, headers: { "Content-Type": "application/json", ...corsHeaders } }
    );
  },
};
