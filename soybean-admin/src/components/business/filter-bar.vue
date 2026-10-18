<script setup lang="ts">
import { computed, ref, useSlots } from 'vue';
import type { SelectOption } from 'naive-ui';
import { $t } from '@/locales';
import PermissionGuard from '@/components/business/permission-guard.vue';
import DictSelect from '@/components/business/dict-select.vue';

export interface FilterField {
  key: string;
  label: string;
  type: 'input' | 'select' | 'number' | 'date' | 'daterange';
  options?: Array<{ label: string; value: string | number | boolean | null }>;
  placeholder?: string;
  /** When set, a select field loads options from this dict type */
  dictType?: string;
  min?: number;
  max?: number;
}

const props = withDefaults(
  defineProps<{
    fields: FilterField[];
    modelValue: Record<string, unknown>;
    /** When set, the search button is rendered only if the user owns this permission */
    searchPerm?: string | string[];
    collapsible?: boolean;
    defaultExpanded?: boolean;
    collapsedVisibleCount?: number;
  }>(),
  {
    collapsible: true,
    defaultExpanded: false,
    collapsedVisibleCount: 3
  }
);

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, unknown>];
  search: [];
  reset: [];
}>();

const slots = useSlots();
const isExpanded = ref(props.defaultExpanded);

const model = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
});

const isInlineLayout = computed(() => props.collapsible && !isExpanded.value);

function setField(key: string, value: unknown) {
  emit('update:modelValue', { ...props.modelValue, [key]: value });
}

function isFieldVisible(index: number) {
  if (!props.collapsible || isExpanded.value) return true;
  return index < props.collapsedVisibleCount;
}

function toggleExpand() {
  isExpanded.value = !isExpanded.value;
}
</script>

<template>
  <NCard size="small" :bordered="false" class="rtd-page__filter mb-12px shrink-0 rounded-8px shadow-sm">
    <div class="filter-bar" :class="{ 'is-inline': isInlineLayout }">
      <NForm label-placement="left" :show-feedback="false" class="filter-fields flex flex-wrap items-end gap-12px">
        <div v-for="(field, index) in fields" v-show="isFieldVisible(index)" :key="field.key" class="min-w-200px">
          <div class="mb-4px text-12px text-base-text/60">{{ field.label }}</div>
          <slot v-if="slots[field.key]" :name="field.key" :field="field" :model="model" />
          <NInput
            v-else-if="field.type === 'input'"
            :value="(model[field.key] as string) ?? ''"
            clearable
            :placeholder="field.placeholder || field.label"
            @update:value="v => setField(field.key, v)"
          />
          <DictSelect
            v-else-if="field.dictType"
            :dict-type="field.dictType"
            :value="(model[field.key] as string) ?? null"
            :placeholder="field.placeholder || field.label"
            @update:value="v => setField(field.key, v)"
          />
          <NSelect
            v-else-if="field.type === 'select'"
            :value="(model[field.key] ?? null) as string"
            clearable
            :options="(field.options || []) as SelectOption[]"
            :placeholder="field.placeholder || field.label"
            @update:value="v => setField(field.key, v)"
          />
          <NInputNumber
            v-else-if="field.type === 'number'"
            :value="(model[field.key] as number | null) ?? null"
            clearable
            :min="field.min"
            :max="field.max"
            :show-button="false"
            class="w-full"
            :placeholder="field.placeholder || field.label"
            @update:value="v => setField(field.key, v)"
          />
          <NDatePicker
            v-else-if="field.type === 'date'"
            type="date"
            clearable
            class="w-full"
            :value="(model[field.key] as number | null) ?? null"
            @update:value="v => setField(field.key, v)"
          />
          <NDatePicker
            v-else-if="field.type === 'daterange'"
            type="daterange"
            clearable
            class="w-full"
            :value="(model[field.key] as [number, number] | null) ?? null"
            @update:value="v => setField(field.key, v)"
          />
        </div>
      </NForm>

      <div class="filter-actions">
        <NSpace>
          <PermissionGuard v-if="searchPerm" :code="searchPerm">
            <NButton type="primary" @click="emit('search')">{{ $t('common.search') }}</NButton>
          </PermissionGuard>
          <NButton v-else type="primary" @click="emit('search')">{{ $t('common.search') }}</NButton>
          <NButton @click="emit('reset')">{{ $t('common.reset') }}</NButton>
          <NButton v-if="collapsible" text @click="toggleExpand">
            {{ isExpanded ? $t('common.collapse') : $t('common.expand') }}
            <template #icon>
              <SvgIcon :icon="isExpanded ? 'mdi:chevron-up' : 'mdi:chevron-down'" />
            </template>
          </NButton>
        </NSpace>
      </div>
    </div>
  </NCard>
</template>

<style scoped>
.filter-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.filter-bar.is-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: flex-end;
}

.filter-bar.is-inline .filter-fields {
  flex: 1;
  min-width: 0;
}

.filter-bar.is-inline .filter-actions {
  flex-shrink: 0;
  margin-left: auto;
}
</style>
