<script setup lang="ts">
import { useAuth } from '@/hooks/business/auth';

defineOptions({ name: 'PermissionGuard' });

/** Render the slot only when the current user owns the given permission code(s) */
defineProps<{ code: string | string[] }>();

const { hasAuth } = useAuth();
</script>

<template>
  <slot v-if="hasAuth(code)" />
</template>
