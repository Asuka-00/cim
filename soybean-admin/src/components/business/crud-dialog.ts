import { ref } from 'vue';
import type { VNode } from 'vue';

export function modalWidthFromClass(cls?: unknown): string | undefined {
  const s = Array.isArray(cls)
    ? cls.filter(Boolean).join(' ')
    : typeof cls === 'object' && cls
      ? Object.keys(cls as Record<string, unknown>)
          .filter(key => Boolean((cls as Record<string, unknown>)[key]))
          .join(' ')
      : String(cls ?? '');
  const px = s.match(/\bw-(\d+)px\b/);
  if (px) return `${px[1]}px`;
  const vw = s.match(/\bw-(\d+)vw\b/);
  if (vw) return `${vw[1]}vw`;
  return undefined;
}

function styleToRecord(style?: Record<string, string | number> | string): Record<string, string | number> {
  if (!style) return {};
  if (typeof style === 'object') return { ...style };
  const width = style.match(/width:\s*([^;]+)/i)?.[1]?.trim();
  const maxWidth = style.match(/max-width:\s*([^;]+)/i)?.[1]?.trim();
  return {
    ...(width ? { width } : {}),
    ...(maxWidth ? { maxWidth } : {})
  };
}

export function resolveModalStyle(
  cls?: unknown,
  style?: Record<string, string | number> | string
): Record<string, string | number> {
  const extra = styleToRecord(style);
  return {
    width: extra.width ?? modalWidthFromClass(cls) ?? '520px',
    maxWidth: extra.maxWidth ?? '90vw',
    ...extra
  };
}

export interface CrudDialogContext {
  id: string;
  closeLoading: () => void;
}

export interface CrudDialogOptions {
  title?: string;
  class?: string;
  style?: Record<string, string | number>;
  maskClosable?: boolean;
  closable?: boolean;
  hideFooter?: boolean;
  confirmText?: string;
  cancelText?: string;
  /** Confirm button shows loading until `done()` or `beforeSure` rejects. Default true */
  confirmLoading?: boolean;
  content: () => VNode;
  beforeSure?: (done: () => void, ctx: CrudDialogContext) => void | Promise<void>;
  beforeCancel?: (done: () => void) => void | Promise<void>;
  onClose?: () => void;
}

export interface CrudDialogItem extends CrudDialogOptions {
  id: string;
  show: boolean;
  loading: boolean;
}

const dialogStore = ref<CrudDialogItem[]>([]);
let seed = 0;

function nextId() {
  seed += 1;
  return `crud-dialog-${seed}`;
}

export function addDialog(options: CrudDialogOptions) {
  const id = nextId();
  dialogStore.value.push({
    confirmLoading: true,
    maskClosable: true,
    closable: true,
    ...options,
    id,
    show: true,
    loading: false
  });
  return id;
}

export function closeDialog(id: string) {
  const item = dialogStore.value.find(d => d.id === id);
  if (!item) return;
  item.show = false;
  item.onClose?.();
  window.setTimeout(() => {
    dialogStore.value = dialogStore.value.filter(d => d.id !== id);
  }, 250);
}

export function closeAllDialog() {
  const ids = dialogStore.value.map(d => d.id);
  ids.forEach(closeDialog);
}

export function updateDialog(id: string, patch: Partial<CrudDialogOptions>) {
  const item = dialogStore.value.find(d => d.id === id);
  if (!item) return;
  Object.assign(item, patch);
}

export function setDialogLoading(id: string, loading: boolean) {
  const item = dialogStore.value.find(d => d.id === id);
  if (item) item.loading = loading;
}

export { dialogStore };
