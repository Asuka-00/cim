import type { useSse } from './sse';

type SseClient = ReturnType<typeof useSse>;

let sseClient: SseClient | null = null;
let suppressForceLogout = false;

export function bindSessionSse(client: SseClient) {
  sseClient = client;
}

export function shouldIgnoreForceLogout() {
  return suppressForceLogout;
}

/** Close SSE and ignore FORCE_LOGOUT while tenant switch re-issues the session. */
export function pauseSessionEventsForTenantSwitch() {
  suppressForceLogout = true;
  sseClient?.disconnect();
}

export function resumeSessionEvents() {
  suppressForceLogout = false;
  sseClient?.connect();
}
