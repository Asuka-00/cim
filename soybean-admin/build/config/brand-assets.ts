import fs from 'node:fs';
import path from 'node:path';

/** Preference: vector first, then raster, then ico */
export const BRAND_IMAGE_EXTS = ['svg', 'png', 'ico'] as const;

export type BrandImageExt = (typeof BRAND_IMAGE_EXTS)[number];

export const BRAND_IMAGE_MIME: Record<BrandImageExt, string> = {
  svg: 'image/svg+xml',
  png: 'image/png',
  ico: 'image/x-icon'
};

export function extOfBrandPath(filePath: string): BrandImageExt | null {
  const m = filePath.toLowerCase().match(/\.(svg|png|ico)(?:$|\?)/);
  return m ? (m[1] as BrandImageExt) : null;
}

export function mimeOfBrandPath(filePath: string): string {
  const ext = extOfBrandPath(filePath);
  return ext ? BRAND_IMAGE_MIME[ext] : 'image/x-icon';
}

/** Named defaults copied from the brand SVGs, before generic logo/favicon.* */
const BRAND_NAMED_FILES: Record<'logo' | 'favicon', string> = {
  logo: 'semi-logo-default.svg',
  favicon: 'semi-logo-small.svg'
};

/** First existing named SVG, else `public/{name}.{svg|png|ico}` */
export function resolveBrandPublicFile(publicDir: string, name: 'logo' | 'favicon'): string {
  const named = BRAND_NAMED_FILES[name];
  if (fs.existsSync(path.join(publicDir, named))) {
    return `/${named}`;
  }
  for (const ext of BRAND_IMAGE_EXTS) {
    if (fs.existsSync(path.join(publicDir, `${name}.${ext}`))) {
      return `/${name}.${ext}`;
    }
  }
  return '';
}

export function resolveBrandPublicAssets(rootDir: string) {
  const publicDir = path.join(rootDir, 'public');
  const logo = resolveBrandPublicFile(publicDir, 'logo');
  const favicon = resolveBrandPublicFile(publicDir, 'favicon') || logo;
  return { logo, favicon, faviconMime: mimeOfBrandPath(favicon) };
}
