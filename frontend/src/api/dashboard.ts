import { get } from './request'
import type { DashboardStatistics, TrendData, Activity, HealthStatus } from '@/types/dashboard'

export const dashboardApi = {
  // 获取统计数据汇总
  getStatistics(): Promise<DashboardStatistics> {
    return get('/dashboard/statistics')
  },

  // 获取趋势数据
  getTrends(days: number = 7): Promise<TrendData[]> {
    return get(`/dashboard/trends?days=${days}`)
  },

  // 获取最近活动
  getRecentActivities(limit: number = 10): Promise<Activity[]> {
    return get(`/dashboard/activities?limit=${limit}`)
  },

  // 获取系统健康状态
  getHealthStatus(): Promise<HealthStatus> {
    return get('/dashboard/health')
  }
}
