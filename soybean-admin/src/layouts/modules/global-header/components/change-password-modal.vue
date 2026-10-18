<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInst, FormRules } from 'naive-ui';
import { changePassword } from '@/service/api/rtd/system-management/user';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';

defineOptions({ name: 'ChangePasswordModal' });

const show = defineModel<boolean>('show', { default: false });

const authStore = useAuthStore();
const formRef = ref<FormInst | null>(null);
const submitting = ref(false);
const form = reactive({
  oldPassword: '',
  newPassword: ''
});

const rules = computed<FormRules>(() => ({
  oldPassword: [{ required: true, message: $t('page.systemManagement.user.oldPasswordRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: $t('page.systemManagement.user.passwordRequired'), trigger: 'blur' },
    { min: 6, message: $t('page.systemManagement.user.passwordMin'), trigger: 'blur' }
  ]
}));

function resetForm() {
  form.oldPassword = '';
  form.newPassword = '';
  formRef.value?.restoreValidation();
}

async function handleSubmit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const { error } = await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    });
    if (error) {
      return;
    }
    window.$message?.success($t('page.systemManagement.user.changePasswordSuccess'));
    show.value = false;
    resetForm();
    await authStore.resetStore();
  } finally {
    submitting.value = false;
  }
}

function handleClose() {
  show.value = false;
}

watch(show, value => {
  if (!value) {
    resetForm();
  }
});
</script>

<template>
  <NModal
    v-model:show="show"
    preset="card"
    :title="$t('page.systemManagement.user.changePassword')"
    :style="{ width: '420px', maxWidth: '90vw' }"
  >
    <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="80">
      <NFormItem :label="$t('page.systemManagement.user.oldPassword')" path="oldPassword">
        <NInput v-model:value="form.oldPassword" type="password" show-password-on="click" />
      </NFormItem>
      <NFormItem :label="$t('page.systemManagement.user.newPassword')" path="newPassword">
        <NInput v-model:value="form.newPassword" type="password" show-password-on="click" />
      </NFormItem>
    </NForm>
    <template #footer>
      <NSpace justify="end">
        <NButton @click="handleClose">{{ $t('common.cancel') }}</NButton>
        <NButton type="primary" :loading="submitting" @click="handleSubmit">
          {{ $t('common.confirm') }}
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>
