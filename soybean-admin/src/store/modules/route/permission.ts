import type { RouteMeta, RouteRecordRaw } from 'vue-router';
import type { ElegantConstRoute } from '@elegant-router/types';
import type { PermissionVO } from '@/service/api/rtd/system-management/permission';
import { PermissionType, VisibilityState } from '@/service/api/common';
import { layouts } from '@/router/elegant/imports';
import { getMenuI18nKey } from '@/locales/menu';

interface PermissionRouteNode {
  id: string;
  parentId: string | null;
  path: string;
  name: string;
  component: string | null;
  meta: RouteMeta;
  children?: PermissionRouteNode[];
}

export interface PermissionRouteBundle {
  /** Menu tree consumed by the global menu / breadcrumb / search */
  menuRoutes: ElegantConstRoute[];
  /** Flat vue-router records mounted under the `root` layout */
  vueRoutes: RouteRecordRaw[];
  /** Route name of the first reachable page */
  home: string;
  /** Path of the first reachable page */
  homePath: string;
}

const NOT_FOUND_VIEW = () => import('@/views/_builtin/404/index.vue');

/** Backend `component` values are absolute `/src/views/**` paths, so the glob must be absolute as well. */
const viewModules = import.meta.glob('/src/views/**/*.vue');

function parseMeta(raw: string | null | undefined): Record<string, unknown> {
  if (!raw) return {};
  try {
    return JSON.parse(raw) as Record<string, unknown>;
  } catch {
    return {};
  }
}

function normalizePath(path: string) {
  return path.startsWith('/') ? path : `/${path}`;
}

/**
 * Convert a backend component path to the route / component name used by views.
 *
 * `/src/views/rtd/system-management/user/index.vue` → `rtd_system-management_user`
 */
export function componentPathToRouteName(component: string): string {
  return component
    .replace(/^\/?src\/views\//, '')
    .replace(/\.vue$/, '')
    .replace(/\/index$/, '')
    .replace(/\//g, '_');
}

function resolveViewComponent(component: string | null, path: string) {
  const keys = Object.keys(viewModules);

  let key: string | undefined;
  if (component) {
    const normalized = normalizePath(component);
    const asIndex = normalized.replace(/\.vue$/, '/index.vue').replace(/\/index\/index\.vue$/, '/index.vue');
    key = keys.find(k => k.includes(normalized) || k.includes(asIndex));
  }
  if (!key && path) {
    const hint = path.replace(/^\//, '');
    key = keys.find(k => k.includes(`/views/${hint}/`) || k.includes(`/views/${hint}.`));
  }

  if (key) {
    return viewModules[key];
  }

  console.warn(`[route] view not found: component=${component} path=${path}`);
  return NOT_FOUND_VIEW;
}

function buildTree(list: PermissionRouteNode[]) {
  const map = new Map<string, PermissionRouteNode>();
  const roots: PermissionRouteNode[] = [];

  list.forEach(item => {
    map.set(item.id, { ...item, children: [] });
  });

  map.forEach(item => {
    const parent = item.parentId ? map.get(item.parentId) : undefined;
    if (parent) {
      parent.children!.push(item);
    } else {
      roots.push(item);
    }
  });

  return roots;
}

function toMenuRoute(node: PermissionRouteNode): ElegantConstRoute {
  const children = (node.children || []).map(toMenuRoute);
  return {
    name: node.name,
    path: normalizePath(node.path),
    meta: node.meta,
    children: children.length ? children : undefined
  } as ElegantConstRoute;
}

function collectLeaves(node: PermissionRouteNode, acc: PermissionRouteNode[] = []): PermissionRouteNode[] {
  if (!node.children?.length) {
    if (node.component) {
      acc.push(node);
    }
    return acc;
  }
  node.children.forEach(child => collectLeaves(child, acc));
  return acc;
}

function toRouteNode(permission: PermissionVO): PermissionRouteNode {
  const meta = parseMeta(permission.meta);
  const titleKey = (meta.title as string) || permission.permissionName;
  const i18nKey = getMenuI18nKey(titleKey);

  return {
    id: permission.id,
    parentId: permission.parentId,
    path: permission.path || '/',
    name: permission.component ? componentPathToRouteName(permission.component) : permission.permissionCode,
    component: permission.component || null,
    meta: {
      title: i18nKey ?? titleKey,
      i18nKey: i18nKey ?? undefined,
      icon: (meta.icon as string) || undefined,
      order: permission.permissionOrder,
      hideInMenu: permission.visible === VisibilityState.HIDDEN,
      keepAlive: Boolean(meta.keepAlive)
    }
  };
}

/** Convert backend menu permissions into the menu tree plus vue-router records. */
export function buildPermissionRoutes(permissions: PermissionVO[]): PermissionRouteBundle {
  const menuPermissions = permissions.filter(p => p.type === PermissionType.MENU);

  if (!menuPermissions.length) {
    return { menuRoutes: [], vueRoutes: [], home: '403', homePath: '/403' };
  }

  const nodes = menuPermissions.map(toRouteNode);
  nodes.sort((a, b) => (a.meta.order ?? 0) - (b.meta.order ?? 0));

  const tree = buildTree(nodes);
  const leaves = tree.flatMap(node => collectLeaves(node));

  const childRoutes: RouteRecordRaw[] = leaves.map(leaf => ({
    path: normalizePath(leaf.path),
    name: leaf.name,
    component: resolveViewComponent(leaf.component, leaf.path),
    meta: leaf.meta
  }));

  const preferred = leaves[0];
  const home = preferred?.name || '403';
  const homePath = preferred ? normalizePath(preferred.path) : '/403';

  const vueRoutes: RouteRecordRaw[] = [
    {
      path: '/',
      name: 'root',
      component: layouts.base,
      redirect: homePath,
      meta: { title: 'root', hideInMenu: true },
      children: childRoutes
    }
  ];

  return { menuRoutes: tree.map(toMenuRoute), vueRoutes, home, homePath };
}

/** Button permission codes (`rtd:xxx:add|edit|delete`) owned by the current user. */
export function extractButtonPermissionCodes(permissions: PermissionVO[]): string[] {
  return permissions
    .filter(p => p.type === PermissionType.BUTTON)
    .map(p => p.permissionCode)
    .filter(Boolean);
}
