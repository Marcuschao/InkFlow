<template>
  <div class="kg-skeleton">
    <n-skeleton height="240px" :sharp="false" />
    <n-space justify="center" :size="8" style="margin-top: 12px">
      <n-skeleton v-for="n in 6" :key="n" circle size="small" />
    </n-space>
  </div>
</template>

<script setup>
import { NSkeleton, NSpace } from 'naive-ui';
</script>
