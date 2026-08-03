<template>
  <div class="sign-widget">
    <div class="sign-widget-head">
      <p class="sign-widget-kicker">每日记录</p>
      <CalendarCheck :size="18" :stroke-width="1.8" aria-hidden="true" />
    </div>

    <div class="sign-widget-main">
      <div class="sign-ring" :class="{ signed: status?.signedToday }">
        <svg class="sign-ring-svg" viewBox="0 0 72 72" aria-hidden="true">
          <circle class="sign-ring-track" cx="36" cy="36" r="30" />
          <circle
            class="sign-ring-progress"
            cx="36"
            cy="36"
            r="30"
            :stroke-dasharray="ringCircumference"
            :stroke-dashoffset="ringOffset"
          />
        </svg>
        <span class="sign-ring-inner">
          <span class="sign-ring-num">{{ status?.streakDays ?? 0 }}</span>
          <span class="sign-ring-unit">连续天数</span>
        </span>
      </div>

      <div class="sign-widget-copy">
        <p class="sign-widget-title">{{ status?.signedToday ? '今天已完成签到' : '留下今天的足迹' }}</p>
        <p v-if="status?.nextBonusDays > 0" class="sign-widget-bonus">
          再签 {{ status.nextBonusDays }} 天可得 {{ status.nextBonusPoints }} 积分
        </p>
        <p v-else class="sign-widget-bonus">累计签到 {{ status?.totalDays ?? 0 }} 天</p>
      </div>
    </div>

    <n-button
      v-if="status && !status.signedToday"
      class="sign-widget-btn"
      size="small"
      type="primary"
      :loading="signing"
      @click="doSign"
    >
      +5 积分
    </n-button>
    <n-tag v-else-if="status" class="sign-widget-status" size="small" type="success">今日已签</n-tag>

    <n-popover trigger="click" placement="left" :width="280">
      <template #trigger>
        <button type="button" class="sign-detail-link">
          查看签到明细
          <ChevronRight :size="14" aria-hidden="true" />
        </button>
      </template>
      <div class="sign-popover">
        <p class="sign-popover-meta muted">
          连续 {{ status?.streakDays ?? 0 }} 天 · 累计 {{ status?.totalDays ?? 0 }} 天
        </p>
        <p v-if="status?.nextBonusDays > 0" class="sign-popover-bonus muted">
          再签 {{ status.nextBonusDays }} 天 +{{ status.nextBonusPoints }} 积分
        </p>
        <SignCalendar />
      </div>
    </n-popover>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { NButton, NPopover, NTag } from 'naive-ui';
import { CalendarCheck, ChevronRight } from 'lucide-vue-next';
import { signIn, getSignStatus } from '../../api/social';
import { useToastStore } from '../../stores/toast';
import SignCalendar from './SignCalendar.vue';

const toast = useToastStore();
const status = ref(null);
const signing = ref(false);

const ringCircumference = 2 * Math.PI * 30;

const ringProgress = computed(() => {
  if (!status.value || status.value.nextBonusDays <= 0) return 100;
  const done = status.value.streakDays;
  const total = done + status.value.nextBonusDays;
  if (total <= 0) return 0;
  return Math.min(100, Math.round((done / total) * 100));
});

const ringOffset = computed(() => ringCircumference * (1 - ringProgress.value / 100));

async function loadStatus() {
  try {
    const res = await getSignStatus();
    status.value = res.data;
  } catch {
    status.value = null;
  }
}

async function doSign() {
  if (status.value?.signedToday) return;
  signing.value = true;
  try {
    const res = await signIn();
    const d = res.data;
    await loadStatus();
    toast.push(d?.alreadySigned ? '今日已签到' : `签到成功，+${d?.pointsEarned || 5} 积分`, 'success');
  } catch {
    /* request toast */
  } finally {
    signing.value = false;
  }
}

onMounted(loadStatus);

defineExpose({ reload: loadStatus });
</script>

<style scoped>
.sign-widget {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--space-4);
  padding: var(--space-5);
  border: var(--border-brutal);
  border-radius: var(--radius-brutal-card);
  background: var(--color-surface);
  box-shadow: none;
  width: 100%;
}

.sign-widget-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-primary);
}

.sign-widget-kicker {
  margin: 0;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.sign-widget-main {
  display: grid;
  grid-template-columns: 4.5rem minmax(0, 1fr);
  gap: var(--space-4);
  align-items: center;
}

.sign-ring {
  position: relative;
  width: 4.5rem;
  height: 4.5rem;
}

.sign-ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.sign-ring-track {
  fill: none;
  stroke: var(--color-border);
  stroke-width: 4;
}

.sign-ring-progress {
  fill: none;
  stroke: var(--color-text-muted);
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.3s ease;
}

.sign-ring.signed .sign-ring-progress {
  stroke: var(--color-success, #16a34a);
}

.sign-ring-inner {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.sign-ring-num {
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--color-text);
}

.sign-ring-unit {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin-top: var(--space-1);
}

.sign-widget-title {
  margin: 0;
  color: var(--color-text);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  line-height: 1.45;
}

.sign-widget-bonus {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  line-height: 1.5;
}

.sign-widget-btn {
  width: 100%;
}

.sign-widget-status {
  align-self: flex-start;
}

.sign-detail-link {
  border: none;
  background: none;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0;
  font-size: var(--text-xs);
  color: var(--color-primary);
  cursor: pointer;
  font-family: inherit;
}

.sign-detail-link:hover {
  color: var(--color-primary-hover);
}

.sign-popover-meta,
.sign-popover-bonus {
  margin: 0 0 var(--space-2);
  font-size: var(--text-xs);
}

.muted {
  color: var(--color-text-muted);
}

@media (max-width: 767px) {
  .sign-widget {
    width: 100%;
    padding: var(--space-4);
    gap: var(--space-3);
  }

  .sign-widget-main {
    grid-template-columns: 3.5rem minmax(0, 1fr);
  }

  .sign-ring {
    width: 3.5rem;
    height: 3.5rem;
  }

  .sign-ring-num {
    font-size: var(--text-lg);
  }

  .sign-ring-unit {
    font-size: 10px;
  }
}
</style>
