<template>
  <div class="mcp-tool-list">
    <div class="header">
      <h3>工具列表</h3>
      <el-button type="primary" :icon="Refresh" :loading="syncing" @click="handleSync">
        同步工具
      </el-button>
    </div>

    <el-table :data="tools" :loading="loading" stripe>
      <el-table-column prop="toolName" label="工具名称" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column label="输入 Schema" width="100" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="showSchema(row, 'input')"> 查看 </el-button>
        </template>
      </el-table-column>
      <el-table-column label="输出 Schema" width="100" align="center">
        <template #default="{ row }">
          <el-button v-if="row.outputSchema" link type="primary" @click="showSchema(row, 'output')">
            查看
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showSchemaDialog"
      :title="`${schemaType === 'input' ? '输入' : '输出'} Schema - ${currentTool?.toolName}`"
      width="600px"
    >
      <pre class="schema-content">{{ formatSchema(currentSchema) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { syncTools } from '@/api/mcp'
import type { McpTool } from '@/types/mcp'

interface Props {
  serverId: number
  tools: McpTool[]
}

interface Emits {
  (e: 'refresh'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const syncing = ref(false)
const showSchemaDialog = ref(false)
const currentTool = ref<McpTool>()
const currentSchema = ref<Record<string, unknown>>()
const schemaType = ref<'input' | 'output'>('input')

async function handleSync() {
  syncing.value = true
  try {
    await syncTools(props.serverId)
    ElMessage.success('工具同步成功')
    emit('refresh')
  } catch (error) {
    ElMessage.error('工具同步失败')
    console.error('Failed to sync tools:', error)
  } finally {
    syncing.value = false
  }
}

function showSchema(tool: McpTool, type: 'input' | 'output') {
  currentTool.value = tool
  schemaType.value = type
  currentSchema.value = type === 'input' ? tool.inputSchema : tool.outputSchema
  showSchemaDialog.value = true
}

function formatSchema(schema?: Record<string, unknown>): string {
  if (!schema) return '-'
  return JSON.stringify(schema, null, 2)
}
</script>

<style scoped lang="scss">
.mcp-tool-list {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 500;
    }
  }

  .schema-content {
    background-color: #f5f5f5;
    padding: 16px;
    border-radius: 4px;
    overflow-x: auto;
    font-family: 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.6;
  }
}
</style>
