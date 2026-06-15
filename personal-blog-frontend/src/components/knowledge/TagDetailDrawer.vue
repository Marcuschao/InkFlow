<template>
  <n-drawer v-model:show="visible" :width="360" placement="right">
    <n-drawer-content :title="detail?.name || '标签详情'" closable>
      <n-spin :show="loading">
        <n-space v-if="detail" vertical :size="16">
          <div class="meta-row">
            <span class="muted">{{ detail.articleCount || 0 }} 篇文章</span>
            <n-button
              v-if="authStore.isLoggedIn"
              size="small"
              :type="detail.subscribed ? 'default' : 'primary'"
              @click="toggleSubscribe"
            >{{ detail.subscribed ? '取消订阅' : '订阅' }}</n-button>
          </div>
          <div v-if="detail.relatedTags?.length">
            <h4 class="section-title">关联标签</h4>
            <n-space :size="6">
              <n-tag
                v-for="t in detail.relatedTags"
                :key="t.tagId"
                size="small"
                checkable
                @click="emit('select-tag', t.tagId)"
              >{{ t.name }}</n-tag>
            </n-space>
          </div>
          <div v-if="detail.articles?.length">
            <h4 class="section-title">相关文章</h4>
            <n-space vertical :size="12">
              <ArticleCard
                v-for="a in detail.articles"
                :key="a.id"
                :article="a"
              />
            </n-space>
          </div>
          <n-empty v-else description="暂无文章" size="small" />
        </n-space>
      </n-spin>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { ref, watch } from 'vue';
import { NButton, NDrawer, NDrawerContent, NEmpty, NSpace, NSpin, NTag } from 'naive-ui';
import { getKnowledgeNode, subscribeTag, unsubscribeTag } from '../../api/knowledge';
import { useAuthStore } from '../../stores/auth';
import ArticleCard from '../ArticleCard.vue';

const props = defineProps({
  tagId: { type: Number, default: null },
  show: { type: Boolean, default: false },
});

const emit = defineEmits(['update:show', 'select-tag']);

const authStore = useAuthStore();
const visible = ref(false);
const loading = ref(false);
const detail = ref(null);

watch(
  () => props.show,
  (v) => {
    visible.value = v;
    if (v && props.tagId) loadDetail(props.tagId);
  }
);

watch(visible, (v) => emit('update:show', v));

watch(
  () => props.tagId,
  (id) => {
    if (visible.value && id) loadDetail(id);
  }
);

async function loadDetail(tagId) {
  loading.value = true;
  try {
    const res = await getKnowledgeNode(tagId);
    detail.value = res.data;
  } catch {
    detail.value = null;
  } finally {
    loading.value = false;
  }
}

async function toggleSubscribe() {
  if (!props.tagId) return;
  try {
    if (detail.value?.subscribed) {
      await unsubscribeTag(props.tagId);
      detail.value = { ...detail.value, subscribed: false };
    } else {
      await subscribeTag(props.tagId);
      detail.value = { ...detail.value, subscribed: true };
    }
  } catch {
    /* ignore */
  }
}
</script>

<style scoped>
.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.section-title {
  margin: 0 0 var(--space-2);
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text);
}

.muted {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}
</style>
