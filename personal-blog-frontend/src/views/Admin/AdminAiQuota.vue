<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">AI 配额管理</h1>
          <p class="ds-page-sub">全站与用户级 Token 配额、白名单</p>
        </div>
        <n-space :size="8">
          <router-link to="/admin/ai-dashboard"><n-button>AI 监控</n-button></router-link>
          <router-link to="/admin"><n-button>返回管理</n-button></router-link>
        </n-space>
      </header>

      <n-card title="配额设置" style="max-width: 28rem; margin-bottom: 24px;">
        <n-form label-placement="left" label-width="120">
          <n-form-item label="全站日限额">
            <n-input-number v-model:value="globalLimit" :min="0" style="width: 100%" />
          </n-form-item>
          <n-form-item label="用户日限额">
            <n-input-number v-model:value="userLimit" :min="0" style="width: 100%" />
          </n-form-item>
          <n-form-item label="今日已用">
            <span>{{ globalUsed }}</span>
          </n-form-item>
          <n-button type="primary" :loading="saving" @click="save">保存</n-button>
        </n-form>
        <n-alert v-if="msg" type="success" style="margin-top: 12px;">{{ msg }}</n-alert>
      </n-card>

      <n-card title="白名单用户">
        <n-space :size="8" style="margin-bottom: 16px;">
          <n-input-number v-model:value="newUserId" placeholder="用户 ID" :min="1" />
          <n-input v-model:value="newRemark" placeholder="备注" style="width: 160px" />
          <n-button type="primary" @click="addWhite">添加</n-button>
        </n-space>
        <n-data-table :columns="whiteColumns" :data="whitelist" :bordered="false" size="small" />
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue';
import {
  NAlert, NButton, NCard, NDataTable, NForm, NFormItem, NInput, NInputNumber, NSpace,
} from 'naive-ui';
import {
  fetchAiQuota, saveAiQuota, fetchAiWhitelist, addAiWhitelist, removeAiWhitelist,
} from '../../api/aiGateway';

const globalLimit = ref(1000000);
const userLimit = ref(50000);
const globalUsed = ref(0);
const whitelist = ref([]);
const newUserId = ref(null);
const newRemark = ref('');
const saving = ref(false);
const msg = ref('');

const whiteColumns = [
  { title: '用户 ID', key: 'userId' },
  { title: '备注', key: 'remark' },
  {
    title: '操作',
    key: 'actions',
    render: (r) => h(NButton, { size: 'small', onClick: () => remove(r.id) }, () => '删除'),
  },
];

async function load() {
  const q = await fetchAiQuota();
  globalLimit.value = q.globalDailyTokens;
  userLimit.value = q.userDailyTokens;
  globalUsed.value = q.globalUsed;
  whitelist.value = await fetchAiWhitelist();
}

async function save() {
  saving.value = true;
  msg.value = '';
  try {
    await saveAiQuota({ globalDailyTokens: globalLimit.value, userDailyTokens: userLimit.value });
    msg.value = '已保存';
    await load();
  } finally {
    saving.value = false;
  }
}

async function addWhite() {
  if (!newUserId.value) return;
  await addAiWhitelist({ userId: newUserId.value, remark: newRemark.value });
  newUserId.value = null;
  newRemark.value = '';
  await load();
}

async function remove(id) {
  await removeAiWhitelist(id);
  await load();
}

onMounted(() => load().catch(() => {}));
</script>
