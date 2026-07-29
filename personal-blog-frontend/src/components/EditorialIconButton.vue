<template><button class="editorial-icon-button" :aria-label="label" :title="label" type="button"><slot /></button></template>
<script setup>defineProps({ label: { type: String, required: true } });</script>
<style scoped>
.editorial-icon-button { width:44px; height:44px; display:inline-grid; place-items:center; border:1px solid var(--color-border); border-radius:50%; color:var(--color-text); background:transparent; cursor:pointer; transition:transform .25s ease,background .25s ease,border-color .25s ease; }
.editorial-icon-button:hover { transform:translateY(-3px); background:var(--surface-primary-tint); border-color:var(--color-accent); }
.editorial-icon-button:focus-visible { outline:2px solid var(--color-accent); outline-offset:3px; }
</style>
