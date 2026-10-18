<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { type FormInst, type FormRules } from 'naive-ui';
import { assignCalendarShifts, createFactoryCalendar, getFactoryCalendar, type FactoryCalendar } from '@/service/api/mes/calendar';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesCalendar' });

function today() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

const date = ref<string | null>(today());
const day = ref<FactoryCalendar | null>(null);
const showCreate = ref(false);
const showShifts = ref(false);
const createRef = ref<FormInst | null>(null);
const shiftRef = ref<FormInst | null>(null);
const createForm = reactive({ isWorkday: true });
const shiftForm = reactive({ shiftCodes: '' });
const shiftRules: FormRules = { shiftCodes: [{ required: true, message: $t('page.mes.calendar.required'), trigger: 'blur' }] };

function openCreate() {
  createForm.isWorkday = true;
  showCreate.value = true;
}

function openShifts() {
  shiftForm.shiftCodes = day.value?.shiftCodes ?? '';
  showShifts.value = true;
}

async function fetchData() {
  if (!date.value) return;
  const res = await getFactoryCalendar(date.value);
  day.value = res.error ? null : (res.data ?? null);
}

async function submitCreate() {
  await createRef.value?.validate();
  if (!date.value) return;
  await createFactoryCalendar({ calDate: date.value, isWorkday: createForm.isWorkday ? 1 : 0 });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submitShifts() {
  await shiftRef.value?.validate();
  if (!day.value) return;
  await assignCalendarShifts(String(day.value.id), { shiftCodes: shiftForm.shiftCodes });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.calendar.title') }}</span>
          <div class="flex items-center gap-8px">
            <NDatePicker v-model:formatted-value="date" type="date" value-format="yyyy-MM-dd" />
            <NButton @click="fetchData">{{ $t('common.search') }}</NButton>
            <PermissionGuard code="mes:calendar:add">
              <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
            <PermissionGuard code="mes:calendar:shift">
              <NButton :disabled="!day" @click="openShifts">{{ $t('page.mes.calendar.assign') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.calendar.hint') }}</div>
      <div v-if="day">
        {{ $t('page.mes.calendar.summary', { date: day.calDate, workday: day.isWorkday === 1 ? $t('page.mes.calendar.workday') : $t('page.mes.calendar.rest'), shifts: day.shiftCodes || $t('page.mes.calendar.none') }) }}
      </div>
      <div v-else>{{ $t('page.mes.calendar.empty') }}</div>
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.calendar.addTitle')" class="w-480px" :on-confirm="submitCreate">
      <NForm ref="createRef" :model="createForm" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.calendar.workdayFlag')">
          <NSwitch v-model:value="createForm.isWorkday" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showShifts" :title="$t('page.mes.calendar.assign')" class="w-560px" :on-confirm="submitShifts">
      <NForm ref="shiftRef" :model="shiftForm" :rules="shiftRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.calendar.shifts')" path="shiftCodes">
          <NInput v-model:value="shiftForm.shiftCodes" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
