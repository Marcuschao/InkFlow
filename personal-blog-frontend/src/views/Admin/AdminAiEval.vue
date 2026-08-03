<template>
  <div class="eval-page">
    <header class="eval-header">
      <div><span class="eyebrow">AI QUALITY / RAG</span><h1>AI 评测</h1><p>用固定问题验证检索、引用与拒答表现。</p></div>
      <n-space><n-button @click="loadAll">刷新</n-button><n-button type="primary" @click="openDataset()">新建评测集</n-button></n-space>
    </header>

    <n-tabs v-model:value="tab" type="line">
      <n-tab-pane name="datasets" tab="评测集">
        <div class="eval-layout">
          <section class="dataset-list">
            <button v-for="d in datasets" :key="d.id" class="dataset-row" :class="{active:selected?.id===d.id}" @click="selectDataset(d)">
              <span><strong>{{ d.name }}</strong><small>{{ d.description || '暂无说明' }}</small></span>
              <n-tag size="small" :type="d.enabled ? 'success' : 'default'">{{ d.enabled ? '启用' : '停用' }}</n-tag>
            </button>
            <n-empty v-if="!datasets.length" description="还没有评测集" />
          </section>
          <section class="case-workspace">
            <template v-if="selected">
              <div class="section-toolbar"><div><h2>{{ selected.name }}</h2><span>{{ cases.length }} 条用例</span></div><n-space><n-button size="small" @click="openDataset(selected)">编辑</n-button><n-button size="small" @click="jsonlOpen=true">导入 JSONL</n-button><n-button size="small" @click="openCase()">添加用例</n-button><n-button size="small" type="primary" :loading="running" @click="runDataset">运行评测</n-button></n-space></div>
              <n-data-table :columns="caseColumns" :data="cases" :bordered="false" :row-key="r=>r.id" />
            </template>
            <n-empty v-else description="选择一个评测集查看用例" />
          </section>
        </div>
      </n-tab-pane>

      <n-tab-pane name="runs" tab="运行记录">
        <div v-if="activeRun" class="metric-strip">
          <div><span>Recall@K</span><strong>{{ pct(activeRun.recallAtK) }}</strong></div><div><span>MRR</span><strong>{{ pct(activeRun.mrr) }}</strong></div><div><span>引用有效</span><strong>{{ pct(activeRun.citationValidity) }}</strong></div><div><span>引用覆盖</span><strong>{{ pct(activeRun.citationCoverage) }}</strong></div><div><span>P95</span><strong>{{ activeRun.p95LatencyMs || 0 }} ms</strong></div><div><span>进度</span><strong>{{ activeRun.completedCases || 0 }}/{{ activeRun.totalCases || 0 }}</strong></div>
        </div>
        <n-data-table :columns="runColumns" :data="runs" :bordered="false" />
      </n-tab-pane>
    </n-tabs>

    <n-modal v-model:show="datasetOpen" preset="dialog" :title="datasetForm.id ? '编辑评测集' : '新建评测集'" :show-icon="false">
      <n-form><n-form-item label="名称"><n-input v-model:value="datasetForm.name" /></n-form-item><n-form-item label="说明"><n-input v-model:value="datasetForm.description" type="textarea" /></n-form-item><n-form-item label="启用"><n-switch :value="datasetForm.enabled===1" @update:value="v=>datasetForm.enabled=v?1:0" /></n-form-item></n-form>
      <template #action><n-button @click="datasetOpen=false">取消</n-button><n-button type="primary" @click="saveDataset">保存</n-button></template>
    </n-modal>
    <n-modal v-model:show="caseOpen" preset="dialog" :title="caseForm.id ? '编辑用例' : '添加用例'" :show-icon="false" style="width:min(680px,92vw)">
      <n-form label-placement="top"><n-form-item label="问题"><n-input v-model:value="caseForm.question" type="textarea" /></n-form-item><div class="form-grid"><n-form-item label="期望文档 ID"><n-input v-model:value="caseForm.expectedDocIds" placeholder="12,18" /></n-form-item><n-form-item label="标签"><n-input v-model:value="caseForm.tags" /></n-form-item></div><n-form-item label="期望答案摘要"><n-input v-model:value="caseForm.expectedAnswer" type="textarea" /></n-form-item><div class="form-grid"><n-form-item label="必含关键词"><n-input v-model:value="caseForm.requiredKeywords" /></n-form-item><n-form-item label="禁用断言"><n-input v-model:value="caseForm.forbiddenClaims" /></n-form-item></div><n-checkbox :checked="caseForm.noAnswer===1" @update:checked="v=>caseForm.noAnswer=v?1:0">无答案题</n-checkbox></n-form>
      <template #action><n-button @click="caseOpen=false">取消</n-button><n-button type="primary" @click="saveCase">保存</n-button></template>
    </n-modal>
    <n-modal v-model:show="jsonlOpen" preset="dialog" title="导入 JSONL" :show-icon="false" style="width:min(720px,92vw)">
      <n-input v-model:value="jsonl" type="textarea" :autosize="{minRows:8,maxRows:18}" placeholder='{"question":"...","expectedDocIds":[12],"noAnswer":false}' />
      <template #action><n-button @click="jsonlOpen=false">取消</n-button><n-button type="primary" @click="importJsonl">导入</n-button></template>
    </n-modal>
    <n-drawer v-model:show="resultOpen" placement="right" :width="680"><n-drawer-content title="评测结果"><n-data-table :columns="resultColumns" :data="results" :bordered="false" /></n-drawer-content></n-drawer>
  </div>
</template>

<script setup>
import {h,onMounted,ref} from 'vue';
import {NButton,NCheckbox,NDataTable,NDrawer,NDrawerContent,NEmpty,NForm,NFormItem,NInput,NModal,NSpace,NSwitch,NTabPane,NTabs,NTag,useMessage} from 'naive-ui';
import * as api from '../../api/aiEval';
const message=useMessage(),tab=ref('datasets'),datasets=ref([]),cases=ref([]),runs=ref([]),results=ref([]),selected=ref(null),activeRun=ref(null),running=ref(false);
const datasetOpen=ref(false),caseOpen=ref(false),jsonlOpen=ref(false),resultOpen=ref(false),jsonl=ref('');
const datasetForm=ref({}),caseForm=ref({});
const btn=(label,fn,type)=>h(NButton,{size:'tiny',type,onClick:fn},{default:()=>label});
const caseColumns=[{title:'问题',key:'question',ellipsis:{tooltip:true}},{title:'文档',key:'expectedDocIds',width:100},{title:'类型',width:90,render:r=>r.noAnswer?'无答案':'问答'},{title:'操作',width:120,render:r=>h(NSpace,{size:6},()=>[btn('编辑',()=>openCase(r)),btn('删除',()=>removeCase(r),'error')])}];
const runColumns=[{title:'运行 ID',key:'id',width:90},{title:'状态',key:'status',width:110,render:r=>h(NTag,{size:'small',type:r.status==='SUCCESS'?'success':r.status==='RUNNING'?'warning':'error'},{default:()=>r.status})},{title:'进度',render:r=>`${r.completedCases||0}/${r.totalCases||0}`},{title:'Recall@K',render:r=>pct(r.recallAtK)},{title:'MRR',render:r=>pct(r.mrr)},{title:'P95',render:r=>`${r.p95LatencyMs||0} ms`},{title:'操作',render:r=>btn('查看结果',()=>openResults(r))}];
const resultColumns=[{title:'问题',key:'question',width:220,ellipsis:{tooltip:true}},{title:'状态',key:'status',width:90},{title:'命中',render:r=>r.recallHit?'是':'否',width:70},{title:'引用',render:r=>r.citationValid?'有效':'异常',width:80},{title:'耗时',render:r=>`${r.latencyMs||0} ms`,width:100},{title:'回答',key:'answer',ellipsis:{tooltip:true}}];
function pct(v){return `${Math.round((Number(v)||0)*100)}%`}
async function loadAll(){const [d,r]=await Promise.all([api.listEvalDatasets({page:1,size:100}),api.listEvalRuns({page:1,size:100})]);datasets.value=d?.records||[];runs.value=r?.records||[];activeRun.value=runs.value[0]||null;if(selected.value){selected.value=datasets.value.find(x=>x.id===selected.value.id)||null;if(selected.value)await loadCases();}}
async function selectDataset(d){selected.value=d;await loadCases()}
async function loadCases(){const d=await api.listEvalCases(selected.value.id,{page:1,size:200});cases.value=d?.records||[]}
function openDataset(d){datasetForm.value=d?{...d}:{name:'',description:'',enabled:1};datasetOpen.value=true}
async function saveDataset(){if(!datasetForm.value.name?.trim())return message.warning('请输入名称');const f=datasetForm.value;f.id?await api.updateEvalDataset(f.id,f):await api.createEvalDataset(f);datasetOpen.value=false;message.success('已保存');await loadAll()}
function openCase(c){caseForm.value=c?{...c}:{question:'',expectedAnswer:'',expectedDocIds:'',requiredKeywords:'',forbiddenClaims:'',tags:'',noAnswer:0,enabled:1};caseOpen.value=true}
async function saveCase(){if(!caseForm.value.question?.trim())return message.warning('请输入问题');const f=caseForm.value;f.id?await api.updateEvalCase(f.id,f):await api.createEvalCase(selected.value.id,f);caseOpen.value=false;message.success('已保存');await loadCases()}
async function removeCase(r){await api.deleteEvalCase(r.id);await loadCases()}
async function importJsonl(){const r=await api.importEvalJsonl(selected.value.id,jsonl.value);message.success(`导入 ${r.success} 条，失败 ${r.failed} 条`);jsonlOpen.value=false;jsonl.value='';await loadCases()}
async function runDataset(){running.value=true;try{await api.startEvalRun(selected.value.id);message.success('评测已开始');tab.value='runs';await loadAll()}finally{running.value=false}}
async function openResults(r){activeRun.value=r;const d=await api.listEvalResults(r.id,{page:1,size:200});results.value=d?.records||[];resultOpen.value=true}
onMounted(loadAll);
</script>

<style scoped>
.eval-page{max-width:1400px}.eval-header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:28px}.eyebrow{color:var(--color-primary);font:600 10px var(--font-mono);letter-spacing:.12em}.eval-header h1{margin:6px 0 4px;font-size:30px}.eval-header p{margin:0;color:var(--color-text-muted)}.eval-layout{display:grid;grid-template-columns:280px minmax(0,1fr);min-height:560px;border:1px solid var(--color-border);background:var(--color-surface)}.dataset-list{border-right:1px solid var(--color-border);padding:8px}.dataset-row{width:100%;display:flex;align-items:center;justify-content:space-between;gap:12px;padding:14px 12px;border:0;border-bottom:1px solid var(--color-border);background:transparent;color:var(--color-text);text-align:left;cursor:pointer}.dataset-row.active{background:var(--color-primary-soft)}.dataset-row span{min-width:0}.dataset-row strong,.dataset-row small{display:block}.dataset-row small{margin-top:4px;color:var(--color-text-muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.case-workspace{min-width:0;padding:20px}.section-toolbar{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:18px}.section-toolbar h2{margin:0 0 3px;font-size:19px}.section-toolbar span{color:var(--color-text-muted);font-size:12px}.metric-strip{display:grid;grid-template-columns:repeat(6,1fr);border:1px solid var(--color-border);margin-bottom:18px;background:var(--color-surface)}.metric-strip div{padding:15px;border-right:1px solid var(--color-border)}.metric-strip div:last-child{border-right:0}.metric-strip span,.metric-strip strong{display:block}.metric-strip span{color:var(--color-text-muted);font:10px var(--font-mono)}.metric-strip strong{margin-top:6px;font-size:18px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}@media(max-width:900px){.eval-header,.section-toolbar{align-items:flex-start;flex-direction:column}.eval-layout{grid-template-columns:1fr}.dataset-list{border-right:0;border-bottom:1px solid var(--color-border)}.metric-strip{grid-template-columns:repeat(2,1fr)}.form-grid{grid-template-columns:1fr}}
</style>
