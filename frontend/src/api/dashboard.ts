import request from './request'
import type {
  DashboardStatistics,
  TrendData,
  Activity,
  HealthStatus
} from '@/types/dashboard'

export const dashboardApi = {
  // 获取统计数据汇总
  getStatistics() {
    return request<DashboardStatistics>({
      url: '/dashboard/statistics',
      method: 'get'
    })
  },

  // 获取趋势数据
  getTrends(days: number = 7) {
    return request<TrendData[]>({
      url: `/dashboard/trends?days=${days}`,
      method: 'get'
    })
  },

  // 获取最近活动
  getRecentActivities(limit: number = 10) {
    return request<Activity[]>({
      url: `/dashboard/activities?limit=${limit}`,
      method: 'get'
    })
  },

  // 获取系统健康状态
  getHealthStatus() {
    return request<HealthStatus>({
      url: '/dashboard/health',
      method: 'get'
    })
  }
}
