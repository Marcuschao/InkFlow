<template>
  <div class="admin-site-page admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">站点设置</h1>
          <p class="ds-page-sub">前台问答入口显示策略</p>
        </div>
        <router-link to="/admin" class="ds-btn ds-btn--secondary ds-btn--pill">返回管理</router-link>
      </header>
      <div class="panel ds-table-shell">
        <p class="hint">选择博客问答浮动按钮的可见范围（保存后立即生效）。</p>
        <div class="choices">
          <label v-for="opt in options" :key="opt.value" class="radio-row">
            <input v-model="mode" type="radio" name="cv" :value="opt.value" />
            <span>{{ opt.label }}</span>
          </label>
        </div>
        <p v-if="msg" class="msg">{{ msg }}</p>
        <p v-if="err" class="err">{{ err }}</p>
        <button type="button" class="ds-btn ds-btn--primary" :disabled="saving" @click="save">
          <span v-if="saving" class="ds-spin" aria-hidden="true" />
          保存
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { fetchAdminSiteConfig, putChatbotVisibility } from '../../api/site';

const mode = ref('NONE');
const saving = ref(false);
const msg = ref('');
const err = ref('');

const options = [
  { value: 'NONE', label: '全局不展示' },
  { value: 'GUEST', label: '未登录也可见（文章详情页）' },
  { value: 'AUTH', label: '仅登录后可见（文章详情页）' },
];

onMounted(async () => {
  try {
    const res = await fetchAdminSiteConfig();
    const d = res?.data;
    if (d?.chatbotVisibility) mode.value = d.chatbotVisibility;
  } catch {
    /* ignore */
  }
});

async function save() {
  msg.value = '';
  err.value = '';
  saving.value = true;
  try {
    await putChatbotVisibility(mode.value);
    msg.value = '已保存';
  } catch (e) {
    err.value = e?.message || '保存失败';
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.panel {
  padding: 1.5rem;
  max-width: 36rem;
}

.hint {
  margin: 0 0 1rem;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.choices {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  margin-bottom: 1.25rem;
}

.radio-row {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.92rem;
  cursor: pointer;
}

.msg {
  color: var(--color-success);
  font-size: 0.88rem;
  margin-bottom: 0.75rem;
}

.err {
  color: var(--color-danger);
  font-size: 0.88rem;
  margin-bottom: 0.75rem;
}
</style>
