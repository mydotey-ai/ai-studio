<template>
  <el-card class="stat-card" :class="{ 'card-hoverable': hoverable }">
    <div class="stat-content">
      <div class="stat-icon" :style="{ backgroundColor: iconBg }">
        <el-icon :size="24" :color="iconColor">
          <component :is="icon" />
        </el-icon>
      </div>

      <div class="stat-info">
        <div class="stat-value">{{ formattedValue }}</div>
        <div class="stat-label">{{ label }}</div>
        <div v-if="trend !== undefined" class="stat-trend" :class="trendClass">
          <el-icon :size="12">
            <component :is="trendIcon" />
          </el-icon>
          <span>{{ Math.abs(trend) }}%{{ trendLabel }}</span>
        </div>
        <div v-if="subtitle" class="stat-subtitle">{{ subtitle }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TrendCharts, ArrowDown } from '@element-plus/icons-vue'

interface Props {
  icon: any
  label: string
  value: number | string
  unit?: string
  trend?: number // 增长率,可以是正数或负数
  trendLabel?: string
  subtitle?: string
  iconColor?: string
  iconBg?: string
  hoverable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  unit: '',
  trendLabel: '较上周',
  iconColor: '#409EFF',
  iconBg: '#ECF5FF',
  hoverable: true
})

const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString()
  }
  return props.value
})

const trendClass = computed(() => {
  if (props.trend === undefined || !props.trend) return ''
  return props.trend >= 0 ? 'trend-up' : 'trend-down'
})

const trendIcon = computed(() => {
  if (props.trend === undefined) return TrendCharts
  return props.trend >= 0 ? TrendCharts : ArrowDown
})
</script>

<style scoped>
.stat-card {
  border-radius: 8px;
  transition: all 0.3s;
}

.card-hoverable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  cursor: pointer;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}

.stat-subtitle {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>
