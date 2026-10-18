<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInst, FormRules } from 'naive-ui';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import { receiveMaterial } from '@/service/api/mes/receipt';
import { getUomList, type Uom } from '@/service/api/mes/uom';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesReceipt' });

const formRef = ref<FormInst | null>(null);
const materials = ref<Material[]>([]);
const uoms = ref<Uom[]>([]);
const form = reactive({
  receiptNo: '',
  lotCode: '',
  materialId: null as string | null,
  locationId: '',
  uomId: null as string | null,
  qty: null as number | null,
  needInspect: false
});

const rules = computed<FormRules>(() => ({
  receiptNo: [{ required: true, message: $t('page.mes.receipt.required'), trigger: 'blur' }],
  lotCode: [{ required: true, message: $t('page.mes.receipt.required'), trigger: 'blur' }],
  materialId: [{ required: true, message: $t('page.mes.receipt.required'), trigger: 'change' }],
  locationId: [{ required: true, message: $t('page.mes.receipt.required'), trigger: 'blur' }],
  uomId: [{ required: true, message: $t('page.mes.receipt.required'), trigger: 'change' }],
  qty: [{ required: true, type: 'number', message: $t('page.mes.receipt.required'), trigger: 'blur' }]
}));

const materialOptions = computed(() =>
  materials.value.map(item => ({ label: `${item.materialCode} ${item.materialName}`, value: String(item.id) }))
);
const uomOptions = computed(() => uoms.value.map(item => ({ label: `${item.uomCode} ${item.uomName}`, value: String(item.id) })));

async function handleSubmit() {
  await formRef.value?.validate();
  if (!form.materialId || !form.uomId || form.qty == null) return;
  await receiveMaterial({
    receiptNo: form.receiptNo,
    lotCode: form.lotCode,
    materialId: form.materialId,
    locationId: form.locationId,
    uomId: form.uomId,
    qty: form.qty,
    needInspect: form.needInspect
  });
  window.$message?.success($t('common.addSuccess'));
  Object.assign(form, {
    receiptNo: '',
    lotCode: '',
    materialId: null,
    locationId: '',
    uomId: null,
    qty: null,
    needInspect: false
  });
}

onMounted(async () => {
  const [materialPage, uomPage] = await Promise.all([
    getMaterialList({ page: 1, pageSize: 200 }),
    getUomList({ page: 1, pageSize: 200 })
  ]);
  materials.value = materialPage.data?.records ?? [];
  uoms.value = uomPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.receipt.title') }}</span>
      </template>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96" class="max-w-560px">
        <NFormItem :label="$t('page.mes.receipt.receiptNo')" path="receiptNo">
          <NInput v-model:value="form.receiptNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.receipt.lotCode')" path="lotCode">
          <NInput v-model:value="form.lotCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.receipt.material')" path="materialId">
          <NSelect v-model:value="form.materialId" :options="materialOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.receipt.location')" path="locationId">
          <NInput v-model:value="form.locationId" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.receipt.uom')" path="uomId">
          <NSelect v-model:value="form.uomId" :options="uomOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.receipt.qty')" path="qty">
          <NInputNumber v-model:value="form.qty" :min="0" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.receipt.needInspect')" path="needInspect">
          <NSwitch v-model:value="form.needInspect" />
        </NFormItem>
        <PermissionGuard code="mes:receipt:add">
          <NButton type="primary" @click="handleSubmit">{{ $t('page.mes.receipt.submit') }}</NButton>
        </PermissionGuard>
      </NForm>
    </NCard>
  </div>
</template>
