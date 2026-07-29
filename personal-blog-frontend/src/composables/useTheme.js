import { computed, ref, watch } from 'vue';
import { darkTheme } from 'naive-ui';

const STORAGE_KEY = 'inkflow-theme';
const LEGACY_STORAGE_KEY = 'blog-theme-dark';
const media = window.matchMedia?.('(prefers-color-scheme: dark)');

function initialMode() {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === 'light' || stored === 'dark' || stored === 'system') return stored;
  const legacy = localStorage.getItem(LEGACY_STORAGE_KEY);
  if (legacy === '1') return 'dark';
  if (legacy === '0') return 'light';
  return 'system';
}

const themeMode = ref(initialMode());
const systemDark = ref(media?.matches ?? false);
const isDark = computed(() => themeMode.value === 'dark' || (themeMode.value === 'system' && systemDark.value));

media?.addEventListener?.('change', (event) => {
  systemDark.value = event.matches;
});

watch(
  isDark,
  (dark) => {
    const root = document.documentElement;
    root.classList.toggle('dark', dark);
    root.dataset.theme = dark ? 'dark' : 'light';
    root.style.colorScheme = dark ? 'dark' : 'light';
  },
  { immediate: true }
);

watch(themeMode, (mode) => {
  localStorage.setItem(STORAGE_KEY, mode);
  localStorage.removeItem(LEGACY_STORAGE_KEY);
});

export function useTheme() {
  const naiveTheme = computed(() => (isDark.value ? darkTheme : null));

  function toggleDark(value) {
    themeMode.value = typeof value === 'boolean' ? (value ? 'dark' : 'light') : isDark.value ? 'light' : 'dark';
  }

  function setThemeMode(mode) {
    if (mode === 'light' || mode === 'dark' || mode === 'system') themeMode.value = mode;
  }

  return { isDark, naiveTheme, themeMode, toggleDark, setThemeMode };
}
