<template>
  <div class="chat-panel">
    <div ref="messagesContainer" class="messages-container">
      <div v-for="message in parsedMessages" :key="message.id" :class="['message', message.role]">
        <div class="message-avatar">
          <el-avatar v-if="message.role === 'assistant'" :size="32" :src="chatbotAvatar">
            {{ chatbotName.charAt(0) }}
          </el-avatar>
          <el-avatar v-else :size="32">
            {{ userInitial }}
          </el-avatar>
        </div>
        <div class="message-content">
          <div class="message-header">
            <span class="message-role">{{ message.role === 'user' ? '你' : chatbotName }}</span>
            <span class="message-time">{{ formatTime(message.createdAt) }}</span>
          </div>
          <div class="message-text" v-html="renderMarkdown(message.content)"></div>
          <div v-if="message.sources && message.sources.length > 0" class="message-sources">
            <div class="sources-title">参考来源:</div>
            <el-collapse>
              <el-collapse-item
                v-for="(source, index) in message.sources"
                :key="index"
                :title="source.documentName"
              >
                <div class="source-content">{{ source.content }}</div>
                <div v-if="source.score" class="source-score">
                  相似度: {{ (source.score * 100).toFixed(1) }}%
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
          <div v-if="message.toolCalls && message.toolCalls.length > 0" class="message-tools">
            <div class="tools-title">工具调用:</div>
            <el-timeline>
              <el-timeline-item
                v-for="(tool, index) in message.toolCalls"
                :key="index"
                :timestamp="tool.toolName"
                placement="top"
              >
                <div class="tool-name">{{ tool.toolName }}</div>
                <div class="tool-args">
                  <el-text type="info" size="small"> 参数: {{ tool.arguments }} </el-text>
                </div>
                <div v-if="tool.error" class="tool-error">
                  <el-text type="danger">{{ tool.error }}</el-text>
                </div>
                <div v-else-if="tool.result" class="tool-result">
                  <el-text type="success">执行成功</el-text>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </div>
      <div v-if="isStreaming" class="message assistant streaming">
        <div class="message-avatar">
          <el-avatar :size="32" :src="chatbotAvatar">
            {{ chatbotName.charAt(0) }}
          </el-avatar>
        </div>
        <div class="message-content">
          <div class="message-text streaming" v-html="renderMarkdown(streamingText)"></div>
        </div>
      </div>
    </div>

    <div class="input-container">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        placeholder="输入消息... (Shift+Enter 换行)"
        :disabled="isStreaming"
        @keydown.shift.enter.exact="handleSend"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :loading="isStreaming"
        :disabled="!inputMessage.trim()"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, computed } from 'vue'
import type { Message } from '@/types/chatbot'
import { renderMarkdown } from '@/utils/markdown'
import dayjs from 'dayjs'
import { Promotion } from '@element-plus/icons-vue'

interface Props {
  messages: Message[]
  chatbotName: string
  chatbotAvatar?: string
  isStreaming: boolean
  streamingText?: string
  userInitial: string
}

interface Emits {
  (e: 'send', message: string): void
}

interface ToolCallResult {
  toolName: string
  arguments: string
  result?: string
  error?: string
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const messagesContainer = ref<HTMLElement>()
const inputMessage = ref('')

// Parse toolCalls from JSON string to array
const parsedMessages = computed(() => {
  return props.messages.map(msg => ({
    ...msg,
    toolCalls: msg.toolCalls ? parseToolCalls(msg.toolCalls) : undefined
  }))
})

function parseToolCalls(toolCalls: string | any[]): ToolCallResult[] {
  if (typeof toolCalls === 'string') {
    try {
      const parsed = JSON.parse(toolCalls)
      return Array.isArray(parsed) ? parsed : [parsed]
    } catch {
      return []
    }
  }
  return toolCalls as any
}

function handleSend() {
  if (!inputMessage.value.trim() || props.isStreaming) return

  const message = inputMessage.value
  inputMessage.value = ''
  emit('send', message)
}

function formatTime(date: string) {
  return dayjs(date).format('HH:mm')
}

watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  }
)

watch(
  () => props.streamingText,
  async () => {
    if (props.isStreaming) {
      await nextTick()
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    }
  }
)
</script>

<style scoped lang="scss">
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 600px;
  background-color: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      background-color: #409eff;
      color: white;
      border-radius: 12px 0 12px 12px;
    }

    .message-header {
      justify-content: flex-end;
    }
  }

  &.assistant {
    .message-content {
      background-color: white;
      border: 1px solid #e4e7ed;
      border-radius: 0 12px 12px 12px;
    }
  }

  &.streaming {
    .message-text.streaming::after {
      content: '|';
      animation: blink 1s infinite;
    }
  }
}

@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  word-wrap: break-word;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  opacity: 0.8;
}

.message-role {
  font-weight: 500;
}

.message-text {
  line-height: 1.6;

  :deep(p) {
    margin: 0.5em 0;
  }

  :deep(code) {
    background-color: rgba(0, 0, 0, 0.05);
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Courier New', monospace;
  }

  :deep(pre) {
    background-color: #282c34;
    color: #abb2bf;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 12px 0;
  }
}

.message-sources {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.sources-title {
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 8px;
  opacity: 0.8;
}

.source-content {
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.source-score {
  font-size: 11px;
  color: #909399;
}

.message-tools {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.tools-title {
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 8px;
  opacity: 0.8;
}

.tool-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.tool-args {
  margin: 4px 0;
}

.tool-error,
.tool-result {
  margin-top: 4px;
}

.input-container {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  background-color: white;
  border-top: 1px solid #e4e7ed;

  .el-textarea {
    flex: 1;
  }

  .el-button {
    align-self: flex-end;
    height: 40px;
  }
}
</style>
