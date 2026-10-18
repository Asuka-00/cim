const BRAND_IMAGE_EXTS = ['svg', 'png', 'ico'] as const;
const BRAND_IMAGE_MIME: Record<(typeof BRAND_IMAGE_EXTS)[number], string> = {
  svg: 'image/svg+xml',
  png: 'image/png',
  ico: 'image/x-icon'
};

/** Public asset path with Vite base */
export function resolvePublicAsset(path?: string | null): string {
  if (!path) return '';
  if (/^(https?:)?\/\//.test(path) || path.startsWith('data:') || path.startsWith('blob:')) {
    return path;
  }
  const base = (import.meta.env.BASE_URL || '/').replace(/\/$/, '');
  const asset = path.startsWith('/') ? path : `/${path}`;
  return base === '' || base === '/' ? asset : `${base}${asset}`;
}

/** SVG source → data URL (for img / favicon, avoid v-html) */
export function svgToDataUrl(svg?: string | null): string {
  if (!svg || !svg.trim()) return '';
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg.trim())}`;
}

export function mimeOfBrandHref(href: string): string {
  if (href.startsWith('data:image/svg')) return BRAND_IMAGE_MIME.svg;
  if (href.startsWith('data:image/png')) return BRAND_IMAGE_MIME.png;
  const ext = href.toLowerCase().match(/\.(svg|png|ico)(?:$|\?)/)?.[1] as (typeof BRAND_IMAGE_EXTS)[number] | undefined;
  return ext ? BRAND_IMAGE_MIME[ext] : BRAND_IMAGE_MIME.ico;
}

export function defaultLogoUrl(): string {
  return resolvePublicAsset(BRAND_DEFAULT_LOGO);
}

export function defaultFaviconUrl(): string {
  return resolvePublicAsset(BRAND_DEFAULT_FAVICON || BRAND_DEFAULT_LOGO);
}

export function defaultLoginBackgroundUrl(): string {
  return resolvePublicAsset('/semi-logo-bg.svg');
}

/** Apply favicon link href + matching MIME (svg / png / ico) */
export function applyFavicon(href?: string | null): void {
  if (!href || typeof document === 'undefined') return;

  let link = document.querySelector<HTMLLinkElement>("link[rel='icon']");
  if (!link) {
    link = document.createElement('link');
    link.rel = 'icon';
    document.head.appendChild(link);
  }
  link.type = mimeOfBrandHref(href);
  link.href = href;
}

export function resolveFaviconHref(faviconSvg?: string | null): string {
  const fromSvg = svgToDataUrl(faviconSvg);
  if (fromSvg) return fromSvg;
  return defaultFaviconUrl();
}
