<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createEquipmentCapability,
  decideEquipmentCapability,
  deleteEquipmentCapability,
  getEquipmentCapabilityList,
  updateEquipmentCapability,
  type EquipmentCapability
} from '@/service/api/mes/equipment-capability';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getRecipeList, type Recipe } from '@/service/api/mes/recipe';
import { getStepList, type Step } from '@/service/api/mes/step';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesEquipmentCapability' });

const STATUSES = ['VALID', 'SUSPENDED', 'EXPIRED', 'REVOKED'];
const { hasAuth } = useAuth();
const equipment = ref<Equipment[]>([]);
const steps = ref<Step[]>([]);
const recipes = ref<Recipe[]>([]);
const products = ref<Product[]>([]);
const equipmentId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<EquipmentCapability[]>([]);
const showModal = ref(false);
const editing = ref<EquipmentCapability | null>(null);
const formRef = ref<FormInst | null>(null);
const decision = ref('');
const form = reactive({
  stepId: null as string | null,
  recipeId: null as string | null,
  productId: null as string | null,
  status: 'VALID',
  validFrom: null as string | null,
  validTo: null as string | null,
  remark: ''
});
const decideForm = reactive({
  stepId: null as string | null,
  recipeId: null as string | null,
  productId: null as string | null,
  groupBound: false
});

const rules = computed<FormRules>(() => ({
  stepId: [{ required: true, message: $t('page.mes.equipmentCapability.required'), trigger: 'change' }]
}));
const equipmentOptions = computed(() =>
  equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }))
);
const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const recipeOptions = computed(() => recipes.value.map(item => ({ label: `${item.recipeCode} ${item.recipeName}`, value: String(item.id) })));
const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));
const statusOptions = STATUSES.map(item => ({ label: item, value: item }));

function labelOf(id: string | null | undefined, kind: 'step' | 'recipe' | 'product') {
  if (!id || id === '0') return $t('page.mes.equipmentCapability.any');
  if (kind === 'step') {
    const step = steps.value.find(item => String(item.id) === String(id));
    return step ? `${step.stepCode} ${step.stepName}` : String(id);
  }
  if (kind === 'recipe') {
    const recipe = recipes.value.find(item => String(item.id) === String(id));
    return recipe ? `${recipe.recipeCode} ${recipe.recipeName}` : String(id);
  }
  const product = products.value.find(item => String(item.id) === String(id));
  return product ? `${product.productCode} ${product.productName}` : String(id);
}

function openCreate() {
  editing.value = null;
  Object.assign(form, { stepId: null, recipeId: null, productId: null, status: 'VALID', validFrom: null, validTo: null, remark: '' });
  showModal.value = true;
}

function openEdit(row: EquipmentCapability) {
  editing.value = row;
  Object.assign(form, {
    stepId: String(row.stepId),
    recipeId: !row.recipeId || row.recipeId === '0' ? null : String(row.recipeId),
    productId: !row.productId || row.productId === '0' ? null : String(row.productId),
    status: row.status || 'VALID',
    validFrom: row.validFrom ?? null,
    validTo: row.validTo ?? null,
    remark: row.remark ?? ''
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<EquipmentCapability>>(() => [
  { title: $t('page.mes.equipmentCapability.step'), key: 'stepId', render: row => labelOf(row.stepId, 'step') },
  { title: $t('page.mes.equipmentCapability.recipe'), key: 'recipeId', render: row => labelOf(row.recipeId, 'recipe') },
  { title: $t('page.mes.equipmentCapability.product'), key: 'productId', render: row => labelOf(row.productId, 'product') },
  { title: $t('page.mes.equipmentCapability.status'), key: 'status', width: 120 },
  { title: $t('page.mes.equipmentCapability.validTo'), key: 'validTo', width: 180 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:equipment-capability:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:equipment-capability:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function fetchData() {
  decision.value = '';
  if (!equipmentId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getEquipmentCapabilityList(equipmentId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (!equipmentId.value || !form.stepId) return;
  const data = {
    equipmentId: equipmentId.value,
    stepId: form.stepId,
    recipeId: form.recipeId,
    productId: form.productId,
    status: form.status,
    validFrom: form.validFrom,
    validTo: form.validTo,
    remark: form.remark
  };
  if (editing.value) await updateEquipmentCapability(String(editing.value.id), data);
  else await createEquipmentCapability(data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

function handleDelete(row: EquipmentCapability) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.equipmentCapability.deleteConfirm'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteEquipmentCapability(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

async function decide() {
  if (!equipmentId.value || !decideForm.stepId) {
    window.$message?.warning($t('page.mes.equipmentCapability.required'));
    return;
  }
  const res = await decideEquipmentCapability({
    equipmentId: equipmentId.value,
    stepId: decideForm.stepId,
    recipeId: decideForm.recipeId || undefined,
    productId: decideForm.productId || undefined,
    groupBound: decideForm.groupBound
  });
  decision.value = res.data
    ? `${res.data.allowed ? $t('page.mes.equipmentCapability.allow') : $t('page.mes.equipmentCapability.deny')} ${res.data.code}`
    : '';
}

onMounted(async () => {
  const [equipmentPage, stepPage, recipePage, productPage] = await Promise.all([
    getEquipmentList({ page: 1, pageSize: 200 }),
    getStepList({ page: 1, pageSize: 200 }),
    getRecipeList({ page: 1, pageSize: 200 }),
    getProductList({ page: 1, pageSize: 200 })
  ]);
  equipment.value = equipmentPage.data?.records ?? [];
  steps.value = stepPage.data?.records ?? [];
  recipes.value = recipePage.data?.records ?? [];
  products.value = productPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.equipmentCapability.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="equipmentId" class="w-260px" :options="equipmentOptions" filterable @update:value="fetchData" />
            <PermissionGuard code="mes:equipment-capability:add">
              <NButton type="primary" :disabled="!equipmentId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.equipmentCapability.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
      <div class="mt-16px flex flex-wrap items-end gap-8px">
        <NSelect v-model:value="decideForm.stepId" class="w-200px" :options="stepOptions" filterable :placeholder="$t('page.mes.equipmentCapability.step')" />
        <NSelect v-model:value="decideForm.recipeId" class="w-180px" :options="recipeOptions" filterable clearable :placeholder="$t('page.mes.equipmentCapability.recipe')" />
        <NSelect v-model:value="decideForm.productId" class="w-180px" :options="productOptions" filterable clearable :placeholder="$t('page.mes.equipmentCapability.product')" />
        <div class="flex items-center gap-8px">
          <span>{{ $t('page.mes.equipmentCapability.groupBound') }}</span>
          <NSwitch v-model:value="decideForm.groupBound" />
        </div>
        <PermissionGuard code="mes:equipment-capability:decide">
          <NButton :disabled="!equipmentId" @click="decide">{{ $t('page.mes.equipmentCapability.decide') }}</NButton>
        </PermissionGuard>
        <span>{{ decision }}</span>
      </div>
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.equipmentCapability.editTitle') : $t('page.mes.equipmentCapability.addTitle')"
      class="w-560px"
      :on-confirm="submit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.equipmentCapability.step')" path="stepId">
          <NSelect v-model:value="form.stepId" :options="stepOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipmentCapability.recipe')" path="recipeId">
          <NSelect v-model:value="form.recipeId" :options="recipeOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipmentCapability.product')" path="productId">
          <NSelect v-model:value="form.productId" :options="productOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipmentCapability.status')" path="status">
          <NSelect v-model:value="form.status" :options="statusOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipmentCapability.validFrom')" path="validFrom">
          <NDatePicker v-model:formatted-value="form.validFrom" type="datetime" value-format="yyyy-MM-dd'T'HH:mm:ss" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.equipmentCapability.validTo')" path="validTo">
          <NDatePicker v-model:formatted-value="form.validTo" type="datetime" value-format="yyyy-MM-dd'T'HH:mm:ss" clearable class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
