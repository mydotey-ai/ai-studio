<template>
  <div class="web-crawl">
    <div class="header">
      <h3>网页抓取任务</h3>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        创建抓取任务
      </el-button>
    </div>

    <el-table :data="tasks" :loading="loading" stripe>
      <el-table-column prop="startUrl" label="起始URL" min-width="300" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="200">
        <template #default="{ row }">
          <el-progress
            :percentage="getProgress(row)"
            :status="row.status === 'COMPLETED' ? 'success' : undefined"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            link
            type="primary"
            :icon="VideoPlay"
            @click="handleStart(row)"
          >
            启动
          </el-button>
          <el-button link :icon="Delete" type="danger" @click="handleDelete(row)"> 删除 </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建网页抓取任务" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="起始URL" prop="startUrl">
          <el-input v-model="form.startUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="URL模式" prop="urlPattern">
          <el-input v-model="form.urlPattern" placeholder="正则表达式，如: .*\\.example\\.com/.*" />
        </el-form-item>
        <el-form-item label="最大深度" prop="maxDepth">
          <el-input-number v-model="form.maxDepth" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="抓取策略" prop="crawlStrategy">
          <el-radio-group v-model="form.crawlStrategy">
            <el-radio value="BFS">广度优先</el-radio>
            <el-radio value="DFS">深度优先</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="并发限制" prop="concurrentLimit">
          <el-input-number v-model="form.concurrentLimit" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate"> 创建 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, VideoPlay, Delete } from '@element-plus/icons-vue'
import {
  getWebCrawlTasks,
  createWebCrawlTask,
  startWebCrawlTask,
  deleteWebCrawlTask
} from '@/api/document'
import type { WebCrawlTask } from '@/types/knowledge-base'
import dayjs from 'dayjs'

interface Props {
  kbId: number
}

const props = defineProps<Props>()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitting = ref(false)
const showCreateDialog = ref(false)
const tasks = ref<WebCrawlTask[]>([])

const form = reactive({
  startUrl: '',
  urlPattern: '',
  maxDepth: 2,
  crawlStrategy: 'BFS' as 'BFS' | 'DFS',
  concurrentLimit: 3
})

const rules: FormRules = {
  startUrl: [
    { required: true, message: '请输入起始URL', trigger: 'blur' },
    { type: 'url', message: '请输入正确的URL格式', trigger: 'blur' }
  ],
  maxDepth: [{ required: true, message: '请输入最大深度', trigger: 'blur' }],
  crawlStrategy: [{ required: true, message: '请选择抓取策略', trigger: 'change' }],
  concurrentLimit: [{ required: true, message: '请输入并发限制', trigger: 'blur' }]
}

async function loadTasks() {
  if (isNaN(props.kbId)) {
    return
  }
  loading.value = true
  try {
    tasks.value = await getWebCrawlTasks(props.kbId)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    submitting.value = true
    try {
      await createWebCrawlTask({ ...form, kbId: props.kbId })
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      resetForm()
      loadTasks()
    } finally {
      submitting.value = false
    }
  })
}

async function handleStart(task: WebCrawlTask) {
  try {
    await startWebCrawlTask(task.id)
    ElMessage.success('已启动抓取任务')
    loadTasks()
  } catch (error) {
    console.error('Start task failed:', error)
    ElMessage.error('启动任务失败，请稍后重试')
  }
}

async function handleDelete(task: WebCrawlTask) {
  try {
    await ElMessageBox.confirm('确定要删除此抓取任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteWebCrawlTask(task.id)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
      ElMessage.error('删除失败')
    }
  }
}

function resetForm() {
  form.startUrl = ''
  form.urlPattern = ''
  form.maxDepth = 2
  form.crawlStrategy = 'BFS'
  form.concurrentLimit = 3
  formRef.value?.resetFields()
}

function getStatusType(status: string) {
  const map: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
    PENDING: 'info',
    RUNNING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger',
    PAUSED: 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '运行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    PAUSED: '已暂停'
  }
  return map[status] || status
}

function getProgress(task: WebCrawlTask) {
  if (task.totalPages === 0) return 0
  return Math.round((task.successPages / task.totalPages) * 100)
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadTasks()
})
</script>

<style scoped lang="scss">
.web-crawl {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }
  }
}
</style>
