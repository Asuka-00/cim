import type { Router } from 'vue-router';
import { useTitle } from '@vueuse/core';
import { $t } from '@/locales';
import { useBrandStore } from '@/store/modules/brand';

export function createDocumentTitleGuard(router: Router) {
  router.afterEach(to => {
    const { i18nKey, title } = to.meta;
    const pageTitle = i18nKey ? $t(i18nKey) : title;
    const brandStore = useBrandStore();
    const brandTitle = brandStore.displayTitle;

    const documentTitle = pageTitle ? `${pageTitle} - ${brandTitle}` : brandTitle;

    useTitle(documentTitle);
  });
}
