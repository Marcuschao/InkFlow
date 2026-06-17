<template>
  <div class="kg-graph-wrap">
    <div v-if="isMobile" class="kg-mobile-list">
      <n-input v-model:value="filter" placeholder="搜索标签…" clearable class="kg-filter" />
      <n-space vertical :size="8">
        <n-tag
          v-for="node in filteredNodes"
          :key="node.id"
          type="primary"
          checkable
          @click="emit('node-click', node)"
          @dblclick="emit('node-dblclick', node)"
        >{{ node.label }} ({{ node.articleCount || 0 }})</n-tag>
      </n-space>
    </div>
    <div v-else ref="containerRef" class="kg-canvas" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue';
import { NInput, NSpace, NTag } from 'naive-ui';
import { useTheme } from '../../composables/useTheme';

const props = defineProps({
  graphData: { type: Object, default: () => ({ nodes: [], edges: [] }) },
  focusTagId: { type: [Number, String], default: null },
});

const emit = defineEmits(['node-click', 'node-dblclick']);
const { isDark } = useTheme();

const containerRef = ref(null);
const filter = ref('');
const isMobile = ref(false);
let graph = null;

function graphPalette(dark) {
  if (dark) {
    return {
      nodeFill: {
        article: 'rgba(255, 255, 255, 0.08)',
        author: 'rgba(255, 255, 255, 0.06)',
        tag: 'rgba(255, 255, 255, 0.1)',
      },
      nodeStroke: 'rgba(255, 255, 255, 0.18)',
      nodeActiveStroke: 'rgba(255, 255, 255, 0.4)',
      labelFill: '#a1a1aa',
      edgeStroke: 'rgba(255, 255, 255, 0.12)',
    };
  }
  return {
    nodeFill: {
      article: 'rgba(24, 24, 27, 0.06)',
      author: 'rgba(24, 24, 27, 0.08)',
      tag: 'rgba(24, 24, 27, 0.12)',
    },
    nodeStroke: 'rgba(24, 24, 27, 0.22)',
    nodeActiveStroke: 'rgba(24, 24, 27, 0.45)',
    labelFill: '#52525b',
    edgeStroke: 'rgba(24, 24, 27, 0.14)',
  };
}

const filteredNodes = computed(() => {
  const nodes = props.graphData?.nodes || [];
  const q = filter.value.trim().toLowerCase();
  if (!q) return nodes.slice(0, 100);
  return nodes.filter((n) => (n.label || '').toLowerCase().includes(q)).slice(0, 100);
});

function syncMobile() {
  isMobile.value = window.matchMedia('(max-width: 767px)').matches;
}

function nodeSize(node) {
  const w = node.weight || 0.3;
  return 24 + Math.round(w * 28);
}

function toG6Data(data, dark) {
  const palette = graphPalette(dark);
  let nodes = (data?.nodes || []).map((n) => ({
    id: n.id,
    data: { ...n },
    style: {
      labelText: n.label,
      size: nodeSize(n),
      fill: palette.nodeFill[n.type] || palette.nodeFill.tag,
      stroke: palette.nodeStroke,
      shadowColor: 'transparent',
      shadowBlur: 0,
    },
  }));
  if (nodes.length > 100) {
    nodes = [...nodes].sort((a, b) => (b.data.weight || 0) - (a.data.weight || 0)).slice(0, 100);
  }
  const ids = new Set(nodes.map((n) => n.id));
  const edges = (data?.edges || [])
    .filter((e) => ids.has(e.source) && ids.has(e.target))
    .map((e) => ({
      id: `${e.source}-${e.target}`,
      source: e.source,
      target: e.target,
      data: e,
      style: {
        lineWidth: Math.max(1, Math.min(4, (e.weight || 1) / 3)),
        stroke: palette.edgeStroke,
        lineDash: [2, 4],
      },
    }));
  return { nodes, edges };
}

async function renderGraph() {
  if (isMobile.value || !containerRef.value) return;
  const { Graph } = await import('@antv/g6');
  if (graph) {
    graph.destroy();
    graph = null;
  }
  const width = containerRef.value.clientWidth || 800;
  const height = Math.max(420, Math.min(640, width * 0.65));
  const palette = graphPalette(isDark.value);
  graph = new Graph({
    container: containerRef.value,
    width,
    height,
    autoFit: 'view',
    layout: {
      type: 'd3-force',
      preventOverlap: true,
      link: { distance: 80 },
    },
    node: {
      style: {
        labelFill: palette.labelFill,
        labelFontSize: 12,
      },
      state: {
        active: {
          stroke: palette.nodeActiveStroke,
          shadowBlur: 0,
        },
      },
    },
    behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
  });
  graph.setData(toG6Data(props.graphData, isDark.value));
  graph.render();
  graph.on('node:click', (evt) => {
    const model = evt.target?.id ? graph.getNodeData(evt.target.id) : null;
    if (model?.data) emit('node-click', model.data);
  });
  graph.on('node:dblclick', (evt) => {
    const model = evt.target?.id ? graph.getNodeData(evt.target.id) : null;
    if (model?.data) emit('node-dblclick', model.data);
  });
}

watch(
  () => props.graphData,
  () => {
    if (!isMobile.value) renderGraph();
  },
  { deep: true }
);

watch(isDark, () => {
  if (!isMobile.value) renderGraph();
});

onMounted(() => {
  syncMobile();
  window.addEventListener('resize', syncMobile);
  if (!isMobile.value) renderGraph();
});

onUnmounted(() => {
  window.removeEventListener('resize', syncMobile);
  if (graph) graph.destroy();
});
</script>

<style scoped>
.kg-graph-wrap {
  width: 100%;
}

.kg-canvas {
  width: 100%;
  min-height: 420px;
  border-radius: var(--radius-brutal-card);
  background: var(--color-surface);
  border: var(--border-brutal);
  box-shadow: var(--shadow-brutal-lg);
}

.kg-mobile-list {
  padding: var(--space-2);
}

.kg-filter {
  margin-bottom: var(--space-3);
}
</style>
