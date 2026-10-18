<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createCustomer, deleteCustomer, getCustomerList, updateCustomer, type Customer } from '@/service/api/mes/customer';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesCustomer' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Customer, { customerCode: string }>({
    initialQuery: { customerCode: '' },
    fetcher: getCustomerList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'customerCode', label: $t('page.mes.customer.customerCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<Customer | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  customerCode: '',
  customerName: '',
  contact: '',
  phone: '',
  remark: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  customerCode: [{ required: true, message: $t('page.mes.customer.required'), trigger: 'blur' }],
  customerName: [{ required: true, message: $t('page.mes.customer.required'), trigger: 'blur' }]
}));

function openCreate() {
  editing.value = null;
  Object.assign(form, { customerCode: '', customerName: '', contact: '', phone: '', remark: '', enabled: 1 });
  showModal.value = true;
}

function openEdit(row: Customer) {
  editing.value = row;
  Object.assign(form, row);
  showModal.value = true;
}

const columns = computed<DataTableColumns<Customer>>(() => [
  { title: $t('page.mes.customer.customerCode'), key: 'customerCode' },
  { title: $t('page.mes.customer.customerName'), key: 'customerName' },
  { title: $t('page.mes.customer.contact'), key: 'contact' },
  { title: $t('page.mes.customer.phone'), key: 'phone', width: 140 },
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
        hasAuth('mes:customer:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:customer:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form };
  if (editing.value) {
    await updateCustomer(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createCustomer(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: Customer) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.customer.deleteConfirm', { name: row.customerCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteCustomer(String(row.id));
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
          <span>{{ $t('page.mes.customer.title') }}</span>
          <PermissionGuard code="mes:customer:add">
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
      :title="editing ? $t('page.mes.customer.editTitle') : $t('page.mes.customer.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.customer.customerCode')" path="customerCode">
          <NInput v-model:value="form.customerCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.customer.customerName')" path="customerName">
          <NInput v-model:value="form.customerName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.customer.contact')" path="contact">
          <NInput v-model:value="form.contact" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.customer.phone')" path="phone">
          <NInput v-model:value="form.phone" />
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
