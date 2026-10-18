import type { DataTableColumns } from 'naive-ui';
import { formatDisplayDateTime, isTimeColumnKey } from '@/utils/datetime';

const SKIP_KEYS = new Set(['index', 'actions', 'operation', 'operate', 'selection']);

export function withCellEllipsis<T>(columns: DataTableColumns<T>): DataTableColumns<T> {
  return columns.map(col => {
    if (!col || typeof col !== 'object') return col;

    const typed = col as {
      type?: string;
      key?: string | number;
      ellipsis?: unknown;
      render?: (row: T, index: number) => unknown;
      children?: DataTableColumns<T>;
    };

    if (typed.type === 'selection' || typed.type === 'expand') {
      return col;
    }

    if (Array.isArray(typed.children)) {
      return { ...col, children: withCellEllipsis(typed.children) };
    }

    const key = String(typed.key ?? '');
    if (SKIP_KEYS.has(key)) return col;

    const next = {
      ...col,
      ellipsis: typed.ellipsis ?? { tooltip: true }
    } as typeof col & { render?: (row: T, index: number) => unknown };

    if (isTimeColumnKey(key)) {
      const prevRender = typed.render;
      next.render = (row: T, index: number) => {
        if (prevRender) {
          const out = prevRender(row, index);
          return typeof out === 'string' || typeof out === 'number' ? formatDisplayDateTime(out) : out;
        }
        return formatDisplayDateTime((row as Record<string, unknown>)[key]);
      };
    }

    return next;
  }) as DataTableColumns<T>;
}
