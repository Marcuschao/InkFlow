<template>
  <div class="sign-widget">
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
        <span class="sign-ring-unit">天</span>
      </span>
    </div>

    <p class="sign-widget-label">每日签到</p>

    <n-button
      v-if="status && !status.signedToday"
      class="sign-widget-btn"
      size="tiny"
      type="primary"
      :loading="signing"
      @click="doSign"
    >
      +5 积分
    </n-button>
    <n-tag v-else-if="status" size="small" type="success">已签</n-tag>

    <n-popover trigger="click" placement="left" :width="280">
      <template #trigger>
        <button type="button" class="sign-detail-link">明细</button>
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
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
  width: 7.5rem;
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
  stroke: var(--color-primary);
  stroke-width: 4;
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

.sign-widget-label {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.sign-widget-btn {
  width: 100%;
}

.sign-detail-link {
  border: none;
  background: none;
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
    flex-direction: row;
    flex-wrap: wrap;
    width: auto;
    max-width: 100%;
    padding: var(--space-2) var(--space-3);
    gap: var(--space-3);
    align-items: center;
  }

  .sign-ring {
    width: 3rem;
    height: 3rem;
  }

  .sign-ring-num {
    font-size: var(--text-base);
  }

  .sign-widget-label {
    flex: 1;
    min-width: 4rem;
    text-align: left;
    font-size: var(--text-sm);
  }

  .sign-detail-link {
    margin-left: auto;
  }
}
</style>
