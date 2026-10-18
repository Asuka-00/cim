<script setup lang="ts">
import { computed } from 'vue';
import { getPaletteColorByNumber, mixColor } from '@sa/color';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useBrandStore } from '@/store/modules/brand';
import PwdLogin from './modules/pwd-login.vue';

defineOptions({
  name: 'Login'
});

const appStore = useAppStore();
const themeStore = useThemeStore();
const brandStore = useBrandStore();

const bgThemeColor = computed(() =>
  themeStore.darkMode ? getPaletteColorByNumber(themeStore.themeColor, 600) : themeStore.themeColor
);

const bgColor = computed(() => {
  const COLOR_WHITE = '#ffffff';
  const ratio = themeStore.darkMode ? 0.55 : 0.12;
  return mixColor(COLOR_WHITE, themeStore.themeColor, ratio);
});

const hasBrandBg = computed(() => Boolean(brandStore.loginBackgroundUrl));

const pageStyle = computed(() => {
  if (hasBrandBg.value) {
    return { backgroundColor: themeStore.darkMode ? '#0b1220' : bgColor.value };
  }
  return { backgroundColor: bgColor.value };
});

const cardStyle = computed(() => {
  if (!hasBrandBg.value) return undefined;
  return {
    backgroundColor: themeStore.darkMode ? 'rgb(24 24 28 / 92%)' : 'rgb(255 255 255 / 92%)'
  };
});
</script>

<template>
  <div class="relative size-full overflow-hidden" :style="pageStyle">
    <img
      v-if="hasBrandBg"
      :src="brandStore.loginBackgroundUrl"
      alt=""
      class="pointer-events-none absolute inset-0 size-full object-cover"
    />
    <div v-else class="pointer-events-none absolute inset-0 opacity-55">
      <WaveBg :theme-color="bgThemeColor" />
    </div>

    <div class="absolute right-24px top-20px z-5 flex-y-center gap-8px">
      <ThemeSchemaSwitch
        :theme-schema="themeStore.themeScheme"
        :show-tooltip="false"
        class="text-18px"
        @switch="themeStore.toggleThemeScheme"
      />
      <LangSwitch
        v-if="themeStore.header.multilingual.visible"
        :lang="appStore.locale"
        :lang-options="appStore.localeOptions"
        :show-tooltip="false"
        @change-lang="appStore.changeLocale"
      />
    </div>

    <div class="relative z-4 size-full flex-col-center px-16px">
      <img
        v-if="brandStore.showLoginTitle"
        :src="brandStore.loginTitleImageUrl"
        :alt="brandStore.displayTitle"
        class="system-title-image"
      />
      <h1 v-else class="system-title">{{ brandStore.displayTitle }}</h1>
      <NCard
        :bordered="false"
        class="login-card w-420px rd-16px lt-sm:w-full"
        :class="{ 'login-card-on-brand': hasBrandBg }"
        :style="cardStyle"
      >
        <header class="mb-28px flex-col-center">
          <h2 class="m-0 text-22px text-[var(--n-text-color)] font-600 tracking-wide">
            {{ $t('page.login.title') }}
          </h2>
        </header>
        <PwdLogin />
        <footer v-if="brandStore.poweredBy" class="mt-20px text-center text-12px text-[var(--n-text-color-3)]">
          {{ $t('page.login.poweredBy', { name: brandStore.poweredBy }) }}
        </footer>
      </NCard>
    </div>
  </div>
</template>

<style scoped>
.system-title {
  margin: 0 0 16px;
  font-size: 28px;
  font-weight: 700;
  color: #2d3a4b;
  text-shadow: 0 2px 8px rgb(0 0 0 / 8%);
  pointer-events: none;
  user-select: none;
}

.system-title-image {
  display: block;
  max-width: min(480px, 80vw);
  max-height: 80px;
  margin-bottom: 16px;
  pointer-events: none;
  user-select: none;
  object-fit: contain;
}

.login-card {
  padding: 8px 4px;
  box-shadow: 0 16px 48px rgb(15 23 42 / 10%);
}

.login-card-on-brand {
  backdrop-filter: blur(8px);
  box-shadow: 0 16px 48px rgb(0 0 0 / 28%);
}
</style>
