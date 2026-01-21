<template>
  <div class="chatbot-list">
    <div class="header">
      <h2>聊天机器人</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        创建聊天机器人
      </el-button>
    </div>

    <el-table
      :data="chatbots"
      :loading="loading"
      stripe
      style="cursor: pointer"
      @row-click="handleRowClick"
    >
      <el-table-column label="头像" width="80" align="center">
        <template #default="{ row }">
          <el-avatar :src="row.styleConfig?.avatarUrl" :icon="UserFilled" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
      <el-table-column prop="agentName" label="绑定 Agent" min-width="150" />
      <el-table-column label="状态" width="120" align="center">
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
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
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
    <el-dialog v-model="showCreateDialog" title="创建聊天机器人" width="700px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入聊天机器人名称" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>

        <el-form-item label="绑定 Agent" prop="agentId">
          <el-select
            v-model="form.agentId"
            placeholder="请选择绑定的 Agent"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            >
              <span>{{ agent.name }}</span>
              <span style="color: #8492a6; font-size: 13px; margin-left: 10px">
                {{ agent.description }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="欢迎语" prop="welcomeMessage">
          <el-input
            v-model="form.welcomeMessage"
            type="textarea"
            :rows="2"
            placeholder="请输入欢迎语"
          />
        </el-form-item>

        <el-form-item label="头像 URL" prop="avatarUrl">
          <el-input
            v-model="form.avatarUrl"
            placeholder="请输入头像图片 URL"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit"> 创建 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, View, Delete, UserFilled } from '@element-plus/icons-vue'
import { getChatbots, createChatbot, deleteChatbot } from '@/api/chatbot'
import { getAgents } from '@/api/agent'
import type { Chatbot } from '@/types/chatbot'
import type { Agent } from '@/types/agent'
import dayjs from 'dayjs'

const router = useRouter()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitting = ref(false)
const showCreateDialog = ref(false)
const chatbots = ref<Chatbot[]>([])
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

async function loadChatbots() {
  loading.value = true
  try {
    const data = await getChatbots({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    chatbots.value = data.records
    pagination.total = data.total
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '加载聊天机器人列表失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

async function loadAgents() {
  try {
    const data = await getAgents({ page: 1, pageSize: 1000 })
    agents.value = data.records
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '加载 Agent 列表失败'
    ElMessage.error(errorMessage)
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    submitting.value = true
    try {
      // Prepare request data with defaults for fields not in form
      const requestData = {
        name: form.name,
        description: form.description,
        agentId: form.agentId!,
        settings: {
          welcomeMessage: form.welcomeMessage
        },
        styleConfig: {
          avatarUrl: form.avatarUrl
        },
        // Required by API but not in form - use defaults
        systemPrompt: '',
        modelConfig: '{}'
      }

      await createChatbot(requestData)
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      resetForm()
      loadChatbots()
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : '创建失败，请稍后重试'
      ElMessage.error(errorMessage)
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.agentId = undefined
  form.welcomeMessage = '你好，有什么可以帮助你的吗？'
  form.avatarUrl = ''
  formRef.value?.resetFields()
}

function handleRowClick(row: Chatbot) {
  router.push(`/chatbots/${row.id}`)
}

function handleView(row: Chatbot) {
  router.push(`/chatbots/${row.id}`)
}

async function handleDelete(row: Chatbot) {
  try {
    await ElMessageBox.confirm(`确定要删除聊天机器人"${row.name}"吗？此操作不可恢复。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChatbot(row.id)
    ElMessage.success('删除成功')
    loadChatbots()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const errorMessage = error instanceof Error ? error.message : '删除失败，请稍后重试'
      ElMessage.error(errorMessage)
    }
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
