import { $t, $te } from './index';
import { getMenuI18nKey } from './menu';

function toCamelCase(snake: string) {
  return snake.replace(/_([a-z0-9])/g, (_, c: string) => c.toUpperCase());
}

/** Backend `rtd.<domain>.<module>` → soybean `page.*` module path. */
const MODULE_PATH: Record<string, string> = {
  'system_management.user': 'page.systemManagement.user',
  'system_management.role': 'page.systemManagement.role',
  'system_management.permission': 'page.systemManagement.permission',
  'system_management.tenant': 'page.systemManagement.tenant',
  'system_management.dict_type': 'page.systemManagement.dictType',
  'system_management.dict_item': 'page.systemManagement.dictItem',
  'system_management.system_config': 'page.systemManagement.systemConfig',
  'system_management.release': 'page.systemManagement.release',
  'log_management.log_file': 'page.logManagement.logFile',
  'log_management.operation_log': 'page.logManagement.operationLog'
};

const ACTION_CANDIDATES: Record<string, string[]> = {
  add: ['addTitle', 'add'],
  edit: ['editTitle', 'edit'],
  delete: ['deleteTitle'],
  enable: [],
  close: [],
  activate: ['activate'],
  rollback: ['rollback'],
  upload: ['uploadPackage', 'upload'],
  reset_password: ['resetPassword'],
  download: [],
  preview: ['preview']
};

const COMMON_ACTION: Record<string, App.I18n.I18nKey> = {
  add: 'common.add',
  edit: 'common.edit',
  delete: 'common.delete',
  enable: 'common.enable',
  close: 'common.close',
  download: 'common.download',
  preview: 'common.view'
};

const EXACT_KEY: Record<string, App.I18n.I18nKey> = {
  'buttons.common_delete': 'common.delete',
  'buttons.common_edit': 'common.edit'
};

function translateIfExists(key: string): string | null {
  if (!$te(key)) return null;
  return $t(key as App.I18n.I18nKey);
}

function translateRtdAction(name: string): string | null {
  const parts = name.split('.');
  if (parts[0] !== 'rtd' || parts.length < 3) return null;

  const action = parts[parts.length - 1];
  const moduleKey = parts.slice(1, -1).join('.');
  const modulePath = MODULE_PATH[moduleKey];
  if (modulePath) {
    for (const candidate of ACTION_CANDIDATES[action] ?? [toCamelCase(action)]) {
      const hit = translateIfExists(`${modulePath}.${candidate}`);
      if (hit) return hit;
    }
  }

  return COMMON_ACTION[action] ? $t(COMMON_ACTION[action]) : null;
}

/** Translate backend `permission_name` (menu / button i18n key) for display. */
export function translatePermissionName(name?: string | null): string {
  if (!name) return '';

  const exact = EXACT_KEY[name];
  if (exact) return $t(exact);

  const menuKey = getMenuI18nKey(name);
  if (menuKey) return $t(menuKey);

  if (name.startsWith('buttons.common_')) {
    const commonKey = `common.${toCamelCase(name.slice('buttons.common_'.length))}`;
    const hit = translateIfExists(commonKey);
    if (hit) return hit;
  }

  return translateRtdAction(name) ?? name;
}
