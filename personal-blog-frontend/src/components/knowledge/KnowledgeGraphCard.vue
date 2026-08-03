<template>
  <div class="kg-card">
    <header class="kg-card-header">
      <p>§ KNOWLEDGE</p>
      <h2>知识星系</h2>
    </header>
    <KnowledgeGraphSkeleton v-if="loading" />
    <KnowledgeGraph
      v-else-if="graphData.nodes?.length"
      :graph-data="graphData"
      @node-click="onNodeClick"
      @node-dblclick="onNodeDblclick"
    />
    <n-empty v-else description="暂无关联" size="small" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { NEmpty } from 'naive-ui';
import { getKnowledgeSubgraph } from '../../api/knowledge';
import KnowledgeGraph from './KnowledgeGraph.vue';
import KnowledgeGraphSkeleton from '../skeleton/KnowledgeGraphSkeleton.vue';

const props = defineProps({
  articleId: { type: [Number, String], required: true },
});

const router = useRouter();
const loading = ref(false);
const graphData = ref({ nodes: [], edges: [] });

async function load() {
  if (!props.articleId) return;
  loading.value = true;
  try {
    const res = await getKnowledgeSubgraph(props.articleId);
    graphData.value = res.data || { nodes: [], edges: [] };
  } catch {
    graphData.value = { nodes: [], edges: [] };
  } finally {
    loading.value = false;
  }
}

function onNodeClick(node) {
  if (node?.type === 'tag' && node.refId) {
    router.push({ path: '/tags', query: { focus: node.refId } });
  }
}

function onNodeDblclick(node) {
  onNodeClick(node);
}

watch(() => props.articleId, load);
onMounted(load);
</script>

<style scoped>
.kg-card {
  width: 100%;
}

.kg-card-header {
  margin-bottom: var(--space-5);
}

.kg-card-header p {
  margin: 0 0 9px;
  color: var(--color-accent-text, var(--color-accent));
  font: 10px/1 var(--font-mono);
  letter-spacing: .16em;
}

.kg-card-header h2 {
  margin: 0;
  color: var(--color-text);
  font: 600 clamp(24px, 3vw, 32px)/1.2 var(--font-display);
  letter-spacing: 0;
}

.kg-card :deep(.kg-canvas) {
  min-height: 360px;
}
</style>
