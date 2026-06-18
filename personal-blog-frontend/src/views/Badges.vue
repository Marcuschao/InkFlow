<template>
  <div class="badges-page ds-page container">
    <h1 class="page-title">徽章墙</h1>
    <n-skeleton v-if="loading" height="320px" :sharp="false" />
    <n-grid v-else :cols="gridCols" :x-gap="16" :y-gap="16">
      <n-gi v-for="b in badges" :key="b.id">
        <n-card class="badge-card" :class="{ earned: b.earned, locked: !b.earned }" size="small">
          <div class="badge-card-inner">
            <img v-if="b.iconUrl" :src="b.iconUrl" :alt="b.name" class="badge-lg" />
            <span v-else class="badge-lg-fallback">{{ b.name?.charAt(0) }}</span>
            <h3 class="badge-card-name">{{ b.name }}</h3>
            <p class="badge-card-desc muted">{{ b.description }}</p>
            <n-tag v-if="b.earned" size="small" type="success">已获得</n-tag>
            <n-tag v-else size="small">未获得</n-tag>
            <n-upload
              v-if="authStore.isAdmin"
              :show-file-list="false"
              accept="image/*"
              :custom-request="(options) => uploadIcon(b, options)"
            >
              <n-button size="tiny" :loading="uploadingId === b.id">上传图片</n-button>
            </n-upload>
          </div>
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useHead } from '@vueuse/head';
import { NButton, NCard, NGi, NGrid, NSkeleton, NTag, NUpload } from 'naive-ui';
import { getBadges } from '../api/social';
import { uploadBadgeIcon } from '../api/upload';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';

useHead({ title: '徽章墙' });

const loading = ref(true);
const uploadingId = ref(null);
const badges = ref([]);
const authStore = useAuthStore();
const toast = useToastStore();

const gridCols = computed(() => (window.innerWidth < 768 ? 2 : 4));

onMounted(async () => {
  try {
    const res = await getBadges();
    badges.value = res.data?.badges || [];
  } catch {
    badges.value = [];
  } finally {
    loading.value = false;
  }
});

async function uploadIcon(badge, { file, onFinish, onError }) {
  uploadingId.value = badge.id;
  try {
    const res = await uploadBadgeIcon(badge.id, file.file);
    badge.iconUrl = res.data;
    toast.push('上传成功', 'success');
    onFinish();
  } catch {
    onError();
  } finally {
    uploadingId.value = null;
  }
}
</script>

<style scoped>
.badges-page {
  padding-bottom: var(--space-16);
}

.page-title {
  margin: 0 0 var(--space-6);
  font-size: var(--text-2xl);
  font-weight: var(--weight-semibold);
}

.badge-card {
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-card);
  text-align: center;
  background: var(--color-surface) !important;
  transition: transform var(--transition-smooth), box-shadow var(--transition-smooth);
}

.badge-card.earned:hover {
  border-color: var(--color-border-strong);
}

.badge-card.locked {
  opacity: 0.45;
}

.badge-card-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.badge-lg {
  width: 64px;
  height: 64px;
  object-fit: contain;
  display: block;
}

.badge-lg-fallback {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-muted);
  color: var(--color-text-muted);
  border-radius: var(--radius-md);
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
}

.badge-card-name {
  margin: 0;
  font-size: var(--text-base);
}

.badge-card-desc {
  margin: 0;
  font-size: var(--text-sm);
}

.muted {
  color: var(--color-text-muted);
}
</style>
