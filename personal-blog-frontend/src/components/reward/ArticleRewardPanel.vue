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

    <n-modal v-model:show="showRewardModal" preset="card" title="选择打赏积分" class="reward-modal">
      <p v-if="!loggedIn" class="reward-desc">登录后可打赏作者。</p>
      <n-space v-else :size="12">
        <n-button
          v-for="points in levels"
          :key="points"
          type="primary"
          secondary
          :loading="submitting && selectedPoints === points"
          @click="submit(points)"
        >
          {{ points }} 积分
        </n-button>
      </n-space>
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
import { NButton, NEmpty, NList, NListItem, NModal, NSpace } from 'naive-ui';
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
  } catch {
    /* request 已 toast */
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
  max-width: 28rem;
}

@media (max-width: 767px) {
  .reward-main {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
