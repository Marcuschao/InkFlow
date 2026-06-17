<template>
  <div class="badges-page ds-page container">
    <h1 class="page-title">徽章墙</h1>
    <n-skeleton v-if="loading" height="320px" :sharp="false" />
    <n-grid v-else :cols="gridCols" :x-gap="16" :y-gap="16">
      <n-gi v-for="b in badges" :key="b.id">
        <n-card class="badge-card" :class="{ earned: b.earned, locked: !b.earned }" size="small">
          <div class="badge-card-inner">
            <img v-if="b.iconUrl" :src="b.iconUrl" :alt="b.name" class="badge-lg" />
            <span v-else class="badge-lg-fallback">{{ b.name?.charAt(0) }}</span>
            <h3 class="badge-card-name">{{ b.name }}</h3>
            <p class="badge-card-desc muted">{{ b.description }}</p>
            <n-tag v-if="b.earned" size="small" type="success">已获得</n-tag>
            <n-tag v-else size="small">未获得</n-tag>
          </div>
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useHead } from '@vueuse/head';
import { NCard, NGi, NGrid, NSkeleton, NTag } from 'naive-ui';
import { getBadges } from '../api/social';

useHead({ title: '徽章墙' });

const loading = ref(true);
const badges = ref([]);

const gridCols = computed(() => (window.innerWidth < 768 ? 2 : 4));

onMounted(async () => {
  try {
    const res = await getBadges();
    badges.value = res.data?.badges || [];
  } catch {
    badges.value = [];
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.badges-page {
  padding-bottom: var(--space-16);
}

.page-title {
  margin: 0 0 var(--space-6);
  font-size: var(--text-2xl);
  font-weight: var(--weight-semibold);
}

.badge-card {
  border: 1px solid var(--color-border);
  box-shadow: none;
  text-align: center;
}

.badge-card.locked {
  opacity: 0.55;
}

.badge-card-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.badge-lg {
  width: 48px;
  height: 48px;
}

.badge-lg-fallback {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary-soft);
  border-radius: var(--radius-md);
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
}

.badge-card-name {
  margin: 0;
  font-size: var(--text-base);
}

.badge-card-desc {
  margin: 0;
  font-size: var(--text-sm);
}

.muted {
  color: var(--color-text-muted);
}
</style>
