<script setup lang="ts">
import { computed } from 'vue';
import { withCellEllipsis } from '@/utils/table-ellipsis';

defineOptions({ name: 'DataTable', inheritAttrs: false });

const props = defineProps<{
  columns?: any[];
  rowKey?: (row: any) => string | number;
}>();

const columns = computed(() => withCellEllipsis(props.columns ?? []));
</script>

<template>
  <NDataTable v-bind="$attrs" :columns="columns" :row-key="rowKey">
    <template v-for="(_, name) in $slots" :key="name" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps || {}" />
    </template>
  </NDataTable>
</template>
