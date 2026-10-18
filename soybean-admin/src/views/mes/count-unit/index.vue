<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createCountUnit, deleteCountUnit, getCountUnitList, updateCountUnit, type CountUnit } from '@/service/api/mes/count-unit';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesCountUnit' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<CountUnit, { unitCode: string }>({
    initialQuery: { unitCode: '' },
    fetcher: getCountUnitList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'unitCode', label: $t('page.mes.countUnit.unitCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<CountUnit | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  unitCode: '',
  unitName: '',
  unitType: 'WAFER',
  isBase: 0,
  sortNo: 0,
  remark: '',
  enabled: 1
});

const typeOptions = ['WAFER', 'DIE', 'STRIP', 'UNIT', 'CARRIER', 'OTHER'].map(value => ({ label: value, value }));

const rules = computed<FormRules>(() => ({
  unitCode: [{ required: true, message: $t('page.mes.countUnit.required'), trigger: 'blur' }],
  unitName: [{ required: true, message: $t('page.mes.countUnit.required'), trigger: 'blur' }],
  unitType: [{ required: true, message: $t('page.mes.countUnit.required'), trigger: 'change' }]
}));

function openCreate() {
  editing.value = null;
  Object.assign(form, { unitCode: '', unitName: '', unitType: 'WAFER', isBase: 0, sortNo: 0, remark: '', enabled: 1 });
  showModal.value = true;
}

function openEdit(row: CountUnit) {
  editing.value = row;
  Object.assign(form, row);
  showModal.value = true;
}

const columns = computed<DataTableColumns<CountUnit>>(() => [
  { title: $t('page.mes.countUnit.unitCode'), key: 'unitCode' },
  { title: $t('page.mes.countUnit.unitName'), key: 'unitName' },
  { title: $t('page.mes.countUnit.unitType'), key: 'unitType', width: 120 },
  {
    title: $t('page.mes.countUnit.isBase'),
    key: 'isBase',
    width: 100,
    render: row => (row.isBase === 1 ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no'))
  },
  { title: $t('page.mes.countUnit.sortNo'), key: 'sortNo', width: 90 },
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
        hasAuth('mes:count-unit:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:count-unit:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form };
  if (editing.value) {
    await updateCountUnit(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createCountUnit(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: CountUnit) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.countUnit.deleteConfirm', { name: row.unitCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteCountUnit(String(row.id));
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
          <span>{{ $t('page.mes.countUnit.title') }}</span>
          <PermissionGuard code="mes:count-unit:add">
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
      :title="editing ? $t('page.mes.countUnit.editTitle') : $t('page.mes.countUnit.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.countUnit.unitCode')" path="unitCode">
          <NInput v-model:value="form.unitCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.countUnit.unitName')" path="unitName">
          <NInput v-model:value="form.unitName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.countUnit.unitType')" path="unitType">
          <NSelect v-model:value="form.unitType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.countUnit.isBase')" path="isBase">
          <NRadioGroup v-model:value="form.isBase">
            <NRadio :value="1">{{ $t('common.yesOrNo.yes') }}</NRadio>
            <NRadio :value="0">{{ $t('common.yesOrNo.no') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem :label="$t('page.mes.countUnit.sortNo')" path="sortNo">
          <NInputNumber v-model:value="form.sortNo" class="w-full" />
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
