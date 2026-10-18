<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { addLotNote, getLotNotes, type LotNote } from '@/service/api/mes/note';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesNote' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.note.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const notes = ref<LotNote[]>([]);
const form = reactive({ noteType: 'NOTE', content: '', pinned: 0 });

const typeOptions = ['NOTE', 'WARNING', 'ENGINEERING', 'TAG'].map(value => ({ label: value, value }));
const rules = computed<FormRules>(() => ({
  content: [{ required: true, message: $t('page.mes.note.required'), trigger: 'blur' }]
}));

const noteColumns = computed<DataTableColumns<LotNote>>(() => [
  { title: $t('page.mes.note.noteType'), key: 'noteType', width: 130 },
  { title: $t('page.mes.note.content'), key: 'content' },
  {
    title: $t('page.mes.note.pinned'),
    key: 'pinned',
    width: 80,
    render: row => (row.pinned === 1 ? $t('page.mes.note.yes') : '')
  }
]);

async function openNotes(row: Lot) {
  current.value = row;
  Object.assign(form, { noteType: 'NOTE', content: '', pinned: 0 });
  const res = await getLotNotes(String(row.id));
  notes.value = res.data ?? [];
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.note.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.note.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:lot:note')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openNotes(row) }, () => $t('page.mes.note.open'))
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return;
  await addLotNote(String(current.value.id), {
    noteType: form.noteType,
    content: form.content,
    pinned: form.pinned
  });
  window.$message?.success($t('common.addSuccess'));
  const res = await getLotNotes(String(current.value.id));
  notes.value = res.data ?? [];
  form.content = '';
  form.pinned = 0;
  return false;
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
        <span>{{ $t('page.mes.note.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.note.addTitle')" class="w-640px" :on-confirm="handleSubmit">
      <DataTable :columns="noteColumns" :data="notes" size="small" class="mb-12px" />
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.note.noteType')" path="noteType">
          <NSelect v-model:value="form.noteType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.note.content')" path="content">
          <NInput v-model:value="form.content" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.note.pinned')" path="pinned">
          <NSwitch :value="form.pinned === 1" @update:value="value => (form.pinned = value ? 1 : 0)" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
