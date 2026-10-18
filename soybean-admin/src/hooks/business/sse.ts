import { ref } from 'vue';
import { localStg } from '@/utils/storage';

const SSE_URL = '/api/v1/auth/sse/events';
const RECONNECT_BASE_DELAY_MS = 3000;
const RECONNECT_BACKOFF = 1.5;
const MAX_RECONNECT_ATTEMPTS = 10;

/** Server-sent event types (`SseNotificationDTO.NotificationType`) */
export enum SseEventType {
  PERMISSION_CHANGED = 'PERMISSION_CHANGED',
  FORCE_LOGOUT = 'FORCE_LOGOUT',
  KEEP_ALIVE = 'KEEP_ALIVE'
}

type SseHandler<T = unknown> = (data: T) => void;

interface SseNotification {
  type?: string;
  data?: unknown;
}

/**
 * Authenticated SSE client.
 *
 * Uses `fetch` streaming instead of `EventSource` so the `Authorization` header can be sent. Reconnects with
 * exponential backoff while a token is present.
 */
export function useSse() {
  const connected = ref(false);
  const handlers = new Map<string, Set<SseHandler>>();

  let abortController: AbortController | null = null;
  let reconnectAttempts = 0;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  function on<T = unknown>(type: SseEventType, handler: SseHandler<T>) {
    if (!handlers.has(type)) handlers.set(type, new Set());
    handlers.get(type)!.add(handler as SseHandler);

    return () => handlers.get(type)?.delete(handler as SseHandler);
  }

  function emit(type: string, data: unknown) {
    handlers.get(type)?.forEach(handler => handler(data));
  }

  /** Named SSE event wins; otherwise unwrap the `{ type, data }` notification envelope. */
  function dispatch(eventName: string, raw: string) {
    try {
      const notification = JSON.parse(raw) as SseNotification;
      const type = eventName && eventName !== 'message' ? eventName : notification.type || 'message';
      const payload =
        notification && typeof notification === 'object' && 'data' in notification ? notification.data : notification;
      emit(type, payload);
    } catch {
      emit(eventName || 'message', raw);
    }
  }

  function parseChunk(chunk: string) {
    let event = 'message';
    let data = '';

    chunk.split('\n').forEach(line => {
      if (line.startsWith('event:')) event = line.slice(6).trim();
      if (line.startsWith('data:')) data += line.slice(5).trim();
    });

    if (data) dispatch(event, data);
  }

  async function connect() {
    disconnect(false);

    const token = localStg.get('token');
    if (!token) return;

    abortController = new AbortController();

    try {
      const res = await fetch(SSE_URL, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        signal: abortController.signal
      });
      if (!res.ok || !res.body) {
        throw new Error(`SSE HTTP ${res.status}`);
      }

      connected.value = true;
      reconnectAttempts = 0;

      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const chunks = buffer.split('\n\n');
        buffer = chunks.pop() || '';
        chunks.forEach(parseChunk);
      }

      if (!abortController?.signal.aborted) {
        scheduleReconnect();
      }
    } catch (error) {
      if ((error as Error).name !== 'AbortError') {
        console.warn('[sse] disconnected', error);
        scheduleReconnect();
      }
    } finally {
      connected.value = false;
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer || !localStg.get('token')) return;
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      console.warn('[sse] max reconnect attempts reached');
      return;
    }

    const delay = RECONNECT_BASE_DELAY_MS * RECONNECT_BACKOFF ** reconnectAttempts;
    reconnectAttempts += 1;
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      connect();
    }, delay);
  }

  function disconnect(resetAttempts = true) {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (resetAttempts) reconnectAttempts = 0;
    abortController?.abort();
    abortController = null;
    connected.value = false;
  }

  return { connected, connect, disconnect, on };
}
