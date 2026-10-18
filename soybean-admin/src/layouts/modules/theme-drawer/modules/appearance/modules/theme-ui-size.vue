<script setup lang="ts">
import { themeUiSizeRecord } from '@/constants/app';
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';
import SettingItem from '../../../components/setting-item.vue';

defineOptions({
  name: 'ThemeUiSize'
});

const themeStore = useThemeStore();

function handleSizeChange(value: string | number) {
  themeStore.setUiSize(value as UnionKey.ThemeUiSize);
}
</script>

<template>
  <NDivider>{{ $t('theme.appearance.uiSize.title') }}</NDivider>
  <SettingItem :label="$t('theme.appearance.uiSize.title')">
    <NTabs type="segment" size="small" class="w-180px" :value="themeStore.uiSize" @update:value="handleSizeChange">
      <NTab v-for="(_, key) in themeUiSizeRecord" :key="key" :name="key">
        {{ $t(themeUiSizeRecord[key]) }}
      </NTab>
    </NTabs>
  </SettingItem>
</template>
