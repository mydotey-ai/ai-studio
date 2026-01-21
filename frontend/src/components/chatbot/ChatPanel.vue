<template>
  <div class="chat-panel">
    <!-- Messages Container -->
    <div ref="messagesContainer" class="messages-container">
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message', message.role === 'user' ? 'user-message' : 'assistant-message']"
      >
        <!-- Avatar -->
        <div class="avatar">
          <el-avatar v-if="message.role === 'user'" :icon="UserFilled" :size="40" />
          <el-avatar v-else :src="chatbotAvatar" :icon="UserFilled" :size="40" />
        </div>

        <!-- Message Content -->
        <div class="message-content">
          <div class="message-header">
            <span class="role-name">
              {{ message.role === 'user' ? userName : chatbotName }}
            </span>
            <span class="message-time">{{ formatTime(message.createdAt) }}</span>
          </div>

          <!-- Text Content with Markdown for assistant -->
          <div
            v-if="message.role === 'assistant'"
            class="message-text markdown-content"
            v-html="renderMarkdown(message.content)"
          />
          <div v-else class="message-text">
            {{ message.content }}
          </div>

          <!-- Streaming indicator -->
          <div v-if="message.id === streamingMessageId" class="streaming-cursor">▋</div>

          <!-- Source References -->
          <div v-if="message.sources && message.sources.length > 0" class="sources-section">
            <el-collapse>
              <el-collapse-item title="参考来源" name="sources">
                <div class="sources-list">
                  <div v-for="(source, index) in message.sources" :key="index" class="source-item">
                    <el-tag :type="getSourceType(source.type)" size="small">
                      {{ source.type }}
                    </el-tag>
                    <span class="source-name">{{ source.name }}</span>
                    <span v-if="source.relevance" class="source-relevance">
                      相关度: {{ (source.relevance * 100).toFixed(1) }}%
                    </span>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>

          <!-- Tool Calls -->
          <div v-if="message.toolCalls && message.toolCalls.length > 0" class="tool-calls-section">
            <el-timeline>
              <el-timeline-item
                v-for="toolCall in message.toolCalls"
                :key="toolCall.id"
                :type="
                  toolCall.status === 'success'
                    ? 'success'
                    : toolCall.status === 'error'
                      ? 'danger'
                      : 'primary'
                "
                :icon="
                  toolCall.status === 'success'
                    ? CircleCheck
                    : toolCall.status === 'error'
                      ? CircleClose
                      : Loading
                "
              >
                <div class="tool-call-item">
                  <div class="tool-name">{{ toolCall.name }}</div>
                  <el-tag :type="getToolStatusType(toolCall.status)" size="small">
                    {{ getToolStatusText(toolCall.status) }}
                  </el-tag>
                  <el-collapse v-if="toolCall.arguments || toolCall.result">
                    <el-collapse-item title="详情" name="details">
                      <div v-if="toolCall.arguments" class="tool-arguments">
                        <strong>参数:</strong>
                        <pre>{{ toolCall.arguments }}</pre>
                      </div>
                      <div v-if="toolCall.result" class="tool-result">
                        <strong>结果:</strong>
                        <pre>{{ toolCall.result }}</pre>
                      </div>
                      <div v-if="toolCall.error" class="tool-error">
                        <strong>错误:</strong>
                        <span>{{ toolCall.error }}</span>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </div>

      <!-- Loading indicator -->
      <div v-if="loading" class="message assistant-message">
        <div class="avatar">
          <el-avatar :src="chatbotAvatar" :icon="UserFilled" :size="40" />
        </div>
        <div class="message-content">
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- Input Area -->
    <div class="input-area">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        placeholder="输入消息... (Shift+Enter 换行, Enter 发送)"
        :disabled="loading"
        @keydown="handleKeydown"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :loading="loading"
        :disabled="!inputMessage.trim()"
        @click="sendMessage"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Promotion, CircleCheck, CircleClose, Loading } from '@element-plus/icons-vue'
import { renderMarkdown } from '@/utils/markdown'
import { sendMessageStream } from '@/api/conversation'
import type { Message, ChatRequest } from '@/types/chatbot'
import dayjs from 'dayjs'

interface Props {
  chatbotId: number
  chatbotName: string
  chatbotAvatar?: string
  userName: string
  conversationId?: number
}

interface Emits {
  (e: 'update:conversationId', id: number): void
  (e: 'messageAdded', message: Message): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const messages = ref<Message[]>([])
const inputMessage = ref('')
const loading = ref(false)
const streamingMessageId = ref<number | null>(null)
const messagesContainer = ref<HTMLElement>()

let currentEventSource: EventSource | null = null

function formatTime(date: string) {
  return dayjs(date).format('HH:mm')
}

function getSourceType(type: string) {
  switch (type) {
    case 'knowledge_base':
      return 'success'
    case 'tool':
      return 'warning'
    case 'url':
      return 'info'
    default:
      return 'info'
  }
}

function getToolStatusType(status: string) {
  switch (status) {
    case 'success':
      return 'success'
    case 'error':
      return 'danger'
    case 'pending':
      return 'warning'
    default:
      return 'info'
  }
}

function getToolStatusText(status: string) {
  switch (status) {
    case 'success':
      return '成功'
    case 'error':
      return '失败'
    case 'pending':
      return '执行中'
    default:
      return status
  }
}

async function sendMessage() {
  const message = inputMessage.value.trim()
  if (!message || loading.value) return

  // Add user message
  const userMessage: Message = {
    id: Date.now(),
    conversationId: props.conversationId || 0,
    role: 'user',
    content: message,
    createdAt: new Date().toISOString()
  }

  messages.value.push(userMessage)
  inputMessage.value = ''

  // Scroll to bottom
  await scrollToBottom()

  // Start streaming
  loading.value = true
  streamingMessageId.value = null

  const request: ChatRequest = {
    chatbotId: props.chatbotId,
    conversationId: props.conversationId,
    message: message
  }

  // Close previous connection if exists
  if (currentEventSource) {
    currentEventSource.close()
  }

  // Create temporary assistant message for streaming
  let assistantMessage: Message = {
    id: Date.now() + 1,
    conversationId: props.conversationId || 0,
    role: 'assistant',
    content: '',
    createdAt: new Date().toISOString()
  }
  messages.value.push(assistantMessage)
  streamingMessageId.value = assistantMessage.id

  try {
    currentEventSource = sendMessageStream(
      request,
      data => {
        // Update streaming message
        if (data.message) {
          assistantMessage = {
            ...assistantMessage,
            ...data.message,
            id: assistantMessage.id
          }

          // Update in array
          const index = messages.value.findIndex(m => m.id === assistantMessage.id)
          if (index !== -1) {
            messages.value[index] = assistantMessage
          }

          // Update conversation ID
          if (data.conversationId && data.conversationId !== props.conversationId) {
            emit('update:conversationId', data.conversationId)
          }

          // Auto-scroll during streaming
          scrollToBottom()
        }
      },
      () => {
        // On complete
        loading.value = false
        streamingMessageId.value = null
        emit('messageAdded', assistantMessage)
        if (currentEventSource) {
          currentEventSource.close()
          currentEventSource = null
        }
      },
      error => {
        // On error
        loading.value = false
        streamingMessageId.value = null
        ElMessage.error(error.message || '发送消息失败')
        if (currentEventSource) {
          currentEventSource.close()
          currentEventSource = null
        }

        // Remove failed message
        const index = messages.value.findIndex(m => m.id === assistantMessage.id)
        if (index !== -1) {
          messages.value.splice(index, 1)
        }
      }
    )
  } catch (error: unknown) {
    loading.value = false
    streamingMessageId.value = null
    const errorMessage = error instanceof Error ? error.message : '发送消息失败'
    ElMessage.error(errorMessage)

    // Remove failed message
    const index = messages.value.findIndex(m => m.id === assistantMessage.id)
    if (index !== -1) {
      messages.value.splice(index, 1)
    }
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function addMessage(message: Message) {
  messages.value.push(message)
  scrollToBottom()
}

function clearMessages() {
  messages.value = []
  if (currentEventSource) {
    currentEventSource.close()
    currentEventSource = null
  }
}

// Watch for conversation ID changes (new conversation)
watch(
  () => props.conversationId,
  () => {
    clearMessages()
  }
)

defineExpose({
  addMessage,
  clearMessages
})
</script>

<style scoped lang="scss">
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  gap: 12px;
  max-width: 800px;

  &.user-message {
    align-self: flex-end;
    flex-direction: row-reverse;
  }

  &.assistant-message {
    align-self: flex-start;
  }
}

.avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 12px;
}

.role-name {
  font-weight: 600;
  color: #303133;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-text {
  background: white;
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.user-message .message-text {
  background: #409eff;
  color: white;
}

.assistant-message .message-text {
  background: white;
  color: #303133;
  border: 1px solid #dcdfe6;
}

.markdown-content {
  :deep(p) {
    margin: 0 0 10px 0;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(code) {
    background: #f5f7fa;
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Courier New', monospace;
    font-size: 0.9em;
  }

  :deep(pre) {
    background: #282c34;
    color: #abb2bf;
    padding: 12px;
    border-radius: 6px;
    overflow-x: auto;
    margin: 10px 0;

    code {
      background: transparent;
      padding: 0;
      color: inherit;
    }
  }

  :deep(ul),
  :deep(ol) {
    margin: 10px 0;
    padding-left: 20px;
  }

  :deep(li) {
    margin: 5px 0;
  }

  :deep(a) {
    color: #409eff;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  :deep(blockquote) {
    border-left: 4px solid #409eff;
    padding-left: 12px;
    margin: 10px 0;
    color: #606266;
  }
}

.streaming-cursor {
  display: inline-block;
  animation: blink 1s infinite;
  color: #409eff;
  font-weight: bold;
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

.sources-section {
  margin-top: 12px;
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;

  .source-name {
    flex: 1;
    font-size: 14px;
  }

  .source-relevance {
    font-size: 12px;
    color: #909399;
  }
}

.tool-calls-section {
  margin-top: 12px;
}

.tool-call-item {
  .tool-name {
    font-weight: 600;
    margin-bottom: 8px;
  }

  .tool-arguments,
  .tool-result,
  .tool-error {
    margin: 8px 0;

    pre {
      background: #f5f7fa;
      padding: 8px;
      border-radius: 4px;
      margin: 5px 0;
      white-space: pre-wrap;
      word-break: break-all;
      font-size: 12px;
    }
  }

  .tool-error {
    color: #f56c6c;
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  width: fit-content;

  span {
    width: 8px;
    height: 8px;
    background: #409eff;
    border-radius: 50%;
    animation: typing 1.4s infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes typing {
  0%,
  60%,
  100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

.input-area {
  display: flex;
  gap: 12px;
  padding: 20px;
  background: white;
  border-top: 1px solid #dcdfe6;

  .el-textarea {
    flex: 1;
  }

  .el-button {
    align-self: flex-end;
  }
}
</style>
