const DATETIME_TO_SECONDS = /^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?$/;

/** Display datetime up to seconds. Leaves non-datetime text unchanged. */
export function formatDisplayDateTime(value: unknown): string {
  if (value == null || value === '') return '-';
  const text = String(value).trim();
  const matched = text.match(DATETIME_TO_SECONDS);
  if (matched) return `${matched[1]} ${matched[2]}`;
  return text;
}

export function isTimeColumnKey(key: string) {
  return /(?:At|Time)$/i.test(key) || key === 'ts' || key === 'effectiveFrom' || key === 'effectiveTo';
}

/** Format Date as backend LocalDateTime: `YYYY-MM-DD HH:mm:ss` (local clock, no Z). */
export function toLocalDateTime(ms: number | null | undefined): string | null {
  if (ms == null || Number.isNaN(Number(ms))) return null;
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

export function rangeToDayBounds(range: [number, number] | null | undefined) {
  if (!range || range.length !== 2) return null;
  return {
    start: `${toLocalDate(range[0])} 00:00:00`,
    end: `${toLocalDate(range[1])} 23:59:59`
  };
}

export function toLocalDate(ms: number): string {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
