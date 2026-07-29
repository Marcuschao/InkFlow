<template>
  <div class="editorial-shell" :class="{ 'editorial-shell--wide': wide }">
    <slot />
  </div>
</template>

<script setup>
defineProps({ wide: { type: Boolean, default: false } });
</script>

<style scoped>
.editorial-shell {
  width: min(100% - 32px, 1180px);
  margin: 0 auto;
  padding: clamp(28px, 5vw, 72px) 0 96px;
}
.editorial-shell--wide { width: min(100% - 32px, 1320px); }
@media (max-width: 640px) { .editorial-shell { width: min(100% - 24px, 1180px); padding-bottom: 72px; } }
</style>
