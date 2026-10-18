<script setup lang="ts">
import { computed, ref, useAttrs } from 'vue';
import { $t } from '@/locales';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { resolveModalStyle } from '@/components/business/crud-dialog';

defineOptions({ name: 'CrudDialog', inheritAttrs: false });

const attrs = useAttrs();
const modalStyle = computed(() =>
  resolveModalStyle(attrs.class, attrs.style as Record<string, string | number> | string | undefined)
);

const show = defineModel<boolean>('show', { default: false });

const props = withDefaults(
  defineProps<{
    title?: string;
    loading?: boolean;
    hideFooter?: boolean;
    confirmText?: string;
    cancelText?: string;
    maskClosable?: boolean;
    closable?: boolean;
    /** Called on confirm. Resolve/return → close. Reject or `false` → stay open. */
    onConfirm?: () => unknown | Promise<unknown>;
    onCancel?: () => unknown | Promise<unknown>;
    confirmPerm?: string | string[];
  }>(),
  {
    loading: false,
    hideFooter: false,
    maskClosable: true,
    closable: true
  }
);

const innerLoading = ref(false);
const busy = computed(() => props.loading || innerLoading.value);

const confirmLabel = computed(() => props.confirmText || $t('common.confirm'));
const cancelLabel = computed(() => props.cancelText || $t('common.cancel'));

async function handleConfirm() {
  if (busy.value) return;
  innerLoading.value = true;
  try {
    const result = props.onConfirm ? await props.onConfirm() : undefined;
    if (result === false) return;
    show.value = false;
  } catch {
    // Validation or request errors stay in the form / HTTP interceptor.
  } finally {
    innerLoading.value = false;
  }
}

async function handleCancel() {
  if (props.onCancel) {
    const result = await props.onCancel();
    if (result === false) return;
  }
  show.value = false;
}

function onUpdateShow(value: boolean) {
  if (value) {
    show.value = true;
    return;
  }
  void handleCancel();
}
</script>

<template>
  <NModal
    :show="show"
    preset="card"
    :title="title"
    :class="attrs.class"
    :style="modalStyle"
    :mask-closable="maskClosable"
    :closable="closable"
    :auto-focus="false"
    @update:show="onUpdateShow"
  >
    <div class="crud-dialog-body">
      <slot />
    </div>
    <template v-if="!hideFooter" #footer>
      <NSpace justify="end">
        <NButton @click="handleCancel">{{ cancelLabel }}</NButton>
        <PermissionGuard v-if="confirmPerm" :code="confirmPerm">
          <NButton type="primary" :loading="busy" @click="handleConfirm">{{ confirmLabel }}</NButton>
        </PermissionGuard>
        <NButton v-else type="primary" :loading="busy" @click="handleConfirm">{{ confirmLabel }}</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped>
.crud-dialog-body :deep(.n-form-item-label),
.crud-dialog-body :deep(.n-form-item-label__text) {
  white-space: nowrap;
}
</style>
