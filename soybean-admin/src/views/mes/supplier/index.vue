<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui';
import { createSupplier, getSuppliers, type Supplier } from '@/service/api/mes/supplier';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesSupplier' });

const loading = ref(false);
const list = ref<Supplier[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ supplierCode: '', supplierName: '', contact: '', phone: '' });

const rules = computed<FormRules>(() => ({
  supplierCode: [{ required: true, message: $t('page.mes.supplier.required'), trigger: 'blur' }],
  supplierName: [{ required: true, message: $t('page.mes.supplier.required'), trigger: 'blur' }]
}));

const columns = computed<DataTableColumns<Supplier>>(() => [
  { title: $t('page.mes.supplier.supplierCode'), key: 'supplierCode' },
  { title: $t('page.mes.supplier.supplierName'), key: 'supplierName' },
  { title: $t('page.mes.supplier.contact'), key: 'contact' },
  { title: $t('page.mes.supplier.phone'), key: 'phone', width: 140 }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getSuppliers();
    list.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, { supplierCode: '', supplierName: '', contact: '', phone: '' });
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  await createSupplier({ ...form });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.supplier.title') }}</span>
          <PermissionGuard code="mes:supplier:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.supplier.addTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.supplier.supplierCode')" path="supplierCode">
          <NInput v-model:value="form.supplierCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.supplier.supplierName')" path="supplierName">
          <NInput v-model:value="form.supplierName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.supplier.contact')" path="contact">
          <NInput v-model:value="form.contact" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.supplier.phone')" path="phone">
          <NInput v-model:value="form.phone" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
