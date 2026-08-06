<template>
  <div ref="rootEl" class="markdown-renderer markdown-prose">
    <div v-html="renderedMarkdown"></div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';
import { copyTextToClipboard } from '../utils/clipboard';

const props = defineProps({
  markdown: {
    type: String,
    default: '',
  },
  decorateCitations: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['headings-extracted']);

const renderedMarkdown = ref('');
const rootEl = ref(null);

const normalizeHeadingId = (text, index, used) => {
  const base = String(text || '')
    .trim()
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-|-$/g, '') || `heading-${index + 1}`;
  let id = base;
  let n = 2;
  while (used.has(id)) {
    id = `${base}-${n}`;
    n += 1;
  }
  used.add(id);
  return id;
};

const normalizeHeadings = (htmlContent) => {
  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');
  const headingElements = doc.querySelectorAll('h1, h2, h3, h4, h5, h6');
  const headings = [];
  const used = new Set();

  headingElements.forEach((el, index) => {
    const level = parseInt(el.tagName.substring(1));
    const text = el.textContent || '';
    const id = el.id && !used.has(el.id) ? el.id : normalizeHeadingId(text, index, used);
    used.add(id);
    el.id = id;
    headings.push({ id, level, text });
  });
  return {
    html: doc.body.innerHTML,
    headings,
  };
};

const decorateCitations = (htmlContent) => {
  if (!props.decorateCitations) return htmlContent;
  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');
  const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT);
  const nodes = [];
  let node;
  while ((node = walker.nextNode())) {
    const parent = node.parentElement;
    if (!parent || parent.closest('code, pre, a, sup')) continue;
    if (/\[\d+\]/.test(node.nodeValue || '')) nodes.push(node);
  }
  nodes.forEach((textNode) => {
    const fragment = doc.createDocumentFragment();
    const parts = textNode.nodeValue.split(/(\[\d+\])/g);
    parts.forEach((part) => {
      if (/^\[\d+\]$/.test(part)) {
        const citation = doc.createElement('sup');
        citation.className = 'markdown-citation';
        citation.textContent = part;
        citation.setAttribute('aria-label', `引用 ${part.slice(1, -1)}`);
        fragment.appendChild(citation);
      } else if (part) {
        fragment.appendChild(doc.createTextNode(part));
      }
    });
    textNode.parentNode.replaceChild(fragment, textNode);
  });
  return doc.body.innerHTML;
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
    const code = pre.querySelector('code');
    if (!code) return;
    pre.classList.add('terminal-code');

    let shell = pre.parentElement;
    if (!shell?.classList.contains('terminal-shell')) {
      shell = document.createElement('div');
      shell.className = 'terminal-shell';
      pre.parentNode.insertBefore(shell, pre);
      shell.appendChild(pre);
    }
    if (shell.querySelector('.code-copy-btn')) return;

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
    shell.appendChild(btn);
  });
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

  const html = DOMPurify.sanitize(marked.parse(props.markdown || ''));
  const normalized = normalizeHeadings(html);
  renderedMarkdown.value = decorateCitations(normalized.html);
  await nextTick();
  emit('headings-extracted', normalized.headings);
  await nextTick();
  applyLazyAllImages();
  attachCodeCopyButtons();
};

watch(() => props.markdown, renderMarkdown, { immediate: true });
</script>

<style scoped>
.markdown-prose {
  font-family: var(--font-prose);
  font-size: 1.08rem;
  font-weight: 400;
  line-height: 1.96;
  letter-spacing: 0;
  color: var(--color-text-muted);
}

.markdown-prose :deep(p) {
  margin-bottom: 1.35em;
}

.markdown-prose :deep(h1) {
  margin-top: 0;
  margin-bottom: 0.65em;
  font-family: var(--font-display);
  font-size: 1.65em;
  font-weight: 600;
  letter-spacing: 0;
  line-height: 1.3;
  color: var(--color-text);
}

.markdown-prose :deep(h2) {
  position: relative;
  margin-top: 1.65em;
  margin-bottom: 0.55em;
  font-family: var(--font-display);
  font-size: 1.48em;
  font-weight: 600;
  letter-spacing: 0;
  line-height: 1.4;
  color: var(--color-text);
  padding-bottom: 0.35em;
  border-bottom: 1px solid var(--color-border);
}

.markdown-prose :deep(h2::after) {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 40px;
  height: 2px;
  background: var(--color-accent);
}

.markdown-prose :deep(h3) {
  margin-top: 1.35em;
  margin-bottom: 0.45em;
  font-family: var(--font-display);
  font-size: 1.12em;
  font-weight: 600;
  letter-spacing: 0;
  color: var(--color-text);
}

.markdown-prose :deep(h4),
.markdown-prose :deep(h5),
.markdown-prose :deep(h6) {
  margin-top: 1.2em;
  margin-bottom: 0.35em;
  font-family: var(--font-display);
  font-weight: 600;
  letter-spacing: 0;
  color: var(--color-text);
}

.markdown-prose :deep(h1),
.markdown-prose :deep(h2),
.markdown-prose :deep(h3),
.markdown-prose :deep(h4),
.markdown-prose :deep(h5),
.markdown-prose :deep(h6) {
  scroll-margin-top: calc(var(--layout-main-pad-top) + var(--space-2));
}

.markdown-prose :deep(a) {
  color: var(--color-primary);
  text-underline-offset: 0.2em;
  transition: opacity var(--transition-fast);
}

.markdown-prose :deep(a:hover) {
  opacity: 0.85;
}

.markdown-prose :deep(.markdown-citation) {
  position: relative;
  top: -0.28em;
  margin-left: 0.12em;
  color: var(--color-accent);
  font-family: var(--font-ui);
  font-size: 0.68em;
  font-weight: 700;
  line-height: 0;
  text-decoration: none;
}

.markdown-prose :deep(.terminal-shell) {
  position: relative;
  background: #171722 !important;
  border-radius: 6px;
  margin: 1.6em 0;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 30px rgba(31, 25, 21, 0.18);
  color: #e7e5ee;
  overflow: hidden;
}

.markdown-prose :deep(.terminal-shell::before) {
  content: '';
  position: absolute;
  top: 1rem;
  left: 1rem;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ff5f57;
  box-shadow: 19px 0 #febc2e, 38px 0 #28c840;
}

.markdown-prose :deep(.terminal-shell pre) {
  position: relative;
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 2.9rem 1.25rem 1.2rem;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  background: transparent !important;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
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
  color: #e7e5ee !important;
  line-height: 1.65;
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
  margin-bottom: 0.48em;
  padding-left: 0.25em;
}

.markdown-prose :deep(li::marker) {
  color: var(--color-accent);
  font-size: 0.72em;
}

.markdown-prose :deep(.code-copy-btn) {
  position: absolute;
  top: 0.65rem;
  right: 0.75rem;
  z-index: 2;
  font-size: 0.72rem;
  font-weight: 650;
  padding: 0.2rem 0.45rem;
  border-radius: 3px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: transparent;
  color: #aaa8b4;
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
  touch-action: pinch-zoom;
}

@media (max-width: 767px) {
  .markdown-prose {
    font-size: 1rem;
    line-height: 1.9;
  }

  .markdown-prose :deep(h2) {
    font-size: 1.38em;
  }
}
</style>
