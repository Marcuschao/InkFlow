<template>
  <Teleport to="body">
    <div class="toast-stack" aria-live="polite">
      <TransitionGroup name="toast-pop">
        <div
          v-for="t in toast.items"
          :key="t.id"
          :class="['toast-item', t.type]"
          role="status"
        >
          {{ t.message }}
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { useToastStore } from '../stores/toast';

const toast = useToastStore();
</script>

<style scoped>
.toast-stack {
  position: fixed;
  top: calc(var(--layout-navbar-bottom) + 0.75rem);
  left: 50%;
  transform: translateX(-50%);
  z-index: 2000;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  pointer-events: none;
  max-width: min(480px, 92vw);
}

.toast-item {
  pointer-events: auto;
  padding: 0.55rem 1rem;
  border-radius: var(--radius-md);
  font-family: system-ui, -apple-system, 'Segoe UI', 'Microsoft YaHei', 'PingFang SC', sans-serif;
  font-size: 0.88rem;
  font-weight: 600;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
  box-shadow: var(--shadow-md);
}

.toast-item.success {
  border-color: rgba(22, 163, 74, 0.35);
  color: var(--color-success);
}

.toast-pop-enter-active,
.toast-pop-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.toast-pop-enter-from,
.toast-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
