<template>
  <div v-if="authStore.isLoggedIn && message?.id" class="feedback-row">
    <button type="button" :class="{active:message.feedback==='UP'}" title="有帮助" @click="submit('UP')"><ThumbsUp :size="14" /></button>
    <button type="button" :class="{active:message.feedback==='DOWN'}" title="需要改进" @click="open=true"><ThumbsDown :size="14" /></button>
    <n-modal v-model:show="open" preset="dialog" title="这条回答哪里需要改进？" :show-icon="false">
      <n-radio-group v-model:value="reason"><n-space vertical><n-radio v-for="item in reasons" :key="item" :value="item">{{ item }}</n-radio></n-space></n-radio-group>
      <template #action><n-button @click="open=false">取消</n-button><n-button type="primary" @click="submit('DOWN',reason)">提交</n-button></template>
    </n-modal>
  </div>
</template>
<script setup>
import {ref} from 'vue';
import {NButton,NModal,NRadio,NRadioGroup,NSpace} from 'naive-ui';
import {ThumbsDown,ThumbsUp} from 'lucide-vue-next';
import {saveAnswerFeedback} from '../../api/aiEval';
import {useAuthStore} from '../../stores/auth';
import {useAiChatStore} from '../../stores/aiChat';
import {useToastStore} from '../../stores/toast';
const props=defineProps({message:{type:Object,required:true}}),authStore=useAuthStore(),aiChat=useAiChatStore(),toast=useToastStore();
const open=ref(false),reason=ref('回答错误'),reasons=['不相关','引用不支持','回答错误','没有回答问题','其他'];
async function submit(vote,why=null){await saveAnswerFeedback({messageId:props.message.id,vote,reason:why});props.message.feedback=vote;aiChat.persist();open.value=false;toast.push('反馈已记录','success')}
</script>
<style scoped>.feedback-row{display:flex;gap:5px}.feedback-row>button{display:grid;place-items:center;width:28px;height:28px;border:1px solid var(--color-border);border-radius:6px;background:transparent;color:var(--color-text-muted);cursor:pointer}.feedback-row>button:hover,.feedback-row>button.active{border-color:var(--color-primary);color:var(--color-primary);background:var(--color-primary-soft)}</style>
