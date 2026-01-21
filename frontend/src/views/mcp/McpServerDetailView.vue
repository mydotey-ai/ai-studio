<template>
  <div class="mcp-server-detail">
    <div class="header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="title">{{ server?.name }}</span>
          <el-tag :type="getStatusType(server?.status)" size="small" style="margin-left: 12px">
            {{ getStatusLabel(server?.status) }}
          </el-tag>
        </template>
        <template #extra>
          <el-button :icon="Refresh" @click="handleSyncTools"> 同步工具 </el-button>
          <el-button :icon="Connection" @click="handleTestConnection"> 测试连接 </el-button>
          <el-button :icon="Edit" @click="showEditDialog = true"> 编辑 </el-button>
          <el-button type="danger" :icon="Delete" @click="handleDelete"> 删除 </el-button>
        </template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="信息" name="info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">
            {{ server?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="连接类型">
            <el-tag :type="server?.connectionType === 'STDIO' ? 'primary' : 'success'">
              {{ server?.connectionType }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ server?.description || '-' }}
          </el-descriptions-item>

          <template v-if="server?.connectionType === 'STDIO'">
            <el-descriptions-item label="命令" :span="2">
              <code>{{ server?.command || '-' }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="工作目录" :span="2">
              {{ server?.workingDir || '-' }}
            </el-descriptions-item>
          </template>

          <template v-if="server?.connectionType === 'HTTP'">
            <el-descriptions-item label="端点 URL" :span="2">
              {{ server?.endpointUrl || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="请求头" :span="2">
              <pre v-if="server?.headers">{{ formatHeaders(server.headers) }}</pre>
              <span v-else>-</span>
            </el-descriptions-item>
          </template>

          <el-descriptions-item label="认证类型">
            {{ server?.authType || 'NONE' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(server?.status)">
              {{ getStatusLabel(server?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(server?.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(server?.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="工具列表" name="tools">
        <McpToolList :server-id="serverId" :tools="tools" @refresh="loadTools" />
      </el-tab-pane>

      <el-tab-pane label="测试日志" name="logs">
        <div class="test-logs">
          <el-timeline>
            <el-timeline-item
              v-for="log in testLogs"
              :key="log.id"
              :timestamp="formatDateTime(log.timestamp)"
              :type="log.success ? 'success' : 'danger'"
            >
              <div>{{ log.message }}</div>
              <div v-if="log.error" class="error">{{ log.error }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="testLogs.length === 0" description="暂无测试记录" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showEditDialog" title="编辑 MCP 服务器" width="600px">
      <McpServerForm :server="server" @submit="handleUpdate" @cancel="showEditDialog = false" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { Refresh, Connection, Edit, Delete } from '@element-plus/icons-vue'
import {
  getMcpServer,
  updateMcpServer,
  deleteMcpServer as deleteMcpServerApi,
  syncTools as syncToolsApi,
  testConnection,
  getMcpTools
} from '@/api/mcp'
import type { McpServer, UpdateMcpServerRequest, McpTool, TestLog } from '@/types/mcp'
import McpServerForm from '@/components/mcp/McpServerForm.vue'
import McpToolList from '@/components/mcp/McpToolList.vue'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const serverId = computed(() => parseInt(route.params.id as string))
const server = ref<McpServer>()
const activeTab = ref('info')
const showEditDialog = ref(false)
const tools = ref<McpTool[]>([])
const testLogs = ref<TestLog[]>([])

async function loadServer() {
  try {
    server.value = await getMcpServer(serverId.value)
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取服务器详情失败'
    console.error('Failed to load server:', error)
    ElMessage.error(errorMessage)
  }
}

async function loadTools() {
  try {
    tools.value = await getMcpTools(serverId.value)
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取工具列表失败'
    console.error('Failed to load tools:', error)
    ElMessage.error(errorMessage)
  }
}

async function handleUpdate(data: UpdateMcpServerRequest) {
  try {
    await updateMcpServer(serverId.value, data)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    await loadServer()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '更新失败'
    console.error('Failed to update server:', error)
    ElMessage.error(errorMessage)
  }
}

async function handleSyncTools() {
  const loading = ElLoading.service({
    lock: true,
    text: '正在同步工具...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    await syncToolsApi(serverId.value)
    ElMessage.success('工具同步成功')
    await loadTools()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '工具同步失败'
    console.error('Failed to sync tools:', error)
    ElMessage.error(errorMessage)
  } finally {
    loading.close()
  }
}

async function handleTestConnection() {
  const loading = ElLoading.service({
    lock: true,
    text: '正在测试连接...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    const result = await testConnection(serverId.value)

    testLogs.value.unshift({
      id: Date.now(),
      serverId: serverId.value,
      timestamp: new Date().toISOString(),
      success: result.success,
      message: result.success ? '连接成功' : '连接失败',
      error: result.message
    })

    if (result.success) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error(`连接测试失败: ${result.message}`)
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '测试请求失败'
    ElMessage.error('测试连接时出错')
    testLogs.value.unshift({
      id: Date.now(),
      serverId: serverId.value,
      timestamp: new Date().toISOString(),
      success: false,
      message: '测试请求失败',
      error: errorMessage
    })
  } finally {
    loading.close()
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm(`确定要删除 MCP 服务器"${server.value?.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteMcpServerApi(serverId.value)
    ElMessage.success('删除成功')
    router.push('/mcp-servers')
  } catch {
    // User cancelled
  }
}

function getStatusType(status?: string): 'success' | 'info' | 'danger' {
  if (!status) return 'info'
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'INACTIVE':
      return 'info'
    case 'ERROR':
      return 'danger'
    default:
      return 'info'
  }
}

function getStatusLabel(status?: string): string {
  if (!status) return '-'
  switch (status) {
    case 'ACTIVE':
      return '活动'
    case 'INACTIVE':
      return '未激活'
    case 'ERROR':
      return '错误'
    default:
      return status
  }
}

function formatDateTime(date?: string): string {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function formatHeaders(headers: string): string {
  try {
    const parsed = JSON.parse(headers)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return headers
  }
}

onMounted(() => {
  loadServer()
  loadTools()
})
</script>

<style scoped lang="scss">
.mcp-server-detail {
  .header {
    margin-bottom: 20px;

    .title {
      font-size: 20px;
      font-weight: 500;
    }
  }

  .tabs {
    :deep(.el-descriptions) {
      margin-top: 20px;
    }

    code {
      background-color: #f5f5f5;
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Courier New', monospace;
    }

    pre {
      background-color: #f5f5f5;
      padding: 8px;
      border-radius: 4px;
      overflow-x: auto;
      font-size: 12px;
    }

    .test-logs {
      padding: 20px;

      .error {
        color: #f56c6c;
        margin-top: 8px;
        font-size: 12px;
      }
    }
  }
}
</style>
