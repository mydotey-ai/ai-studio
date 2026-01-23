# 仪表盘趋势图表实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 为 AI Studio 仪表盘添加趋势图表组件，显示 API 调用量和活跃用户数的时间序列折线图。

**架构:** 前端使用 ECharts 折线图渲染趋势数据，后端已有 getTrends API（返回日期、API 调用、活跃用户），前端通过 dashboardApi.getTrends() 获取数据并可视化。

**Tech Stack:** Vue 3.5, TypeScript, ECharts 5.5, vue-echarts 6.6, Element Plus

---

## 实施任务概览

本计划包含以下任务组:

1. **TrendChart 组件** - 创建趋势折线图组件（双 Y 轴：API 调用量 + 活跃用户）
2. **DashboardView 集成** - 在仪表盘页面添加趋势图表卡片
3. **类型和测试** - 确保 TypeScript 类型安全并验证构建

---

## Task Group 1: TrendChart 组件

### Task 1.1: 创建 TrendChart 组件

**Files:**
- Create: `frontend/src/components/dashboard/TrendChart.vue`

**Step 1: 创建组件模板和逻辑**

创建文件: `frontend/src/components/dashboard/TrendChart.vue`

```vue
<template>
  <div class="trend-chart">
    <v-chart
      class="chart"
      :option="chartOption"
      :loading="loading"
      autoresize
    />
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
import type { TrendData } from '@/types/dashboard'

use([
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

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
```

**Step 2: 提交 TrendChart 组件**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio
git add frontend/src/components/dashboard/TrendChart.vue
git commit -m "feat: add TrendChart component with dual Y-axis line chart"
```

---

## Task Group 2: DashboardView 集成

### Task 2.1: 修改 DashboardView 添加趋势图表

**Files:**
- Modify: `frontend/src/views/DashboardView.vue`

**Step 1: 在 template 中添加趋势图表区域**

在图表区域 `<el-row class="charts-row">` 后添加新的行:

```vue
    <!-- 趋势图表区域 -->
    <el-row :gutter="20" class="trend-row">
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>趋势分析</span>
              <el-radio-group v-model="trendDays" @change="loadTrends" size="small">
                <el-radio-button :label="7">近 7 天</el-radio-button>
                <el-radio-button :label="14">近 14 天</el-radio-button>
                <el-radio-button :label="30">近 30 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <TrendChart :data="trends" :loading="trendsLoading" v-if="trends.length > 0" />
          <el-empty v-else description="暂无趋势数据" />
        </el-card>
      </el-col>
    </el-row>
```

插入位置: 在 `<!-- 图表区域 -->` 的 `<el-row class="charts-row">` 结束后（约第 49 行后）

**Step 2: 在 script 中添加导入和状态变量**

在 import 区域添加:

```typescript
import TrendChart from '@/components/dashboard/TrendChart.vue'
import type { TrendData } from '@/types/dashboard'
```

在 script 顶部的 ref 声明区域添加:

```typescript
const trends = ref<TrendData[]>([])
const trendsLoading = ref(false)
const trendDays = ref(7)
```

**Step 3: 添加 loadTrends 函数**

在 `loadActivities` 函数后添加:

```typescript
const loadTrends = async () => {
  trendsLoading.value = true
  try {
    const data = await dashboardApi.getTrends(trendDays.value)
    trends.value = data.data
  } catch (error) {
    ElMessage.error('加载趋势数据失败')
  } finally {
    trendsLoading.value = false
  }
}
```

**Step 4: 在 refreshAll 中添加趋势刷新**

修改 `refreshAll` 函数:

```typescript
const refreshAll = async () => {
  refreshing.value = true
  try {
    await Promise.all([loadStatistics(), loadActivities(), loadTrends()])
    ElMessage.success('刷新成功')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}
```

**Step 5: 在 onMounted 中添加趋势加载**

修改 `onMounted`:

```typescript
onMounted(() => {
  loadStatistics()
  loadActivities()
  loadTrends()
  startAutoRefresh()
})
```

**Step 6: 添加样式**

在 `<style scoped>` 区域添加:

```css
.trend-row {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
```

**Step 7: 验证修改**

运行:

```bash
cd frontend
npm run build
```

预期输出: 构建成功，无类型错误

**Step 8: 提交 DashboardView 修改**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio
git add frontend/src/views/DashboardView.vue
git commit -m "feat: integrate TrendChart into DashboardView with day selector"
```

---

## Task Group 3: 类型验证和测试

### Task 3.1: 验证 TrendData 类型定义

**Files:**
- Check: `frontend/src/types/dashboard.ts`

**Step 1: 确认 TrendData 类型存在**

检查文件 `frontend/src/types/dashboard.ts` 是否包含以下定义:

```typescript
export interface TrendData {
  date: string // YYYY-MM-DD
  apiCalls: number
  activeUsers: number
}
```

如果不存在，在文件中添加此定义。

**Step 2: 如果添加了类型定义，提交**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio
git add frontend/src/types/dashboard.ts
git commit -m "feat: ensure TrendData type definition exists"
```

---

### Task 3.2: 类型检查和构建测试

**Step 1: TypeScript 类型检查**

```bash
cd frontend
npm run build
```

预期输出:
```
frontend/dist/index.html                  0.46 kB │ gzip:  0.30 kB
frontend/dist/assets/[...].js             XXX kB │ gzip: XX kB
Build completed in X.XXs
```

**Step 2: ESLint 检查**

```bash
npm run lint
```

预期输出: 无新的错误或警告

**Step 3: 后端编译验证（确保后端 API 存在）**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio
mvn clean compile
```

预期输出: `BUILD SUCCESS`

---

## Task Group 4: 手动测试验证

### Task 4.1: 前端开发服务器测试

**Step 1: 启动后端服务器**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio
mvn spring-boot:run
```

等待应用启动完成

**Step 2: 启动前端开发服务器**

在另一个终端:

```bash
cd frontend
npm run dev
```

**Step 3: 访问仪表盘页面**

打开浏览器访问: http://localhost:5173/dashboard

**验证清单:**
- [ ] 页面加载成功
- [ ] 统计卡片正常显示
- [ ] 资源分布饼图正常显示
- [ ] 活动时间线正常显示
- [ ] **新增：趋势图表卡片显示**
- [ ] 趋势图表显示两条折线（API 调用 + 活跃用户）
- [ ] 趋势图表双 Y 轴正确标注
- [ ] 点击"近 7 天/14 天/30 天"切换，数据更新
- [ ] 鼠标悬停显示 tooltip
- [ ] 折线图有平滑曲线效果
- [ ] 区域渐变填充正常显示
- [ ] 点击"刷新"按钮，所有数据包括趋势数据更新
- [ ] 移动端布局正常（趋势图表全宽显示）

**Step 4: 测试空数据场景**

如果后端返回空趋势数据:
- [ ] 显示"暂无趋势数据"空状态
- [ ] 不显示图表组件

**Step 5: 测试加载状态**

观察网络请求，确认:
- [ ] 趋势数据 API 调用: `GET /api/dashboard/trends?days=7`
- [ ] 切换天数时，重新请求数据
- [ ] 数据加载时，显示 loading 状态

---

### Task 4.2: API 响应验证

**Step 1: 直接测试后端 API**

```bash
curl -H "Authorization: Bearer <your-token>" \
  "http://localhost:8080/api/dashboard/trends?days=7"
```

预期输出示例:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "date": "2026-01-17",
      "apiCalls": 150,
      "activeUsers": 12
    },
    {
      "date": "2026-01-18",
      "apiCalls": 200,
      "activeUsers": 15
    },
    ...
  ]
}
```

**Step 2: 验证前端接收数据格式**

在浏览器开发者工具 Console 中:

```javascript
// 在组件中添加临时调试
console.log('Trends data:', trends.value)
```

预期输出: 数组包含 7/14/30 个对象，每个对象有 `date`, `apiCalls`, `activeUsers` 字段

---

## 最终提交和文档更新

### Task 5.1: 最终代码提交

**Step 1: 检查所有修改**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio
git status
```

预期输出:
- `frontend/src/components/dashboard/TrendChart.vue` (新文件)
- `frontend/src/views/DashboardView.vue` (已修改)
- `frontend/src/types/dashboard.ts` (可能修改)

**Step 2: 提交所有前端更改**

```bash
git add frontend/
git commit -m "feat: implement trend chart with dual Y-axis visualization"
```

---

### Task 5.2: 更新项目进度文档

**Files:**
- Modify: `docs/PROJECT_PROGRESS.md`

**Step 1: 在 Phase 11 前端实现部分添加趋势图表记录**

在 Phase 11 的"已完成任务"部分，第 9 项后添加:

```markdown
10. ✅ **趋势图表界面** (Commits: XXX, XXX)
   - TrendChart 组件（双 Y 轴折线图）
   - 趋势数据可视化（API 调用 + 活跃用户）
   - 时间范围选择器（7 天/14 天/30 天）
   - 平滑曲线和区域渐变效果
   - DashboardView 集成和刷新支持
   - 响应式布局适配
```

在"下一步计划"部分移除"仪表盘完善（趋势图表）"

**Step 2: 提交进度更新**

```bash
git add docs/PROJECT_PROGRESS.md
git commit -m "docs: mark trend chart implementation as completed"
```

---

## 总结

完成此计划后，仪表盘将包含完整的趋势可视化功能:

✅ **新增组件:**
- `TrendChart.vue` - 双 Y 轴折线图组件
  - 左 Y 轴: API 调用量
  - 右 Y 轴: 活跃用户数
  - 平滑曲线效果
  - 区域渐变填充
  - 响应式自动调整

✅ **DashboardView 增强:**
- 趋势图表卡片集成
- 时间范围切换（7/14/30 天）
- 自动刷新支持
- 空状态处理
- 加载状态显示

✅ **用户体验:**
- 直观的 API 使用趋势
- 用户活跃度可视化
- 多时间范围对比
- 交互式数据探索

✅ **代码质量:**
- TypeScript 类型安全
- Vue 3 Composition API
- ECharts 最佳实践
- 响应式设计

**新增文件统计:**
- 前端: 1 个新组件（~120 行代码）
- 修改: 1 个视图文件（~50 行新增）

**总计:** 约 170 行代码，提供完整的趋势分析可视化功能。
