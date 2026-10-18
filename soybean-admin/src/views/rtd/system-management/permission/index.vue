<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createPermission,
  deletePermission,
  getPermissionOptions,
  PermissionType,
  updatePermission,
  VisibilityState,
  type PermissionVO
} from '@/service/api/rtd/system-management/permission';
import { CommonState, type CommonStateValue } from '@/service/api/common';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';
import { translatePermissionName } from '@/locales/permission';
import { renderBoolTag, renderEnabledTag, renderTag } from '@/utils/tag-render';

defineOptions({ name: 'RtdSystemManagementPermission' });

const { hasAuth } = useAuth();
const authStore = useAuthStore();
const canWriteCatalog = computed(() => authStore.platformAdmin);

interface TreeNode extends PermissionVO {
  children?: TreeNode[];
}

const loading = ref(false);
const flat = ref<PermissionVO[]>([]);
const showModal = ref(false);
const editing = ref<PermissionVO | null>(null);
const formRef = ref<FormInst | null>(null);

const form = reactive({
  permissionCode: '',
  permissionName: '',
  permissionDescription: '',
  type: PermissionType.MENU as string,
  path: '',
  pathName: '',
  component: '',
  meta: '',
  permissionOrder: 0,
  visible: VisibilityState.VISIBLE as string,
  parentId: null as string | null,
  enabled: CommonState.ENABLED as CommonStateValue
});

const rules = computed<FormRules>(() => ({
  permissionCode: [
    { required: true, message: $t('page.systemManagement.permission.permissionCodeRequired'), trigger: 'blur' }
  ],
  permissionName: [
    { required: true, message: $t('page.systemManagement.permission.permissionNameRequired'), trigger: 'blur' }
  ],
  type: [{ required: true, message: $t('page.systemManagement.permission.typeRequired'), trigger: 'change' }]
}));

function buildTree(items: PermissionVO[]): TreeNode[] {
  const map = new Map<string, TreeNode>();
  items.forEach(i => map.set(i.id, { ...i, children: [] }));
  const roots: TreeNode[] = [];
  map.forEach(node => {
    if (node.parentId && map.has(node.parentId)) {
      map.get(node.parentId)!.children!.push(node);
    } else {
      roots.push(node);
    }
  });
  const sortNodes = (nodes: TreeNode[]) => {
    nodes.sort((a, b) => (a.permissionOrder ?? 0) - (b.permissionOrder ?? 0));
    nodes.forEach(n => n.children && sortNodes(n.children));
  };
  sortNodes(roots);
  return roots;
}

const treeData = computed(() => buildTree(flat.value));

const parentOptions = computed(() =>
  flat.value.map(i => ({
    label: `${translatePermissionName(i.permissionName)} (${i.permissionCode})`,
    value: i.id
  }))
);

const columns = computed<DataTableColumns<TreeNode>>(() => [
  {
    title: $t('page.systemManagement.permission.permissionName'),
    key: 'permissionName',
    ellipsis: { tooltip: true },
    render: row => translatePermissionName(row.permissionName)
  },
  { title: $t('page.systemManagement.permission.permissionCode'), key: 'permissionCode', ellipsis: { tooltip: true } },
  {
    title: $t('common.type'),
    key: 'type',
    width: 90,
    render: row =>
      renderTag(
        row.type,
        row.type === PermissionType.MENU ? 'info' : row.type === PermissionType.BUTTON ? 'warning' : 'default'
      )
  },
  { title: $t('page.systemManagement.permission.path'), key: 'path', ellipsis: { tooltip: true } },
  { title: $t('common.sort'), key: 'permissionOrder', width: 70 },
  {
    title: $t('page.systemManagement.permission.visible'),
    key: 'visible',
    width: 80,
    render: row => renderBoolTag(row.visible === VisibilityState.VISIBLE, 'info')
  },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 80,
    render: row => renderEnabledTag(row.enabled)
  },
  ...(canWriteCatalog.value
    ? ([
        {
          title: $t('common.operate'),
          key: 'actions',
          width: 200,
          render: (row: TreeNode) =>
            h('div', { class: 'flex gap-8px' }, [
              hasAuth('rtd:permission:add')
                ? h(NButton, { size: 'small', quaternary: true, onClick: () => openCreate(row.id) }, () =>
                    $t('page.systemManagement.permission.addChild')
                  )
                : null,
              hasAuth('rtd:permission:edit')
                ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () =>
                    $t('common.edit')
                  )
                : null,
              hasAuth('rtd:permission:delete')
                ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
                    $t('common.delete')
                  )
                : null
            ])
        }
      ] as DataTableColumns<TreeNode>)
    : [])
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getPermissionOptions();
    flat.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate(parentId: string | null = null) {
  editing.value = null;
  Object.assign(form, {
    permissionCode: '',
    permissionName: '',
    permissionDescription: '',
    type: PermissionType.MENU,
    path: '',
    pathName: '',
    component: '',
    meta: '',
    permissionOrder: 0,
    visible: VisibilityState.VISIBLE,
    parentId,
    enabled: CommonState.ENABLED
  });
  showModal.value = true;
}

function openEdit(row: PermissionVO) {
  editing.value = row;
  Object.assign(form, {
    permissionCode: row.permissionCode,
    permissionName: row.permissionName,
    permissionDescription: row.permissionDescription ?? '',
    type: row.type,
    path: row.path ?? '',
    pathName: row.pathName ?? '',
    component: row.component ?? '',
    meta: row.meta ?? '',
    permissionOrder: row.permissionOrder ?? 0,
    visible: row.visible,
    parentId: row.parentId,
    enabled: row.enabled
  });
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = {
    permissionName: form.permissionName,
    permissionDescription: form.permissionDescription,
    type: form.type as typeof PermissionType.MENU,
    path: form.path || undefined,
    pathName: form.pathName || undefined,
    component: form.component || undefined,
    meta: form.meta || undefined,
    permissionOrder: form.permissionOrder,
    visible: form.visible as typeof VisibilityState.VISIBLE,
    parentId: form.parentId,
    enabled: form.enabled
  };
  if (editing.value) {
    await updatePermission(editing.value.id, payload);
    window.$message?.success($t('common.updateSuccess'));
  } else {
    await createPermission({ permissionCode: form.permissionCode, ...payload });
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: TreeNode) {
  if (row.children && row.children.length > 0) {
    window.$message?.warning($t('page.systemManagement.permission.hasChildrenCannotDelete'));
    return;
  }
  window.$dialog?.warning({
    title: $t('page.systemManagement.common.confirmDeleteTitle'),
    content: $t('page.systemManagement.permission.deleteConfirm', {
      name: translatePermissionName(row.permissionName)
    }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deletePermission(row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.systemManagement.permission.title') }}</span>
          <PermissionGuard v-if="canWriteCatalog" code="rtd:permission:add">
            <NButton type="primary" @click="openCreate(null)">
              {{ $t('page.systemManagement.permission.addRoot') }}
            </NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable
        flex-height
        :columns="columns"
        :data="treeData"
        :loading="loading"
        :bordered="false"
        size="small"
        default-expand-all
        :row-key="row => row.id"
      />
    </NCard>

    <CrudDialog
      v-model:show="showModal"
      :title="
        editing ? $t('page.systemManagement.permission.editTitle') : $t('page.systemManagement.permission.addTitle')
      "
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="auto">
        <NFormItem :label="$t('page.systemManagement.permission.permissionCode')" path="permissionCode">
          <NInput v-model:value="form.permissionCode" :disabled="Boolean(editing)" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.permission.permissionName')" path="permissionName">
          <NInput v-model:value="form.permissionName" />
        </NFormItem>
        <NFormItem :label="$t('common.description')" path="permissionDescription">
          <NInput v-model:value="form.permissionDescription" />
        </NFormItem>
        <NFormItem :label="$t('common.type')" path="type">
          <NSelect
            v-model:value="form.type"
            :options="[
              { label: 'MENU', value: PermissionType.MENU },
              { label: 'BUTTON', value: PermissionType.BUTTON },
              { label: 'API', value: PermissionType.API }
            ]"
          />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.permission.parent')" path="parentId">
          <NSelect v-model:value="form.parentId" :options="parentOptions" clearable filterable />
        </NFormItem>
        <NFormItem
          v-if="form.type === PermissionType.MENU"
          :label="$t('page.systemManagement.permission.path')"
          path="path"
        >
          <NInput v-model:value="form.path" />
        </NFormItem>
        <NFormItem
          v-if="form.type === PermissionType.MENU"
          :label="$t('page.systemManagement.permission.pathName')"
          path="pathName"
        >
          <NInput v-model:value="form.pathName" />
        </NFormItem>
        <NFormItem
          v-if="form.type === PermissionType.MENU"
          :label="$t('page.systemManagement.permission.component')"
          path="component"
        >
          <NInput v-model:value="form.component" />
        </NFormItem>
        <NFormItem label="Meta JSON" path="meta">
          <NInput v-model:value="form.meta" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem :label="$t('common.sort')" path="permissionOrder">
          <NInputNumber v-model:value="form.permissionOrder" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.permission.visible')" path="visible">
          <NRadioGroup v-model:value="form.visible">
            <NRadio :value="VisibilityState.VISIBLE">{{ $t('page.systemManagement.permission.visible') }}</NRadio>
            <NRadio :value="VisibilityState.HIDDEN">{{ $t('page.systemManagement.permission.hidden') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem :label="$t('common.status')" path="enabled">
          <NRadioGroup v-model:value="form.enabled">
            <NRadio :value="CommonState.ENABLED">{{ $t('common.enabled') }}</NRadio>
            <NRadio :value="CommonState.DISABLED">{{ $t('common.disabled') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
