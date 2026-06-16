<template>
  <n-card title="热门知识节点">
    <n-space v-if="loading" vertical :size="12">
      <n-skeleton height="240px" :sharp="false" />
    </n-space>
    <n-empty v-else-if="!items.length" description="暂无数据" size="small" />
    <template v-else>
        <div
          ref="sphereRef"
          class="tag-sphere"
          aria-label="热门知识节点"
          @pointerdown="onSphereDown"
          @pointermove="onSphereMove"
          @pointerup="onSphereUp"
          @pointercancel="onSphereUp"
        >
          <a
            v-for="item in items"
            :key="item.tagId"
            ref="tagRefs"
            class="tag-sphere-item"
            href="#"
            @click.prevent="goTag(item.tagId)"
          >
            {{ item.name }}
          </a>
        </div>
        <div class="tag-sphere-fallback" aria-label="热门知识节点">
          <router-link
            v-for="item in items"
            :key="item.tagId"
            :to="{ path: '/tags', query: { focus: item.tagId } }"
            class="fallback-tag"
          >
            {{ item.name }}
          </router-link>
        </div>
    </template>
  </n-card>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { NCard, NEmpty, NSkeleton, NSpace } from 'naive-ui';
import { getHotTags } from '../../api/knowledge';

const TAG_COLORS = ['#1E6FFF', '#0050E6', '#00B4FF', '#4E5969', '#86909C', '#00B42A'];
const DEFAULT_VEL_X = 0.0006;
const DEFAULT_VEL_Y = 0.0012;
const DRAG_THRESHOLD = 5;

const router = useRouter();
const loading = ref(false);
const items = ref([]);
const sphereRef = ref(null);
const tagRefs = ref([]);

let nodes = [];
let angleX = 0.22;
let angleY = 0;
let velX = DEFAULT_VEL_X;
let velY = DEFAULT_VEL_Y;
let dragging = false;
let moved = false;
let downX = 0;
let downY = 0;
let lastX = 0;
let lastY = 0;
let rafId = 0;
let scoreMin = 0;
let scoreMax = 1;
let reducedMotion = false;
let motionQuery;

function goTag(tagId) {
  router.push({ path: '/tags', query: { focus: tagId } });
}

function buildNodes(list) {
  const n = list.length;
  const golden = Math.PI * (3 - Math.sqrt(5));
  const scores = list.map((t) => t.score || 0);
  scoreMin = Math.min(...scores, 0);
  scoreMax = Math.max(...scores, 1);

  nodes = list.map((item, i) => {
    const t = n > 1 ? i / (n - 1) : 0.5;
    const y = 1 - t * 2;
    const r = Math.sqrt(Math.max(0, 1 - y * y));
    const theta = golden * i;
    return {
      tagId: item.tagId,
      score: item.score || 0,
      x: Math.cos(theta) * r,
      y,
      z: Math.sin(theta) * r,
      color: TAG_COLORS[i % TAG_COLORS.length],
    };
  });
}

function scoreRatio(score) {
  if (scoreMax === scoreMin) return 0.6;
  return (score - scoreMin) / (scoreMax - scoreMin);
}

function rotateY(p, a) {
  const c = Math.cos(a);
  const s = Math.sin(a);
  return { x: p.x * c + p.z * s, y: p.y, z: -p.x * s + p.z * c };
}

function rotateX(p, a) {
  const c = Math.cos(a);
  const s = Math.sin(a);
  return { x: p.x, y: p.y * c - p.z * s, z: p.y * s + p.z * c };
}

function applyLayout() {
  const el = sphereRef.value;
  const els = tagRefs.value;
  if (!el || !nodes.length || !els.length) return;

  const w = el.clientWidth;
  const h = el.clientHeight;
  const cx = w / 2;
  const cy = h / 2;
  const baseR = Math.min(w, h) * 0.42;

  nodes.forEach((node, i) => {
    const dom = els[i];
    if (!dom) return;

    let p = rotateY(node, angleY);
    p = rotateX(p, angleX);
    const depth = (p.z + 1) / 2;
    const persp = 300 / (300 - p.z * 110);
    const sx = cx + p.x * baseR * persp;
    const sy = cy + p.y * baseR * persp;
    const hot = scoreRatio(node.score);
    const fontSize = (11 + hot * 8) * (0.5 + depth * 0.75);
    const opacity = 0.28 + depth * 0.72;

    dom.style.left = `${sx}px`;
    dom.style.top = `${sy}px`;
    dom.style.fontSize = `${fontSize}px`;
    dom.style.opacity = String(opacity);
    dom.style.color = node.color;
    dom.style.zIndex = String(Math.round(depth * 100));
    dom.style.fontWeight = depth > 0.55 ? '600' : '500';
    dom.style.pointerEvents = depth > 0.4 ? 'auto' : 'none';
  });
}

function tick() {
  if (!dragging && !reducedMotion) {
    angleY += velY;
    angleX += velX;
  }
  applyLayout();
  rafId = requestAnimationFrame(tick);
}

function isTagTarget(target) {
  return target?.closest?.('.tag-sphere-item');
}

function onSphereDown(e) {
  if (reducedMotion || isTagTarget(e.target)) return;
  dragging = true;
  moved = false;
  downX = e.clientX;
  downY = e.clientY;
  lastX = e.clientX;
  lastY = e.clientY;
}

function onSphereMove(e) {
  if (!dragging || reducedMotion) return;
  const dx = e.clientX - lastX;
  const dy = e.clientY - lastY;
  if (Math.abs(e.clientX - downX) > DRAG_THRESHOLD || Math.abs(e.clientY - downY) > DRAG_THRESHOLD) {
    moved = true;
  }
  angleY += dx * 0.004;
  angleX += dy * 0.004;
  velY = dx * 0.0008;
  velX = dy * 0.0008;
  lastX = e.clientX;
  lastY = e.clientY;
}

function onSphereUp() {
  dragging = false;
  if (!moved) {
    velX = DEFAULT_VEL_X;
    velY = DEFAULT_VEL_Y;
  }
}

function onMotionChange(e) {
  reducedMotion = e.matches;
  if (reducedMotion && rafId) {
    cancelAnimationFrame(rafId);
    rafId = 0;
  } else if (!reducedMotion && !rafId && nodes.length) {
    rafId = requestAnimationFrame(tick);
  }
}

onMounted(async () => {
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
  reducedMotion = motionQuery.matches;
  motionQuery.addEventListener('change', onMotionChange);

  loading.value = true;
  try {
    const res = await getHotTags(20);
    items.value = res.data || [];
    buildNodes(items.value);
    await nextTick();
    applyLayout();
    if (!reducedMotion && items.value.length) {
      rafId = requestAnimationFrame(tick);
    }
  } catch {
    items.value = [];
  } finally {
    loading.value = false;
  }
});

onUnmounted(() => {
  if (rafId) cancelAnimationFrame(rafId);
  motionQuery?.removeEventListener('change', onMotionChange);
});
</script>

<style scoped>
.tag-sphere {
  position: relative;
  height: 240px;
  overflow: hidden;
  cursor: grab;
  user-select: none;
}

.tag-sphere:active {
  cursor: grabbing;
}

.tag-sphere-item {
  position: absolute;
  transform: translate(-50%, -50%);
  white-space: nowrap;
  text-decoration: none;
  line-height: 1.2;
  cursor: pointer;
}

.tag-sphere-item:hover,
.tag-sphere-item:focus-visible {
  opacity: 1 !important;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.tag-sphere-fallback {
  display: none;
  flex-wrap: wrap;
  gap: var(--space-2);
  padding-top: var(--space-2);
}

.fallback-tag {
  display: inline-block;
  padding: var(--space-1) var(--space-3);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  background: var(--surface-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  text-decoration: none;
  transition: color var(--transition-fast), border-color var(--transition-fast);
}

.fallback-tag:hover,
.fallback-tag:focus-visible {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

@media (prefers-reduced-motion: reduce) {
  .tag-sphere {
    display: none;
  }

  .tag-sphere-fallback {
    display: flex;
  }
}
</style>
