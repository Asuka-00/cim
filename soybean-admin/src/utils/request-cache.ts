/** Short-lived in-memory cache for "list all" dropdown data sources */

const cache = new Map<string, { expireAt: number; value: Promise<unknown> }>();
const DEFAULT_TTL_MS = 30_000;

export function cached<T>(key: string, factory: () => Promise<T>, ttlMs = DEFAULT_TTL_MS): Promise<T> {
  const now = Date.now();
  const hit = cache.get(key);
  if (hit && hit.expireAt > now) {
    return hit.value as Promise<T>;
  }

  const value = factory().catch(err => {
    cache.delete(key);
    throw err;
  });

  cache.set(key, { expireAt: now + ttlMs, value });
  return value;
}

export function invalidateCached(prefix: string) {
  for (const key of cache.keys()) {
    if (key === prefix || key.startsWith(`${prefix}:`)) {
      cache.delete(key);
    }
  }
}
