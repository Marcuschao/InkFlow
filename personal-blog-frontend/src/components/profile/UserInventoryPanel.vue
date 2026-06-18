<template>
  <section class="inventory-panel">
    <div class="inventory-head">
      <p class="muted">管理已兑换道具，同一类型只能装备一个。</p>
      <router-link class="inventory-shop-link" to="/shop">去商城</router-link>
    </div>
    <n-skeleton v-if="loading" height="8rem" :sharp="false" />
    <n-empty v-else-if="!items.length" description="暂无道具" />
    <n-list v-else bordered>
      <n-list-item v-for="item in items" :key="item.id">
        <div class="inventory-row">
          <div>
            <h3 class="inventory-name">{{ item.name }}</h3>
            <p class="inventory-desc">{{ item.description }}</p>
            <p class="inventory-time">{{ expireLabel(item) }}</p>
          </div>
          <n-space :size="8" align="center">
            <n-tag :type="tagType(item)" :bordered="false">{{ statusLabel(item.status) }}</n-tag>
            <n-button
              v-if="item.status === 'ACTIVE'"
              size="small"
              :loading="operatingId === item.id"
              @click="unequip(item.id)"
            >
              卸下
            </n-button>
            <n-button
              v-else-if="item.status === 'UNUSED'"
              size="small"
              type="primary"
              :loading="operatingId === item.id"
              @click="equip(item.id)"
            >
              装备
            </n-button>
          </n-space>
        </div>
      </n-list-item>
    </n-list>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { NButton, NEmpty, NList, NListItem, NSkeleton, NSpace, NTag } from 'naive-ui';
import { equipUserItem, fetchUserItems, unequipUserItem } from '../../api/userItems';
import { useToastStore } from '../../stores/toast';

const emit = defineEmits(['changed']);
const toast = useToastStore();
const loading = ref(false);
const operatingId = ref(null);
const items = ref([]);

function statusLabel(status) {
  if (status === 'ACTIVE') return '使用中';
  if (status === 'EXPIRED') return '已过期';
  return '未使用';
}

function tagType(item) {
  if (item.status === 'ACTIVE') return 'success';
  if (item.status === 'EXPIRED') return 'error';
  return 'default';
}

function expireLabel(item) {
  if (!item.expireTime) return '永久有效';
  return `有效期至 ${new Date(item.expireTime).toLocaleString()}`;
}

async function loadItems() {
  loading.value = true;
  try {
    const res = await fetchUserItems();
    items.value = Array.isArray(res.data) ? res.data : [];
  } catch {
    items.value = [];
  } finally {
    loading.value = false;
  }
}

async function equip(id) {
  operatingId.value = id;
  try {
    await equipUserItem(id);
    toast.push('已装备', 'success');
    await loadItems();
    emit('changed');
  } catch {
    /* request 已 toast */
  } finally {
    operatingId.value = null;
  }
}

async function unequip(id) {
  operatingId.value = id;
  try {
    await unequipUserItem(id);
    toast.push('已卸下', 'success');
    await loadItems();
    emit('changed');
  } catch {
    /* request 已 toast */
  } finally {
    operatingId.value = null;
  }
}

onMounted(loadItems);
</script>

<style scoped>
.inventory-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.inventory-shop-link {
  color: var(--color-primary);
  font-weight: var(--weight-semibold);
  text-decoration: none;
}

.inventory-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  width: 100%;
}

.inventory-name {
  margin: 0;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
}

.inventory-desc,
.inventory-time,
.muted {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

@media (max-width: 767px) {
  .inventory-row,
  .inventory-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
