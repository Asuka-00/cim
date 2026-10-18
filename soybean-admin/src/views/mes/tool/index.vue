<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { checkoutTool, createTool, getTools, returnTool, type Tool } from '@/service/api/mes/tool';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesTool' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<Tool[]>([]);
const equipment = ref<Equipment[]>([]);
const showCreate = ref(false);
const showCheckout = ref(false);
const createRef = ref<FormInst | null>(null);
const checkoutRef = ref<FormInst | null>(null);
const current = ref<Tool | null>(null);
const createForm = reactive({ toolCode: '', toolName: '', typeId: '' });
const checkoutForm = reactive({ equipmentId: null as string | null });

const createRules = computed<FormRules>(() => ({
  toolCode: [{ required: true, message: $t('page.mes.tool.required'), trigger: 'blur' }],
  toolName: [{ required: true, message: $t('page.mes.tool.required'), trigger: 'blur' }],
  typeId: [{ required: true, message: $t('page.mes.tool.required'), trigger: 'blur' }]
}));
const checkoutRules = computed<FormRules>(() => ({
  equipmentId: [{ required: true, message: $t('page.mes.tool.required'), trigger: 'change' }]
}));

const equipmentName = computed(() => {
  const names = new Map(equipment.value.map(item => [String(item.id), `${item.equipmentCode} ${item.equipmentName}`]));
  return (id?: string | null) => (id ? (names.get(String(id)) ?? id) : '');
});
const equipmentOptions = computed(() =>
  equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }))
);

function openCreate() {
  Object.assign(createForm, { toolCode: '', toolName: '', typeId: '' });
  showCreate.value = true;
}

function openCheckout(row: Tool) {
  current.value = row;
  checkoutForm.equipmentId = null;
  showCheckout.value = true;
}

const columns = computed<DataTableColumns<Tool>>(() => [
  { title: $t('page.mes.tool.code'), key: 'toolCode' },
  { title: $t('page.mes.tool.name'), key: 'toolName' },
  { title: $t('page.mes.tool.status'), key: 'status', width: 120 },
  { title: $t('page.mes.tool.equipment'), key: 'currentEquipmentId', render: row => equipmentName.value(row.currentEquipmentId) },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'AVAILABLE' && hasAuth('mes:tool:checkout')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openCheckout(row) }, () =>
              $t('page.mes.tool.checkout')
            )
          : null,
        row.status === 'IN_USE' && hasAuth('mes:tool:return')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => handleReturn(row) }, () =>
              $t('page.mes.tool.return')
            )
          : null
      ])
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [toolRes, equipmentPage] = await Promise.all([getTools(), getEquipmentList({ page: 1, pageSize: 200 })]);
    rows.value = toolRes.data ?? [];
    equipment.value = equipmentPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await createRef.value?.validate();
  await createTool(createForm);
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submitCheckout() {
  await checkoutRef.value?.validate();
  if (!current.value || !checkoutForm.equipmentId) return;
  await checkoutTool(String(current.value.id), { equipmentId: checkoutForm.equipmentId });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

function handleReturn(row: Tool) {
  window.$dialog?.warning({
    title: $t('page.mes.tool.return'),
    content: $t('page.mes.tool.returnConfirm', { name: row.toolCode }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await returnTool(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.tool.title') }}</span>
          <PermissionGuard code="mes:tool:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.tool.addTitle')" class="w-560px" :on-confirm="submitCreate">
      <div class="mb-12px">{{ $t('page.mes.tool.typeHint') }}</div>
      <NForm ref="createRef" :model="createForm" :rules="createRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.tool.code')" path="toolCode">
          <NInput v-model:value="createForm.toolCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.tool.name')" path="toolName">
          <NInput v-model:value="createForm.toolName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.tool.typeId')" path="typeId">
          <NInput v-model:value="createForm.typeId" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showCheckout" :title="$t('page.mes.tool.checkoutTitle')" class="w-560px" :on-confirm="submitCheckout">
      <NForm ref="checkoutRef" :model="checkoutForm" :rules="checkoutRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.tool.equipment')" path="equipmentId">
          <NSelect v-model:value="checkoutForm.equipmentId" :options="equipmentOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
