<template>
  <div class="list-skeleton">
    <n-grid :cols="1" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <n-gi v-for="n in count" :key="n" :span="span">
        <n-card><n-skeleton text :repeat="repeat" /></n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup>
import { NCard, NGi, NGrid, NSkeleton } from 'naive-ui';

defineProps({
  count: { type: Number, default: 6 },
  repeat: { type: Number, default: 3 },
  span: { type: String, default: '24 m:12 l:8' },
});
</script>
