<template>
  <div class="activity-timeline">
    <el-timeline>
      <el-timeline-item
        v-for="activity in activities"
        :key="activity.id"
        :timestamp="formatTime(activity.createdAt)"
        placement="top"
      >
        <el-card>
          <div class="activity-content">
            <div class="activity-header">
              <el-tag :type="getActionType(activity.action)" size="small">
                {{ formatAction(activity.action) }}
              </el-tag>
              <span class="activity-user">{{ activity.username }}</span>
            </div>
            <div class="activity-resource">
              {{ activity.resourceType }}
            </div>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import { type Activity } from '@/types/dashboard'
import dayjs from 'dayjs'

interface Props {
  activities: Activity[]
}

const props = defineProps<Props>()

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatAction = (action: string) => {
  const map: Record<string, string> = {
    'USER_LOGIN': '登录',
    'USER_LOGOUT': '登出',
    'KB_CREATE': '创建知识库',
    'KB_UPDATE': '更新知识库',
    'KB_DELETE': '删除知识库',
    'AGENT_CREATE': '创建 Agent',
    'AGENT_UPDATE': '更新 Agent',
    'AGENT_DELETE': '删除 Agent',
    'AGENT_EXECUTE': '执行 Agent',
    'CHATBOT_CREATE': '创建聊天机器人',
    'CHATBOT_UPDATE': '更新聊天机器人',
    'CHATBOT_DELETE': '删除聊天机器人'
  }
  return map[action] || action
}

const getActionType = (action: string) => {
  if (action.includes('CREATE')) return 'success'
  if (action.includes('DELETE')) return 'danger'
  if (action.includes('UPDATE')) return 'warning'
  if (action.includes('LOGIN')) return 'info'
  return ''
}
</script>

<style scoped>
.activity-timeline {
  padding: 10px 0;
}

.activity-content {
  padding: 0;
}

.activity-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.activity-user {
  font-size: 14px;
  color: #606266;
}

.activity-resource {
  font-size: 12px;
  color: #909399;
}
</style>
