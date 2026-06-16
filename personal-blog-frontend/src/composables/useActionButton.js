import { ref, computed } from 'vue';

export function useActionButton(actionFn) {
  const state = ref('idle');
  const errorMessage = ref('');

  async function run(...args) {
    if (state.value === 'loading') return;
    state.value = 'loading';
    errorMessage.value = '';
    try {
      await actionFn(...args);
      state.value = 'success';
      setTimeout(() => {
        if (state.value === 'success') state.value = 'idle';
      }, 1200);
    } catch (e) {
      state.value = 'error';
      errorMessage.value = e?.message || '操作失败';
      setTimeout(() => {
        if (state.value === 'error') state.value = 'idle';
      }, 1600);
      throw e;
    }
  }

  return { state, errorMessage, run, isLoading: computed(() => state.value === 'loading') };
}
