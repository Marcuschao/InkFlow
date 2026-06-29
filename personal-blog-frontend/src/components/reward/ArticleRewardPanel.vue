<template>
  <section class="reward-panel" aria-label="文章打赏">
    <div class="reward-main">
      <div>
        <h2 class="reward-title">喜欢这篇文章？</h2>
        <p class="reward-desc">用积分支持作者，打赏后会展示在支持者列表中。</p>
      </div>
      <n-button
        type="primary"
        :disabled="!loggedIn"
        :loading="submitting"
        @click="showRewardModal = true"
      >
        {{ rewarded ? '已打赏' : '打赏' }}
      </n-button>
    </div>
    <button v-if="records.length" type="button" class="reward-users" @click="showListModal = true">
      <span class="reward-avatar-stack">
        <UserAvatar
          v-for="item in records.slice(0, 6)"
          :key="item.id"
          :src="item.fromAvatar"
          :name="item.fromNickname || '用户'"
          :size="28"
        />
      </span>
      <span>{{ records.length }} 人已打赏</span>
    </button>
    <p v-else class="reward-empty">暂未收到打赏</p>

    <n-modal v-model:show="showRewardModal" preset="card" class="reward-modal">
      <div class="reward-modal-head">
        <span class="reward-modal-kicker">支持作者</span>
        <h3>选择打赏积分</h3>
        <p>你的支持会展示在文章支持者列表中。</p>
      </div>
      <p v-if="!loggedIn" class="reward-login-tip">登录后可打赏作者。</p>
      <div v-else class="reward-level-grid">
        <button
          v-for="points in levels"
          :key="points"
          type="button"
          class="reward-level"
          :class="{ 'is-loading': submitting && selectedPoints === points }"
          :disabled="submitting"
          @click="submit(points)"
        >
          <span class="reward-level-points">{{ points }}</span>
          <span class="reward-level-unit">积分</span>
        </button>
      </div>
    </n-modal>

    <n-modal v-model:show="showListModal" preset="card" title="打赏记录" class="reward-modal">
      <n-list v-if="records.length">
        <n-list-item v-for="item in records" :key="item.id">
          <template #prefix>
            <UserAvatar :src="item.fromAvatar" :name="item.fromNickname || '用户'" :size="32" />
          </template>
          <div class="reward-record">
            <strong>{{ item.fromNickname || '用户' }}</strong>
            <span>{{ item.points }} 积分</span>
          </div>
        </n-list-item>
      </n-list>
      <n-empty v-else description="暂无打赏" />
    </n-modal>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue';
import { NButton, NEmpty, NList, NListItem, NModal } from 'naive-ui';
import UserAvatar from '../UserAvatar.vue';
import { rewardArticle } from '../../api/reward';
import { useToastStore } from '../../stores/toast';

const props = defineProps({
  articleId: { type: Number, required: true },
  records: { type: Array, default: () => [] },
  loggedIn: { type: Boolean, default: false },
  currentUserId: { type: Number, default: null },
});

const emit = defineEmits(['rewarded']);
const toast = useToastStore();
const levels = [5, 10, 20, 50];
const showRewardModal = ref(false);
const showListModal = ref(false);
const submitting = ref(false);
const selectedPoints = ref(null);

const rewarded = computed(() =>
  props.currentUserId != null && props.records.some((item) => item.fromUserId === props.currentUserId)
);

async function submit(points) {
  submitting.value = true;
  selectedPoints.value = points;
  try {
    await rewardArticle(props.articleId, points);
    toast.push('打赏成功', 'success');
    showRewardModal.value = false;
    emit('rewarded');
  } catch (err) {
    if (String(err?.message || '').includes('不能给自己打赏')) {
      showRewardModal.value = false;
    }
  } finally {
    submitting.value = false;
    selectedPoints.value = null;
  }
}
</script>

<style scoped>
.reward-panel {
  margin-top: var(--space-8);
  padding-top: var(--space-5);
  border-top: 1px dashed var(--color-border);
}

.reward-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.reward-title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
}

.reward-desc,
.reward-empty {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.reward-users {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
}

.reward-avatar-stack {
  display: flex;
  align-items: center;
}

.reward-avatar-stack :deep(.n-avatar) {
  margin-right: calc(var(--space-2) * -1);
  border: 1px solid var(--color-surface);
}

.reward-record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  width: 100%;
}

.reward-modal {
  width: min(520px, calc(100vw - var(--space-8)));
  max-width: 520px;
}

.reward-modal :deep(.n-card__content) {
  padding: var(--space-6);
}

.reward-modal-head {
  margin-bottom: var(--space-5);
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-raised);
}

.reward-modal-kicker {
  display: inline-flex;
  margin-bottom: var(--space-2);
  color: var(--color-accent);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
}

.reward-modal-head h3 {
  margin: 0;
  color: var(--color-text);
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  line-height: 1.25;
}

.reward-modal-head p,
.reward-login-tip {
  margin: var(--space-2) 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.reward-level-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.reward-level {
  display: flex;
  min-height: 88px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  padding: var(--space-4) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    background var(--transition-fast),
    transform var(--transition-fast);
}

.reward-level:hover:not(:disabled),
.reward-level:focus-visible {
  border-color: var(--color-accent);
  background: var(--color-primary-soft);
  transform: translateY(-2px);
}

.reward-level:focus-visible {
  outline: 2px solid var(--border-focus-input);
  outline-offset: 2px;
}

.reward-level:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.reward-level.is-loading {
  border-color: var(--color-accent);
  background: var(--color-primary-soft);
}

.reward-level-points {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  line-height: 1.25;
}

.reward-level-unit {
  color: var(--color-text-muted);
  font-size: var(--text-xs);
}

@media (max-width: 767px) {
  .reward-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .reward-modal {
    width: calc(100vw - var(--space-8));
  }

  .reward-modal :deep(.n-card__content) {
    padding: var(--space-4);
  }

  .reward-modal-head {
    padding: var(--space-4);
  }

  .reward-level-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
