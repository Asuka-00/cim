<script setup lang="ts">
import { $t } from '@/locales';
import {
  closeDialog,
  dialogStore,
  resolveModalStyle,
  setDialogLoading,
  type CrudDialogItem
} from '@/components/business/crud-dialog';

defineOptions({ name: 'CrudDialogHost' });

function isExpectedConfirmError(error: unknown) {
  if (!error || typeof error !== 'object') return false;
  if ('code' in error || 'isAxiosError' in error) return true;
  return error instanceof Error && ['Validation failed', 'invalid'].includes(error.message);
}

function closeLoading(item: CrudDialogItem) {
  setDialogLoading(item.id, false);
}

async function handleConfirm(item: CrudDialogItem) {
  if (item.loading) return;

  if (item.confirmLoading !== false) {
    setDialogLoading(item.id, true);
  }

  let closedByDone = false;
  const done = () => {
    closedByDone = true;
    closeLoading(item);
    closeDialog(item.id);
  };

  try {
    if (item.beforeSure) {
      await item.beforeSure(done, { id: item.id, closeLoading: () => closeLoading(item) });
      if (!closedByDone) closeLoading(item);
      return;
    }
    done();
  } catch (error) {
    closeLoading(item);
    if (!isExpectedConfirmError(error)) {
      console.error('[CrudDialog] beforeSure failed:', error);
    }
  }
}

async function handleCancel(item: CrudDialogItem) {
  const done = () => closeDialog(item.id);
  if (item.beforeCancel) {
    await item.beforeCancel(done);
    return;
  }
  done();
}

function onUpdateShow(item: CrudDialogItem, value: boolean) {
  if (value) {
    item.show = true;
    return;
  }
  void handleCancel(item);
}
</script>

<template>
  <NModal
    v-for="item in dialogStore"
    :key="item.id"
    :show="item.show"
    preset="card"
    :title="item.title"
    :class="item.class"
    :style="resolveModalStyle(item.class, item.style)"
    :mask-closable="item.maskClosable"
    :closable="item.closable"
    :auto-focus="false"
    @update:show="v => onUpdateShow(item, v)"
  >
    <component :is="{ render: item.content }" />
    <template v-if="!item.hideFooter" #footer>
      <NSpace justify="end">
        <NButton @click="handleCancel(item)">{{ item.cancelText || $t('common.cancel') }}</NButton>
        <NButton type="primary" :loading="item.loading" @click="handleConfirm(item)">
          {{ item.confirmText || $t('common.confirm') }}
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>
