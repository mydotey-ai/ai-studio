<template>
  <router-view v-slot="{ Component: RouteComponent, route }">
    <div v-if="route.name === 'McpServers'" class="mcp-server-list">
    <div class="header">
      <h2>MCP服务器</h2>
      <el-button type="primary" :icon="Plus" @click="handleCreate"> 创建服务器 </el-button>
    </div>

    <el-table
      :data="servers"
      :loading="loading"
      stripe
      style="cursor: pointer"
      @row-click="handleRowClick"
    >
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
      <el-table-column label="连接类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.connectionType === 'STDIO' ? 'primary' : 'success'" size="small">
            {{ row.connectionType === 'STDIO' ? 'STDIO' : 'HTTP' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click.stop="handleView(row)">
            详情
          </el-button>
          <el-button link type="primary" :icon="Refresh" @click.stop="handleSyncTools(row)">
            同步工具
          </el-button>
          <el-button link type="primary" :icon="Edit" @click.stop="handleEdit(row)">
            编辑
          </el-button>
          <el-button link type="danger" :icon="Delete" @click.stop="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

    <component v-else :is="RouteComponent" />
  </router-view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { Plus, View, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { getMcpServers, deleteMcpServer, syncTools, type McpServerListItem } from '@/api/mcp'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const servers = ref<McpServerListItem[]>([])

async function loadServers() {
  loading.value = true
  try {
    const data = await getMcpServers()
    servers.value = data
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '加载MCP服务器列表失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

function getStatusType(status: string): 'success' | 'info' | 'danger' {
  const statusMap: Record<string, 'success' | 'info' | 'danger'> = {
    ACTIVE: 'success',
    INACTIVE: 'info',
    ERROR: 'danger'
  }
  return statusMap[status] || 'info'
}

function getStatusLabel(status: string): string {
  const labelMap: Record<string, string> = {
    ACTIVE: '活动',
    INACTIVE: '未激活',
    ERROR: '错误'
  }
  return labelMap[status] || status
}

function handleRowClick(row: McpServerListItem) {
  router.push(`/mcp-servers/${row.id}`)
}

function handleView(row: McpServerListItem) {
  router.push(`/mcp-servers/${row.id}`)
}

function handleEdit(row: McpServerListItem) {
  router.push(`/mcp-servers/${row.id}?mode=edit`)
}

function handleCreate() {
  router.push('/mcp-servers/create')
}

async function handleSyncTools(row: McpServerListItem) {
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '正在同步工具...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    await syncTools(row.id)
    ElMessage.success('同步工具成功')
    loadServers()
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '同步工具失败'
    ElMessage.error(errorMessage)
  } finally {
    loadingInstance.close()
  }
}

async function handleDelete(row: McpServerListItem) {
  try {
    await ElMessageBox.confirm(`确定要删除MCP服务器"${row.name}"吗？此操作不可恢复。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteMcpServer(row.id)
    ElMessage.success('删除成功')
    loadServers()
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
  loadServers()
})
</script>

<style scoped lang="scss">
.mcp-server-list {
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
}
</style>
