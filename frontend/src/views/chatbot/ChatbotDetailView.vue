<template>
  <div v-loading="loading" class="chatbot-detail">
    <!-- Page Header -->
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
      <div class="header-actions">
        <template v-if="!isChatMode">
          <el-button type="primary" @click="startChat">
            <el-icon><ChatDotRound /></el-icon>
            开始聊天
          </el-button>
          <el-button @click="showEditDialog = true">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-popconfirm
            title="确定删除这个聊天机器人吗?"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleDelete"
          >
            <template #reference>
              <el-button type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
        <el-button v-else type="default" @click="exitChat">
          <el-icon><ArrowLeft /></el-icon>
          退出聊天
        </el-button>
      </div>
    </div>

    <!-- Detail Mode -->
    <div v-if="!isChatMode && chatbot" class="detail-mode">
      <el-card class="chatbot-card">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-avatar :src="chatbot.styleConfig?.avatarUrl" :icon="UserFilled" :size="60" />
              <div class="header-info">
                <h2>{{ chatbot.name }}</h2>
                <div class="meta-info">
                  <el-tag :type="chatbot.isPublic ? 'success' : 'info'" size="small">
                    {{ chatbot.isPublic ? '公开' : '私有' }}
                  </el-tag>
                  <el-tag :type="chatbot.isPublished ? 'success' : 'info'" size="small">
                    {{ chatbot.isPublished ? '已发布' : '草稿' }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </template>

        <el-tabs v-model="activeTab">
          <!-- Info Tab -->
          <el-tab-pane label="基本信息" name="info">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="描述" :span="2">
                {{ chatbot.description || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="系统提示词" :span="2">
                <div class="prompt-text">{{ chatbot.systemPrompt }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="模型配置" :span="2">
                <pre class="json-config">{{ formatJson(chatbot.modelConfig) }}</pre>
              </el-descriptions-item>
              <el-descriptions-item label="欢迎语">
                {{ chatbot.settings?.welcomeMessage || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="温度">
                {{ chatbot.settings?.temperature ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="最大 Token">
                {{ chatbot.settings?.maxTokens ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="启用记忆">
                <el-tag :type="chatbot.settings?.enableMemory ? 'success' : 'info'" size="small">
                  {{ chatbot.settings?.enableMemory ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="启用知识库">
                <el-tag
                  :type="chatbot.settings?.enableKnowledgeBase ? 'success' : 'info'"
                  size="small"
                >
                  {{ chatbot.settings?.enableKnowledgeBase ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="知识库" :span="2">
                <el-tag
                  v-for="kbId in chatbot.settings?.knowledgeBaseIds || []"
                  :key="kbId"
                  style="margin-right: 8px"
                >
                  KB {{ kbId }}
                </el-tag>
                <span v-if="!chatbot.settings?.knowledgeBaseIds?.length">未绑定</span>
              </el-descriptions-item>
              <el-descriptions-item label="启用工具">
                <el-tag :type="chatbot.settings?.enableTools ? 'success' : 'info'" size="small">
                  {{ chatbot.settings?.enableTools ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="工具" :span="2">
                <el-tag
                  v-for="toolId in chatbot.settings?.toolIds || []"
                  :key="toolId"
                  type="warning"
                  style="margin-right: 8px"
                >
                  Tool {{ toolId }}
                </el-tag>
                <span v-if="!chatbot.settings?.toolIds?.length">未绑定</span>
              </el-descriptions-item>
              <el-descriptions-item label="建议问题" :span="2">
                <div
                  v-if="chatbot.settings?.suggestedQuestions?.length"
                  class="suggested-questions"
                >
                  <el-tag
                    v-for="(q, index) in chatbot.settings.suggestedQuestions"
                    :key="index"
                    style="margin: 4px"
                  >
                    {{ q }}
                  </el-tag>
                </div>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatDateTime(chatbot.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="更新时间">
                {{ formatDateTime(chatbot.updatedAt) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- Conversations Tab -->
          <el-tab-pane label="会话历史" name="conversations">
            <ConversationsList
              :chatbot-id="chatbot.id"
              @select="handleSelectConversation"
              @created="handleConversationCreated"
            />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <!-- Chat Mode -->
    <div v-if="isChatMode" class="chat-mode">
      <ChatPanel
        ref="chatPanelRef"
        :chatbot-id="chatbot!.id"
        :chatbot-name="chatbot!.name"
        :chatbot-avatar="chatbot!.styleConfig?.avatarUrl"
        :user-name="userName"
        :conversation-id="currentConversationId"
        @update:conversation-id="handleUpdateConversationId"
      />
    </div>

    <!-- Edit Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑聊天机器人" width="700px" @close="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入聊天机器人名称" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="请输入描述"
          />
        </el-form-item>

        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="formData.systemPrompt"
            type="textarea"
            :rows="5"
            placeholder="请输入系统提示词"
          />
        </el-form-item>

        <el-form-item label="模型配置" prop="modelConfig">
          <el-input
            v-model="formData.modelConfig"
            type="textarea"
            :rows="3"
            placeholder='{"temperature": 0.7, "max_tokens": 2000}'
          />
        </el-form-item>

        <el-form-item label="欢迎语" prop="welcomeMessage">
          <el-input
            v-model="formData.welcomeMessage"
            type="textarea"
            :rows="2"
            placeholder="请输入欢迎语"
          />
        </el-form-item>

        <el-form-item label="温度" prop="temperature">
          <el-input-number v-model="formData.temperature" :min="0" :max="2" :step="0.1" />
        </el-form-item>

        <el-form-item label="最大 Token" prop="maxTokens">
          <el-input-number v-model="formData.maxTokens" :min="1" :max="32000" />
        </el-form-item>

        <el-form-item label="启用记忆" prop="enableMemory">
          <el-switch v-model="formData.enableMemory" />
        </el-form-item>

        <el-form-item label="是否公开" prop="isPublic">
          <el-switch v-model="formData.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit"> 确定 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Edit, ChatDotRound, UserFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getChatbot, updateChatbot, deleteChatbot } from '@/api/chatbot'
import { getConversation } from '@/api/conversation'
import { useUserStore } from '@/stores/user'
import type { Chatbot, UpdateChatbotRequest, Message, Conversation } from '@/types/chatbot'
import ConversationsList from '@/components/chatbot/ConversationsList.vue'
import ChatPanel from '@/components/chatbot/ChatPanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const chatbotId = ref<number>(parseInt(route.params.id as string))
const chatbot = ref<Chatbot | null>(null)
const loading = ref(false)
const showEditDialog = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// View mode
const activeTab = ref('info')
const isChatMode = ref(false)
const currentConversationId = ref<number | undefined>(undefined)
const chatPanelRef = ref<InstanceType<typeof ChatPanel>>()

const userName = computed(() => userStore.userInfo?.username || 'User')

const formData = reactive<
  UpdateChatbotRequest & {
    temperature?: number
    maxTokens?: number
    enableMemory?: boolean
    welcomeMessage?: string
  }
>({
  name: '',
  description: '',
  systemPrompt: '',
  modelConfig: '',
  isPublic: false,
  temperature: 0.7,
  maxTokens: 2000,
  enableMemory: true,
  welcomeMessage: ''
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入聊天机器人名称', trigger: 'blur' }],
  systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }]
}

async function fetchChatbot() {
  loading.value = true
  try {
    chatbot.value = await getChatbot(chatbotId.value)
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '获取聊天机器人详情失败'
    console.error('Failed to fetch chatbot:', error)
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push({ name: 'Chatbots' })
}

async function handleDelete() {
  if (!chatbot.value) return
  try {
    await deleteChatbot(chatbot.value.id)
    ElMessage.success('删除成功')
    goBack()
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '删除失败'
    console.error('Failed to delete chatbot:', error)
    ElMessage.error(errorMessage)
  }
}

async function handleSubmit() {
  if (!formRef.value || !chatbot.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const requestData: UpdateChatbotRequest = {
      name: formData.name,
      description: formData.description,
      systemPrompt: formData.systemPrompt,
      modelConfig: formData.modelConfig,
      isPublic: formData.isPublic,
      settings: {
        temperature: formData.temperature,
        maxTokens: formData.maxTokens,
        enableMemory: formData.enableMemory,
        welcomeMessage: formData.welcomeMessage
      }
    }

    await updateChatbot(chatbot.value.id, requestData)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    await fetchChatbot()
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '更新失败'
    console.error('Failed to update chatbot:', error)
    ElMessage.error(errorMessage)
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  if (chatbot.value) {
    formData.name = chatbot.value.name
    formData.description = chatbot.value.description || ''
    formData.systemPrompt = chatbot.value.systemPrompt
    formData.modelConfig = chatbot.value.modelConfig
    formData.isPublic = chatbot.value.isPublic
    formData.temperature = chatbot.value.settings?.temperature ?? 0.7
    formData.maxTokens = chatbot.value.settings?.maxTokens ?? 2000
    formData.enableMemory = chatbot.value.settings?.enableMemory ?? true
    formData.welcomeMessage = chatbot.value.settings?.welcomeMessage || ''
  }
}

function startChat() {
  isChatMode.value = true
  currentConversationId.value = undefined
}

function exitChat() {
  isChatMode.value = false
  currentConversationId.value = undefined
}

async function handleSelectConversation(conversation: Conversation) {
  currentConversationId.value = conversation.id
  isChatMode.value = true

  // Load conversation messages
  if (chatbot.value) {
    try {
      const data = await getConversation(chatbot.value.id, conversation.id)
      chatPanelRef.value?.clearMessages()
      data.messages.forEach((msg: Message) => {
        chatPanelRef.value?.addMessage(msg)
      })
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : '加载会话失败'
      ElMessage.error(errorMessage)
    }
  }
}

function handleConversationCreated(conversation: Conversation) {
  currentConversationId.value = conversation.id
  isChatMode.value = true
}

function handleUpdateConversationId(id: number) {
  currentConversationId.value = id
}

function formatJson(json: string) {
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch {
    return json
  }
}

function formatDateTime(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(async () => {
  await fetchChatbot()
  if (chatbot.value) {
    resetForm()
  }
})
</script>

<style scoped lang="scss">
.chatbot-detail {
  padding: 20px;
  height: calc(100vh - 100px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.detail-mode {
  .chatbot-card {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-left {
    display: flex;
    gap: 16px;
    align-items: center;
  }

  .header-info {
    h2 {
      margin: 0 0 8px 0;
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }

    .meta-info {
      display: flex;
      gap: 8px;
    }
  }
}

.json-config {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.prompt-text {
  white-space: pre-wrap;
  line-height: 1.6;
}

.suggested-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chat-mode {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.chat-panel) {
    height: 100%;
  }
}
</style>
