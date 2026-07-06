<template>
  <div class="admin-knowledge-page admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header" style="margin-bottom: 24px;">
        <div>
          <h1 class="ds-page-title">知识库</h1>
          <p class="ds-page-sub">上传文档供 AI 问答检索（PDF / Word / Markdown / TXT 等）</p>
        </div>
        <router-link to="/admin">
          <n-button>返回管理</n-button>
        </router-link>
      </header>

      <n-alert v-if="loadErr" type="error" style="margin-bottom: 16px;">{{ loadErr }}</n-alert>

      <n-grid :cols="1" :x-gap="24" :y-gap="24" responsive="screen" item-responsive>
        <n-gi span="24 m:8">
          <n-card title="上传文档">
            <n-upload
              :custom-request="handleUpload"
              :show-file-list="false"
              :disabled="uploading"
              accept=".pdf,.doc,.docx,.md,.markdown,.txt,.html,.htm,.ppt,.pptx,.xls,.xlsx"
            >
              <n-upload-dragger>
                <div style="padding: 12px 0;">
                  <p>点击或拖拽文件到此处</p>
                  <p style="color: var(--color-text-muted); font-size: 14px; margin-top: 8px;">
                    上传后自动解析、分块并向量化
                  </p>
                </div>
              </n-upload-dragger>
            </n-upload>
            <n-alert v-if="uploadMsg" type="success" style="margin-top: 16px;">{{ uploadMsg }}</n-alert>
            <n-alert v-if="uploadErr" type="error" style="margin-top: 16px;">{{ uploadErr }}</n-alert>
          </n-card>
        </n-gi>

        <n-gi span="24 m:16">
          <n-card>
            <n-space style="margin-bottom: 16px;" :size="12" align="center">
              <n-select
                v-model:value="statusFilter"
                :options="statusOptions"
                placeholder="全部状态"
                clearable
                style="width: 160px;"
                @update:value="onFilterChange"
              />
              <n-button :loading="loading" @click="load">刷新</n-button>
            </n-space>
            <n-data-table
              :columns="columns"
              :data="rows"
              :loading="loading"
              :bordered="false"
              :single-line="false"
              :scroll-x="720"
            />
            <n-empty v-if="!rows.length && !loading" description="暂无文档" />
            <Pagination
              v-if="total > pageSize"
              :total="total"
              :page-size="pageSize"
              :current-page="page"
              @changePage="onPage"
            />
          </n-card>
        </n-gi>
      </n-grid>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, h } from 'vue';
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NEmpty,
  NGi,
  NGrid,
  NSelect,
  NSpace,
  NTag,
  NUpload,
  NUploadDragger,
} from 'naive-ui';
import Pagination from '../../components/Pagination.vue';
import {
  deleteKnowledgeDocument,
  fetchKnowledgeDocuments,
  uploadKnowledgeDocument,
} from '../../api/adminKnowledge';
import { formatShortDateTime } from '../../utils/format';

const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const loading = ref(false);
const loadErr = ref('');
const uploading = ref(false);
const uploadMsg = ref('');
const uploadErr = ref('');
const statusFilter = ref(null);
let pollTimer = null;

const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '失败', value: 'FAILED' },
];

const statusMap = {
  PENDING: { label: '待处理', type: 'default' },
  PROCESSING: { label: '处理中', type: 'info' },
  COMPLETED: { label: '已完成', type: 'success' },
  FAILED: { label: '失败', type: 'error' },
};

const columns = [
  { title: 'ID', key: 'id', width: 64 },
  { title: '标题', key: 'title', minWidth: 160, ellipsis: { tooltip: true } },
  { title: '类型', key: 'fileType', width: 72 },
  {
    title: '状态',
    key: 'status',
    width: 96,
    render(row) {
      const s = statusMap[row.status] || { label: row.status || '—', type: 'default' };
      return h(NTag, { type: s.type, size: 'small', title: row.errorMsg || undefined }, () => s.label);
    },
  },
  {
    title: '分块数',
    key: 'chunkCount',
    width: 72,
    render: (row) => row.chunkCount ?? '—',
  },
  {
    title: '上传时间',
    key: 'createTime',
    width: 120,
    render: (row) => formatShortDateTime(row.createTime),
  },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    fixed: 'right',
    render(row) {
      return h(
        NButton,
        {
          size: 'small',
          type: 'error',
          secondary: true,
          onClick: () => confirmDelete(row.id),
        },
        () => '删除'
      );
    },
  },
];

async function load(silent = false) {
  if (!silent) loading.value = true;
  loadErr.value = '';
  try {
    const pr = await fetchKnowledgeDocuments({
      page: page.value,
      size: pageSize.value,
      status: statusFilter.value || undefined,
    });
    rows.value = pr?.records || [];
    total.value = Number(pr?.total) || 0;
    schedulePoll();
  } catch (e) {
    loadErr.value = e?.message || '加载失败';
    rows.value = [];
    total.value = 0;
  } finally {
    if (!silent) loading.value = false;
  }
}

function schedulePoll() {
  clearPoll();
  const pending = rows.value.some((r) => r.status === 'PENDING' || r.status === 'PROCESSING');
  if (pending) {
    pollTimer = setTimeout(() => load(true), 4000);
  }
}

function clearPoll() {
  if (pollTimer) {
    clearTimeout(pollTimer);
    pollTimer = null;
  }
}

function onPage(p) {
  page.value = p;
  load();
}

function onFilterChange() {
  page.value = 1;
  load();
}

async function handleUpload({ file, onFinish, onError }) {
  uploadMsg.value = '';
  uploadErr.value = '';
  uploading.value = true;
  try {
    await uploadKnowledgeDocument(file.file);
    uploadMsg.value = '上传成功，正在后台处理';
    page.value = 1;
    await load();
    onFinish();
  } catch (e) {
    uploadErr.value = e?.message || '上传失败';
    onError();
  } finally {
    uploading.value = false;
  }
}

async function confirmDelete(id) {
  if (!confirm('确定删除该文档？索引分块将一并移除。')) return;
  try {
    await deleteKnowledgeDocument(id);
    await load();
  } catch (e) {
    loadErr.value = e?.message || '删除失败';
  }
}

onMounted(() => load());
onUnmounted(clearPoll);
</script>
