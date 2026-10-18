import { $t } from './index';
import zhMenu from './langs/zh-cn/menu';

/** Title key convention used by backend `sys_permission.meta.title` / `permission_name`. */
const BACKEND_MENU_KEY_PREFIX = 'menus.pure_';

const menuKeys = new Set(Object.keys(zhMenu));

function toCamelCase(snake: string) {
  return snake.replace(/_([a-z0-9])/g, (_, c: string) => c.toUpperCase());
}

/**
 * Map a backend menu title key to a local i18n key.
 *
 * `menus.pure_realtime_board` → `menu.realtimeBoard`; returns `null` when the key is unknown.
 */
export function getMenuI18nKey(titleKey?: string | null): App.I18n.I18nKey | null {
  if (!titleKey?.startsWith(BACKEND_MENU_KEY_PREFIX)) return null;

  const key = toCamelCase(titleKey.slice(BACKEND_MENU_KEY_PREFIX.length));

  return menuKeys.has(key) ? (`menu.${key}` as App.I18n.I18nKey) : null;
}

/** Translate a backend menu title key, falling back to the raw value. */
export function translateMenuTitle(titleKey?: string | null): string {
  const i18nKey = getMenuI18nKey(titleKey);

  return i18nKey ? $t(i18nKey) : (titleKey ?? '');
}
