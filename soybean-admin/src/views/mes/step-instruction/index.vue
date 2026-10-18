<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type FormInst, type FormRules } from 'naive-ui';
import { getStepInstruction, saveStepInstruction, type StepInstruction } from '@/service/api/mes/step-instruction';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getStepList, type Step } from '@/service/api/mes/step';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesStepInstruction' });

const steps = ref<Step[]>([]);
const products = ref<Product[]>([]);
const stepId = ref<string | null>(null);
const productId = ref<string | null>(null);
const current = ref<StepInstruction | null>(null);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ title: '', content: '', fileId: '' });

const rules = computed<FormRules>(() => ({
  title: [{ required: true, message: $t('page.mes.instruction.required'), trigger: 'blur' }],
  content: [{ required: true, message: $t('page.mes.instruction.required'), trigger: 'blur' }]
}));
const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));

function openCreate() {
  Object.assign(form, { title: '', content: '', fileId: '' });
  showCreate.value = true;
}

async function fetchData() {
  if (!stepId.value) {
    current.value = null;
    return;
  }
  const res = await getStepInstruction({
    stepId: stepId.value,
    productId: productId.value || undefined
  });
  current.value = res.data ?? null;
}

async function submitCreate() {
  await formRef.value?.validate();
  if (!stepId.value) return;
  await saveStepInstruction({
    stepId: stepId.value,
    productId: productId.value,
    title: form.title,
    content: form.content,
    fileId: form.fileId || null
  });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(async () => {
  const [stepPage, productPage] = await Promise.all([
    getStepList({ page: 1, pageSize: 200 }),
    getProductList({ page: 1, pageSize: 200 })
  ]);
  steps.value = stepPage.data?.records ?? [];
  products.value = productPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.instruction.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="stepId" class="w-220px" :options="stepOptions" filterable :placeholder="$t('page.mes.instruction.step')" />
            <NSelect v-model:value="productId" class="w-220px" :options="productOptions" clearable filterable :placeholder="$t('page.mes.instruction.product')" />
            <NButton @click="fetchData">{{ $t('common.search') }}</NButton>
            <PermissionGuard code="mes:instruction:add">
              <NButton type="primary" :disabled="!stepId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.instruction.hint') }}</div>
      <div v-if="current">
        <div class="mb-8px font-600">{{ current.title }}</div>
        <div class="whitespace-pre-wrap">{{ current.content }}</div>
        <div v-if="current.fileId" class="mt-8px">{{ $t('page.mes.instruction.file', { id: current.fileId }) }}</div>
      </div>
      <div v-else>{{ $t('page.mes.instruction.empty') }}</div>
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.instruction.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.instruction.titleText')" path="title">
          <NInput v-model:value="form.title" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.instruction.content')" path="content">
          <NInput v-model:value="form.content" type="textarea" :rows="6" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.instruction.fileId')" path="fileId">
          <NInput v-model:value="form.fileId" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
