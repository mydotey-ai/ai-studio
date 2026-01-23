<template>
  <div class="resource-pie-chart">
    <v-chart
      class="chart"
      :option="chartOption"
      :loading="loading"
      autoresize
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import type { DashboardStatistics } from '@/types/dashboard'

use([PieChart, TitleComponent, TooltipComponent, LegendComponent])

interface Props {
  data: DashboardStatistics
}

const props = defineProps<Props>()

const loading = ref(false)

const chartOption = computed(() => {
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '资源统计',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          {
            value: props.data.knowledgeBases.totalCount,
            name: '知识库',
            itemStyle: { color: '#5470C6' }
          },
          {
            value: props.data.agents.totalCount,
            name: 'Agent',
            itemStyle: { color: '#91CC75' }
          },
          {
            value: props.data.chatbots.totalCount,
            name: '聊天机器人',
            itemStyle: { color: '#FAC858' }
          },
          {
            value: props.data.documents.totalCount,
            name: '文档',
            itemStyle: { color: '#EE6666' }
          },
          {
            value: props.data.users.totalCount,
            name: '用户',
            itemStyle: { color: '#73C0DE' }
          },
          {
            value: props.data.storage.fileCount,
            name: '存储文件',
            itemStyle: { color: '#3BA272' }
          }
        ]
      }
    ]
  }
})
</script>

<style scoped>
.resource-pie-chart {
  width: 100%;
  height: 300px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
