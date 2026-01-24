<template>
  <div class="data-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>数据管理</h2>
      <p>管理您的 AI 助手数据导入导出</p>
    </div>

    <!-- 操作按钮区域 -->
    <div class="action-section">
      <el-card class="action-card">
        <template #header>
          <div class="card-header">
            <span>数据导入</span>
          </div>
        </template>
        <div class="card-content">
          <p>导入之前导出的数据，支持 .json 格式文件</p>
          <el-button type="primary" @click="openImportDialog">
            <el-icon><upload-filled /></el-icon>
            导入数据
          </el-button>
        </div>
      </el-card>

      <el-card class="action-card">
        <template #header>
          <div class="card-header">
            <span>数据导出</span>
          </div>
        </template>
        <div class="card-content">
          <p>导出您的数据到 JSON 文件，支持全部数据或选择范围</p>
          <el-button type="primary" @click="openExportDialog">
            <el-icon><download /></el-icon>
            导出数据
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 任务列表区域 -->
    <div class="tasks-section">
      <el-card class="tasks-card">
        <template #header>
          <div class="card-header">
            <span>任务记录</span>
            <el-radio-group v-model="activeTab" size="small">
              <el-radio-button label="export">导出任务</el-radio-button>
              <el-radio-button label="import">导入任务</el-radio-button>
            </el-radio-group>
          </div>
        </template>

        <!-- 导出任务列表 -->
        <el-table
          v-if="activeTab === 'export'"
          :data="exportTasks"
          style="width: 100%"
          stripe
          :loading="loading.export"
        >
          <el-table-column prop="id" label="任务ID" width="80" />
          <el-table-column prop="scope" label="导出范围" width="120">
            <template #default="scope">
              {{ getScopeLabel(scope.row.scope) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="文件大小" width="100">
            <template #default="scope">
              {{ formatFileSize(scope.row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column prop="completedAt" label="完成时间" width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 'COMPLETED'"
                type="text"
                size="small"
                @click="handleDownload"
              >
                下载
              </el-button>
              <el-button
                type="text"
                size="small"
                @click="handleViewDetail"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 导入任务列表 -->
        <el-table
          v-if="activeTab === 'import'"
          :data="importTasks"
          style="width: 100%"
          stripe
          :loading="loading.import"
        >
          <el-table-column prop="id" label="任务ID" width="80" />
          <el-table-column prop="strategy" label="导入策略" width="120">
            <template #default="scope">
              {{ getStrategyLabel(scope.row.strategy) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column prop="completedAt" label="完成时间" width="180" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default>
              <el-button type="text" size="small" @click="handleViewDetail">
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 对话框 -->
    <ExportDialog ref="exportDialogRef" />
    <ImportDialog ref="importDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Download } from '@element-plus/icons-vue'
import ExportDialog from '@/components/data-management/ExportDialog.vue'
import ImportDialog from '@/components/data-management/ImportDialog.vue'
import {
  ExportScopeLabels,
  ImportStrategyLabels,
  ExportTask,
  ImportTask
} from '@/types/data-management'
import {
  getExportTasks,
  getImportTasks
} from '@/api/data-management'

// 组件引用
const exportDialogRef = ref()
const importDialogRef = ref()

// 响应式数据
const activeTab = ref('export')
const loading = ref({
  export: false,
  import: false
})

const exportTasks = ref<ExportTask[]>([])
const importTasks = ref<ImportTask[]>([])

// 方法
const openExportDialog = () => {
  exportDialogRef.value?.open()
}

const openImportDialog = () => {
  importDialogRef.value?.open()
}

const getScopeLabel = (scope: string) => {
  return ExportScopeLabels[scope as keyof typeof ExportScopeLabels] || scope
}

const getStrategyLabel = (strategy: string) => {
  return ImportStrategyLabels[strategy as keyof typeof ImportStrategyLabels] || strategy
}

const getStatusLabel = (status: string) => {
  const statusMap: Record<string, string> = {
    PENDING: '待处理',
    IN_PROGRESS: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败',
    VALIDATING: '验证中',
    VALIDATED: '已验证',
    IMPORTING: '导入中'
  }
  return statusMap[status] || status
}

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    PENDING: 'info',
    IN_PROGRESS: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger',
    VALIDATING: 'info',
    VALIDATED: 'warning',
    IMPORTING: 'warning'
  }
  return typeMap[status] || 'info'
}

const formatFileSize = (bytes: number) => {
  if (!bytes) return '-'

  if (bytes < 1024) {
    return bytes + ' B'
  } else if (bytes < 1024 * 1024) {
    return (bytes / 1024).toFixed(1) + ' KB'
  } else if (bytes < 1024 * 1024 * 1024) {
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  } else {
    return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
  }
}

const handleDownload = () => {
  ElMessage.warning('下载功能暂未实现')
}

const handleViewDetail = () => {
  ElMessage.warning('详情功能暂未实现')
}

// 加载数据
const loadExportTasks = async () => {
  loading.value.export = true
  try {
    const response = await getExportTasks()
    exportTasks.value = response.data || []
  } catch (error) {
    console.error('加载导出任务失败:', error)
    ElMessage.error('加载导出任务失败')
  } finally {
    loading.value.export = false
  }
}

const loadImportTasks = async () => {
  loading.value.import = true
  try {
    const response = await getImportTasks()
    importTasks.value = response.data || []
  } catch (error) {
    console.error('加载导入任务失败:', error)
    ElMessage.error('加载导入任务失败')
  } finally {
    loading.value.import = false
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadExportTasks()
  loadImportTasks()
})
</script>

<style scoped>
.data-management {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 8px 0 0 0;
  font-size: 14px;
  color: #909399;
}

.action-section {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.action-card {
  flex: 1;
  min-width: 300px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-content {
  padding: 20px;
  text-align: center;
}

.card-content p {
  margin: 0 0 20px 0;
  color: #606266;
}

.tasks-section {
  margin-bottom: 30px;
}

.tasks-card {
  min-height: 400px;
}
</style>
