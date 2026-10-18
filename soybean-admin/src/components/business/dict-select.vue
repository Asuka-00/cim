<script setup lang="ts">
import { computed, onMounted } from 'vue';
import type { SelectOption } from 'naive-ui';
import { useDictOptions } from '@/hooks/business/dict-options';

defineOptions({ name: 'DictSelect' });

const props = withDefaults(
  defineProps<{
    dictType: string;
    value?: string | string[] | null;
    multiple?: boolean;
    clearable?: boolean;
    disabled?: boolean;
    filterable?: boolean;
    placeholder?: string;
    size?: 'tiny' | 'small' | 'medium' | 'large';
  }>(),
  {
    value: null,
    multiple: false,
    clearable: true,
    disabled: false,
    filterable: false,
    size: 'medium'
  }
);

const emit = defineEmits<{
  'update:value': [value: string | string[] | null];
}>();

const { options, loading, load } = useDictOptions(props.dictType);
const selectOptions = computed<SelectOption[]>(() => options.value.map(({ label, value }) => ({ label, value })));

onMounted(() => {
  void load();
});
</script>

<template>
  <NSelect
    :value="value ?? (multiple ? [] : null)"
    :options="selectOptions"
    :multiple="multiple"
    :clearable="clearable"
    :disabled="disabled"
    :filterable="filterable"
    :loading="loading"
    :placeholder="placeholder"
    :size="size"
    @update:value="v => emit('update:value', v)"
  />
</template>
