<template>
  <div class="landscape-panel">
    <n-spin :show="loading">
      <n-empty v-if="!nodes.length" description="暂无知识版图数据" />
      <div v-else class="landscape-cloud">
        <router-link
          v-for="n in nodes"
          :key="n.tagId"
          :to="{ path: '/tags', query: { focus: n.tagId } }"
          class="landscape-tag"
          :style="{ '--w': tagScale(n.weight) }"
        >
          <n-tag type="primary" :bordered="false">{{ n.name }}</n-tag>
          <span class="landscape-src">{{ sourceLabel(n.source) }}</span>
        </router-link>
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { NEmpty, NSpin, NTag } from 'naive-ui';
import { getUserLandscape } from '../../api/knowledge';
import { useReadingHistory } from '../../composables/useReadingHistory';

const props = defineProps({
  userId: { type: Number, required: true },
});

const { getRecentArticleIds } = useReadingHistory();
const loading = ref(false);
const nodes = ref([]);

function tagScale(weight) {
  const w = Number(weight) || 1;
  return Math.min(2, 0.8 + w * 0.08);
}

function sourceLabel(s) {
  if (s === 'write') return '创作';
  if (s === 'favorite') return '收藏';
  if (s === 'read') return '阅读';
  return '';
}

async function load() {
  if (!props.userId) return;
  loading.value = true;
  try {
    const res = await getUserLandscape(props.userId, getRecentArticleIds(20));
    nodes.value = res.data?.nodes || [];
  } catch {
    nodes.value = [];
  } finally {
    loading.value = false;
  }
}

watch(() => props.userId, load);
onMounted(load);
</script>

<style scoped>
.landscape-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.landscape-tag {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  text-decoration: none;
  transform: scale(var(--w, 1));
}

.landscape-src {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}
</style>
