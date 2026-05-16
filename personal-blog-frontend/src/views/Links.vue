<template>
  <div class="links-page ds-page">
    <div class="container">
      <header class="ds-page-hero">
        <h1 class="ds-page-title ds-page-title-lg">友情链接</h1>
        <p class="ds-page-sub">值得收藏的站点</p>
      </header>
      <p v-if="err" class="state-err">{{ err }}</p>
      <div v-if="loading" class="sk-grid">
        <div v-for="n in 6" :key="'l-' + n" class="ui-skeleton sk-card" />
      </div>
      <div v-else-if="!items.length" class="ds-empty-panel">暂无友链</div>
      <ul v-else class="link-grid">
        <li v-for="l in items" :key="l.id" class="link-card">
          <a :href="l.url" class="link-a" target="_blank" rel="noopener noreferrer">
            <img v-if="l.logo" :src="l.logo" alt="" class="link-logo" loading="lazy" decoding="async" />
            <span class="link-name">{{ l.name }}</span>
          </a>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { fetchFriendLinks } from '../api/links';

const items = ref([]);
const loading = ref(true);
const err = ref('');

onMounted(async () => {
  loading.value = true;
  err.value = '';
  try {
    const res = await fetchFriendLinks();
    items.value = Array.isArray(res.data) ? res.data : [];
  } catch (e) {
    err.value = e?.message || '加载失败';
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.sk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 0.75rem;
}

.sk-card {
  height: 4.5rem;
  border-radius: var(--radius-lg);
}

.state-err {
  color: var(--color-danger);
  margin-bottom: 1rem;
}

.link-grid {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
}

.link-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  box-shadow: var(--shadow-xs);
}

.link-a {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.1rem;
  text-decoration: none;
  color: var(--color-text);
  font-weight: 650;
}

.link-a:hover {
  color: var(--color-primary);
}

.link-logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border-radius: var(--radius-sm);
}

.link-name {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}
</style>
