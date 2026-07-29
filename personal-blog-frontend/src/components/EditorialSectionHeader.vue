<template>
  <header class="editorial-section-header">
    <div class="editorial-kicker"><span>{{ index }}</span><i></i><span>{{ eyebrow }}</span></div>
    <div class="editorial-heading-row">
      <h2>{{ title }}</h2>
      <slot name="action" />
    </div>
    <p v-if="description">{{ description }}</p>
  </header>
</template>
<script setup>
defineProps({ index: { type: String, default: '01' }, eyebrow: { type: String, default: 'SECTION' }, title: { type: String, required: true }, description: { type: String, default: '' } });
</script>
<style scoped>
.editorial-section-header { margin-bottom: 24px; }
.editorial-kicker { display:flex; align-items:center; gap:10px; color:var(--color-text-soft); font:600 11px/1 var(--font-mono); letter-spacing:.12em; text-transform:uppercase; }
.editorial-kicker i { display:block; width:34px; height:1px; background:var(--color-accent); }
.editorial-heading-row { display:flex; align-items:baseline; justify-content:space-between; gap:20px; margin-top:12px; border-bottom:1px solid var(--color-border); padding-bottom:14px; }
.editorial-heading-row h2 { margin:0; color:var(--color-text); font-size:clamp(26px,4vw,48px); line-height:1; letter-spacing:-.045em; font-weight:700; }
.editorial-section-header p { margin:12px 0 0; color:var(--color-text-muted); max-width:58ch; font-size:14px; }
</style>
