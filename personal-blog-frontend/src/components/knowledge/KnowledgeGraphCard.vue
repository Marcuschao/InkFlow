<template>
  <n-card title="知识星系" size="small" class="kg-card ds-surface-card">
    <KnowledgeGraphSkeleton v-if="loading" />
    <KnowledgeGraph
      v-else-if="graphData.nodes?.length"
      :graph-data="graphData"
      @node-click="onNodeClick"
      @node-dblclick="onNodeDblclick"
    />
    <n-empty v-else description="暂无关联" size="small" />
  </n-card>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { NCard, NEmpty } from 'naive-ui';
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
.kg-card :deep(.kg-canvas) {
  min-height: 280px;
}
</style>
