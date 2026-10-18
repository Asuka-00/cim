import type { Plugin } from 'vite';

interface HtmlBrandAssets {
  favicon: string;
  faviconMime: string;
}

export function setupHtmlPlugin(buildTime: string, brandAssets: HtmlBrandAssets) {
  const plugin: Plugin = {
    name: 'html-plugin',
    transformIndexHtml(html) {
      const icon = brandAssets.favicon
        ? `<link rel="icon" type="${brandAssets.faviconMime}" href="${brandAssets.favicon}" />`
        : '';
      return html
        .replace('<head>', `<head>\n    <meta name="buildTime" content="${buildTime}">`)
        .replace(/<link\s+rel="icon"[^>]*>/, icon);
    }
  };

  return plugin;
}
