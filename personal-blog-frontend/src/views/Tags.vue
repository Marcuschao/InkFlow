<template>
  <div class="tags-page ds-page">
    <div class="container">
      <header class="ds-page-hero">
        <h1 class="ds-page-title ds-page-title-md">知识星系</h1>
        <p class="ds-page-sub">探索标签之间的关联网络</p>
      </header>

      <n-tabs v-model:value="activeTab" type="line" class="tags-tabs">
        <n-tab-pane name="graph" tab="图谱">
          <KnowledgeGraphSkeleton v-if="graphLoading" />
          <template v-else>
            <KnowledgeGraph
              v-if="graphData.nodes?.length"
              :graph-data="graphData"
              :focus-tag-id="focusTagId"
              @node-click="onNodeClick"
              @node-dblclick="onNodeDblclick"
            />
            <n-empty v-else description="暂无图谱数据" />
          </template>
          <div v-if="activeTab === 'graph'" class="path-explore">
            <n-input v-model:value="pathGoal" placeholder="输入学习目标，如：学会微服务" />
            <n-button type="primary" :loading="pathLoading" @click="generatePath">生成学习路径</n-button>
          </div>
        </n-tab-pane>
        <n-tab-pane name="cloud" tab="标签云">
          <n-card v-if="articleStore.tags.length" class="tag-cloud-card">
            <div class="tag-cloud-panel">
              <router-link
                v-for="(tag, idx) in articleStore.tags"
                :key="tag.id"
                :to="{ path: '/', query: { tag: tag.id } }"
                class="tag-cloud-link"
              >
                <n-tag
                  type="primary"
                  :bordered="true"
                  size="large"
                  class="tag-cloud-item"
                  :style="{ '--enter-delay': `${Math.min(idx, 14) * 52}ms` }"
                >{{ tag.name }}</n-tag>
              </router-link>
            </div>
          </n-card>
          <n-empty v-else description="暂无标签" />
        </n-tab-pane>
      </n-tabs>

      <TagDetailDrawer
        v-model:show="drawerOpen"
        :tag-id="selectedTagId"
        @select-tag="openTag"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NButton, NCard, NEmpty, NInput, NTabPane, NTabs, NTag } from 'naive-ui';
import KnowledgeGraphSkeleton from '../components/skeleton/KnowledgeGraphSkeleton.vue';
import { useArticleStore } from '../stores/article';
import { getKnowledgeGraph } from '../api/knowledge';
import KnowledgeGraph from '../components/knowledge/KnowledgeGraph.vue';
import TagDetailDrawer from '../components/knowledge/TagDetailDrawer.vue';
import request from '../utils/request';

const articleStore = useArticleStore();
const route = useRoute();
const router = useRouter();

const activeTab = ref('graph');
const graphLoading = ref(false);
const graphData = ref({ nodes: [], edges: [] });
const drawerOpen = ref(false);
const selectedTagId = ref(null);
const pathGoal = ref('');
const pathLoading = ref(false);

const focusTagId = computed(() => {
  const f = route.query.focus;
  return f ? Number(f) : null;
});

async function loadGraph() {
  graphLoading.value = true;
  try {
    const res = await getKnowledgeGraph();
    graphData.value = res.data || { nodes: [], edges: [] };
  } catch {
    graphData.value = { nodes: [], edges: [] };
  } finally {
    graphLoading.value = false;
  }
}

function onNodeClick(node) {
  if (node?.type === 'tag' && node.refId) {
    openTag(node.refId);
  }
}

function onNodeDblclick(node) {
  if (node?.type === 'tag' && node.refId) {
    router.push({ path: '/', query: { tag: node.refId } });
  } else if (node?.type === 'article' && node.refId) {
    router.push(`/article/${node.refId}`);
  }
}

function openTag(tagId) {
  selectedTagId.value = Number(tagId);
  drawerOpen.value = true;
}

async function generatePath() {
  const goal = pathGoal.value.trim();
  if (!goal) return;
  pathLoading.value = true;
  try {
    const res = await request({
      url: '/agent/learning-path',
      method: 'post',
      data: { goal },
      timeout: 120000,
    });
    router.push({ path: '/discover/learning-path', query: { goal }, state: { pathResult: res.data } });
  } catch {
    /* ignore */
  } finally {
    pathLoading.value = false;
  }
}

onMounted(() => {
  articleStore.fetchTags();
  loadGraph();
  if (focusTagId.value) openTag(focusTagId.value);
});
</script>

<style scoped>
.tags-tabs {
  max-width: 960px;
  margin: 0 auto;
}

.tag-cloud-card {
  max-width: 880px;
  margin: 0 auto;
}

.tag-cloud-panel {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-4);
}

.tag-cloud-link {
  text-decoration: none;
}

.tag-cloud-item {
  cursor: pointer;
  animation: tag-pop-in 0.55s var(--ease-out-soft) backwards;
  animation-delay: var(--enter-delay, 0ms);
  transition: transform var(--transition-fast);
}

.tag-cloud-item:hover {
  transform: translateY(-2px);
}

.path-explore {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-6);
  max-width: 560px;
}

.path-explore :deep(.n-input) {
  flex: 1 1 200px;
}
</style>
