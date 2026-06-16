<template>
  <div class="article-list-skeleton">
    <n-grid cols="1 m:2 l:3" :x-gap="16" :y-gap="16" responsive="screen">
      <n-gi v-for="n in count" :key="n">
        <n-card>
          <n-skeleton v-if="showCover" height="160px" :sharp="false" style="margin-bottom: 12px" />
          <n-skeleton text style="width: 72%; margin-bottom: 8px" />
          <n-skeleton text :repeat="repeat" />
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup>
import { NCard, NGi, NGrid, NSkeleton } from 'naive-ui';

defineProps({
  count: { type: Number, default: 6 },
  repeat: { type: Number, default: 2 },
  span: { type: String, default: '24 m:12 l:8' },
  showCover: { type: Boolean, default: true },
});
</script>
