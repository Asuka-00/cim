<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createProductFamily,
  deleteProductFamily,
  getProductFamilyList,
  updateProductFamily,
  type ProductFamily
} from '@/service/api/mes/product-family';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesProductFamily' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<ProductFamily, { familyCode: string }>({
    initialQuery: { familyCode: '' },
    fetcher: getProductFamilyList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'familyCode', label: $t('page.mes.productFamily.familyCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<ProductFamily | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  familyCode: '',
  familyName: '',
  remark: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  familyCode: [{ required: true, message: $t('page.mes.productFamily.required'), trigger: 'blur' }],
  familyName: [{ required: true, message: $t('page.mes.productFamily.required'), trigger: 'blur' }]
}));

function openCreate() {
  editing.value = null;
  Object.assign(form, { familyCode: '', familyName: '', remark: '', enabled: 1 });
  showModal.value = true;
}

function openEdit(row: ProductFamily) {
  editing.value = row;
  Object.assign(form, row);
  showModal.value = true;
}

const columns = computed<DataTableColumns<ProductFamily>>(() => [
  { title: $t('page.mes.productFamily.familyCode'), key: 'familyCode' },
  { title: $t('page.mes.productFamily.familyName'), key: 'familyName' },
  { title: $t('common.remark'), key: 'remark' },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 1 ? $t('common.enabled') : $t('common.disabled'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:product-family:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:product-family:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form };
  if (editing.value) {
    await updateProductFamily(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createProductFamily(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: ProductFamily) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.productFamily.deleteConfirm', { name: row.familyCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteProductFamily(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
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
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.productFamily.title') }}</span>
          <PermissionGuard code="mes:product-family:add">
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
      :title="editing ? $t('page.mes.productFamily.editTitle') : $t('page.mes.productFamily.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.productFamily.familyCode')" path="familyCode">
          <NInput v-model:value="form.familyCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.productFamily.familyName')" path="familyName">
          <NInput v-model:value="form.familyName" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem :label="$t('common.status')" path="enabled">
          <NRadioGroup v-model:value="form.enabled">
            <NRadio :value="1">{{ $t('common.enabled') }}</NRadio>
            <NRadio :value="0">{{ $t('common.disabled') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
