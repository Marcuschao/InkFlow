import { ref, watch } from 'vue';

export function useStaleWhileRevalidate(fetcher, { immediate = true } = {}) {
  const data = ref(null);
  const pending = ref(false);
  const error = ref(null);
  let generation = 0;

  async function refresh(...args) {
    const gen = ++generation;
    const hasData = data.value != null;
    if (!hasData) pending.value = true;
    error.value = null;
    try {
      const result = await fetcher(...args);
      if (gen === generation) data.value = result;
      return result;
    } catch (e) {
      if (gen === generation) error.value = e;
      throw e;
    } finally {
      if (gen === generation) pending.value = false;
    }
  }

  if (immediate) {
    refresh();
  }

  return { data, pending, error, refresh };
}
