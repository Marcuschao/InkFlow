<template>
  <div ref="rootEl" class="markdown-renderer markdown-prose">
    <div v-html="renderedMarkdown"></div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css';
import { copyTextToClipboard } from '../utils/clipboard';

const props = defineProps({
  markdown: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['headings-extracted']);

const renderedMarkdown = ref('');
const rootEl = ref(null);

let rafScroll = 0;

const canParallax = () => {
  if (typeof window === 'undefined') return false;
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return false;
  if (window.matchMedia('(max-width: 767px)').matches) return false;
  return true;
};

const extractHeadings = (htmlContent) => {
  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');
  const headingElements = doc.querySelectorAll('h1, h2, h3, h4, h5, h6');
  const headings = [];

  headingElements.forEach((el) => {
    const level = parseInt(el.tagName.substring(1));
    const text = el.textContent;
    let id = el.id;
    if (!id) {
      id = text.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
      el.id = id;
    }
    headings.push({ id, level, text });
  });
  return headings;
};

const applyLazyAllImages = () => {
  const root = rootEl.value;
  if (!root) return;
  root.querySelectorAll('img').forEach((img) => {
    img.loading = 'lazy';
    img.decoding = 'async';
    img.style.touchAction = 'pinch-zoom';
  });
};

const attachCodeCopyButtons = () => {
  const root = rootEl.value;
  if (!root) return;
  root.querySelectorAll('pre').forEach((pre) => {
    if (pre.querySelector('.code-copy-btn')) return;
    const code = pre.querySelector('code');
    if (!code) return;
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'code-copy-btn';
    btn.textContent = '复制';
    btn.addEventListener('click', async () => {
      const ok = await copyTextToClipboard(code.innerText || '');
      btn.textContent = ok ? '已复制' : '失败';
      setTimeout(() => {
        btn.textContent = '复制';
      }, 1600);
    });
    pre.style.position = 'relative';
    pre.insertBefore(btn, pre.firstChild);
  });
};

const wrapImagesAndParallax = () => {
  const root = rootEl.value;
  if (!root || !canParallax()) return;

  root.querySelectorAll('img:not([data-parallax-wrap])').forEach((img) => {
    img.setAttribute('data-parallax-wrap', '1');
    img.loading = 'lazy';
    img.decoding = 'async';
    const wrap = document.createElement('span');
    wrap.className = 'img-parallax-wrap';
    img.parentNode.insertBefore(wrap, img);
    wrap.appendChild(img);
  });
};

const onScrollParallax = () => {
  if (!canParallax() || !rootEl.value) return;
  cancelAnimationFrame(rafScroll);
  rafScroll = requestAnimationFrame(() => {
    const wraps = rootEl.value.querySelectorAll('.img-parallax-wrap');
    const vh = window.innerHeight || 1;
    wraps.forEach((wrap) => {
      const img = wrap.querySelector('img');
      if (!img) return;
      const rect = wrap.getBoundingClientRect();
      const centerDelta = rect.top + rect.height / 2 - vh / 2;
      const move = -(centerDelta / vh) * 10;
      img.style.transform = `translate3d(0, ${Math.max(-14, Math.min(14, move))}px, 0)`;
    });
  });
};

const bindScroll = () => {
  window.addEventListener('scroll', onScrollParallax, { passive: true });
  window.addEventListener('resize', onScrollParallax, { passive: true });
};

const unbindScroll = () => {
  window.removeEventListener('scroll', onScrollParallax);
  window.removeEventListener('resize', onScrollParallax);
};

const renderMarkdown = async () => {
  marked.setOptions({
    gfm: true,
    breaks: true,
    highlight: function (code, lang) {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext';
      return hljs.highlight(code, { language }).value;
    },
    langPrefix: 'hljs-',
    headerIds: true,
    mangle: false,
  });

  const html = marked.parse(props.markdown || '');
  renderedMarkdown.value = html;
  await nextTick();
  emit('headings-extracted', extractHeadings(html));
  await nextTick();
  applyLazyAllImages();
  wrapImagesAndParallax();
  attachCodeCopyButtons();
  onScrollParallax();
};

watch(() => props.markdown, renderMarkdown, { immediate: true });

onMounted(() => {
  bindScroll();
});

onUnmounted(() => {
  cancelAnimationFrame(rafScroll);
  unbindScroll();
});
</script>

<style scoped>
.markdown-prose {
  font-size: 1.05rem;
  line-height: 1.78;
  color: var(--color-text);
}

.markdown-prose :deep(p) {
  margin-bottom: 1.1em;
}

.markdown-prose :deep(h1) {
  margin-top: 0;
  margin-bottom: 0.65em;
  font-family: var(--font-ui);
  font-size: 1.65em;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.22;
}

.markdown-prose :deep(h2) {
  margin-top: 1.65em;
  margin-bottom: 0.55em;
  font-family: var(--font-ui);
  font-size: 1.32em;
  font-weight: 650;
  letter-spacing: -0.02em;
  padding-bottom: 0.35em;
  border-bottom: 1px solid var(--color-border);
}

.markdown-prose :deep(h3) {
  margin-top: 1.35em;
  margin-bottom: 0.45em;
  font-family: var(--font-ui);
  font-size: 1.12em;
  font-weight: 650;
}

.markdown-prose :deep(h4),
.markdown-prose :deep(h5),
.markdown-prose :deep(h6) {
  margin-top: 1.2em;
  margin-bottom: 0.35em;
  font-family: var(--font-ui);
  font-weight: 600;
}

.markdown-prose :deep(a) {
  color: var(--color-primary);
  text-underline-offset: 0.2em;
  transition: opacity var(--transition-fast);
}

.markdown-prose :deep(a:hover) {
  opacity: 0.85;
}

.markdown-prose :deep(pre) {
  position: relative;
  background: var(--color-surface-raised) !important;
  padding: 1.1em 1.15em;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 1.15em 0;
  border: var(--border-brutal);
  box-shadow: var(--shadow-brutal-sm);
}

.markdown-prose :deep(code) {
  font-family: ui-monospace, 'SFMono-Regular', Menlo, Monaco, Consolas, monospace;
  font-size: 0.88em;
}

.markdown-prose :deep(p code),
.markdown-prose :deep(li code),
.markdown-prose :deep(td code) {
  background: var(--color-primary-soft);
  color: var(--color-primary-hover);
  padding: 0.15em 0.4em;
  border-radius: 6px;
  font-weight: 500;
}

.markdown-prose :deep(pre code) {
  padding: 0 !important;
  background: transparent !important;
}

.markdown-prose :deep(blockquote) {
  border-left: 4px solid var(--color-accent);
  padding: 0.65rem 1rem 0.65rem 1.15rem;
  margin: 1.15em 0;
  background: var(--surface-primary-tint);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  color: var(--color-text-muted);
}

.markdown-prose :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.15em 0;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: var(--border-brutal);
  box-shadow: var(--shadow-brutal-sm);
  font-size: 0.93em;
}

.markdown-prose :deep(tr:nth-child(even)) {
  background: rgba(248, 250, 252, 0.65);
}

.markdown-prose :deep(th),
.markdown-prose :deep(td) {
  border-bottom: 1px solid var(--color-border);
  padding: 0.65rem 0.85rem;
  text-align: left;
}

.markdown-prose :deep(th) {
  background: var(--surface-primary-misty);
  font-weight: 600;
}

.markdown-prose :deep(tr:last-child td) {
  border-bottom: none;
}

.markdown-prose :deep(ul),
.markdown-prose :deep(ol) {
  margin: 1em 0 1em 1.35em;
  padding-left: 0.35em;
}

.markdown-prose :deep(li) {
  margin-bottom: 0.35em;
}

.markdown-prose :deep(.code-copy-btn) {
  position: absolute;
  top: 0.45rem;
  right: 0.45rem;
  z-index: 2;
  font-size: 0.72rem;
  font-weight: 650;
  padding: 0.2rem 0.45rem;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: pointer;
}

.markdown-prose :deep(.code-copy-btn:hover) {
  color: var(--color-primary);
  border-color: var(--color-border-strong);
}

.markdown-prose :deep(img) {
  max-width: 100%;
  height: auto;
  display: block;
  border-radius: var(--radius-md);
  transition: transform 0.08s linear;
  touch-action: pinch-zoom;
}

.markdown-prose :deep(.img-parallax-wrap) {
  display: block;
  overflow: hidden;
  border-radius: var(--radius-md);
  margin: 1.25em 0;
  box-shadow: var(--shadow-md);
}

@media (prefers-reduced-motion: reduce) {
  .markdown-prose :deep(img) {
    transition: none;
  }
}
</style>
