<template>
  <div ref="rootRef" class="search-suggest">
    <n-input
      v-model:value="query"
      type="text"
      placeholder="搜索文章…"
      clearable
      @keyup.enter="goSearch"
      @focus="onFocus"
      @blur="onBlur"
    />
    <div v-if="open && suggestions.length" class="suggest-dropdown">
      <button
        v-for="item in suggestions"
        :key="item.id"
        type="button"
        class="suggest-item"
        @mousedown.prevent="pick(item)"
      >
        <span class="suggest-title" v-html="item.highlightTitle || item.title"></span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { NInput } from 'naive-ui';
import { searchSuggest } from '../api/search';

const router = useRouter();
const query = ref('');
const suggestions = ref([]);
const open = ref(false);
const rootRef = ref(null);
let debounceTimer = null;

function scheduleSuggest() {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(loadSuggest, 300);
}

async function loadSuggest() {
  const q = query.value.trim();
  if (!q) {
    suggestions.value = [];
    open.value = false;
    return;
  }
  try {
    const res = await searchSuggest({ keyword: q, limit: 5 });
    suggestions.value = res.data || [];
    open.value = suggestions.value.length > 0;
  } catch {
    suggestions.value = [];
    open.value = false;
  }
}

function goSearch() {
  const q = query.value.trim();
  if (!q) return;
  open.value = false;
  router.push({ path: '/search', query: { q } });
}

function pick(item) {
  open.value = false;
  if (item?.id) {
    router.push(`/article/${item.id}`);
  } else {
    goSearch();
  }
}

function onFocus() {
  if (suggestions.value.length) open.value = true;
}

function onBlur() {
  setTimeout(() => { open.value = false; }, 150);
}

function onDocClick(e) {
  if (rootRef.value && !rootRef.value.contains(e.target)) {
    open.value = false;
  }
}

watch(query, scheduleSuggest);

onMounted(() => document.addEventListener('click', onDocClick));
onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer);
  document.removeEventListener('click', onDocClick);
});
</script>

<style scoped>
.search-suggest {
  position: relative;
  width: 100%;
  max-width: 240px;
}
.suggest-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: var(--space-1, 4px);
  background: var(--color-surface);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  z-index: 200;
  max-height: 240px;
  overflow-y: auto;
}
.suggest-item {
  display: block;
  width: 100%;
  padding: var(--space-2, 8px) var(--space-3, 12px);
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  font-size: var(--text-sm, 14px);
  color: var(--color-text);
}
.suggest-item:hover {
  background: var(--surface-muted);
}
.suggest-title :deep(mark) {
  background: #fef08a;
  color: inherit;
  padding: 0 2px;
  border-radius: 2px;
}
</style>
