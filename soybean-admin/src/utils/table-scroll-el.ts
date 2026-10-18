function overflowYOf(el: HTMLElement) {
  return getComputedStyle(el).overflowY;
}

function canOverflowY(el: HTMLElement) {
  const oy = overflowYOf(el);
  return oy === 'auto' || oy === 'scroll';
}

/** Naive DataTable 真正带 overflow 的节点（表体是 NScrollbar，滚的是内部 container）。 */
export function findTableScrollEl(root: HTMLElement | null): HTMLElement | null {
  if (!root) return null;
  const body = root.querySelector<HTMLElement>('.n-data-table-base-table-body');
  const candidates: HTMLElement[] = [];
  if (body) {
    const inner = body.querySelector<HTMLElement>('.n-scrollbar-container');
    if (inner) candidates.push(inner);
    candidates.push(body);
  } else {
    const fallback = root.querySelector<HTMLElement>('.n-scrollbar-container');
    if (fallback) candidates.push(fallback);
  }
  if (candidates.length === 0) return null;
  return (
    candidates.find(el => canOverflowY(el) && el.scrollHeight > el.clientHeight + 1) ??
    candidates.find(canOverflowY) ??
    candidates[0]
  );
}
