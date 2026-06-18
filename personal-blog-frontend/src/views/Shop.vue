<template>
  <div class="shop-page ds-page container">
    <header class="shop-head">
      <div>
        <p class="shop-kicker">积分商城</p>
        <h1 class="shop-title">兑换虚拟道具</h1>
      </div>
      <router-link class="shop-bag-link" to="/user/me?tab=inventory">我的背包</router-link>
    </header>

    <n-skeleton v-if="loading" height="12rem" :sharp="false" />
    <n-empty v-else-if="!items.length" description="暂无可兑换道具" />
    <n-grid v-else :cols="1" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <n-gi v-for="item in items" :key="item.id" span="24 m:12 l:8">
        <n-card class="shop-card ds-brutal-surface">
          <div class="shop-card-top">
            <div class="shop-icon" aria-hidden="true">
              <img v-if="item.iconUrl" :src="item.iconUrl" :alt="item.name" class="shop-icon-img" />
              <span v-else>{{ (item.name || '道').slice(0, 1) }}</span>
            </div>
            <div>
              <h2 class="shop-item-name">{{ item.name }}</h2>
              <p class="shop-desc">{{ item.description }}</p>
            </div>
          </div>
          <n-space class="shop-meta" :size="8">
            <n-tag type="warning" :bordered="false">{{ item.price }} 积分</n-tag>
            <n-tag :bordered="false">{{ durationLabel(item) }}</n-tag>
            <n-tag v-if="item.owned" type="success" :bordered="false">已拥有</n-tag>
          </n-space>
          <n-button
            class="shop-buy"
            type="primary"
            block
            :disabled="item.owned || !item.affordable"
            :loading="buyingId === item.id"
            @click="buy(item)"
          >
            {{ buttonText(item) }}
          </n-button>
          <n-upload
            v-if="authStore.isAdmin"
            class="shop-upload"
            :show-file-list="false"
            accept="image/*"
            :custom-request="(options) => uploadIcon(item, options)"
          >
            <n-button size="small" block :loading="uploadingId === item.id">上传图片</n-button>
          </n-upload>
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { NButton, NCard, NEmpty, NGi, NGrid, NSkeleton, NSpace, NTag, NUpload } from 'naive-ui';
import { buyItem, fetchShopItems } from '../api/shop';
import { uploadItemIcon } from '../api/upload';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';

const toast = useToastStore();
const authStore = useAuthStore();
const loading = ref(false);
const buyingId = ref(null);
const uploadingId = ref(null);
const items = ref([]);

function durationLabel(item) {
  if (!item.durationDays) return '永久';
  return `${item.durationDays} 天`;
}

function buttonText(item) {
  if (item.owned) return '已拥有';
  if (!item.affordable) return '积分不足';
  return '兑换';
}

async function loadItems() {
  loading.value = true;
  try {
    const res = await fetchShopItems();
    items.value = Array.isArray(res.data) ? res.data : [];
  } catch {
    items.value = [];
  } finally {
    loading.value = false;
  }
}

async function buy(item) {
  buyingId.value = item.id;
  try {
    await buyItem(item.id);
    toast.push('兑换成功', 'success');
    await loadItems();
  } catch {
    /* request 已 toast */
  } finally {
    buyingId.value = null;
  }
}

async function uploadIcon(item, { file, onFinish, onError }) {
  uploadingId.value = item.id;
  try {
    const res = await uploadItemIcon(item.id, file.file);
    item.iconUrl = res.data;
    toast.push('上传成功', 'success');
    onFinish();
  } catch {
    onError();
  } finally {
    uploadingId.value = null;
  }
}

onMounted(loadItems);
</script>

<style scoped>
.shop-page {
  padding-bottom: var(--space-16);
}

.shop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin: 0 auto var(--space-6);
  max-width: 64rem;
}

.shop-kicker {
  margin: 0 0 var(--space-1);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.shop-title {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: var(--weight-black);
}

.shop-bag-link {
  color: var(--color-text);
  text-decoration: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-2) var(--space-3);
}

.shop-card {
  height: 100%;
}

.shop-card-top {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.shop-icon {
  width: var(--space-12);
  height: var(--space-12);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  display: grid;
  place-items: center;
  font-weight: var(--weight-bold);
  color: var(--color-primary);
  background: var(--color-surface);
}

.shop-icon-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.shop-item-name {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
}

.shop-desc {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.shop-meta {
  margin: var(--space-4) 0;
}

.shop-buy {
  margin-top: auto;
}

.shop-upload {
  margin-top: var(--space-2);
}
</style>
