<template>
  <div class="admin-site-page admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header" style="margin-bottom: 24px;">
        <div>
          <h1 class="ds-page-title">站点设置</h1>
          <p class="ds-page-sub">前台 AI 问答入口显示策略</p>
        </div>
        <router-link to="/admin">
          <n-button>返回管理</n-button>
        </router-link>
      </header>

      <n-card class="panel-card" style="max-width: 36rem;">
        <p class="hint" style="margin-bottom: 16px; color: var(--color-text-muted);">
          控制全站 AI 问答入口：悬浮按钮、导航「AI 问答」、移动端 Dock、文章/搜索/知识星系渗透入口及独立对话页（保存后立即生效）。
        </p>

        <n-radio-group v-model:value="mode" name="cv" style="margin-bottom: 24px;">
          <n-space vertical :size="12">
            <n-radio v-for="opt in options" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </n-radio>
          </n-space>
        </n-radio-group>

        <n-alert v-if="msg" type="success" style="margin-bottom: 16px;">{{ msg }}</n-alert>
        <n-alert v-if="err" type="error" style="margin-bottom: 16px;">{{ err }}</n-alert>

        <n-button type="primary" :loading="saving" @click="save">
          保存
        </n-button>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { NButton, NCard, NRadioGroup, NRadio, NSpace, NAlert } from 'naive-ui';
import { fetchAdminSiteConfig, putChatbotVisibility } from '../../api/site';
import { useSiteStore } from '../../stores/site';

const siteStore = useSiteStore();

const mode = ref('NONE');
const saving = ref(false);
const msg = ref('');
const err = ref('');

const options = [
  { value: 'NONE', label: '全局不展示（关闭所有 AI 问答入口）' },
  { value: 'GUEST', label: '所有人可见（含未登录访客）' },
  { value: 'AUTH', label: '仅登录用户可见' },
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
    await siteStore.loadPublicConfig();
    msg.value = '已保存';
  } catch (e) {
    err.value = e?.message || '保存失败';
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
</style>
