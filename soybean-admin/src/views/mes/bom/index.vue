<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createBom, deleteBom, getBomList, updateBom, type Bom } from '@/service/api/mes/bom';
import VersionDrawer from './version-drawer.vue';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesBom' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Bom, { bomCode: string }>({
    initialQuery: { bomCode: '' },
    fetcher: getBomList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'bomCode', label: $t('page.mes.bom.bomCode'), type: 'input' }]);

const showModal = ref(false);
const versionShow = ref(false);
const versionBom = ref<Bom | null>(null);
const editing = ref<Bom | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  bomCode: '',
  bomName: '',
  description: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  bomCode: [{ required: true, message: $t('page.mes.bom.required'), trigger: 'blur' }],
  bomName: [{ required: true, message: $t('page.mes.bom.required'), trigger: 'blur' }]
}));

function openCreate() {
  editing.value = null;
  Object.assign(form, { bomCode: '', bomName: '', description: '', enabled: 1 });
  showModal.value = true;
}

function openEdit(row: Bom) {
  editing.value = row;
  Object.assign(form, {
    bomCode: row.bomCode,
    bomName: row.bomName,
    description: row.description ?? '',
    enabled: row.enabled
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<Bom>>(() => [
  { title: $t('page.mes.bom.bomCode'), key: 'bomCode' },
  { title: $t('page.mes.bom.bomName'), key: 'bomName' },
  { title: $t('page.mes.bom.description'), key: 'description' },
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
              versionBom.value = row;
              versionShow.value = true;
            }
          },
          () => $t('page.mes.bomVersion.title')
        ),
        hasAuth('mes:bom:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:bom:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (editing.value) {
    await updateBom(String(editing.value.id), form);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createBom(form);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: Bom) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.bom.deleteConfirm', { name: row.bomCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteBom(String(row.id));
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
          <span>{{ $t('page.mes.bom.title') }}</span>
          <PermissionGuard code="mes:bom:add">
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
      :title="editing ? $t('page.mes.bom.editTitle') : $t('page.mes.bom.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.bom.bomCode')" path="bomCode">
          <NInput v-model:value="form.bomCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bom.bomName')" path="bomName">
          <NInput v-model:value="form.bomName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bom.description')" path="description">
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
    <VersionDrawer v-if="versionBom" v-model:show="versionShow" :bom-id="String(versionBom.id)" :bom-code="versionBom.bomCode" />
  </div>
</template>
