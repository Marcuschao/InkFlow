<template>
  <div class="article-detail-skeleton">
    <n-skeleton text style="width: 68%; height: 32px; margin-bottom: 16px" />
    <n-space :size="12" style="margin-bottom: 24px">
      <n-skeleton circle size="small" />
      <n-skeleton text style="width: 120px" />
      <n-skeleton text style="width: 80px" />
    </n-space>
    <n-skeleton text :repeat="8" />
    <n-divider />
    <n-skeleton text style="width: 100px; margin-bottom: 12px" />
    <n-space vertical :size="16">
      <n-space v-for="n in 3" :key="n" align="start">
        <n-skeleton circle size="medium" />
        <n-skeleton text :repeat="2" style="flex: 1" />
      </n-space>
    </n-space>
  </div>
</template>

<script setup>
import { NDivider, NSkeleton, NSpace } from 'naive-ui';
</script>
