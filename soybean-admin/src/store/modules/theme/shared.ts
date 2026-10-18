import type { GlobalThemeOverrides } from 'naive-ui';
import { defu } from 'defu';
import { addColorAlpha, getColorPalette, getPaletteColorByNumber, getRgb } from '@sa/color';
import { DARK_CLASS } from '@/constants/app';
import { toggleHtmlClass } from '@/utils/common';
import { localStg } from '@/utils/storage';
import { overrideThemeSettings, themeSettings } from '@/theme/settings';
import { themeVars } from '@/theme/vars';

/** Init theme settings from localStorage, falling back to defaults. */
export function initThemeSettings() {
  const localSettings = localStg.get('themeSettings');

  let settings = defu(localSettings, themeSettings);

  // New publish can force a subset of defaults via `overrideThemeSettings`
  const isOverride = localStg.get('overrideThemeFlag') === BUILD_TIME;

  if (!isOverride) {
    settings = defu(overrideThemeSettings, settings);

    localStg.set('overrideThemeFlag', BUILD_TIME);
  }

  return settings;
}

/**
 * create theme token css vars value by theme settings
 *
 * @param colors Theme colors
 * @param tokens Theme setting tokens
 * @param [recommended=false] Use recommended color. Default is `false`
 */
export function createThemeToken(
  colors: App.Theme.ThemeColor,
  tokens?: App.Theme.ThemeSetting['tokens'],
  recommended = false
) {
  const paletteColors = createThemePaletteColors(colors, recommended);

  const { light, dark } = tokens || themeSettings.tokens;

  const themeTokens: App.Theme.ThemeTokenCSSVars = {
    colors: {
      ...paletteColors,
      nprogress: paletteColors.primary,
      ...light.colors
    },
    boxShadow: {
      ...light.boxShadow
    }
  };

  const darkThemeTokens: App.Theme.ThemeTokenCSSVars = {
    colors: {
      ...themeTokens.colors,
      ...dark?.colors
    },
    boxShadow: {
      ...themeTokens.boxShadow,
      ...dark?.boxShadow
    }
  };

  return {
    themeTokens,
    darkThemeTokens
  };
}

/**
 * Create theme palette colors
 *
 * @param colors Theme colors
 * @param [recommended=false] Use recommended color. Default is `false`
 */
function createThemePaletteColors(colors: App.Theme.ThemeColor, recommended = false) {
  const colorKeys = Object.keys(colors) as App.Theme.ThemeColorKey[];
  const colorPaletteVar = {} as App.Theme.ThemePaletteColor;

  colorKeys.forEach(key => {
    const colorMap = getColorPalette(colors[key], recommended);

    colorPaletteVar[key] = colorMap.get(500)!;

    colorMap.forEach((hex, number) => {
      colorPaletteVar[`${key}-${number}`] = hex;
    });
  });

  return colorPaletteVar;
}

/**
 * Get css var by tokens
 *
 * @param tokens Theme base tokens
 */
function getCssVarByTokens(tokens: App.Theme.BaseToken) {
  const styles: string[] = [];

  function removeVarPrefix(value: string) {
    return value.replace('var(', '').replace(')', '');
  }

  function removeRgbPrefix(value: string) {
    return value.replace('rgb(', '').replace(')', '');
  }

  for (const [key, tokenValues] of Object.entries(themeVars)) {
    for (const [tokenKey, tokenValue] of Object.entries(tokenValues)) {
      let cssVarsKey = removeVarPrefix(tokenValue);
      let cssValue = tokens[key][tokenKey];

      if (key === 'colors') {
        cssVarsKey = removeRgbPrefix(cssVarsKey);
        const { r, g, b } = getRgb(cssValue);
        cssValue = `${r} ${g} ${b}`;
      }

      styles.push(`${cssVarsKey}: ${cssValue}`);
    }
  }

  const styleStr = styles.join(';');

  return styleStr;
}

/**
 * Add theme vars to global
 *
 * @param tokens
 */
export function addThemeVarsToGlobal(tokens: App.Theme.BaseToken, darkTokens: App.Theme.BaseToken) {
  const cssVarStr = getCssVarByTokens(tokens);
  const darkCssVarStr = getCssVarByTokens(darkTokens);

  const css = `
    :root {
      ${cssVarStr}
    }
  `;

  const darkCss = `
    html.${DARK_CLASS} {
      ${darkCssVarStr}
    }
  `;

  const styleId = 'theme-vars';

  const style = document.querySelector(`#${styleId}`) || document.createElement('style');

  style.id = styleId;

  style.textContent = css + darkCss;

  document.head.appendChild(style);
}

/**
 * Toggle css dark mode
 *
 * @param darkMode Is dark mode
 */
export function toggleCssDarkMode(darkMode = false) {
  const { add, remove } = toggleHtmlClass(DARK_CLASS);

  if (darkMode) {
    add();
  } else {
    remove();
  }
}

/**
 * Toggle auxiliary color modes
 *
 * @param grayscaleMode
 * @param colourWeakness
 */
export function toggleAuxiliaryColorModes(grayscaleMode = false, colourWeakness = false) {
  const htmlElement = document.documentElement;
  htmlElement.style.filter = [grayscaleMode ? 'grayscale(100%)' : '', colourWeakness ? 'invert(80%)' : '']
    .filter(Boolean)
    .join(' ');
}

type NaiveColorScene = '' | 'Suppl' | 'Hover' | 'Pressed' | 'Active';
type NaiveColorKey = `${App.Theme.ThemeColorKey}Color${NaiveColorScene}`;
type NaiveThemeColor = Partial<Record<NaiveColorKey, string>>;
interface NaiveColorAction {
  scene: NaiveColorScene;
  handler: (color: string) => string;
}

/**
 * Get naive theme colors
 *
 * @param colors Theme colors
 * @param [recommended=false] Use recommended color. Default is `false`
 */
function getNaiveThemeColors(colors: App.Theme.ThemeColor, recommended = false) {
  const colorActions: NaiveColorAction[] = [
    { scene: '', handler: color => color },
    { scene: 'Suppl', handler: color => color },
    { scene: 'Hover', handler: color => getPaletteColorByNumber(color, 500, recommended) },
    { scene: 'Pressed', handler: color => getPaletteColorByNumber(color, 700, recommended) },
    { scene: 'Active', handler: color => addColorAlpha(color, 0.1) }
  ];

  const themeColors: NaiveThemeColor = {};

  const colorEntries = Object.entries(colors) as [App.Theme.ThemeColorKey, string][];

  colorEntries.forEach(color => {
    colorActions.forEach(action => {
      const [colorType, colorValue] = color;
      const colorKey: NaiveColorKey = `${colorType}Color${action.scene}`;
      themeColors[colorKey] = action.handler(colorValue);
    });
  });

  return themeColors;
}

const UI_SIZE_SCALE: Record<UnionKey.ThemeUiSize, number> = {
  small: 0.875,
  default: 1,
  large: 1.125
};

const NAIVE_SIZE_BASE = {
  fontSize: 14,
  fontSizeMini: 12,
  fontSizeTiny: 12,
  fontSizeSmall: 14,
  fontSizeMedium: 14,
  fontSizeLarge: 15,
  fontSizeHuge: 16,
  heightMini: 16,
  heightTiny: 22,
  heightSmall: 28,
  heightMedium: 34,
  heightLarge: 40,
  heightHuge: 46,
  menuItemHeight: 42
} as const;

function scalePx(base: number, scale: number) {
  return `${Math.round(base * scale)}px`;
}

/** Naive common/component tokens that follow the global UI size. */
export function getUiSizeThemeOverrides(uiSize: UnionKey.ThemeUiSize): GlobalThemeOverrides {
  const scale = UI_SIZE_SCALE[uiSize] ?? 1;

  return {
    common: {
      fontSize: scalePx(NAIVE_SIZE_BASE.fontSize, scale),
      fontSizeMini: scalePx(NAIVE_SIZE_BASE.fontSizeMini, scale),
      fontSizeTiny: scalePx(NAIVE_SIZE_BASE.fontSizeTiny, scale),
      fontSizeSmall: scalePx(NAIVE_SIZE_BASE.fontSizeSmall, scale),
      fontSizeMedium: scalePx(NAIVE_SIZE_BASE.fontSizeMedium, scale),
      fontSizeLarge: scalePx(NAIVE_SIZE_BASE.fontSizeLarge, scale),
      fontSizeHuge: scalePx(NAIVE_SIZE_BASE.fontSizeHuge, scale),
      heightMini: scalePx(NAIVE_SIZE_BASE.heightMini, scale),
      heightTiny: scalePx(NAIVE_SIZE_BASE.heightTiny, scale),
      heightSmall: scalePx(NAIVE_SIZE_BASE.heightSmall, scale),
      heightMedium: scalePx(NAIVE_SIZE_BASE.heightMedium, scale),
      heightLarge: scalePx(NAIVE_SIZE_BASE.heightLarge, scale),
      heightHuge: scalePx(NAIVE_SIZE_BASE.heightHuge, scale)
    },
    Menu: {
      fontSize: scalePx(NAIVE_SIZE_BASE.fontSize, scale),
      itemHeight: scalePx(NAIVE_SIZE_BASE.menuItemHeight, scale)
    },
    DataTable: {
      fontSizeSmall: scalePx(NAIVE_SIZE_BASE.fontSizeSmall, scale),
      fontSizeMedium: scalePx(NAIVE_SIZE_BASE.fontSizeMedium, scale),
      fontSizeLarge: scalePx(NAIVE_SIZE_BASE.fontSizeLarge, scale)
    }
  };
}

/** Persist the UI size on <html> so CSS can scale leftover page text. */
export function toggleHtmlUiSize(uiSize: UnionKey.ThemeUiSize = 'default') {
  document.documentElement.dataset.uiSize = uiSize;
}

/**
 * Get naive theme
 *
 * @param colors Theme colors
 * @param settings Theme settings object
 * @param overrides Optional manual overrides from preset
 */
export function getNaiveTheme(
  colors: App.Theme.ThemeColor,
  settings: App.Theme.ThemeSetting,
  overrides?: GlobalThemeOverrides
) {
  const { primary: colorLoading } = colors;
  const uiSizeTheme = getUiSizeThemeOverrides(settings.uiSize ?? 'default');

  const theme: GlobalThemeOverrides = {
    common: {
      ...getNaiveThemeColors(colors, settings.recommendColor),
      borderRadius: `${settings.themeRadius}px`,
      ...uiSizeTheme.common
    },
    LoadingBar: {
      colorLoading
    },
    Tag: {
      borderRadius: `${settings.themeRadius}px`
    },
    Menu: uiSizeTheme.Menu,
    DataTable: uiSizeTheme.DataTable
  };

  // If there are overrides, merge them with priority
  // overrides has higher priority than auto-generated theme
  return overrides ? defu(overrides, theme) : theme;
}
