<template>
  <div class="audit-logs">
    <div class="header">
      <h3>审计日志</h3>
      <el-text type="info" size="small">查看系统操作记录</el-text>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="操作类型">
          <el-input v-model="query.action" placeholder="操作类型" clearable @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select
            v-model="query.resourceType"
            placeholder="资源类型"
            clearable
            @clear="handleSearch"
          >
            <el-option label="用户" value="User" />
            <el-option label="组织" value="Organization" />
            <el-option label="知识库" value="KnowledgeBase" />
            <el-option label="文档" value="Document" />
            <el-option label="Agent" value="Agent" />
            <el-option label="聊天机器人" value="Chatbot" />
            <el-option label="MCP服务器" value="McpServer" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"> 查询 </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table v-loading="loading" :data="logs" :loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="action" label="操作类型" width="180">
        <template #default="{ row }">
          <el-tag size="small">{{ formatAction(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resourceType" label="资源类型" width="120" />
      <el-table-column prop="resourceId" label="资源ID" width="100" />
      <el-table-column prop="ipAddress" label="IP地址" width="140" />
      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetails(row)">
            详情
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
        @current-change="loadLogs"
        @size-change="loadLogs"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="日志详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ selectedLog?.id }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ selectedLog?.username }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{
          formatAction(selectedLog?.action || '')
        }}</el-descriptions-item>
        <el-descriptions-item label="资源类型">{{
          selectedLog?.resourceType
        }}</el-descriptions-item>
        <el-descriptions-item label="资源ID">{{ selectedLog?.resourceId }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ selectedLog?.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="User Agent">
          <el-text truncated>{{ selectedLog?.userAgent }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="时间">
          {{ selectedLog ? formatDate(selectedLog.createdAt) : '' }}
        </el-descriptions-item>
        <el-descriptions-item label="详情">
          <pre class="json-details">{{ formatDetails(selectedLog?.details) }}</pre>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { auditApi } from '@/api/audit'
import type { AuditLog, AuditLogQuery } from '@/types/audit'
import dayjs from 'dayjs'

const logs = ref<AuditLog[]>([])
const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedLog = ref<AuditLog>()
const dateRange = ref<[string, string] | null>(null)

const query = ref<AuditLogQuery>({
  page: 1,
  pageSize: 20
})

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0
})

const loadLogs = async () => {
  loading.value = true
  try {
    const data = await auditApi.queryAuditLogs(query.value)
    logs.value = data.records
    pagination.value.total = data.total
    pagination.value.page = data.current
    pagination.value.pageSize = data.size
  } catch (error) {
    ElMessage.error('加载审计日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.value.page = 1
  loadLogs()
}

const handleReset = () => {
  query.value = {
    page: 1,
    pageSize: 20
  }
  dateRange.value = null
  loadLogs()
}

const handleDateChange = (values: [string, string] | null) => {
  if (values) {
    query.value.startDate = values[0]
    query.value.endDate = values[1]
  } else {
    delete query.value.startDate
    delete query.value.endDate
  }
}

const handleViewDetails = (log: AuditLog) => {
  selectedLog.value = log
  detailDialogVisible.value = true
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const formatAction = (action: string) => {
  const map: Record<string, string> = {
    USER_LOGIN: '用户登录',
    USER_LOGOUT: '用户登出',
    USER_CREATE: '创建用户',
    USER_UPDATE: '更新用户',
    USER_DELETE: '删除用户',
    USER_STATUS_UPDATE: '更新用户状态',
    ORGANIZATION_CREATE: '创建组织',
    ORGANIZATION_UPDATE: '更新组织',
    KB_CREATE: '创建知识库',
    KB_UPDATE: '更新知识库',
    KB_DELETE: '删除知识库',
    DOC_UPLOAD: '上传文档',
    DOC_DELETE: '删除文档',
    AGENT_CREATE: '创建Agent',
    AGENT_UPDATE: '更新Agent',
    AGENT_DELETE: '删除Agent',
    AGENT_EXECUTE: '执行Agent',
    CHATBOT_CREATE: '创建聊天机器人',
    CHATBOT_UPDATE: '更新聊天机器人',
    CHATBOT_DELETE: '删除聊天机器人',
    MCP_CREATE: '创建MCP服务器',
    MCP_UPDATE: '更新MCP服务器',
    MCP_DELETE: '删除MCP服务器',
    STORAGE_CONFIG_CREATE: '创建存储配置',
    STORAGE_CONFIG_UPDATE: '更新存储配置',
    STORAGE_CONFIG_DELETE: '删除存储配置'
  }
  return map[action] || action
}

const formatDetails = (details?: string) => {
  if (!details) return '{}'
  try {
    return JSON.stringify(JSON.parse(details), null, 2)
  } catch {
    return details
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.audit-logs {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}

.filter-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.json-details {
  margin: 0;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  max-height: 300px;
  overflow-y: auto;
}
</style>
