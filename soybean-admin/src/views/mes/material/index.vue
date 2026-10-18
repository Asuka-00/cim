<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules, type SelectOption } from 'naive-ui';
import { getUomList, type Uom } from '@/service/api/mes/uom';
import { createMaterial, deleteMaterial, getMaterialList, updateMaterial, type Material } from '@/service/api/mes/material';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesMaterial' });

const { hasAuth } = useAuth();
const uoms = ref<Uom[]>([]);
const typeOptions = ['WAFER', 'RAW', 'AUX', 'CHEMICAL', 'GAS', 'PACKAGE', 'OTHER'].map(value => ({ label: value, value }));
const uomOptions = computed<SelectOption[]>(() => uoms.value.map(item => ({ label: item.uomCode, value: item.id })));

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Material, { materialCode: string }>({
    initialQuery: { materialCode: '' },
    fetcher: getMaterialList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'materialCode', label: $t('page.mes.material.materialCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<Material | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  materialCode: '',
  materialName: '',
  materialType: 'RAW',
  spec: '',
  baseUomId: null as string | null,
  isCritical: 0,
  shelfLifeDays: null as number | null,
  needIncomingInspect: 0,
  remark: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  materialCode: [{ required: true, message: $t('page.mes.material.required'), trigger: 'blur' }],
  materialName: [{ required: true, message: $t('page.mes.material.required'), trigger: 'blur' }],
  baseUomId: [{ required: true, message: $t('page.mes.material.required'), trigger: 'change' }]
}));

function blank() {
  Object.assign(form, {
    materialCode: '',
    materialName: '',
    materialType: 'RAW',
    spec: '',
    baseUomId: null,
    isCritical: 0,
    shelfLifeDays: null,
    needIncomingInspect: 0,
    remark: '',
    enabled: 1
  });
}

function openCreate() {
  editing.value = null;
  blank();
  showModal.value = true;
}

function openEdit(row: Material) {
  editing.value = row;
  Object.assign(form, { ...row, shelfLifeDays: row.shelfLifeDays ?? null });
  showModal.value = true;
}

function uomLabel(id?: string | null) {
  return uoms.value.find(item => item.id === id)?.uomCode ?? '';
}

const columns = computed<DataTableColumns<Material>>(() => [
  { title: $t('page.mes.material.materialCode'), key: 'materialCode' },
  { title: $t('page.mes.material.materialName'), key: 'materialName' },
  { title: $t('page.mes.material.materialType'), key: 'materialType', width: 110 },
  { title: $t('page.mes.material.baseUom'), key: 'baseUomId', width: 110, render: row => uomLabel(row.baseUomId) },
  {
    title: $t('page.mes.material.isCritical'),
    key: 'isCritical',
    width: 90,
    render: row => (row.isCritical === 1 ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:material:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:material:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form, baseUomId: form.baseUomId! };
  if (editing.value) {
    await updateMaterial(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createMaterial(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: Material) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.material.deleteConfirm', { name: row.materialCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteMaterial(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const { data } = await getUomList({ page: 1, pageSize: 200 });
  uoms.value = data?.records ?? [];
  await fetchData();
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
          <span>{{ $t('page.mes.material.title') }}</span>
          <PermissionGuard code="mes:material:add">
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
      :title="editing ? $t('page.mes.material.editTitle') : $t('page.mes.material.addTitle')"
      class="w-640px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="108">
        <NFormItem :label="$t('page.mes.material.materialCode')" path="materialCode">
          <NInput v-model:value="form.materialCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.material.materialName')" path="materialName">
          <NInput v-model:value="form.materialName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.material.materialType')" path="materialType">
          <NSelect v-model:value="form.materialType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.material.spec')" path="spec">
          <NInput v-model:value="form.spec" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.material.baseUom')" path="baseUomId">
          <NSelect v-model:value="form.baseUomId" :options="uomOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.material.isCritical')" path="isCritical">
          <NRadioGroup v-model:value="form.isCritical">
            <NRadio :value="1">{{ $t('common.yesOrNo.yes') }}</NRadio>
            <NRadio :value="0">{{ $t('common.yesOrNo.no') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem :label="$t('page.mes.material.shelfLifeDays')" path="shelfLifeDays">
          <NInputNumber v-model:value="form.shelfLifeDays" :min="1" class="w-full" clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.material.needIncomingInspect')" path="needIncomingInspect">
          <NRadioGroup v-model:value="form.needIncomingInspect">
            <NRadio :value="1">{{ $t('common.yesOrNo.yes') }}</NRadio>
            <NRadio :value="0">{{ $t('common.yesOrNo.no') }}</NRadio>
          </NRadioGroup>
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
