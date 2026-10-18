<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules, type SelectOption } from 'naive-ui';
import { getAreaList, type Area } from '@/service/api/mes/area';
import {
  createEquipmentGroup,
  deleteEquipmentGroup,
  getEquipmentGroupList,
  updateEquipmentGroup,
  type EquipmentGroup
} from '@/service/api/mes/equipment-group';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesEquipmentGroup' });

const { hasAuth } = useAuth();
const areas = ref<Area[]>([]);
const areaOptions = computed<SelectOption[]>(() => areas.value.map(item => ({ label: item.areaCode, value: item.id })));

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<EquipmentGroup, { groupCode: string }>({
    initialQuery: { groupCode: '' },
    fetcher: getEquipmentGroupList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'groupCode', label: $t('page.mes.equipmentGroup.groupCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<EquipmentGroup | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  groupCode: '',
  groupName: '',
  areaId: null as string | null,
  remark: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  groupCode: [{ required: true, message: $t('page.mes.equipmentGroup.required'), trigger: 'blur' }],
  groupName: [{ required: true, message: $t('page.mes.equipmentGroup.required'), trigger: 'blur' }]
}));

function openCreate() {
  editing.value = null;
  Object.assign(form, { groupCode: '', groupName: '', areaId: null, remark: '', enabled: 1 });
  showModal.value = true;
}

function openEdit(row: EquipmentGroup) {
  editing.value = row;
  Object.assign(form, { ...row, areaId: row.areaId ?? null });
  showModal.value = true;
}

function areaLabel(id?: string | null) {
  return areas.value.find(item => item.id === id)?.areaCode ?? '';
}

const columns = computed<DataTableColumns<EquipmentGroup>>(() => [
  { title: $t('page.mes.equipmentGroup.groupCode'), key: 'groupCode' },
  { title: $t('page.mes.equipmentGroup.groupName'), key: 'groupName' },
  { title: $t('page.mes.equipmentGroup.area'), key: 'areaId', render: row => areaLabel(row.areaId) },
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
        hasAuth('mes:equipment-group:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:equipment-group:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form, areaId: form.areaId || null };
  if (editing.value) {
    await updateEquipmentGroup(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createEquipmentGroup(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: EquipmentGroup) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.equipmentGroup.deleteConfirm', { name: row.groupCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteEquipmentGroup(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const { data } = await getAreaList({ page: 1, pageSize: 200 });
  areas.value = data?.records ?? [];
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
          <span>{{ $t('page.mes.equipmentGroup.title') }}</span>
          <PermissionGuard code="mes:equipment-group:add">
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
      :title="editing ? $t('page.mes.equipmentGroup.editTitle') : $t('page.mes.equipmentGroup.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.equipmentGroup.groupCode')" path="groupCode">
          <NInput v-model:value="form.groupCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipmentGroup.groupName')" path="groupName">
          <NInput v-model:value="form.groupName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipmentGroup.area')" path="areaId">
          <NSelect v-model:value="form.areaId" :options="areaOptions" filterable clearable />
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
