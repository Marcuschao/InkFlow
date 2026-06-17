<template>
  <div class="sign-calendar">
    <div class="calendar-head">
      <n-button size="tiny" quaternary @click="prevMonth">&lt;</n-button>
      <span>{{ year }}年{{ month }}月</span>
      <n-button size="tiny" quaternary @click="nextMonth">&gt;</n-button>
    </div>
    <div class="calendar-grid">
      <span v-for="d in days" :key="d.day" class="day" :class="{ signed: d.signed, today: d.today }">
        {{ d.day }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { NButton } from 'naive-ui';
import { getSignCalendar } from '../../api/social';

const now = new Date();
const year = ref(now.getFullYear());
const month = ref(now.getMonth() + 1);
const days = ref([]);

async function load() {
  try {
    const res = await getSignCalendar(year.value, month.value);
    const data = res.data;
    const today = new Date();
    const isCurrent = today.getFullYear() === year.value && today.getMonth() + 1 === month.value;
    days.value = (data?.days || []).map((signed, i) => ({
      day: i + 1,
      signed,
      today: isCurrent && i + 1 === today.getDate(),
    }));
  } catch {
    days.value = [];
  }
}

function prevMonth() {
  if (month.value === 1) {
    year.value -= 1;
    month.value = 12;
  } else {
    month.value -= 1;
  }
}

function nextMonth() {
  if (month.value === 12) {
    year.value += 1;
    month.value = 1;
  } else {
    month.value += 1;
  }
}

watch([year, month], load);
onMounted(load);
</script>

<style scoped>
.calendar-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
  font-size: var(--text-sm);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--space-1);
}

.day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-xs);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.day.signed {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.day.today {
  font-weight: var(--weight-semibold);
}
</style>
