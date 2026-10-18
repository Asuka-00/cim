<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { useBrandStore } from '@/store/modules/brand';
import { useNaiveForm } from '@/hooks/common/form';
import { buildOAuth2AuthorizeUrl, fetchOAuth2Providers, fetchTenantDropdown } from '@/service/api/auth';
import { localStg } from '@/utils/storage';
import { $t } from '@/locales';

defineOptions({
  name: 'PwdLogin'
});

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const brandStore = useBrandStore();
const { formRef, validate } = useNaiveForm();

interface FormModel {
  tenantId: string;
  userName: string;
  password: string;
}

const model: FormModel = reactive({
  tenantId: localStg.get('loginTenantId') || '',
  userName: '',
  password: ''
});

const tenants = ref<Api.Auth.TenantSelect[]>([]);
const providers = ref<Api.Auth.OAuth2Provider[]>([]);

const rules = computed(() => ({
  tenantId: [{ required: true, message: $t('page.login.tenantPlaceholder'), trigger: ['blur', 'change'] }],
  userName: [{ required: true, message: $t('page.login.userNamePlaceholder'), trigger: 'blur' }],
  password: [{ required: true, message: $t('page.login.passwordPlaceholder'), trigger: 'blur' }]
}));

async function loadTenants() {
  const { data, error } = await fetchTenantDropdown();
  if (!error && data?.length) {
    tenants.value = data;
    if (!model.tenantId) {
      model.tenantId = data[0].id;
    }
  }
}

async function loadProviders() {
  const { data, error } = await fetchOAuth2Providers();
  if (!error && data) {
    providers.value = data;
  }
}

watch(
  () => model.tenantId,
  id => {
    if (id) {
      localStg.set('loginTenantId', id);
      brandStore.loadBrand(id);
    }
  }
);

onMounted(async () => {
  const oauthError = String(route.query.error ?? '');
  if (oauthError) {
    window.$message?.error($t('page.login.oauth2Failed', { error: oauthError }));
    router.replace({ path: '/login', query: {} });
  }
  await loadTenants();
  await loadProviders();
  if (model.tenantId) await brandStore.loadBrand(model.tenantId);
});

async function handleSubmit() {
  await validate();
  await authStore.login(model.userName, model.password, model.tenantId);
}

function handleOAuth2(registrationId: string) {
  if (!model.tenantId) {
    window.$message?.warning($t('page.login.tenantRequired'));
    return;
  }
  window.location.href = buildOAuth2AuthorizeUrl(registrationId, model.tenantId);
}
</script>

<template>
  <NForm ref="formRef" :model="model" :rules="rules" size="large" :show-label="false" @keyup.enter="handleSubmit">
    <NFormItem path="tenantId">
      <NSelect
        v-model:value="model.tenantId"
        :options="tenants.map(t => ({ label: `${t.tenantId} - ${t.tenantName}`, value: t.id }))"
        :placeholder="$t('page.login.tenantPlaceholder')"
      />
    </NFormItem>
    <NFormItem path="userName">
      <NInput v-model:value="model.userName" :placeholder="$t('page.login.userNamePlaceholder')">
        <template #prefix>
          <SvgIcon icon="mdi:account-outline" class="text-16px text-[var(--n-text-color-3)]" />
        </template>
      </NInput>
    </NFormItem>
    <NFormItem path="password">
      <NInput
        v-model:value="model.password"
        type="password"
        show-password-on="click"
        :placeholder="$t('page.login.passwordPlaceholder')"
      >
        <template #prefix>
          <SvgIcon icon="mdi:lock-outline" class="text-16px text-[var(--n-text-color-3)]" />
        </template>
      </NInput>
    </NFormItem>
    <NButton
      type="primary"
      size="large"
      block
      class="mt-4px h-44px text-16px"
      :loading="authStore.loginLoading"
      @click="handleSubmit"
    >
      {{ $t('page.login.login') }}
    </NButton>
    <template v-if="providers.length">
      <NDivider class="text-12px text-[var(--n-text-color-3)] !mt-20px !mb-16px">
        {{ $t('page.login.oauth2') }}
      </NDivider>
      <div class="flex-center flex-wrap gap-12px">
        <NButton v-for="p in providers" :key="p.registrationId" quaternary @click="handleOAuth2(p.registrationId)">
          {{ p.displayName || p.registrationId }}
        </NButton>
      </div>
    </template>
  </NForm>
</template>
