<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createSamplingRule, getSamplingRules, type SamplingRule } from '@/service/api/mes/sampling-rule';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesSamplingRule' });

const TYPES = ['EVERY_N_LOT', 'PERCENT', 'ALL'];
const loading = ref(false);
const rows = ref<SamplingRule[]>([]);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  ruleCode: '',
  ruleName: '',
  sampleType: 'EVERY_N_LOT',
  nValue: 1 as number | null,
  percent: null as number | null,
  waferCount: null as number | null,
  slotList: ''
});

const rules = computed<FormRules>(() => ({
  ruleCode: [{ required: true, message: $t('page.mes.sampling.required'), trigger: 'blur' }],
  ruleName: [{ required: true, message: $t('page.mes.sampling.required'), trigger: 'blur' }],
  nValue: form.sampleType === 'EVERY_N_LOT' ? [{ required: true, type: 'number', message: $t('page.mes.sampling.required'), trigger: 'blur' }] : [],
  percent: form.sampleType === 'PERCENT' ? [{ required: true, type: 'number', message: $t('page.mes.sampling.required'), trigger: 'blur' }] : []
}));
const typeOptions = TYPES.map(item => ({ label: item, value: item }));

function openCreate() {
  Object.assign(form, { ruleCode: '', ruleName: '', sampleType: 'EVERY_N_LOT', nValue: 1, percent: null, waferCount: null, slotList: '' });
  showCreate.value = true;
}

const columns = computed<DataTableColumns<SamplingRule>>(() => [
  { title: $t('page.mes.sampling.code'), key: 'ruleCode', width: 140 },
  { title: $t('page.mes.sampling.name'), key: 'ruleName' },
  { title: $t('page.mes.sampling.type'), key: 'sampleType', width: 140 },
  { title: $t('page.mes.sampling.nValue'), key: 'nValue', width: 90 },
  { title: $t('page.mes.sampling.percent'), key: 'percent', width: 90 },
  { title: $t('page.mes.sampling.slots'), key: 'slotList' }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getSamplingRules();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await formRef.value?.validate();
  await createSamplingRule({
    ruleCode: form.ruleCode,
    ruleName: form.ruleName,
    sampleType: form.sampleType,
    nValue: form.sampleType === 'EVERY_N_LOT' ? form.nValue : null,
    percent: form.sampleType === 'PERCENT' ? form.percent : null,
    waferCount: form.waferCount,
    slotList: form.slotList
  });
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
          <span>{{ $t('page.mes.sampling.title') }}</span>
          <PermissionGuard code="mes:sampling:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.sampling.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <div class="mb-12px">{{ $t('page.mes.sampling.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.sampling.code')" path="ruleCode">
          <NInput v-model:value="form.ruleCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sampling.name')" path="ruleName">
          <NInput v-model:value="form.ruleName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sampling.type')" path="sampleType">
          <NSelect v-model:value="form.sampleType" :options="typeOptions" />
        </NFormItem>
        <NFormItem v-if="form.sampleType === 'EVERY_N_LOT'" :label="$t('page.mes.sampling.nValue')" path="nValue">
          <NInputNumber v-model:value="form.nValue" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem v-if="form.sampleType === 'PERCENT'" :label="$t('page.mes.sampling.percent')" path="percent">
          <NInputNumber v-model:value="form.percent" :min="0.01" :max="100" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sampling.waferCount')" path="waferCount">
          <NInputNumber v-model:value="form.waferCount" :min="1" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sampling.slots')" path="slotList">
          <NInput v-model:value="form.slotList" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
