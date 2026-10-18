<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import { NButton, type DataTableColumns, type SelectOption } from 'naive-ui';
import { getBomList, type Bom } from '@/service/api/mes/bom';
import {
  approveProductVersion,
  copyProductVersion,
  createProductVersion,
  getProductVersionList,
  rejectProductVersion,
  submitProductVersion,
  updateProductVersion,
  type ProductVersion
} from '@/service/api/mes/product-version';
import { getRouteList, type Route } from '@/service/api/mes/route';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  productId: string;
  productCode: string;
}>();

const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<ProductVersion[]>([]);
const routes = ref<Route[]>([]);
const boms = ref<Bom[]>([]);
const editing = ref<ProductVersion | null>(null);
const form = reactive({ routeId: null as string | null, bomId: null as string | null, remark: '', actor: '' });

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});
const routeOptions = computed<SelectOption[]>(() => routes.value.map(item => ({ label: item.routeCode, value: item.id })));
const bomOptions = computed<SelectOption[]>(() => boms.value.map(item => ({ label: item.bomCode, value: item.id })));

function labelOf(options: SelectOption[], id?: string | null) {
  return options.find(item => item.value === id)?.label ?? '';
}

function resetForm() {
  editing.value = null;
  form.routeId = null;
  form.bomId = null;
  form.remark = '';
}

async function load() {
  if (!props.productId) return;
  loading.value = true;
  try {
    const [versions, routePage, bomPage] = await Promise.all([
      getProductVersionList(props.productId, { page: 1, pageSize: 50 }),
      getRouteList({ page: 1, pageSize: 200 }),
      getBomList({ page: 1, pageSize: 200 })
    ]);
    rows.value = versions.data?.records ?? [];
    routes.value = routePage.data?.records ?? [];
    boms.value = bomPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.productId] as const,
  ([show]) => {
    if (show) {
      resetForm();
      load();
    }
  }
);

function editRow(row: ProductVersion) {
  editing.value = row;
  form.routeId = row.routeId ?? null;
  form.bomId = row.bomId ?? null;
  form.remark = row.remark ?? '';
}

function payload() {
  return { routeId: form.routeId || null, bomId: form.bomId || null, remark: form.remark };
}

function requireActor() {
  if (!form.actor.trim()) {
    window.$message?.warning($t('page.mes.productVersion.actorRequired'));
    return false;
  }
  return true;
}

async function save() {
  if (editing.value) {
    await updateProductVersion(props.productId, editing.value.id, payload());
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createProductVersion(props.productId, payload());
    window.$message?.success($t('common.addSuccess'));
  }
  resetForm();
  await load();
}

async function copy(row: ProductVersion) {
  await copyProductVersion(props.productId, row.id);
  window.$message?.success($t('common.addSuccess'));
  await load();
}

async function submit(row: ProductVersion) {
  if (!requireActor()) return;
  await submitProductVersion(props.productId, row.id, { assignee: form.actor.trim() });
  window.$message?.success($t('page.mes.productVersion.submitted'));
  await load();
}

async function approve(row: ProductVersion) {
  if (!requireActor()) return;
  await approveProductVersion(props.productId, row.id, { operator: form.actor.trim() });
  window.$message?.success($t('page.mes.productVersion.approved'));
  await load();
}

async function reject(row: ProductVersion) {
  if (!requireActor()) return;
  await rejectProductVersion(props.productId, row.id, { operator: form.actor.trim() });
  window.$message?.success($t('page.mes.productVersion.rejected'));
  await load();
}

const columns = computed<DataTableColumns<ProductVersion>>(() => [
  { title: $t('page.mes.productVersion.versionNo'), key: 'versionNo', width: 80 },
  { title: $t('page.mes.productVersion.status'), key: 'status', width: 110 },
  { title: $t('page.mes.productVersion.route'), key: 'routeId', render: row => labelOf(routeOptions.value, row.routeId) },
  { title: $t('page.mes.productVersion.bom'), key: 'bomId', render: row => labelOf(bomOptions.value, row.bomId) },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 280,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        hasAuth('mes:product:edit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => copy(row) }, () => $t('page.mes.productVersion.copy'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:product:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editRow(row) }, () => $t('common.edit'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:product:edit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => submit(row) }, () => $t('page.mes.productVersion.submit'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:product:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'success', onClick: () => approve(row) }, () =>
              $t('page.mes.productVersion.approve')
            )
          : null,
        row.status === 'PENDING' && hasAuth('mes:product:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => reject(row) }, () =>
              $t('page.mes.productVersion.reject')
            )
          : null
      ])
  }
]);
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-920px" :title="`${$t('page.mes.productVersion.title')} ${productCode}`">
    <DataTable :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    <NForm :model="form" label-placement="left" label-width="96" class="mt-16px">
      <NFormItem :label="$t('page.mes.productVersion.route')">
        <NSelect v-model:value="form.routeId" :options="routeOptions" filterable clearable />
      </NFormItem>
      <NFormItem :label="$t('page.mes.productVersion.bom')">
        <NSelect v-model:value="form.bomId" :options="bomOptions" filterable clearable />
      </NFormItem>
      <NFormItem :label="$t('common.remark')">
        <NInput v-model:value="form.remark" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.productVersion.actor')">
        <NInput v-model:value="form.actor" :placeholder="$t('page.mes.productVersion.actorHint')" />
      </NFormItem>
      <div class="flex justify-end gap-8px">
        <NButton @click="resetForm">{{ $t('common.reset') }}</NButton>
        <NButton v-if="!editing && hasAuth('mes:product:add')" type="primary" @click="save">{{ $t('common.add') }}</NButton>
        <NButton v-else-if="editing && hasAuth('mes:product:edit')" type="primary" @click="save">{{ $t('common.save') }}</NButton>
      </div>
    </NForm>
  </NModal>
</template>
