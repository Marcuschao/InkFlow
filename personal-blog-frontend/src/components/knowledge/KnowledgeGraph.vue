<template>
  <div class="kg-graph-wrap">
    <div v-if="isMobile" class="kg-mobile-list">
      <n-input v-model:value="filter" placeholder="搜索标签…" clearable class="kg-filter" />
      <div class="kg-relations">
        <button
          v-for="node in filteredNodes"
          :key="node.id"
          type="button"
          class="kg-relation"
          @click="emit('node-click', node)"
          @dblclick="emit('node-dblclick', node)"
        >
          <span class="kg-relation-type">{{ nodeTypeLabel(node.type) }}</span>
          <span class="kg-relation-name">{{ node.label }}</span>
          <span v-if="node.articleCount" class="kg-relation-count">{{ node.articleCount }}</span>
        </button>
      </div>
    </div>
    <template v-else>
      <p class="kg-hover-label" aria-live="polite">{{ hoveredLabel || '悬停节点查看完整名称' }}</p>
      <div ref="containerRef" class="kg-canvas" />
    </template>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, computed, nextTick } from 'vue';
import { NInput } from 'naive-ui';
import { useTheme } from '../../composables/useTheme';

const props = defineProps({
  graphData: { type: Object, default: () => ({ nodes: [], edges: [] }) },
  focusTagId: { type: [Number, String], default: null },
});

const emit = defineEmits(['node-click', 'node-dblclick']);
const { isDark } = useTheme();

const containerRef = ref(null);
const filter = ref('');
const hoveredLabel = ref('');
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

function nodeTypeLabel(type) {
  return { article: '文章', tag: '标签', author: '作者' }[type] || '关联';
}

function syncMobile() {
  isMobile.value = window.matchMedia('(max-width: 767px)').matches;
}

function nodeSize(node) {
  const w = node.weight || 0.3;
  return 24 + Math.round(w * 28);
}

function truncateLabel(label, type) {
  const text = String(label || '未命名');
  const limit = type === 'article' ? 18 : type === 'author' ? 8 : 10;
  return text.length > limit ? `${text.slice(0, limit)}…` : text;
}

function toG6Data(data, dark) {
  const palette = graphPalette(dark);
  let nodes = [...(data?.nodes || [])]
    .sort((a, b) => Number(b.weight || b.articleCount || 0) - Number(a.weight || a.articleCount || 0))
    .slice(0, 12)
    .map((n) => ({
    id: n.id,
    data: { ...n, fullLabel: n.label },
    style: {
      labelText: truncateLabel(n.label, n.type),
      size: nodeSize(n),
      fill: palette.nodeFill[n.type] || palette.nodeFill.tag,
      stroke: palette.nodeStroke,
      shadowColor: 'transparent',
      shadowBlur: 0,
    },
  }));
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
  const height = Math.max(340, Math.min(400, width * 0.48));
  const palette = graphPalette(isDark.value);
  graph = new Graph({
    container: containerRef.value,
    width,
    height,
    autoFit: 'view',
    layout: {
      type: 'd3-force',
      preventOverlap: true,
      link: { distance: 118, strength: 0.7 },
      manyBody: { strength: -420 },
      collide: { radius: 56, strength: 0.9 },
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
  graph.on('node:pointerenter', (evt) => {
    const model = evt.target?.id ? graph.getNodeData(evt.target.id) : null;
    hoveredLabel.value = model?.data?.fullLabel || model?.data?.label || '';
  });
  graph.on('node:pointerleave', () => {
    hoveredLabel.value = '';
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

watch(isMobile, async (mobile) => {
  if (mobile) {
    graph?.destroy();
    graph = null;
    return;
  }
  await nextTick();
  renderGraph();
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
  min-height: 340px;
  background: transparent;
  border: 0;
  overflow: hidden;
}

.kg-mobile-list {
  padding: 0;
}

.kg-filter {
  margin-bottom: var(--space-3);
}

.kg-hover-label {
  min-height: 24px;
  margin: 0;
  color: var(--color-text-muted);
  font: 11px/1.5 var(--font-mono);
  letter-spacing: .04em;
}

.kg-relations {
  border-top: 1px solid var(--color-border);
}

.kg-relation {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 13px 0;
  border: 0;
  border-bottom: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text);
  text-align: left;
  cursor: pointer;
}

.kg-relation-type,
.kg-relation-count {
  color: var(--color-text-soft);
  font: 10px var(--font-mono);
}

.kg-relation-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font: 15px/1.4 var(--font-prose);
}

.kg-relation:hover .kg-relation-name {
  color: var(--color-accent-text, var(--color-accent));
}
</style>
