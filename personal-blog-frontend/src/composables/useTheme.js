import { ref, computed, watch } from 'vue';
import { darkTheme } from 'naive-ui';

const STORAGE_KEY = 'blog-theme-dark';
const stored = localStorage.getItem(STORAGE_KEY);
const isDark = ref(stored === null ? false : stored === '1');

watch(
  isDark,
  (v) => {
    localStorage.setItem(STORAGE_KEY, v ? '1' : '0');
    document.documentElement.classList.toggle('dark', v);
    document.documentElement.style.colorScheme = v ? 'dark' : 'light';
  },
  { immediate: true }
);

export function useTheme() {
  const naiveTheme = computed(() => (isDark.value ? darkTheme : null));

  function toggleDark(val) {
    isDark.value = typeof val === 'boolean' ? val : !isDark.value;
  }

  return { isDark, naiveTheme, toggleDark };
}
