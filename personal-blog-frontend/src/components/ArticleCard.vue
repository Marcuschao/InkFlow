<template>
  <router-link :to="`/article/${article.id}`" class="article-card">
    <span class="card-accent" aria-hidden="true" />
    <div class="card-body">
      <h3 class="title">{{ article.title }}</h3>
      <p v-if="reasonLine" class="reason">{{ reasonLine }}</p>
      <p class="excerpt">{{ excerpt }}</p>
      <div class="meta">
        <time>{{ formatDate(article.createTime || article.createdAt) }}</time>
        <span
          v-for="tag in (article.tags || []).slice(0, 5)"
          :key="tag.id"
          class="tag-pill"
        >{{ tag.name }}</span>
        <span v-if="showLike" class="like-stat" @click.prevent>
          <svg class="like-icon" :class="{ liked: displayLiked }" viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
              :fill="displayLiked ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="1.5"
            />
          </svg>
          {{ displayLikeCount }}
        </span>
      </div>
      <span class="read-hint">阅读正文</span>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  article: { type: Object, required: true },
  reason: { type: String, default: '' },
  showLike: { type: Boolean, default: false },
  likeCount: { type: Number, default: undefined },
  liked: { type: Boolean, default: undefined },
});

const displayLikeCount = computed(() => {
  if (props.likeCount !== undefined) return props.likeCount;
  return props.article.likeCount ?? 0;
});

const displayLiked = computed(() => {
  if (props.liked !== undefined) return props.liked;
  return !!props.article.liked;
});

const reasonLine = computed(() => {
  const r = (props.reason || props.article.reason || '').trim();
  return r;
});

const excerpt = computed(() => {
  const a = props.article;
  if (a.summary && a.summary.trim()) return a.summary.trim();
  const raw = (a.content || '').replace(/\s+/g, ' ').trim();
  return raw.length > 160 ? `${raw.slice(0, 160)}…` : raw;
});

const formatDate = (dateString) => {
  if (!dateString) return '';
  const options = { year: 'numeric', month: 'short', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
};
</script>

<style scoped>
.article-card {
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  background: var(--color-surface);
  padding: 0;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-xs);
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast),
    border-color var(--transition-fast);
  height: 100%;
  box-sizing: border-box;
}

.card-accent {
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  height: auto;
  background: var(--color-primary);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.article-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-border-strong);
}

.article-card:hover .card-accent {
  opacity: 1;
}

.card-body {
  padding: 1.35rem 1.45rem 1.15rem;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.reason {
  margin: -0.35rem 0 0.65rem;
  font-size: var(--text-xs);
  font-weight: 650;
  color: var(--color-primary);
  line-height: 1.35;
}

.title {
  margin: 0 0 0.75rem;
  font-size: 1.18rem;
  font-weight: 650;
  letter-spacing: -0.02em;
  color: var(--color-text);
  line-height: 1.35;
  transition: color var(--transition-fast);
}

.article-card:hover .title {
  color: var(--color-primary);
}

.excerpt {
  margin: 0 0 1rem;
  font-size: 0.92rem;
  color: var(--color-text-muted);
  line-height: 1.62;
  min-height: 3em;
  flex: 1;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.8rem;
  color: var(--color-text-soft);
}

.meta time {
  font-variant-numeric: tabular-nums;
}

.tag-pill {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  padding: 0.15rem 0.55rem;
  border-radius: var(--radius-pill);
  font-size: 0.72rem;
  font-weight: 600;
}

.like-stat {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  margin-left: auto;
  color: var(--color-text-soft);
}

.like-icon {
  width: 0.85rem;
  height: 0.85rem;
}

.like-icon.liked {
  color: var(--color-primary);
}

.read-hint {
  margin-top: var(--space-4);
  align-self: flex-start;
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--color-primary);
  opacity: 0;
  transform: translateY(6px);
  transition: opacity var(--transition-fast), transform var(--transition-fast);
}

.article-card:hover .read-hint {
  opacity: 1;
  transform: translateY(0);
}

.read-hint::after {
  content: '→';
  margin-left: 0.35rem;
  transition: transform var(--transition-fast);
  display: inline-block;
}

.article-card:hover .read-hint::after {
  transform: translateX(4px);
}

@media (prefers-reduced-motion: reduce) {
  .article-card:hover {
    transform: none;
  }

  .read-hint {
    opacity: 1;
    transform: none;
  }
}
</style>
