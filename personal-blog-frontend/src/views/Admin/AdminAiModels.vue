<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">AI 模型管理</h1>
          <p class="ds-page-sub">查看模型列表、健康状态与启停，支持添加/删除</p>
        </div>
        <n-space :size="8">
          <n-button type="primary" @click="openAddModal">添加模型</n-button>
          <router-link to="/admin/ai-dashboard"><n-button>AI 监控</n-button></router-link>
          <router-link to="/admin"><n-button>返回管理</n-button></router-link>
        </n-space>
      </header>

      <n-alert v-if="err" type="error">{{ err }}</n-alert>

      <n-card title="模型列表">
        <n-data-table :columns="columns" :data="models" :bordered="false" />
      </n-card>

      <n-modal v-model:show="showAddModal" preset="dialog" title="添加模型" :show-icon="false"
               style="width: 520px;" :mask-closable="false">
        <n-form label-placement="left" label-width="90">
          <n-form-item label="Provider ID">
            <n-input v-model:value="form.providerId" placeholder="如 deepseek" />
          </n-form-item>
          <n-form-item label="名称">
            <n-input v-model:value="form.name" placeholder="可选，默认同 Provider ID" />
          </n-form-item>
          <n-form-item label="API Key">
            <n-input v-model:value="form.apiKey" type="password" placeholder="sk-xxx" />
          </n-form-item>
          <n-form-item label="Base URL">
            <n-input v-model:value="form.baseUrl" placeholder="https://api.deepseek.com" />
          </n-form-item>
          <n-form-item label="模型列表">
            <n-input v-model:value="form.models" placeholder="多个用逗号分隔，如 deepseek-v4-flash,deepseek-v4-pro" />
          </n-form-item>
          <n-form-item label="优先级">
            <n-input-number v-model:value="form.priority" :min="0" style="width: 100%" />
          </n-form-item>
          <n-form-item label="启用">
            <n-switch v-model:value="form.enabled" />
          </n-form-item>
        </n-form>
        <template #action>
          <n-space>
            <n-button @click="showAddModal = false">取消</n-button>
            <n-button type="primary" :loading="submitting" @click="onAdd">创建</n-button>
          </n-space>
        </template>
      </n-modal>
    </div>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue';
import { NAlert, NButton, NCard, NDataTable, NSpace, NTag, NSwitch,
         NModal, NForm, NFormItem, NInput, NInputNumber, useMessage } from 'naive-ui';
import { fetchAiModels, fetchAiModelHealth, updateAiModelEnabled,
         createAiModel, deleteAiModel } from '../../api/aiGateway';

const message = useMessage();
const models = ref([]);
const healthMap = ref({});
const err = ref('');
const showAddModal = ref(false);
const submitting = ref(false);
const form = ref(defaultForm());

function defaultForm() {
  return { providerId: '', name: '', apiKey: '', baseUrl: '', models: '', priority: 1, enabled: true };
}

const columns = [
  { title: 'Provider', key: 'providerId' },
  { title: 'Model', key: 'model', render: (r) => r.model || (r.models ? r.models.join(', ') : '—') },
  { title: '优先级', key: 'priority', width: 80 },
  {
    title: '健康',
    key: 'health',
    width: 100,
    render: (r) => {
      const st = healthMap.value[r.providerId] || r.health || 'UNKNOWN';
      const type = st === 'HEALTHY' ? 'success' : st === 'HALF_OPEN' ? 'warning' : 'error';
      return h(NTag, { type, size: 'small' }, () => st);
    },
  },
  {
    title: '启用',
    key: 'enabled',
    width: 80,
    render: (r) => {
      if (!r.id) return '—';
      return h(NSwitch, {
        value: !!r.enabled,
        onUpdateValue: (v) => toggle(r.id, v),
      });
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    render: (r) => {
      if (!r.id) return '—';
      return h(NButton, {
        size: 'small',
        type: 'error',
        secondary: true,
        onClick: () => onDelete(r.id),
      }, () => '删除');
    },
  },
];

function openAddModal() {
  form.value = defaultForm();
  showAddModal.value = true;
}

async function onAdd() {
  if (!form.value.providerId || !form.value.apiKey || !form.value.baseUrl || !form.value.models) {
    message.warning('Provider ID、API Key、Base URL、模型列表为必填');
    return;
  }
  submitting.value = true;
  try {
    await createAiModel({ ...form.value });
    message.success('已添加');
    showAddModal.value = false;
    await load();
  } catch (e) {
    message.error(e?.message || '添加失败');
  } finally {
    submitting.value = false;
  }
}

async function onDelete(id) {
  if (!confirm('确定要删除这个模型吗？删除后立即失效。')) return;
  try {
    await deleteAiModel(id);
    message.success('已删除');
    await load();
  } catch (e) {
    message.error(e?.message || '删除失败');
  }
}

async function toggle(id, enabled) {
  try {
    await updateAiModelEnabled(id, enabled);
    await load();
  } catch (e) {
    err.value = e?.message || '更新失败';
  }
}

async function load() {
  const [m, h] = await Promise.all([fetchAiModels(), fetchAiModelHealth()]);
  models.value = m || [];
  const map = {};
  (h || []).forEach((item) => { map[item.providerId] = item.status; });
  healthMap.value = map;
}

onMounted(async () => {
  try {
    await load();
  } catch (e) {
    err.value = e?.message || '加载失败';
  }
});
</script>
