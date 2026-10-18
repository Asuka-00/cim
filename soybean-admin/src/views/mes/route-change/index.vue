<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getRouteList, type Route } from '@/service/api/mes/route';
import { changeLotRoute } from '@/service/api/mes/route-change';
import { getRouteVersionList, type RouteVersion } from '@/service/api/mes/route-version';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesRouteChange' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: 'WAITING' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.routeChange.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const routes = ref<Route[]>([]);
const versions = ref<RouteVersion[]>([]);
const form = reactive({
  routeId: null as string | null,
  routeVersionId: null as string | null,
  routeSeqNo: null as number | null,
  reasonCode: ''
});

const rules = computed<FormRules>(() => ({
  routeVersionId: [{ required: true, message: $t('page.mes.routeChange.required'), trigger: 'change' }],
  routeSeqNo: [{ required: true, type: 'number', message: $t('page.mes.routeChange.required'), trigger: 'blur' }],
  reasonCode: [{ required: true, message: $t('page.mes.routeChange.required'), trigger: 'blur' }]
}));

const routeOptions = computed(() => routes.value.map(item => ({ label: `${item.routeCode} ${item.routeName}`, value: String(item.id) })));
const versionOptions = computed(() => versions.value.map(item => ({ label: `${item.versionNo} ${item.status}`, value: String(item.id) })));

async function openChange(row: Lot) {
  current.value = row;
  Object.assign(form, { routeId: null, routeVersionId: null, routeSeqNo: null, reasonCode: '' });
  versions.value = [];
  const page = await getRouteList({ page: 1, pageSize: 200 });
  routes.value = page.data?.records ?? [];
  showModal.value = true;
}

async function onRoute(routeId: string | null) {
  form.routeId = routeId;
  form.routeVersionId = null;
  versions.value = [];
  if (!routeId) return;
  const page = await getRouteVersionList(routeId, { page: 1, pageSize: 100 });
  versions.value = page.data?.records ?? [];
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.routeChange.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.routeChange.status'), key: 'status', width: 120 },
  { title: $t('page.mes.routeChange.routeSeqNo'), key: 'routeSeqNo', width: 110 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status === 'WAITING' && !row.reworkId && hasAuth('mes:lot:change-route')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openChange(row) }, () =>
            $t('page.mes.routeChange.change')
          )
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value || !form.routeVersionId || form.routeSeqNo == null) return;
  const res = await changeLotRoute(String(current.value.id), {
    routeVersionId: form.routeVersionId,
    routeSeqNo: form.routeSeqNo,
    reasonCode: form.reasonCode
  });
  window.$message?.success($t('page.mes.routeChange.done', { count: res.data ?? 0 }));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <FilterBar
      :fields="filterFields"
      :model-value="searchParams as Record<string, unknown>"
      @update:model-value="v => Object.assign(searchParams, v)"
      @search="handleSearch"
      @reset="handleReset"
    />
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.routeChange.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="currentPage"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="onPageUpdate"
          @update:page-size="onSizeUpdate"
        />
      </div>
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.routeChange.changeTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.routeChange.route')" path="routeId">
          <NSelect v-model:value="form.routeId" :options="routeOptions" filterable @update:value="onRoute" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeChange.version')" path="routeVersionId">
          <NSelect v-model:value="form.routeVersionId" :options="versionOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeChange.targetSeq')" path="routeSeqNo">
          <NInputNumber v-model:value="form.routeSeqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeChange.reason')" path="reasonCode">
          <NInput v-model:value="form.reasonCode" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
