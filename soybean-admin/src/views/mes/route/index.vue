<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createRoute, deleteRoute, getRouteList, updateRoute, type Route } from '@/service/api/mes/route';
import VersionDrawer from './version-drawer.vue';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesRoute' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Route, { routeCode: string }>({
    initialQuery: { routeCode: '' },
    fetcher: getRouteList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'routeCode', label: $t('page.mes.route.routeCode'), type: 'input' }]);

const showModal = ref(false);
const versionShow = ref(false);
const versionRoute = ref<Route | null>(null);
const editing = ref<Route | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  routeCode: '',
  routeName: '',
  description: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  routeCode: [{ required: true, message: $t('page.mes.route.required'), trigger: 'blur' }],
  routeName: [{ required: true, message: $t('page.mes.route.required'), trigger: 'blur' }]
}));

function openCreate() {
  editing.value = null;
  Object.assign(form, { routeCode: '', routeName: '', description: '', enabled: 1 });
  showModal.value = true;
}

function openEdit(row: Route) {
  editing.value = row;
  Object.assign(form, {
    routeCode: row.routeCode,
    routeName: row.routeName,
    description: row.description ?? '',
    enabled: row.enabled
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<Route>>(() => [
  { title: $t('page.mes.route.routeCode'), key: 'routeCode' },
  { title: $t('page.mes.route.routeName'), key: 'routeName' },
  { title: $t('page.mes.route.description'), key: 'description' },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 1 ? $t('common.enabled') : $t('common.disabled'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 240,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => {
              versionRoute.value = row;
              versionShow.value = true;
            }
          },
          () => $t('page.mes.routeVersion.title')
        ),
        hasAuth('mes:route:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:route:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (editing.value) {
    await updateRoute(String(editing.value.id), form);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createRoute(form);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: Route) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.route.deleteConfirm', { name: row.routeCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRoute(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(() => {
  fetchData();
});
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
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.route.title') }}</span>
          <PermissionGuard code="mes:route:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
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
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.route.editTitle') : $t('page.mes.route.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.route.routeCode')" path="routeCode">
          <NInput v-model:value="form.routeCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.route.routeName')" path="routeName">
          <NInput v-model:value="form.routeName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.route.description')" path="description">
          <NInput v-model:value="form.description" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem :label="$t('common.status')" path="enabled">
          <NRadioGroup v-model:value="form.enabled">
            <NRadio :value="1">{{ $t('common.enabled') }}</NRadio>
            <NRadio :value="0">{{ $t('common.disabled') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
      </NForm>
    </CrudDialog>
    <VersionDrawer
      v-if="versionRoute"
      v-model:show="versionShow"
      :route-id="String(versionRoute.id)"
      :route-code="versionRoute.routeCode"
    />
  </div>
</template>
