<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { trackInLot, trackOutLot } from '@/service/api/mes/track';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesTrack' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.track.lotNo'), type: 'input' },
  { key: 'status', label: $t('page.mes.track.status'), type: 'input' }
]);

const showIn = ref(false);
const showOut = ref(false);
const inFormRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const equipment = ref<Equipment[]>([]);
const inForm = reactive({
  stepId: '',
  equipmentId: null as string | null
});
const outForm = reactive({ remark: '' });

const inRules = computed<FormRules>(() => ({
  stepId: [{ required: true, message: $t('page.mes.track.required'), trigger: 'blur' }]
}));

const equipmentOptions = computed(() =>
  equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }))
);

function openTrackIn(row: Lot) {
  current.value = row;
  inForm.stepId = row.stepId ? String(row.stepId) : '';
  inForm.equipmentId = null;
  showIn.value = true;
}

function openTrackOut(row: Lot) {
  current.value = row;
  outForm.remark = '';
  showOut.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.track.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.track.status'), key: 'status', width: 120 },
  { title: $t('page.mes.track.routeSeqNo'), key: 'routeSeqNo', width: 110 },
  { title: $t('page.mes.track.qty'), key: 'qty', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'WAITING' && hasAuth('mes:lot:track-in')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openTrackIn(row) }, () =>
              $t('page.mes.track.trackIn')
            )
          : null,
        row.status === 'RUNNING' && hasAuth('mes:lot:track-out')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openTrackOut(row) }, () =>
              $t('page.mes.track.trackOut')
            )
          : null
      ])
  }
]);

async function submitTrackIn() {
  await inFormRef.value?.validate();
  if (!current.value) return;
  await trackInLot(String(current.value.id), {
    stepId: inForm.stepId,
    equipmentId: inForm.equipmentId
  });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

async function submitTrackOut() {
  if (!current.value) return;
  await trackOutLot(String(current.value.id), { remark: outForm.remark });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(async () => {
  const page = await getEquipmentList({ page: 1, pageSize: 200 });
  equipment.value = page.data?.records ?? [];
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
        <span>{{ $t('page.mes.track.title') }}</span>
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
    <CrudDialog v-model:show="showIn" :title="$t('page.mes.track.trackInTitle')" class="w-560px" :on-confirm="submitTrackIn">
      <NForm ref="inFormRef" :model="inForm" :rules="inRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.track.stepId')" path="stepId">
          <NInput v-model:value="inForm.stepId" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.track.equipment')" path="equipmentId">
          <NSelect v-model:value="inForm.equipmentId" :options="equipmentOptions" filterable clearable />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showOut" :title="$t('page.mes.track.trackOutTitle')" class="w-560px" :on-confirm="submitTrackOut">
      <NForm :model="outForm" label-placement="left" label-width="96">
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="outForm.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
