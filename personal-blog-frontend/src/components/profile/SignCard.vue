<template>
  <n-card class="sign-card" size="small">
    <div class="sign-card-inner">
      <div class="sign-card-info">
        <p class="sign-title">每日签到</p>
        <p v-if="status" class="sign-meta muted">
          连续 {{ status.streakDays }} 天 · 累计 {{ status.totalDays }} 天
        </p>
        <p v-if="status && status.nextBonusDays > 0" class="sign-bonus muted">
          再签到 {{ status.nextBonusDays }} 天可获额外 {{ status.nextBonusPoints }} 积分
        </p>
        <n-progress
          v-if="status && status.nextBonusDays > 0"
          type="line"
          :percentage="bonusProgress"
          :show-indicator="false"
          :height="8"
        />
      </div>
      <div class="sign-card-action">
        <n-button v-if="status && !status.signedToday" type="primary" :loading="signing" @click="doSign">
          签到 +5
        </n-button>
        <n-tag v-else-if="status" type="success" size="small">已签到</n-tag>
        <p v-if="lastEarned" class="sign-earned muted">今日 +{{ lastEarned }} 积分</p>
      </div>
    </div>
    <SignCalendar v-if="showCalendar" class="sign-calendar-wrap" />
  </n-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { NButton, NCard, NProgress, NTag } from 'naive-ui';
import { signIn, getSignStatus } from '../../api/social';
import { useToastStore } from '../../stores/toast';
import SignCalendar from './SignCalendar.vue';

const props = defineProps({
  showCalendar: { type: Boolean, default: false },
});

const toast = useToastStore();
const status = ref(null);
const signing = ref(false);
const lastEarned = ref(0);

const bonusProgress = computed(() => {
  if (!status.value || status.value.nextBonusDays <= 0) return 100;
  const target = status.value.streakDays + status.value.nextBonusDays;
  const base = target - status.value.nextBonusDays;
  if (target <= base) return 0;
  return Math.min(100, Math.round(((status.value.streakDays - base) / (target - base)) * 100));
});

async function loadStatus() {
  try {
    const res = await getSignStatus();
    status.value = res.data;
  } catch {
    status.value = null;
  }
}

async function doSign() {
  signing.value = true;
  try {
    const res = await signIn();
    const d = res.data;
    lastEarned.value = d?.pointsEarned || 0;
    await loadStatus();
    toast.push(d?.alreadySigned ? '今日已签到' : `签到成功，+${lastEarned.value} 积分`, 'success');
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
.sign-card {
  margin-top: var(--space-4);
  border: 1px solid var(--color-border);
  box-shadow: none;
}

.sign-card-inner {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.sign-title {
  margin: 0;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
}

.sign-meta,
.sign-bonus,
.sign-earned {
  margin: var(--space-1) 0 0;
  font-size: var(--text-sm);
}

.sign-calendar-wrap {
  margin-top: var(--space-4);
}

.muted {
  color: var(--color-text-muted);
}
</style>
