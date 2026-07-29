<template>
  <router-link :to="`/article/${article.id}`" class="editorial-card" :class="`editorial-card--${mode}`">
    <div class="editorial-card-media"><img v-if="cover" :src="cover" :alt="article.title" loading="lazy" /><div v-else class="editorial-card-placeholder"><span>{{ number }}</span></div></div>
    <div class="editorial-card-body"><div class="editorial-card-meta"><span>{{ number }}</span><span>{{ date }}</span></div><h3>{{ article.title }}</h3><p v-if="mode !== 'compact'">{{ excerpt }}</p><div v-if="mode !== 'compact'" class="editorial-card-tags"><span v-for="tag in (article.tags || []).slice(0,3)" :key="tag.id">#{{ tag.name }}</span></div></div>
  </router-link>
</template>
<script setup>
import { computed } from 'vue';
const props = defineProps({ article:{type:Object,required:true}, mode:{type:String,default:'editorial'}, number:{type:String,default:'01'} });
const cover = computed(() => props.article.cover || props.article.coverUrl || '');
const date = computed(() => { const d=props.article.createTime||props.article.createdAt; return d ? new Date(d).toLocaleDateString('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit'}) : ''; });
const excerpt = computed(() => { const text=(props.article.summary || props.article.content || '').replace(/\s+/g,' ').trim(); return text.length>130 ? `${text.slice(0,130)}...` : text; });
</script>
<style scoped>
.editorial-card { display:grid; color:var(--color-text); text-decoration:none; min-width:0; }
.editorial-card--featured { grid-template-rows:auto 1fr; }
.editorial-card--editorial { grid-template-columns: minmax(150px,.85fr) minmax(0,1fr); gap:20px; padding:18px 0; border-top:1px solid var(--color-border); }
.editorial-card--compact { grid-template-columns:76px minmax(0,1fr); gap:12px; align-items:start; padding:12px 0; border-top:1px solid var(--color-border); }
.editorial-card-media { overflow:hidden; background:var(--surface-muted); aspect-ratio:16/10; }
.editorial-card--featured .editorial-card-media { aspect-ratio:16/9; }
.editorial-card--compact .editorial-card-media { aspect-ratio:1; }
.editorial-card-media img { width:100%; height:100%; object-fit:cover; display:block; transition:transform .7s cubic-bezier(.2,.8,.2,1),filter .4s ease; }
.editorial-card:hover .editorial-card-media img { transform:scale(1.055); filter:saturate(1.08); }
.editorial-card-placeholder { height:100%; display:grid; place-items:center; color:var(--color-accent); background:linear-gradient(135deg,var(--color-surface-raised),var(--color-page)); font:700 28px var(--font-mono); }
.editorial-card-body { min-width:0; padding-top:14px; }
.editorial-card--editorial .editorial-card-body,.editorial-card--compact .editorial-card-body { padding-top:0; }
.editorial-card-meta { display:flex; justify-content:space-between; gap:12px; color:var(--color-text-soft); font:11px var(--font-mono); letter-spacing:.04em; }
.editorial-card h3 { margin:10px 0 0; font-size:clamp(18px,2.2vw,30px); line-height:1.12; letter-spacing:-.035em; font-weight:650; transition:color .25s ease; }
.editorial-card--compact h3 { font-size:15px; line-height:1.3; margin-top:7px; }
.editorial-card:hover h3 { color:var(--color-accent); }
.editorial-card p { margin:12px 0 0; color:var(--color-text-muted); font-size:14px; line-height:1.65; display:-webkit-box; -webkit-line-clamp:3; -webkit-box-orient:vertical; overflow:hidden; }
.editorial-card-tags { display:flex; gap:10px; flex-wrap:wrap; margin-top:16px; color:var(--color-text-soft); font:11px var(--font-mono); }
@media(max-width:700px){ .editorial-card--editorial{grid-template-columns:120px minmax(0,1fr);gap:14px}.editorial-card--featured h3{font-size:28px}.editorial-card p{font-size:13px} }
</style>
