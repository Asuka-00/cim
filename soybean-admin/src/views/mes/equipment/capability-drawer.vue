<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import { NButton, type DataTableColumns, type SelectOption } from 'naive-ui';
import {
  createEquipmentCapability,
  decideEquipmentCapability,
  deleteEquipmentCapability,
  getEquipmentCapabilityList,
  updateEquipmentCapability,
  type EquipmentCapability
} from '@/service/api/mes/equipment-capability';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getRecipeList, type Recipe } from '@/service/api/mes/recipe';
import { getStepList, type Step } from '@/service/api/mes/step';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  equipmentId: string;
  equipmentCode: string;
}>();

const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<EquipmentCapability[]>([]);
const stations = ref<Step[]>([]);
const recipes = ref<Recipe[]>([]);
const products = ref<Product[]>([]);
const decision = ref('');
const editing = ref<EquipmentCapability | null>(null);
const form = reactive({
  stepId: null as string | null,
  recipeId: null as string | null,
  productId: null as string | null,
  status: 'VALID',
  groupBound: false,
  validFrom: null as string | null,
  validTo: null as string | null,
  remark: ''
});
const stationOptions = computed<SelectOption[]>(() => stations.value.map(item => ({ label: item.stepCode, value: item.id })));
const recipeOptions = computed<SelectOption[]>(() => recipes.value.map(item => ({ label: item.recipeCode, value: item.id })));
const productOptions = computed<SelectOption[]>(() => products.value.map(item => ({ label: item.productCode, value: item.id })));
const statusOptions = ['VALID', 'SUSPENDED', 'REVOKED'].map(value => ({ label: value, value }));

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

function labelOf(options: SelectOption[], id?: string | null) {
  if (!id || id === '0') return $t('page.mes.equipmentCapability.any');
  return String(options.find(item => item.value === id)?.label ?? id);
}

async function load() {
  loading.value = true;
  try {
    const [caps, stepPage, recipePage, productPage] = await Promise.all([
      getEquipmentCapabilityList(props.equipmentId),
      getStepList({ page: 1, pageSize: 200 }),
      getRecipeList({ page: 1, pageSize: 200 }),
      getProductList({ page: 1, pageSize: 200 })
    ]);
    rows.value = caps.data ?? [];
    stations.value = stepPage.data?.records ?? [];
    recipes.value = recipePage.data?.records ?? [];
    products.value = productPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.equipmentId] as const,
  ([show]) => {
    if (show) {
      decision.value = '';
      load();
    }
  }
);

function resetForm() {
  editing.value = null;
  form.stepId = null;
  form.recipeId = null;
  form.productId = null;
  form.status = 'VALID';
  form.validFrom = null;
  form.validTo = null;
  form.remark = '';
}

function editRow(row: EquipmentCapability) {
  editing.value = row;
  form.stepId = row.stepId;
  form.recipeId = row.recipeId === '0' ? null : row.recipeId;
  form.productId = row.productId === '0' ? null : row.productId;
  form.status = row.status;
  form.validFrom = row.validFrom ?? null;
  form.validTo = row.validTo ?? null;
  form.remark = row.remark ?? '';
}

async function save() {
  if (!form.stepId) {
    window.$message?.warning($t('page.mes.equipmentCapability.required'));
    return;
  }
  const payload = {
    equipmentId: props.equipmentId,
    stepId: form.stepId,
    recipeId: form.recipeId || null,
    productId: form.productId || null,
    status: form.status,
    validFrom: form.validFrom,
    validTo: form.validTo,
    remark: form.remark
  };
  if (editing.value) {
    await updateEquipmentCapability(editing.value.id, payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createEquipmentCapability(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  resetForm();
  await load();
}

function remove(row: EquipmentCapability) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.equipmentCapability.deleteConfirm'),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteEquipmentCapability(row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await load();
    }
  });
}

async function decide() {
  if (!form.stepId) {
    window.$message?.warning($t('page.mes.equipmentCapability.required'));
    return;
  }
  const { data } = await decideEquipmentCapability({
    equipmentId: props.equipmentId,
    stepId: form.stepId,
    recipeId: form.recipeId || undefined,
    productId: form.productId || undefined,
    groupBound: form.groupBound
  });
  decision.value = data ? `${data.allowed ? $t('page.mes.equipmentCapability.allow') : $t('page.mes.equipmentCapability.deny')} ${data.code}` : '';
}

const columns = computed<DataTableColumns<EquipmentCapability>>(() => [
  { title: $t('page.mes.equipmentCapability.step'), key: 'stepId', render: row => labelOf(stationOptions.value, row.stepId) },
  { title: $t('page.mes.equipmentCapability.recipe'), key: 'recipeId', render: row => labelOf(recipeOptions.value, row.recipeId) },
  { title: $t('page.mes.equipmentCapability.product'), key: 'productId', render: row => labelOf(productOptions.value, row.productId) },
  { title: $t('page.mes.equipmentCapability.status'), key: 'status', width: 120 },
  { title: $t('page.mes.equipmentCapability.validTo'), key: 'validTo', width: 170 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:equipment:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editRow(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:equipment:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => remove(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-860px" :title="`${$t('page.mes.equipmentCapability.title')} ${equipmentCode}`">
    <DataTable :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    <NForm :model="form" label-placement="left" label-width="96" class="mt-16px">
      <NFormItem :label="$t('page.mes.equipmentCapability.step')">
        <NSelect v-model:value="form.stepId" :options="stationOptions" filterable />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentCapability.recipe')">
        <NSelect v-model:value="form.recipeId" :options="recipeOptions" filterable clearable />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentCapability.product')">
        <NSelect v-model:value="form.productId" :options="productOptions" filterable clearable />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentCapability.status')">
        <NSelect v-model:value="form.status" :options="statusOptions" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentCapability.validFrom')">
        <NDatePicker
          v-model:formatted-value="form.validFrom"
          type="datetime"
          value-format="yyyy-MM-dd'T'HH:mm:ss"
          clearable
          class="w-full"
        />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentCapability.validTo')">
        <NDatePicker
          v-model:formatted-value="form.validTo"
          type="datetime"
          value-format="yyyy-MM-dd'T'HH:mm:ss"
          clearable
          class="w-full"
        />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentCapability.groupBound')">
        <NSwitch v-model:value="form.groupBound" />
      </NFormItem>
      <div class="mb-12px">{{ decision }}</div>
      <div class="flex justify-end gap-8px">
        <NButton @click="resetForm">{{ $t('common.reset') }}</NButton>
        <NButton @click="decide">{{ $t('page.mes.equipmentCapability.decide') }}</NButton>
        <NButton v-if="!editing && hasAuth('mes:equipment:add')" type="primary" @click="save">{{ $t('common.add') }}</NButton>
        <NButton v-else-if="editing && hasAuth('mes:equipment:edit')" type="primary" @click="save">{{ $t('common.save') }}</NButton>
      </div>
    </NForm>
  </NModal>
</template>
