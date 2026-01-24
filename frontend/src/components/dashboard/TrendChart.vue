<template>
  <div class="trend-chart">
    <v-chart class="chart" :option="chartOption" :loading="loading" autoresize />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { TrendData } from '@/types/dashboard'

use([LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

interface Props {
  data: TrendData[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const chartOption = computed(() => {
  const dates = props.data.map(d => d.date)
  const apiCalls = props.data.map(d => d.apiCalls)
  const activeUsers = props.data.map(d => d.activeUsers)

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['API 调用', '活跃用户'],
      top: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
    },
    yAxis: [
      {
        type: 'value',
        name: 'API 调用',
        position: 'left',
        axisLine: {
          show: true,
          lineStyle: {
            color: '#5470C6'
          }
        },
        axisLabel: {
          formatter: '{value}'
        }
      },
      {
        type: 'value',
        name: '活跃用户',
        position: 'right',
        axisLine: {
          show: true,
          lineStyle: {
            color: '#91CC75'
          }
        },
        axisLabel: {
          formatter: '{value}'
        }
      }
    ],
    series: [
      {
        name: 'API 调用',
        type: 'line',
        yAxisIndex: 0,
        smooth: true,
        data: apiCalls,
        itemStyle: {
          color: '#5470C6'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(84, 112, 198, 0.3)' },
              { offset: 1, color: 'rgba(84, 112, 198, 0.05)' }
            ]
          }
        }
      },
      {
        name: '活跃用户',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: activeUsers,
        itemStyle: {
          color: '#91CC75'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(145, 204, 117, 0.3)' },
              { offset: 1, color: 'rgba(145, 204, 117, 0.05)' }
            ]
          }
        }
      }
    ]
  }
})
</script>

<style scoped>
.trend-chart {
  width: 100%;
  height: 320px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
