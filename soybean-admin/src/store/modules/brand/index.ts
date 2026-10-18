import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { fetchPublicBrand } from '@/service/api/auth';
import { SetupStoreId } from '@/enum';
import {
  applyFavicon,
  defaultLoginBackgroundUrl,
  defaultLogoUrl,
  resolveFaviconHref,
  resolvePublicAsset,
  svgToDataUrl
} from '@/utils/brand';

const DEFAULT_TITLE = import.meta.env.VITE_APP_TITLE || 'RTD';

export const useBrandStore = defineStore(SetupStoreId.Brand, () => {
  const title = ref<string | null>(null);
  const poweredBy = ref<string | null>(null);
  const logoSvg = ref<string | null>(null);
  const faviconSvg = ref<string | null>(null);
  const loginTitleSvg = ref<string | null>(null);
  const loginBgSvg = ref<string | null>(null);
  const showLoginTitleImage = ref<boolean | null>(null);
  const tenantId = ref<string | undefined>(undefined);
  const loaded = ref(false);

  const displayTitle = computed(() => (title.value?.trim() ? title.value.trim() : DEFAULT_TITLE));

  const logoUrl = computed(() => svgToDataUrl(logoSvg.value) || defaultLogoUrl());

  const loginTitleImageUrl = computed(() => {
    const fromSvg = svgToDataUrl(loginTitleSvg.value);
    if (!fromSvg) return '';
    if (showLoginTitleImage.value === false) return '';
    return fromSvg;
  });

  const showLoginTitle = computed(() => Boolean(loginTitleImageUrl.value));

  const loginBackgroundUrl = computed(() => svgToDataUrl(loginBgSvg.value) || defaultLoginBackgroundUrl());

  function clearRuntime() {
    title.value = null;
    poweredBy.value = null;
    logoSvg.value = null;
    faviconSvg.value = null;
    loginTitleSvg.value = null;
    loginBgSvg.value = null;
    showLoginTitleImage.value = null;
    tenantId.value = undefined;
    loaded.value = false;
  }

  async function loadBrand(id?: string) {
    const tid = id?.trim() || '';
    if (!tid) {
      clearRuntime();
      applyFavicon(resolveFaviconHref(null));
      return;
    }

    try {
      const { data, error } = await fetchPublicBrand(tid);
      if (!error && data) {
        title.value = data.title ?? null;
        poweredBy.value = data.poweredBy ?? null;
        logoSvg.value = data.logoSvg ?? null;
        faviconSvg.value = data.faviconSvg ?? null;
        loginTitleSvg.value = data.loginTitleSvg ?? null;
        loginBgSvg.value = data.loginBgSvg ?? null;
        showLoginTitleImage.value =
          data.showLoginTitleImage === null || data.showLoginTitleImage === undefined
            ? null
            : Boolean(data.showLoginTitleImage);
        tenantId.value = tid;
        loaded.value = true;
        applyFavicon(resolveFaviconHref(faviconSvg.value));
        return;
      }
    } catch {
      // network failure → fall back to static
    }

    clearRuntime();
    applyFavicon(resolveFaviconHref(null));
  }

  return {
    title,
    poweredBy,
    logoSvg,
    faviconSvg,
    loginTitleSvg,
    loginBgSvg,
    showLoginTitleImage,
    tenantId,
    loaded,
    displayTitle,
    logoUrl,
    loginTitleImageUrl,
    showLoginTitle,
    loginBackgroundUrl,
    loadBrand,
    clearRuntime,
    resolvePublicAsset
  };
});
