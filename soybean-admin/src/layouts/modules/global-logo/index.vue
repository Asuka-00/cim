<script setup lang="ts">
import { computed } from 'vue';
import { useBrandStore } from '@/store/modules/brand';
import { useThemeStore } from '@/store/modules/theme';

defineOptions({
  name: 'GlobalLogo'
});

interface Props {
  /** Whether to show the title */
  showTitle?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showTitle: true
});

const brandStore = useBrandStore();
const themeStore = useThemeStore();

const showLogoImg = computed(() => themeStore.logo.visible && Boolean(brandStore.logoUrl));
const showBlock = computed(() => showLogoImg.value || props.showTitle);
</script>

<template>
  <RouterLink v-if="showBlock" to="/" class="w-full flex-center nowrap-hidden">
    <img v-if="showLogoImg" :src="brandStore.logoUrl" alt="logo" class="h-32px w-auto object-contain" />
    <h2
      v-show="showTitle"
      class="text-16px text-primary font-bold transition duration-300 ease-in-out"
      :class="{ 'pl-8px': showLogoImg }"
    >
      {{ brandStore.displayTitle }}
    </h2>
  </RouterLink>
</template>

<style scoped></style>
