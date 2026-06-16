<template>
  <n-space vertical :size="16">
    <n-space v-for="n in count" :key="n" align="start">
      <n-skeleton circle size="medium" />
      <n-skeleton text :repeat="3" style="flex: 1" />
    </n-space>
  </n-space>
</template>

<script setup>
import { NSkeleton, NSpace } from 'naive-ui';

defineProps({ count: { type: Number, default: 4 } });
</script>
