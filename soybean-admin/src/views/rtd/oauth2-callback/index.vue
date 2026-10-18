<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';

defineOptions({ name: 'RtdOauth2Callback' });

const BACK_TO_LOGIN_DELAY = 1500;

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { redirectAfterAuthReady } = authStore;
const errorMsg = ref('');

function failAndBackToLogin(message: string) {
  errorMsg.value = message;
  window.$message?.error(message);
  setTimeout(() => router.replace('/login'), BACK_TO_LOGIN_DELAY);
}

onMounted(async () => {
  const error = String(route.query.error || '');
  if (error) {
    failAndBackToLogin($t('page.layout.oauth2.failed', { error }));
    return;
  }

  const ticket = String(route.query.ticket || '');
  if (!ticket) {
    failAndBackToLogin($t('page.layout.oauth2.missingTicket'));
    return;
  }

  const ok = await authStore.loginByOAuth2Ticket(ticket);
  if (ok) {
    await redirectAfterAuthReady(true);
  } else {
    failAndBackToLogin($t('page.layout.oauth2.exchangeFailed'));
  }
});
</script>

<template>
  <div class="h-full flex-center">
    <NSpin v-if="!errorMsg" size="large" />
    <NResult v-else status="error" :title="errorMsg" />
  </div>
</template>
