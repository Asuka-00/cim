<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules, type SelectOption } from 'naive-ui';
import { getAreaList, type Area } from '@/service/api/mes/area';
import { getEquipmentGroupList, type EquipmentGroup } from '@/service/api/mes/equipment-group';
import {
  createEquipment,
  deleteEquipment,
  getEquipmentList,
  updateEquipment,
  type Equipment
} from '@/service/api/mes/equipment';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import CapabilityDrawer from './capability-drawer.vue';
import PortDrawer from './port-drawer.vue';
import StatusModal from './status-modal.vue';

defineOptions({ name: 'MesEquipment' });

const { hasAuth } = useAuth();
const areas = ref<Area[]>([]);
const groups = ref<EquipmentGroup[]>([]);
const typeOptions = ['PROCESS', 'MEASURE', 'TEST', 'PACK', 'TRANSPORT', 'OTHER'].map(value => ({ label: value, value }));
const areaOptions = computed<SelectOption[]>(() => areas.value.map(item => ({ label: item.areaCode, value: item.id })));
const groupOptions = computed<SelectOption[]>(() => groups.value.map(item => ({ label: item.groupCode, value: item.id })));

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Equipment, { equipmentCode: string }>({
    initialQuery: { equipmentCode: '' },
    fetcher: getEquipmentList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'equipmentCode', label: $t('page.mes.equipment.equipmentCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<Equipment | null>(null);
const statusTarget = ref<Equipment | null>(null);
const statusShow = ref(false);
const portTarget = ref<Equipment | null>(null);
const portShow = ref(false);
const capabilityTarget = ref<Equipment | null>(null);
const capabilityShow = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  equipmentCode: '',
  equipmentName: '',
  equipmentType: 'PROCESS',
  groupId: null as string | null,
  areaId: null as string | null,
  vendor: '',
  model: '',
  serialNo: '',
  remark: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  equipmentCode: [{ required: true, message: $t('page.mes.equipment.required'), trigger: 'blur' }],
  equipmentName: [{ required: true, message: $t('page.mes.equipment.required'), trigger: 'blur' }]
}));

function blank() {
  Object.assign(form, {
    equipmentCode: '',
    equipmentName: '',
    equipmentType: 'PROCESS',
    groupId: null,
    areaId: null,
    vendor: '',
    model: '',
    serialNo: '',
    remark: '',
    enabled: 1
  });
}

function openCreate() {
  editing.value = null;
  blank();
  showModal.value = true;
}

function openEdit(row: Equipment) {
  editing.value = row;
  Object.assign(form, { ...row, groupId: row.groupId ?? null, areaId: row.areaId ?? null });
  showModal.value = true;
}

const columns = computed<DataTableColumns<Equipment>>(() => [
  { title: $t('page.mes.equipment.equipmentCode'), key: 'equipmentCode' },
  { title: $t('page.mes.equipment.equipmentName'), key: 'equipmentName' },
  { title: $t('page.mes.equipment.equipmentType'), key: 'equipmentType', width: 110 },
  { title: $t('page.mes.equipment.e10State'), key: 'e10State', width: 150 },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 1 ? $t('common.enabled') : $t('common.disabled'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 280,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => {
              portTarget.value = row;
              portShow.value = true;
            }
          },
          () => $t('page.mes.equipmentPort.title')
        ),
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => {
              capabilityTarget.value = row;
              capabilityShow.value = true;
            }
          },
          () => $t('page.mes.equipmentCapability.title')
        ),
        hasAuth('mes:equipment:edit')
          ? h(
              NButton,
              {
                size: 'small',
                quaternary: true,
                onClick: () => {
                  statusTarget.value = row;
                  statusShow.value = true;
                }
              },
              () => $t('page.mes.equipment.changeStatus')
            )
          : null,
        hasAuth('mes:equipment:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:equipment:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form, groupId: form.groupId || null, areaId: form.areaId || null };
  if (editing.value) {
    await updateEquipment(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createEquipment(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: Equipment) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.equipment.deleteConfirm', { name: row.equipmentCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteEquipment(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [areaRes, groupRes] = await Promise.all([
    getAreaList({ page: 1, pageSize: 200 }),
    getEquipmentGroupList({ page: 1, pageSize: 200 })
  ]);
  areas.value = areaRes.data?.records ?? [];
  groups.value = groupRes.data?.records ?? [];
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
          <span>{{ $t('page.mes.equipment.title') }}</span>
          <PermissionGuard code="mes:equipment:add">
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
      :title="editing ? $t('page.mes.equipment.editTitle') : $t('page.mes.equipment.addTitle')"
      class="w-640px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.equipment.equipmentCode')" path="equipmentCode">
          <NInput v-model:value="form.equipmentCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipment.equipmentName')" path="equipmentName">
          <NInput v-model:value="form.equipmentName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipment.equipmentType')" path="equipmentType">
          <NSelect v-model:value="form.equipmentType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipment.group')" path="groupId">
          <NSelect v-model:value="form.groupId" :options="groupOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipment.area')" path="areaId">
          <NSelect v-model:value="form.areaId" :options="areaOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipment.vendor')" path="vendor">
          <NInput v-model:value="form.vendor" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipment.model')" path="model">
          <NInput v-model:value="form.model" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipment.serialNo')" path="serialNo">
          <NInput v-model:value="form.serialNo" />
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
    <CapabilityDrawer
      v-if="capabilityTarget"
      v-model:show="capabilityShow"
      :equipment-id="String(capabilityTarget.id)"
      :equipment-code="capabilityTarget.equipmentCode"
    />
    <PortDrawer
      v-if="portTarget"
      v-model:show="portShow"
      :equipment-id="String(portTarget.id)"
      :equipment-code="portTarget.equipmentCode"
    />
    <StatusModal
      v-if="statusTarget"
      v-model:show="statusShow"
      :equipment-id="String(statusTarget.id)"
      :equipment-code="statusTarget.equipmentCode"
      :e10-state="statusTarget.e10State"
      @changed="fetchData"
    />
  </div>
</template>
