<template>
  <div class="dashboard-view">
    <div class="dashboard-header">
      <h2>AI Studio 概览</h2>
      <el-button :icon="Refresh" @click="refreshAll" :loading="refreshing">
        刷新
      </el-button>
    </div>

    <!-- 统计卡片区域 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="stat in statistics" :key="stat.key">
        <StatCard
          :icon="stat.icon"
          :label="stat.label"
          :value="stat.value"
          :unit="stat.unit"
          :trend="stat.trend"
          :trendLabel="stat.trendLabel"
          :subtitle="stat.subtitle"
          :iconColor="stat.iconColor"
          :iconBg="stat.iconBg"
          @click="handleCardClick(stat.key)"
        />
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 左侧图表区 -->
      <el-col :xs="24" :md="14">
        <el-card class="chart-card">
          <template #header>
            <span>资源分布</span>
          </template>
          <ResourcePieChart :data="statisticsData" v-if="statisticsData" />
        </el-card>
      </el-col>

      <!-- 右侧图表区 -->
      <el-col :xs="24" :md="10">
        <el-card class="chart-card">
          <template #header>
            <span>最近活动</span>
          </template>
          <ActivityTimeline :activities="activities" v-if="activities" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/dashboard/StatCard.vue'
import ResourcePieChart from '@/components/dashboard/ResourcePieChart.vue'
import ActivityTimeline from '@/components/dashboard/ActivityTimeline.vue'
import { dashboardApi } from '@/api/dashboard'
import type { DashboardStatistics, Activity } from '@/types/dashboard'
import {
  FolderOpened,
  User,
  ChatDotSquare,
  Document,
  Files
} from '@element-plus/icons-vue'

const statisticsData = ref<DashboardStatistics | null>(null)
const activities = ref<Activity[]>([])
const refreshing = ref(false)

let refreshTimer: number | null = null

// 统计卡片配置
const statistics = ref([
  {
    key: 'knowledgeBases',
    label: '知识库',
    icon: FolderOpened,
    value: 0,
    unit: '个',
    trend: 12,
    trendLabel: '较上周',
    subtitle: '活跃 / 归档',
    iconColor: '#5470C6',
    iconBg: '#ECF5FF'
  },
  {
    key: 'agents',
    label: 'Agent',
    icon: User,
    value: 0,
    unit: '个',
    trend: 3,
    trendLabel: '本月新增',
    subtitle: 'ReAct / Workflow',
    iconColor: '#91CC75',
    iconBg: '#E6F7FE'
  },
  {
    key: 'chatbots',
    label: '聊天机器人',
    icon: ChatDotSquare,
    value: 0,
    unit: '个',
    trend: -5,
    trendLabel: '较上周',
    subtitle: '已发布 / 草稿',
    iconColor: '#FAC858',
    iconBg: '#FFF7E6'
  },
  {
    key: 'documents',
    label: '文档',
    icon: Document,
    value: 0,
    unit: '个',
    trend: 8,
    trendLabel: '较上周',
    subtitle: '处理中 / 已完成',
    iconColor: '#EE6666',
    iconBg: '#FFEBEE'
  },
  {
    key: 'users',
    label: '用户',
    icon: User,
    value: 0,
    unit: '人',
    trend: 15,
    trendLabel: '本周新增',
    subtitle: '管理员 / 普通用户',
    iconColor: '#73C0DE',
    iconBg: '#E6F7FE'
  },
  {
    key: 'storage',
    label: '存储',
    icon: Files,
    value: 0,
    unit: 'GB',
    trend: 2,
    trendLabel: '较上周',
    subtitle: 'LOCAL / OSS / S3',
    iconColor: '#3BA272',
    iconBg: '#E6F8F3'
  }
])

const loadStatistics = async () => {
  try {
    const data = await dashboardApi.getStatistics()
    statisticsData.value = data

    // 更新卡片数据
    statistics.value[0].value = data.knowledgeBases.totalCount
    statistics.value[0].subtitle =
      `活跃 ${data.knowledgeBases.activeCount} / 归档 ${data.knowledgeBases.archivedCount}`
    statistics.value[0].trend = data.knowledgeBases.weeklyGrowthRate

    statistics.value[1].value = data.agents.totalCount
    statistics.value[1].subtitle =
      `ReAct ${data.agents.reactCount} / Workflow ${data.agents.workflowCount}`
    statistics.value[1].trend = data.agents.monthlyNewCount

    statistics.value[2].value = data.chatbots.totalCount
    statistics.value[2].subtitle =
      `已发布 ${data.chatbots.publishedCount} / 草稿 ${data.chatbots.draftCount}`

    statistics.value[3].value = data.documents.totalCount
    statistics.value[3].subtitle =
      `处理中 ${data.documents.processingCount} / 已完成 ${data.documents.completedCount}`

    statistics.value[4].value = data.users.totalCount
    statistics.value[4].subtitle =
      `管理员 ${data.users.adminCount} / 普通用户 ${data.users.regularCount}`

    statistics.value[5].value = (data.storage.totalSizeBytes / (1024 * 1024 * 1024)).toFixed(1)
    statistics.value[5].subtitle =
      `文件 ${data.storage.fileCount} 个`
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  }
}

const loadActivities = async () => {
  try {
    const data = await dashboardApi.getRecentActivities(10)
    activities.value = data
  } catch (error) {
    ElMessage.error('加载活动记录失败')
  }
}

const refreshAll = async () => {
  refreshing.value = true
  try {
    await Promise.all([loadStatistics(), loadActivities()])
    ElMessage.success('刷新成功')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

const handleCardClick = (key: string) => {
  // 根据点击的卡片跳转到相应页面
  const routes: Record<string, string> = {
    knowledgeBases: '/knowledge-bases',
    agents: '/agents',
    chatbots: '/chatbots',
    documents: '/knowledge-bases', // 文档暂无独立页面
    users: '/settings', // 用户管理在设置中
    storage: '/settings'  // 存储配置在设置中
  }

  const route = routes[key]
  if (route) {
    // 使用 vue-router 跳转
    console.log('Navigate to:', route)
  }
}

// 自动刷新 (30秒)
const startAutoRefresh = () => {
  refreshTimer = window.setInterval(() => {
    loadActivities()
  }, 30000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadStatistics()
  loadActivities()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.dashboard-view {
  padding: 20px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dashboard-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.stats-row {
  margin-bottom: 20px;
}

.charts-row {
  margin-top: 20px;
}

.chart-card {
  height: 100%;
}

.chart-card :deep(.el-card__body) {
  min-height: 300px;
}
</style>
