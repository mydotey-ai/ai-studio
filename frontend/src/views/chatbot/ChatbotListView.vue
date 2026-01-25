<template>
  <router-view v-slot="{ Component: RouteComponent, route }">
    <div v-if="route.name === 'Chatbots'" class="chatbot-list">
    <div class="header">
      <h2>聊天机器人</h2>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog"> 创建聊天机器人 </el-button>
    </div>

    <el-table
      :data="chatbots"
      :loading="loading"
      stripe
      style="cursor: pointer"
      @row-click="handleRowClick"
    >
      <el-table-column label="头像" width="80">
        <template #default="{ row }">
          <el-avatar :size="50" :src="row.avatarUrl">
            {{ row.name.charAt(0) }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="agentName" label="绑定 Agent" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isPublished ? 'success' : 'info'" size="small">
            {{ row.isPublished ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="访问次数" width="100" align="center">
        <template #default="{ row }">
          {{ row.accessCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="ChatDotSquare" @click.stop="handleChat(row)">
            对话
          </el-button>
          <el-button link type="primary" :icon="View" @click.stop="handleView(row)">
            查看
          </el-button>
          <el-button link type="danger" :icon="Delete" @click.stop="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadChatbots"
        @current-change="loadChatbots"
      />
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建聊天机器人" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入聊天机器人名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="绑定 Agent" prop="agentId">
          <el-select v-model="form.agentId" placeholder="请选择 Agent" style="width: 100%">
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="欢迎语">
          <el-input
            v-model="form.welcomeMessage"
            type="textarea"
            :rows="2"
            placeholder="请输入欢迎语"
          />
        </el-form-item>
        <el-form-item label="头像 URL">
          <el-input v-model="form.avatarUrl" placeholder="请输入头像 URL (可选)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate"> 创建 </el-button>
      </template>
    </el-dialog>
    </div>

    <component v-else :is="RouteComponent" />
  </router-view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, ChatDotSquare, View, Delete } from '@element-plus/icons-vue'
import { getChatbots, createChatbot, deleteChatbot as deleteChatbotApi } from '@/api/chatbot'
import { getAgents, type Agent } from '@/api/agent'
import type { ChatbotResponse } from '@/types/chatbot'
import type { PaginationResponse } from '@/types/common'
import dayjs from 'dayjs'

const router = useRouter()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitting = ref(false)
const showCreateDialog = ref(false)

function openCreateDialog() {
  if (agents.value.length === 0) {
    ElMessage.warning('暂无可用的 Agent，请先创建 Agent')
    return
  }
  showCreateDialog.value = true
}
const chatbots = ref<ChatbotResponse[]>([])
const agents = ref<Agent[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const form = reactive({
  name: '',
  description: '',
  agentId: undefined as number | undefined,
  welcomeMessage: '你好，有什么可以帮助你的吗？',
  avatarUrl: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入聊天机器人名称', trigger: 'blur' }],
  agentId: [{ required: true, message: '请选择 Agent', trigger: 'change' }]
}

async function loadChatbots(forceRefresh: boolean = false) {
  loading.value = true
  try {
    const requestConfig: any = {
      params: {
        page: pagination.page,
        pageSize: pagination.pageSize
      }
    }

    // 如果需要强制刷新，则跳过缓存
    if (forceRefresh) {
      requestConfig.headers = { 'X-Skip-Cache': 'true' }
    }

    const data = await getChatbots(requestConfig.params, requestConfig)

    // Handle both paginated and non-paginated responses
    const paginatedData = data as unknown as PaginationResponse<ChatbotResponse>
    if (paginatedData.records !== undefined) {
      chatbots.value = paginatedData.records
      pagination.total = paginatedData.total
    } else {
      chatbots.value = data as ChatbotResponse[]
    }
  } catch (error) {
    console.error('Failed to load chatbots:', error)
    ElMessage.error('加载聊天机器人列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function loadAgents() {
  try {
    const data = await getAgents()
    agents.value = data.records
  } catch (error) {
    console.error('Failed to load agents:', error)
    ElMessage.error('加载 Agent 列表失败，请稍后重试')
  }
}

async function handleCreate() {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    // Ensure agentId is defined (validation should have already checked this)
    if (form.agentId === undefined) {
      ElMessage.error('请选择 Agent')
      return
    }

    submitting.value = true
    try {
      await createChatbot({
        agentId: form.agentId,
        name: form.name,
        description: form.description,
        welcomeMessage: form.welcomeMessage,
        avatarUrl: form.avatarUrl || undefined
      })
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      resetForm()
      await loadChatbots(true) // 强制刷新以获取最新数据
    } catch (error) {
      console.error('Failed to create chatbot:', error)
      ElMessage.error('创建聊天机器人失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  formRef.value?.resetFields()
  // Reset to default values
  form.welcomeMessage = '你好，有什么可以帮助你的吗？'
}

function handleRowClick(row: ChatbotResponse) {
  router.push(`/chatbots/${row.id}`)
}

function handleChat(row: ChatbotResponse) {
  router.push(`/chatbots/${row.id}?mode=chat`)
}

function handleView(row: ChatbotResponse) {
  router.push(`/chatbots/${row.id}`)
}

async function handleDelete(row: ChatbotResponse) {
  try {
    await ElMessageBox.confirm(`确定要删除聊天机器人"${row.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChatbotApi(row.id)
    ElMessage.success('删除成功')
    // 清除相关缓存并强制重新加载数据
    await loadChatbots(true)
  } catch {
    // User cancelled
  }
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadChatbots()
  loadAgents()
})
</script>

<style scoped lang="scss">
.chatbot-list {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 24px;
      color: #303133;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
