<template>
  <EditorialPageShell wide>
    <div class="magazine-topline">
      <span>{{ siteStore.siteTitle || '个人技术期刊' }}</span>
      <span>VOL. I · NOTES ON SOFTWARE &amp; LIFE</span>
      <span>{{ today }}</span>
    </div>

    <section class="magazine-intro" aria-labelledby="magazine-title">
      <p class="magazine-overline">A PERSONAL JOURNAL OF COMPUTING</p>
      <h1 id="magazine-title">记录技术，<em>也记录正在发生的生活。</em></h1>
      <p class="magazine-deck">关于代码、系统、产品与日常思考的长期笔记。慢一点阅读，认真一点留下证据。</p>
    </section>

    <section v-if="articles.length" class="magazine-featured" aria-label="精选文章">
      <EditorialArticleCard :article="articles[0]" mode="featured" number="01" />
      <div class="magazine-featured-side">
        <EditorialArticleCard v-for="(article, index) in articles.slice(1, 3)" :key="article.id" :article="article" mode="editorial" :number="`0${index + 2}`" />
      </div>
    </section>

    <section class="magazine-columns">
      <main>
        <EditorialSectionHeader index="02" eyebrow="LATEST DISPATCHES" title="最新文章" description="按时间顺序，浏览最近写下的内容。" />
        <TransitionGroup v-if="articles.length > 3" name="feed-article" tag="div" class="magazine-list">
          <EditorialArticleCard v-for="(article, index) in articles.slice(3)" :key="article.id" :article="article" mode="editorial" :number="String(index + 4).padStart(2, '0')" />
        </TransitionGroup>
        <p v-else-if="!loading && !articles.length" class="magazine-empty">暂无公开文章。</p>

        <div ref="loadSentinel" class="magazine-load-more" aria-live="polite">
          <div v-if="loading" class="magazine-loading" role="status">
            <span></span><span></span><span></span>
            <p>正在载入后续文章</p>
          </div>
          <div v-else-if="loadError" class="magazine-load-error">
            <p>{{ loadError }}</p>
            <button type="button" @click="loadNextPage">重新加载</button>
          </div>
          <p v-else-if="articles.length && !hasMore" class="magazine-list-end">
            <span>已阅至此</span>
          </p>
          <span v-else class="magazine-load-anchor" aria-hidden="true"></span>
        </div>
      </main>
      <aside>
        <div class="magazine-aside-block">
          <EditorialSectionHeader index="03" eyebrow="THE EDITOR" title="关于这里" />
          <p>这是一个不追逐热点的个人博客。写下实践，也保留一些尚未完成的想法。</p>
          <router-link to="/about" class="magazine-text-link">阅读站点介绍 <span>→</span></router-link>
        </div>
        <div class="magazine-aside-block">
          <EditorialSectionHeader index="04" eyebrow="RECENTLY" title="最近发布" />
          <EditorialArticleCard v-for="(article, index) in articles.slice(0, 5)" :key="`recent-${article.id}`" :article="article" mode="compact" :number="String(index + 1).padStart(2, '0')" />
        </div>
      </aside>
    </section>
  </EditorialPageShell>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import EditorialPageShell from '../components/EditorialPageShell.vue';
import EditorialSectionHeader from '../components/EditorialSectionHeader.vue';
import EditorialArticleCard from '../components/EditorialArticleCard.vue';
import { useSiteStore } from '../stores/site';
import { useArticleStore } from '../stores/article';

const siteStore = useSiteStore();
const articleStore = useArticleStore();
const articles = computed(() => articleStore.homeFeed.records);
const loadSentinel = ref(null);
const loading = computed(() => articleStore.homeFeed.loading);
const loadError = computed(() => articleStore.homeFeed.error);
const hasMore = computed(() => articleStore.homeFeed.hasMore);
let loadObserver = null;
const today = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }));

const loadNextPage = async () => {
  await articleStore.loadHomeNextPage(10);
};

const startInfiniteLoading = () => {
  if (!loadSentinel.value || typeof IntersectionObserver === 'undefined') return;
  loadObserver = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) loadNextPage();
    },
    { rootMargin: '500px 0px', threshold: 0.01 },
  );
  loadObserver.observe(loadSentinel.value);
};

onMounted(async () => {
  if (!articleStore.homeFeed.initialized) await loadNextPage();
  await nextTick();
  startInfiniteLoading();
  const restoreY = articleStore.consumeHomePosition();
  if (restoreY > 0) {
    requestAnimationFrame(() => window.scrollTo({ top: restoreY, left: 0, behavior: 'auto' }));
  }
});

onBeforeRouteLeave((to) => {
  articleStore.rememberHomePosition(window.scrollY, to.name === 'ArticleDetail');
});

onUnmounted(() => {
  loadObserver?.disconnect();
  loadObserver = null;
});
</script>

<style scoped>
.magazine-topline { display:flex; justify-content:space-between; gap:16px; border-bottom:1px solid var(--color-border); padding-bottom:12px; color:var(--color-text-soft); font:10px var(--font-mono); letter-spacing:.12em; text-transform:uppercase; }
.magazine-intro { padding:clamp(48px, 7vw, 82px) 0 clamp(48px, 6vw, 68px); border-bottom:1px solid var(--color-border); }
.magazine-overline { margin:0 0 20px; color:var(--color-accent-text,var(--color-accent)); font:11px var(--font-mono); letter-spacing:.18em; animation:magazine-intro-in .42s var(--ease-out-soft) both; }
.magazine-intro h1 { max-width:900px; margin:0; color:var(--color-text); font:600 clamp(48px, 7vw, 90px)/1.02 var(--font-display); letter-spacing:0; animation:magazine-intro-in .46s .07s var(--ease-out-soft) both; }
.magazine-intro h1 em { color:var(--color-accent); font-style:italic; }
.magazine-deck { max-width:500px; margin:24px 0 0; color:var(--color-text-muted); font:18px/1.7 var(--font-prose); animation:magazine-intro-in .46s .14s var(--ease-out-soft) both; }
.magazine-featured { display:grid; grid-template-columns:minmax(0,1.4fr) minmax(300px,.75fr); gap:44px; padding:48px 0 80px; }
.magazine-featured-side { display:flex; flex-direction:column; }
.magazine-columns { display:grid; grid-template-columns:minmax(0,1fr) 300px; gap:72px; }
.magazine-aside-block { padding-bottom:40px; }
.magazine-aside-block p { margin:0 0 20px; color:var(--color-text-muted); font:15px/1.8 var(--font-prose); }
.magazine-text-link { color:var(--color-text); text-decoration:none; font:11px var(--font-mono); letter-spacing:.08em; border-bottom:1px solid var(--color-accent); padding-bottom:5px; }
.magazine-list :deep(.editorial-card:first-child) { border-top:0; padding-top:0; }
.feed-article-enter-active { transition:opacity .42s var(--ease-out-soft),transform .42s var(--ease-out-soft); }
.feed-article-enter-from { opacity:0; transform:translateY(8px); }
.magazine-empty { color:var(--color-text-muted); font-family:var(--font-prose); }
.magazine-load-more { min-height:108px; display:grid; place-items:center; border-top:1px solid var(--color-border); }
.magazine-loading { display:flex; align-items:center; justify-content:center; gap:7px; color:var(--color-text-soft); }
.magazine-loading span { width:5px; height:5px; border-radius:50%; background:var(--color-accent); animation:magazine-loading-pulse 1.2s ease-in-out infinite; }
.magazine-loading span:nth-child(2) { animation-delay:.15s; }
.magazine-loading span:nth-child(3) { animation-delay:.3s; }
.magazine-loading p { margin:0 0 0 7px; font:10px var(--font-mono); letter-spacing:.12em; }
.magazine-load-error { text-align:center; color:var(--color-text-muted); }
.magazine-load-error p { margin:0 0 12px; font:14px/1.6 var(--font-prose); }
.magazine-load-error button { border:0; border-bottom:1px solid var(--color-accent); padding:4px 0; color:var(--color-text); background:transparent; font:11px var(--font-mono); cursor:pointer; }
.magazine-load-error button:focus-visible { outline:2px solid var(--color-accent); outline-offset:4px; }
.magazine-list-end { width:100%; display:flex; align-items:center; gap:14px; margin:0; color:var(--color-text-soft); font:10px var(--font-mono); letter-spacing:.14em; white-space:nowrap; }
.magazine-list-end::before,.magazine-list-end::after { content:''; height:1px; flex:1; background:var(--color-border); }
.magazine-load-anchor { width:1px; height:1px; }
@keyframes magazine-loading-pulse { 0%,100%{ opacity:.25; transform:translateY(0) } 50%{ opacity:1; transform:translateY(-3px) } }
@keyframes magazine-intro-in { from{opacity:0;transform:translateY(9px)} to{opacity:1;transform:translateY(0)} }
@media(prefers-reduced-motion:reduce){ .magazine-loading span{animation:none;opacity:.75} }
@media(prefers-reduced-motion:reduce){ .feed-article-enter-active{transition:none}.feed-article-enter-from{opacity:1;transform:none}.magazine-overline,.magazine-intro h1,.magazine-deck{animation:none} }
@media(max-width:900px){ .magazine-featured,.magazine-columns{grid-template-columns:1fr;gap:44px}.magazine-featured-side{display:grid;grid-template-columns:1fr 1fr;gap:24px}.magazine-columns aside{display:grid;grid-template-columns:1fr 1fr;gap:32px} }
@media(max-width:600px){ .magazine-topline span:nth-child(2){display:none}.magazine-intro{padding:42px 0 46px}.magazine-intro h1{font-size:clamp(38px,12vw,48px)}.magazine-deck{font-size:16px}.magazine-featured{padding:32px 0 56px}.magazine-featured-side,.magazine-columns aside{display:block}.magazine-columns{gap:52px} }
</style>
