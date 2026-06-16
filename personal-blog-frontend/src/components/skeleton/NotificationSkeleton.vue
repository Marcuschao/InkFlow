<template>
  <n-space vertical :size="12">
    <div v-for="n in count" :key="n" class="notif-row">
      <n-skeleton circle size="small" />
      <n-skeleton text :repeat="2" style="flex: 1" />
    </div>
  </n-space>
</template>

<script setup>
import { NSkeleton, NSpace } from 'naive-ui';

defineProps({ count: { type: Number, default: 6 } });
</script>

<style scoped>
.notif-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: 56px;
}
</style>
