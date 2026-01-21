<template>
  <div class="chatbot-detail">
    <div class="header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="title">{{ chatbot?.name }}</span>
        </template>
        <template #extra>
          <el-button v-if="mode !== 'chat'" type="primary" :icon="ChatDotSquare" @click="startChat">
            开始对话
          </el-button>
        </template>
      </el-page-header>
    </div>

    <div v-if="mode === 'chat'" class="chat-container">
      <ChatPanel
        :messages="messages"
        :chatbot-name="chatbot?.name || 'Assistant'"
        :chatbot-avatar="chatbot?.styleConfig?.avatarUrl"
        :is-streaming="isStreaming"
        :streaming-text="streamingText"
        :user-initial="userInitial"
        @send="handleSendMessage"
      />
    </div>

    <el-tabs v-else v-model="activeTab" class="tabs">
      <el-tab-pane label="信息" name="info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">
            {{ chatbot?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="绑定 Agent">
            {{ displayChatbot?.agentName }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ chatbot?.description || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="欢迎语" :span="2">
            {{ displayChatbot?.welcomeMessage }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="chatbot?.isPublished ? 'success' : 'info'">
              {{ chatbot?.isPublished ? '已发布' : '草稿' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="访问次数">
            {{ displayChatbot?.accessCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(chatbot?.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDate(chatbot?.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="actions">
          <el-button :icon="Edit" @click="showEditDialog = true">编辑</el-button>
          <el-button
            v-if="!chatbot?.isPublished"
            type="success"
            :icon="Promotion"
            @click="handlePublish"
          >
            发布
          </el-button>
          <el-button v-else type="warning" :icon="Close" @click="handleUnpublish">
            取消发布
          </el-button>
          <el-button type="danger" :icon="Delete" @click="handleDelete">删除</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="对话历史" name="conversations">
        <ConversationsList :chatbot-id="chatbotId" @select="handleSelectConversation" />
      </el-tab-pane>
    </el-tabs>

    <!-- Edit Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑聊天机器人" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="欢迎语">
          <el-input v-model="form.welcomeMessage" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="头像 URL">
          <el-input v-model="form.avatarUrl" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdate"> 保存 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Edit, Promotion, Close, Delete, ChatDotSquare } from '@element-plus/icons-vue'
import {
  getChatbot,
  updateChatbot,
  deleteChatbot,
  publishChatbot,
  unpublishChatbot
} from '@/api/chatbot'
import { sendMessageStream, getConversation } from '@/api/conversation'
import { useUserStore } from '@/stores/user'
import type { Chatbot, ChatRequest, Message } from '@/types/chatbot'
import dayjs from 'dayjs'
import ChatPanel from '@/components/chatbot/ChatPanel.vue'
import ConversationsList from '@/components/chatbot/ConversationsList.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()

const chatbotId = computed(() => parseInt(route.params.id as string))
const mode = computed(() => (route.query.mode as string) || 'detail')

const chatbot = ref<Chatbot>()
const activeTab = ref('info')
const showEditDialog = ref(false)
const submitting = ref(false)

const messages = ref<Message[]>([])
const currentConversationId = ref<number>()
const isStreaming = ref(false)
const streamingText = ref('')
const eventSourceRef = ref<EventSource | null>(null)

const userInitial = computed(() => userStore.userInfo?.username?.charAt(0).toUpperCase() || 'U')

// Computed properties for display to match spec expectations
const displayChatbot = computed(() => {
  if (!chatbot.value) return undefined
  return {
    ...chatbot.value,
    agentName: 'Agent', // Default, should come from backend
    welcomeMessage: chatbot.value.settings?.welcomeMessage || '',
    avatarUrl: chatbot.value.styleConfig?.avatarUrl || '',
    accessCount: 0 // Default, should come from backend
  }
})

const form = reactive({
  name: '',
  description: '',
  welcomeMessage: '',
  avatarUrl: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function loadChatbot() {
  chatbot.value = await getChatbot(chatbotId.value)
  if (chatbot.value) {
    form.name = chatbot.value.name
    form.description = chatbot.value.description || ''
    form.welcomeMessage = chatbot.value.settings?.welcomeMessage || ''
    form.avatarUrl = chatbot.value.styleConfig?.avatarUrl || ''
  }
}

async function handleUpdate() {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    submitting.value = true
    try {
      await updateChatbot(chatbotId.value, form)
      ElMessage.success('更新成功')
      showEditDialog.value = false
      loadChatbot()
    } finally {
      submitting.value = false
    }
  })
}

async function handlePublish() {
  try {
    await publishChatbot(chatbotId.value)
    ElMessage.success('发布成功')
    loadChatbot()
  } catch (error) {
    ElMessage.error('发布失败')
    console.error('Failed to publish chatbot:', error)
  }
}

async function handleUnpublish() {
  try {
    await unpublishChatbot(chatbotId.value)
    ElMessage.success('已取消发布')
    loadChatbot()
  } catch (error) {
    ElMessage.error('取消发布失败')
    console.error('Failed to unpublish chatbot:', error)
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除此聊天机器人吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChatbot(chatbotId.value)
    ElMessage.success('删除成功')
    router.push('/chatbots')
  } catch {
    // User cancelled
  }
}

function startChat() {
  router.push(`/chatbots/${chatbotId.value}?mode=chat`)
}

async function handleSendMessage(messageText: string) {
  const userMessage: Message = {
    id: Date.now(),
    conversationId: currentConversationId.value || 0,
    role: 'user',
    content: messageText,
    createdAt: new Date().toISOString()
  }
  messages.value.push(userMessage)

  const request: ChatRequest = {
    chatbotId: chatbotId.value,
    conversationId: currentConversationId.value,
    message: messageText,
    stream: true
  }

  try {
    isStreaming.value = true
    streamingText.value = ''

    let assistantMessage: Message | null = null

    eventSourceRef.value = sendMessageStream(
      request,
      data => {
        // Extract content from ChatResponse
        if (data.message && data.message.content) {
          streamingText.value += data.message.content
          if (!assistantMessage) {
            assistantMessage = {
              id: data.messageId || Date.now() + 1,
              conversationId: data.conversationId || currentConversationId.value || 0,
              role: 'assistant',
              content: streamingText.value,
              createdAt: new Date().toISOString()
            }
            messages.value.push(assistantMessage)
          } else {
            assistantMessage.content = streamingText.value
          }
        }
      },
      () => {
        isStreaming.value = false
        streamingText.value = ''
        eventSourceRef.value = null
      },
      error => {
        ElMessage.error('发送失败: ' + error.message)
        isStreaming.value = false
        streamingText.value = ''
        eventSourceRef.value = null
      }
    )
  } catch (error) {
    ElMessage.error('发送失败')
    isStreaming.value = false
  }
}

async function handleSelectConversation(conversationId: number) {
  currentConversationId.value = conversationId
  try {
    const data = await getConversation(chatbotId.value, conversationId)
    messages.value = data.messages || []
  } catch (error) {
    ElMessage.error('加载对话失败')
    console.error('Failed to load conversation:', error)
  }
}

function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

onMounted(() => {
  loadChatbot()
})

onBeforeUnmount(() => {
  eventSourceRef.value?.close()
})
</script>

<style scoped lang="scss">
.chatbot-detail {
  .header {
    margin-bottom: 20px;

    .title {
      font-size: 20px;
      font-weight: 500;
    }
  }

  .chat-container {
    height: calc(100vh - 120px);
  }

  .actions {
    margin-top: 20px;
    display: flex;
    gap: 12px;
  }
}
</style>
