<template>
  <picture class="smart-image" :class="{ loaded: isLoaded, 'has-placeholder': showPlaceholder }">
    <source v-if="webpSrcset" type="image/webp" :srcset="webpSrcset" :sizes="resolvedSizes" />
    <img
      ref="imgRef"
      :src="displaySrc"
      :srcset="fallbackSrcset"
      :sizes="resolvedSizes"
      :alt="alt"
      :loading="lazy ? 'lazy' : 'eager'"
      :fetchpriority="priority"
      :decoding="asyncDecode ? 'async' : 'auto'"
      @load="onLoad"
      @error="onError"
    />
  </picture>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue';
import { resolveMediaUrl, resolveVariantUrl, pickVariantForWidth } from '~/utils/mediaUrl';

const props = defineProps({
  src: { type: [String, Object], default: '' },
  variants: { type: Object, default: null },
  width: { type: Number, default: 600 },
  alt: { type: String, default: '' },
  lazy: { type: Boolean, default: true },
  priority: { type: String, default: 'auto' },
  asyncDecode: { type: Boolean, default: true },
  sizes: { type: String, default: '' },
});

const imgRef = ref(null);
const isLoaded = ref(false);
const inView = ref(!props.lazy);
const observer = ref(null);

const normalized = computed(() => {
  if (props.variants) return props.variants;
  if (props.src && typeof props.src === 'object') return props.src;
  const url = typeof props.src === 'string' ? props.src : '';
  return {
    original: url,
    webp200: resolveVariantUrl(url, 200),
    webp600: resolveVariantUrl(url, 600),
    webp1200: resolveVariantUrl(url, 1200),
  };
});

const displaySrc = computed(() => {
  const v = normalized.value;
  const picked = pickVariantForWidth(v, props.width);
  return resolveMediaUrl(picked || v.original || '');
});

const webpSrcset = computed(() => {
  const v = normalized.value;
  const parts = [v.webp200, v.webp600, v.webp1200]
    .filter(Boolean)
    .map((url, i) => `${resolveMediaUrl(url)} ${[200, 600, 1200][i]}w`);
  return parts.length ? parts.join(', ') : '';
});

const fallbackSrcset = computed(() => {
  const original = normalized.value.original;
  if (!original) return '';
  return `${resolveMediaUrl(original)} 1200w`;
});

const resolvedSizes = computed(() => props.sizes || `(max-width: 768px) 100vw, ${props.width}px`);

const showPlaceholder = computed(() => props.lazy && !isLoaded.value && inView.value);

function onLoad() {
  isLoaded.value = true;
}

function onError() {
  isLoaded.value = true;
}

onMounted(() => {
  if (!props.lazy || !imgRef.value) return;
  observer.value = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) {
        inView.value = true;
        observer.value?.disconnect();
      }
    },
    { rootMargin: '200px' }
  );
  observer.value.observe(imgRef.value);
});

onUnmounted(() => observer.value?.disconnect());
</script>

<style scoped>
.smart-image {
  display: block;
  overflow: hidden;
  border-radius: var(--radius-sm);
}

.smart-image img {
  display: block;
  width: 100%;
  height: auto;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.smart-image.loaded img {
  opacity: 1;
}

.smart-image.has-placeholder img {
  filter: blur(8px);
  transform: scale(1.02);
}

.smart-image.loaded.has-placeholder img {
  filter: none;
  transform: none;
}
</style>
