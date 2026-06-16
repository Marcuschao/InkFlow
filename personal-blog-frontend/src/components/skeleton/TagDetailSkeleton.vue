<template>
  <div class="tag-detail-skeleton">
    <n-skeleton text style="width: 60%; margin-bottom: 16px" />
    <n-skeleton text :repeat="4" />
    <n-divider />
    <n-skeleton text style="width: 40%; margin-bottom: 12px" />
    <n-skeleton text :repeat="3" />
  </div>
</template>

<script setup>
import { NDivider, NSkeleton } from 'naive-ui';
</script>

<style scoped>
.tag-detail-skeleton {
  width: 360px;
  max-width: 100%;
}
</style>
