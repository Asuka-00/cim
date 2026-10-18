<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createSequenceRule,
  deleteSequenceRule,
  getSequenceRuleList,
  previewSequenceRule,
  updateSequenceRule,
  type SequenceRule
} from '@/service/api/mes/sequence-rule';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesSequenceRule' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<SequenceRule, { ruleCode: string }>({
    initialQuery: { ruleCode: '' },
    fetcher: getSequenceRuleList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'ruleCode', label: $t('page.mes.sequenceRule.ruleCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<SequenceRule | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  ruleCode: '',
  ruleName: '',
  bizType: '',
  prefix: '',
  datePattern: '',
  seqLength: 4,
  seqStart: 1,
  seqStep: 1,
  resetCycle: 'NEVER',
  remark: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  ruleCode: [{ required: true, message: $t('page.mes.sequenceRule.required'), trigger: 'blur' }],
  ruleName: [{ required: true, message: $t('page.mes.sequenceRule.required'), trigger: 'blur' }],
  bizType: [{ required: true, message: $t('page.mes.sequenceRule.required'), trigger: 'blur' }],
  resetCycle: [{ required: true, message: $t('page.mes.sequenceRule.required'), trigger: 'change' }]
}));

const cycleOptions = [
  { label: 'NEVER', value: 'NEVER' },
  { label: 'DAY', value: 'DAY' },
  { label: 'MONTH', value: 'MONTH' },
  { label: 'YEAR', value: 'YEAR' }
];

function openCreate() {
  editing.value = null;
  Object.assign(form, {
    ruleCode: '',
    ruleName: '',
    bizType: '',
    prefix: '',
    datePattern: '',
    seqLength: 4,
    seqStart: 1,
    seqStep: 1,
    resetCycle: 'NEVER',
    remark: '',
    enabled: 1
  });
  showModal.value = true;
}

function openEdit(row: SequenceRule) {
  editing.value = row;
  Object.assign(form, row);
  showModal.value = true;
}

const columns = computed<DataTableColumns<SequenceRule>>(() => [
  { title: $t('page.mes.sequenceRule.ruleCode'), key: 'ruleCode' },
  { title: $t('page.mes.sequenceRule.ruleName'), key: 'ruleName' },
  { title: $t('page.mes.sequenceRule.bizType'), key: 'bizType' },
  { title: $t('page.mes.sequenceRule.resetCycle'), key: 'resetCycle', width: 120 },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 1 ? $t('common.enabled') : $t('common.disabled'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 220,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:sequence-rule:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () =>
              $t('common.edit')
            )
          : null,
        h(
          NButton,
          { size: 'small', quaternary: true, onClick: () => preview(row.ruleCode) },
          () => $t('page.mes.sequenceRule.preview')
        ),
        hasAuth('mes:sequence-rule:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form };
  if (editing.value) {
    await updateSequenceRule(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createSequenceRule(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

async function preview(ruleCode: string) {
  const { data, error } = await previewSequenceRule(ruleCode);
  if (!error) {
    window.$message?.success(String(data ?? ''));
  }
}

function handleDelete(row: SequenceRule) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.sequenceRule.deleteConfirm', { name: row.ruleCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteSequenceRule(String(row.id));
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
          <span>{{ $t('page.mes.sequenceRule.title') }}</span>
          <PermissionGuard code="mes:sequence-rule:add">
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
      :title="editing ? $t('page.mes.sequenceRule.editTitle') : $t('page.mes.sequenceRule.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.sequenceRule.ruleCode')" path="ruleCode">
          <NInput v-model:value="form.ruleCode" :disabled="Boolean(editing)" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sequenceRule.ruleName')" path="ruleName">
          <NInput v-model:value="form.ruleName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sequenceRule.bizType')" path="bizType">
          <NInput v-model:value="form.bizType" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sequenceRule.prefix')" path="prefix">
          <NInput v-model:value="form.prefix" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sequenceRule.datePattern')" path="datePattern">
          <NInput v-model:value="form.datePattern" placeholder="yyyyMMdd / yyMM" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sequenceRule.seqLength')" path="seqLength">
          <NInputNumber v-model:value="form.seqLength" :min="1" :max="18" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sequenceRule.seqStart')" path="seqStart">
          <NInputNumber v-model:value="form.seqStart" :min="0" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sequenceRule.seqStep')" path="seqStep">
          <NInputNumber v-model:value="form.seqStep" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.sequenceRule.resetCycle')" path="resetCycle">
          <NSelect v-model:value="form.resetCycle" :options="cycleOptions" />
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
