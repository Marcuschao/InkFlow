<template>
  <ol v-if="items.length" class="activity-ledger">
    <li v-for="(item, i) in items" :key="`${item.actionType}-${item.eventTime}-${i}`" class="activity-entry">
      <span class="activity-icon" :class="`is-${actionMeta(item).tone}`" aria-hidden="true">
        <component :is="actionMeta(item).icon" :size="16" :stroke-width="1.8" />
      </span>
      <div class="activity-content">
        <p class="activity-description">
          <router-link v-if="item.actorId" :to="`/user/${item.actorId}`" class="activity-actor">
            {{ item.actorName || '用户' }}
          </router-link>
          <strong v-else class="activity-actor">{{ item.actorName || '用户' }}</strong>
          <span>{{ actionMeta(item).verb }}</span>
          <router-link
            v-if="item.articleId && item.articleTitle"
            :to="`/article/${item.articleId}`"
            class="activity-article"
          >
            《{{ item.articleTitle }}》
          </router-link>
        </p>
        <time v-if="item.eventTime" class="activity-time" :datetime="item.eventTime">
          {{ formatTime(item.eventTime) }}
        </time>
      </div>
    </li>
  </ol>
  <n-empty v-else class="activity-empty" description="暂无动态，新的互动会记录在这里" />
</template>

<script setup>
import { NEmpty } from 'naive-ui';
import { Activity, Bookmark, Heart, MessageCircle, UserPlus } from 'lucide-vue-next';

defineProps({
  items: { type: Array, default: () => [] },
});

function formatTime(t) {
  if (!t) return '';
  const d = new Date(t);
  const diff = Date.now() - d.getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return '刚刚';
  if (mins < 60) return `${mins}分钟前`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}小时前`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}天前`;
  return d.toLocaleDateString();
}

function actionMeta(item) {
  if (item.actionType === 'FOLLOW') return { verb: '关注了你', icon: UserPlus, tone: 'follow' };
  if (item.actionType === 'LIKE') return { verb: '赞了你的文章', icon: Heart, tone: 'like' };
  if (item.actionType === 'FAVORITE') return { verb: '收藏了你的文章', icon: Bookmark, tone: 'favorite' };
  if (item.actionType === 'COMMENT') return { verb: '评论了你的文章', icon: MessageCircle, tone: 'comment' };
  return { verb: '与你互动', icon: Activity, tone: 'default' };
}
</script>

<style scoped>
.activity-ledger {
  margin: 0;
  padding: 0;
  list-style: none;
}

.activity-entry {
  position: relative;
  display: grid;
  grid-template-columns: 2rem minmax(0, 1fr);
  gap: var(--space-4);
  min-height: 5.25rem;
  padding: 0 0 var(--space-5);
}

.activity-entry:not(:last-child)::before {
  position: absolute;
  top: 2rem;
  bottom: 0;
  left: calc(1rem - 0.5px);
  width: 1px;
  background: var(--color-border-strong);
  content: '';
}

.activity-icon {
  position: relative;
  z-index: 1;
  display: inline-flex;
  width: 2rem;
  height: 2rem;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border-strong);
  border-radius: 50%;
  color: var(--color-text-muted);
  background: var(--color-page);
}

.activity-icon.is-like,
.activity-icon.is-favorite,
.activity-icon.is-comment {
  border-color: color-mix(in srgb, var(--color-primary) 45%, var(--color-border));
  color: var(--color-primary);
}

.activity-content {
  min-width: 0;
  padding-top: 2px;
}

.activity-description {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--text-base);
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.activity-actor,
.activity-article {
  color: var(--color-text);
  text-decoration: none;
}

.activity-actor {
  margin-right: 0.35em;
  font-weight: var(--weight-semibold);
}

.activity-article {
  margin-left: 0.25em;
  font-family: var(--font-prose);
}

.activity-actor:hover,
.activity-article:hover {
  color: var(--color-primary);
}

.activity-time {
  display: block;
  margin-top: var(--space-1);
  color: var(--color-text-soft);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.activity-empty {
  padding: var(--space-10) 0;
}

@media (max-width: 767px) {
  .activity-entry {
    gap: var(--space-3);
    padding-bottom: var(--space-4);
  }

  .activity-description {
    font-size: var(--text-sm);
  }
}
</style>
