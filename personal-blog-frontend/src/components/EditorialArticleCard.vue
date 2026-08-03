<template>
  <router-link
    :to="`/article/${article.id}`"
    class="editorial-card"
    :class="[`editorial-card--${mode}`, cover ? 'editorial-card--has-cover' : 'editorial-card--text-only']"
  >
    <div v-if="cover" class="editorial-card-media">
      <img :src="cover" :alt="article.title" loading="lazy" />
    </div>
    <div v-else-if="mode !== 'compact'" class="editorial-card-index" aria-hidden="true">{{ number }}</div>
    <div class="editorial-card-body">
      <div class="editorial-card-meta">
        <span v-if="cover || mode === 'compact'">{{ number }}</span>
        <span v-else class="editorial-card-meta-rule" aria-hidden="true"></span>
        <span>{{ date }}</span>
      </div>
      <h3>{{ article.title }}</h3>
      <p v-if="mode !== 'compact'">{{ excerpt }}</p>
      <div v-if="mode !== 'compact'" class="editorial-card-tags">
        <span v-for="tag in (article.tags || []).slice(0,3)" :key="tag.id">#{{ tag.name }}</span>
      </div>
    </div>
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
.editorial-card--featured.editorial-card--has-cover { grid-template-rows:auto 1fr; }
.editorial-card--featured.editorial-card--text-only { grid-template-columns:96px minmax(0,1fr); gap:28px; align-items:start; padding:24px 0 30px; border-top:1px solid var(--color-border); border-bottom:1px solid var(--color-border); }
.editorial-card--editorial { gap:20px; padding:18px 0; border-top:1px solid var(--color-border); }
.editorial-card--editorial.editorial-card--has-cover { grid-template-columns:minmax(150px,.85fr) minmax(0,1fr); }
.editorial-card--editorial.editorial-card--text-only { grid-template-columns:80px minmax(0,1fr); gap:24px; }
.editorial-card--compact { gap:12px; align-items:start; padding:12px 0; border-top:1px solid var(--color-border); }
.editorial-card--compact.editorial-card--has-cover { grid-template-columns:76px minmax(0,1fr); }
.editorial-card--compact.editorial-card--text-only { grid-template-columns:1fr; }
.editorial-card-media { overflow:hidden; background:var(--surface-muted); aspect-ratio:16/10; }
.editorial-card--featured .editorial-card-media { aspect-ratio:16/9; }
.editorial-card--compact .editorial-card-media { aspect-ratio:1; }
.editorial-card-media img { width:100%; height:100%; object-fit:cover; display:block; transition:transform .7s cubic-bezier(.2,.8,.2,1),filter .4s ease; }
.editorial-card:hover .editorial-card-media img { transform:scale(1.055); filter:saturate(1.08); }
.editorial-card-index { color:var(--color-accent); font:600 clamp(24px,3vw,42px)/1 var(--font-mono); padding-top:4px; transition:color var(--transition-fast),transform var(--transition-fast); }
.editorial-card:hover .editorial-card-index { color:var(--color-primary-hover); transform:translateX(3px); }
.editorial-card-body { min-width:0; padding-top:14px; }
.editorial-card--editorial .editorial-card-body,.editorial-card--compact .editorial-card-body { padding-top:0; }
.editorial-card-meta { display:flex; justify-content:space-between; gap:12px; color:var(--color-text-soft); font:11px var(--font-mono); letter-spacing:.04em; }
.editorial-card-meta-rule { width:28px; height:1px; margin-top:.55em; background:var(--color-accent); }
.editorial-card h3 { margin:10px 0 0; font-family:var(--font-display); font-size:clamp(18px,2.2vw,30px); line-height:1.22; letter-spacing:0; font-weight:600; transition:color .25s ease; }
.editorial-card--featured.editorial-card--text-only h3 { margin-top:14px; font-size:clamp(30px,4.6vw,58px); line-height:1.08; }
.editorial-card--compact h3 { font-size:15px; line-height:1.3; margin-top:7px; }
.editorial-card:hover h3 { color:var(--color-accent); }
.editorial-card p { margin:12px 0 0; color:var(--color-text-muted); font-family:var(--font-prose); font-size:14px; line-height:1.75; display:-webkit-box; -webkit-line-clamp:3; -webkit-box-orient:vertical; overflow:hidden; }
.editorial-card-tags { display:flex; gap:10px; flex-wrap:wrap; margin-top:16px; color:var(--color-text-soft); font:11px var(--font-mono); }
@media(max-width:700px){ .editorial-card--editorial.editorial-card--has-cover{grid-template-columns:110px minmax(0,1fr);gap:14px}.editorial-card--editorial.editorial-card--text-only{grid-template-columns:52px minmax(0,1fr);gap:14px}.editorial-card--featured.editorial-card--text-only{grid-template-columns:56px minmax(0,1fr);gap:16px}.editorial-card--featured h3,.editorial-card--featured.editorial-card--text-only h3{font-size:28px}.editorial-card p{font-size:13px}.editorial-card-index{font-size:22px} }

@media(prefers-reduced-motion:reduce){ .editorial-card-media img,.editorial-card-index{transition:none}.editorial-card:hover .editorial-card-media img,.editorial-card:hover .editorial-card-index{transform:none} }
</style>
