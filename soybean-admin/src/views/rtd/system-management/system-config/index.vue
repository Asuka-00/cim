<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInst, UploadFileInfo } from 'naive-ui';
import {
  getSystemConfigList,
  updateSystemConfig,
  type SystemConfigVO
} from '@/service/api/rtd/system-management/system-config';
import { CommonState } from '@/service/api/common';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useBrandStore } from '@/store/modules/brand';
import { localStg } from '@/utils/storage';
import { svgToDataUrl } from '@/utils/brand';
import { $t } from '@/locales';

defineOptions({ name: 'RtdSystemManagementSystemConfig' });

const brandStore = useBrandStore();

const loading = ref(false);
const list = ref<SystemConfigVO[]>([]);
/** Empty array = all groups collapsed by default */
const activeGroups = ref<Array<string | number>>([]);
const showModal = ref(false);
const editing = ref<SystemConfigVO | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  configValue: '',
  valueType: '',
  groupCode: '',
  remark: '',
  state: CommonState.ENABLED as string
});

interface ConfigGroup {
  groupCode: string;
  items: SystemConfigVO[];
}

const ungroupedLabel = computed(() => $t('page.systemManagement.systemConfig.ungrouped'));

const groupedConfigs = computed<ConfigGroup[]>(() => {
  const map = new Map<string, SystemConfigVO[]>();
  for (const item of list.value) {
    const key = item.groupCode?.trim() || '';
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(item);
  }
  return Array.from(map.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([groupCode, items]) => ({
      groupCode: groupCode || ungroupedLabel.value,
      items: items.sort((x, y) => (x.configCode || '').localeCompare(y.configCode || ''))
    }));
});

const valueType = computed(() => (editing.value?.valueType ?? form.valueType ?? '').toLowerCase());
const isBooleanType = computed(() => valueType.value === 'boolean' || valueType.value === 'bool');
const isNumberType = computed(() => ['int', 'integer', 'number', 'long'].includes(valueType.value));
const isJsonType = computed(() => ['json', 'object', 'array'].includes(valueType.value));
const isSvgType = computed(() => valueType.value === 'svg');

const booleanValue = computed({
  get: () => form.configValue === 'true' || form.configValue === '1',
  set: (val: boolean) => {
    form.configValue = val ? 'true' : 'false';
  }
});

const numberValue = computed({
  get: () => {
    const n = Number(form.configValue);
    return Number.isFinite(n) ? n : 0;
  },
  set: (val: number | null) => {
    form.configValue = val == null ? '' : String(val);
  }
});

const displayLabelOf = (item: Pick<SystemConfigVO, 'remark' | 'configCode'>) =>
  item.remark?.trim() || item.configCode || '';

const displayLabel = computed(() => (editing.value ? displayLabelOf(editing.value) : ''));
const svgPreviewUrl = computed(() => svgToDataUrl(form.configValue));
const modalWidth = computed(() => (isJsonType.value || isSvgType.value ? '640px' : '520px'));
const modalTitle = computed(() =>
  editing.value
    ? `${$t('page.systemManagement.systemConfig.editTitle')} - ${editing.value.configCode}`
    : $t('page.systemManagement.systemConfig.editTitle')
);

function isBooleanItem(type?: string) {
  const t = (type || '').toLowerCase();
  return t === 'boolean' || t === 'bool';
}

function isSvgItem(type?: string) {
  return (type || '').toLowerCase() === 'svg';
}

function displayValue(item: SystemConfigVO) {
  if (isBooleanItem(item.valueType)) {
    return item.configValue === 'true' || item.configValue === '1' ? $t('common.enabled') : $t('common.disabled');
  }
  if (isSvgItem(item.valueType)) {
    return item.configValue?.trim() ? $t('page.systemManagement.systemConfig.svgConfigured') : '';
  }
  return item.configValue ?? '';
}

async function fetchData() {
  loading.value = true;
  try {
    const res = await getSystemConfigList();
    list.value = res.data ?? [];
    activeGroups.value = [];
  } finally {
    loading.value = false;
  }
}

function openEdit(row: SystemConfigVO) {
  editing.value = row;
  Object.assign(form, {
    configValue: row.configValue ?? '',
    valueType: row.valueType,
    groupCode: row.groupCode,
    remark: row.remark ?? '',
    state: row.state
  });
  showModal.value = true;
}

function onSvgFileChange(options: { file: UploadFileInfo }) {
  const raw = options.file.file;
  if (!raw) return;
  const reader = new FileReader();
  reader.addEventListener('load', () => {
    form.configValue = String(reader.result ?? '');
  });
  reader.readAsText(raw);
}

function clearSvg() {
  form.configValue = '';
}

async function handleSubmit() {
  if (!editing.value) return false;
  if (!isSvgType.value && !String(form.configValue ?? '').trim()) {
    window.$message?.warning($t('page.systemManagement.systemConfig.configValueRequired'));
    return false;
  }
  const updateData = {
    configValue: form.configValue,
    valueType: form.valueType,
    groupCode: form.groupCode,
    remark: form.remark,
    state: form.state as typeof CommonState.ENABLED
  };
  await updateSystemConfig(editing.value.id, updateData);
  Object.assign(editing.value, updateData);
  if ((editing.value.groupCode || '').toUpperCase() === 'BRAND') {
    const tenantId = editing.value.tenantId || localStg.get('loginTenantId') || '';
    if (tenantId) await brandStore.loadBrand(tenantId);
  }
  window.$message?.success($t('common.updateSuccess'));
}

onMounted(fetchData);
</script>

<template>
  <div class="system-config-page rtd-page">
    <div class="system-config-toolbar">
      <NButton :loading="loading" @click="fetchData">{{ $t('common.reload') }}</NButton>
    </div>

    <NSpin :show="loading">
      <NEmpty
        v-if="!loading && groupedConfigs.length === 0"
        :description="$t('page.systemManagement.systemConfig.empty')"
      />

      <NCollapse v-else v-model:expanded-names="activeGroups" display-directive="show" class="system-config-collapse">
        <NCollapseItem v-for="group in groupedConfigs" :key="group.groupCode" :name="group.groupCode">
          <template #header>
            <div class="group-title">
              <span class="group-title__name">{{ group.groupCode }}</span>
              <span class="group-title__count">{{ group.items.length }}</span>
            </div>
          </template>

          <div v-for="item in group.items" :key="item.id" class="config-item">
            <div class="col-label">
              <NTooltip :disabled="!displayLabelOf(item)" placement="top">
                <template #trigger>
                  <span class="cell-text">{{ displayLabelOf(item) }}</span>
                </template>
                {{ displayLabelOf(item) }}
              </NTooltip>
            </div>
            <div class="col-code">
              <NTooltip :disabled="!item.configCode" placement="top">
                <template #trigger>
                  <span class="cell-text">{{ item.configCode }}</span>
                </template>
                {{ item.configCode }}
              </NTooltip>
            </div>
            <div class="col-type">
              <NTag v-if="item.valueType" size="small" :bordered="true">{{ item.valueType }}</NTag>
            </div>
            <div class="col-value">
              <NTooltip :disabled="!displayValue(item)" placement="top">
                <template #trigger>
                  <span class="cell-text">{{ displayValue(item) }}</span>
                </template>
                {{ displayValue(item) }}
              </NTooltip>
            </div>
            <div class="col-actions">
              <PermissionGuard code="rtd:system-config:edit">
                <NButton type="primary" text @click="openEdit(item)">
                  {{ $t('common.edit') }}
                </NButton>
              </PermissionGuard>
            </div>
          </div>
        </NCollapseItem>
      </NCollapse>
    </NSpin>

    <CrudDialog
      v-model:show="showModal"
      :title="modalTitle"
      :style="{ width: modalWidth }"
      confirm-perm="rtd:system-config:edit"
      :confirm-text="$t('common.save')"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" label-placement="left" label-width="auto">
        <NFormItem :label="$t('page.systemManagement.systemConfig.configCode')">
          <NInput :value="editing?.configCode" disabled />
        </NFormItem>
        <NFormItem :label="$t('common.remark')">
          <NInput :value="displayLabel" disabled />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.systemConfig.valueType')">
          <NInput :value="editing?.valueType" disabled />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.systemConfig.configValue')" path="configValue">
          <NSwitch v-if="isBooleanType" v-model:value="booleanValue" />
          <NInputNumber v-else-if="isNumberType" v-model:value="numberValue" class="w-full" />
          <div v-else-if="isSvgType" class="flex w-full flex-col gap-10px">
            <NSpace>
              <NUpload
                :show-file-list="false"
                accept=".svg,image/svg+xml"
                :default-upload="false"
                @change="onSvgFileChange"
              >
                <NButton type="primary" ghost>{{ $t('page.systemManagement.systemConfig.pickSvg') }}</NButton>
              </NUpload>
              <NButton ghost @click="clearSvg">{{ $t('page.systemManagement.systemConfig.clearSvg') }}</NButton>
            </NSpace>
            <NInput
              v-model:value="form.configValue"
              type="textarea"
              :rows="8"
              :placeholder="$t('page.systemManagement.systemConfig.svgPlaceholder')"
              class="font-mono text-12px"
            />
            <div v-if="svgPreviewUrl" class="rounded-6px bg-layout p-8px">
              <div class="mb-6px text-12px text-base-text/55">
                {{ $t('page.systemManagement.systemConfig.svgPreview') }}
              </div>
              <img :src="svgPreviewUrl" alt="svg preview" class="max-h-96px max-w-full object-contain" />
            </div>
          </div>
          <NInput
            v-else-if="isJsonType"
            v-model:value="form.configValue"
            type="textarea"
            :rows="8"
            class="font-mono text-12px"
          />
          <NInput v-else v-model:value="form.configValue" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>

<style scoped>
.system-config-page {
  overflow-y: auto;
}

.system-config-toolbar {
  display: flex;
  flex-shrink: 0;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.system-config-collapse {
  background: transparent;
}

.system-config-collapse :deep(.n-collapse-item) {
  margin: 0 0 16px;
  overflow: hidden;
  background: rgb(var(--container-bg-color));
  border: 1px solid rgb(var(--base-text-color) / 10%);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgb(0 0 0 / 4%);
}

.system-config-collapse :deep(.n-collapse-item:last-child) {
  margin-bottom: 0;
}

.system-config-collapse :deep(.n-collapse-item:not(:first-child)) {
  border-top: 1px solid rgb(var(--base-text-color) / 10%);
}

.system-config-collapse :deep(.n-collapse-item__header) {
  padding: 12px 16px;
}

.system-config-collapse :deep(.n-collapse-item:first-child > .n-collapse-item__header) {
  padding-top: 12px;
}

.system-config-collapse :deep(.n-collapse-item--active > .n-collapse-item__header) {
  border-bottom: 1px solid rgb(var(--base-text-color) / 10%);
}

.system-config-collapse :deep(.n-collapse-item__content-inner) {
  padding: 4px 4px 8px;
}

.group-title {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.group-title__name {
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600;
  white-space: nowrap;
}

.group-title__count {
  flex-shrink: 0;
  min-width: 20px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  color: rgb(var(--base-text-color) / 55%);
  text-align: center;
  background: rgb(var(--layout-bg-color));
  border-radius: 10px;
}

.config-item {
  display: grid;
  grid-template-columns: minmax(140px, 1.2fr) minmax(160px, 1.4fr) 96px minmax(160px, 1.6fr) 80px;
  gap: 12px;
  align-items: center;
  width: 100%;
  min-height: 44px;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.15s ease;
}

.config-item + .config-item {
  border-top: 1px solid rgb(var(--base-text-color) / 8%);
}

.config-item:hover {
  background: rgb(var(--layout-bg-color));
}

.col-label,
.col-code,
.col-value {
  min-width: 0;
}

.col-label {
  font-size: 14px;
  font-weight: 500;
  line-height: 22px;
}

.col-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 18px;
  color: rgb(var(--base-text-color) / 55%);
}

.col-type {
  display: flex;
  align-items: center;
}

.col-value {
  font-size: 14px;
  line-height: 22px;
  color: rgb(var(--base-text-color) / 75%);
}

.col-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.cell-text {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
