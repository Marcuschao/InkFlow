<template>
  <div class="user-profile-skeleton">
    <n-space align="center" :size="16" style="margin-bottom: 24px">
      <n-skeleton circle size="large" />
      <n-skeleton text style="width: 140px" />
    </n-space>
    <n-grid :cols="3" :x-gap="16">
      <n-gi v-for="n in 3" :key="n">
        <n-skeleton text :repeat="2" />
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup>
import { NGi, NGrid, NSkeleton, NSpace } from 'naive-ui';
</script>
