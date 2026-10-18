<script setup lang="ts">
import { computed } from 'vue';
import { useBrandStore } from '@/store/modules/brand';

defineOptions({
  name: 'GlobalFooter'
});

const brandStore = useBrandStore();

const year = new Date().getFullYear();
const owner = computed(() => brandStore.poweredBy || brandStore.displayTitle);
</script>

<template>
  <DarkModeContainer class="h-full flex-center text-12px text-[var(--n-text-color-3)]">
    <span>Copyright © {{ year }} {{ owner }}</span>
  </DarkModeContainer>
</template>

<style scoped></style>
