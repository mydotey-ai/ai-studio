<template>
  <div class="mcp-tool-list">
    <div class="tool-list-header">
      <el-button type="primary" :loading="syncing" @click="handleSync"> 同步工具 </el-button>
    </div>
    <el-table :data="tools" stripe>
      <el-table-column prop="toolName" label="工具名称" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column label="输入 Schema" width="100">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewInputSchema(row)">
            查看
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="输出 Schema" width="100">
        <template #default="{ row }">
          <el-button
            v-if="row.outputSchema"
            type="primary"
            link
            size="small"
            @click="viewOutputSchema(row)"
          >
            查看
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="schemaDialogVisible"
      :title="`输入/输出 Schema - ${currentTool?.toolName || ''}`"
      width="60%"
      :close-on-click-modal="false"
    >
      <pre class="schema-content">{{ formatSchema(currentSchema) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { syncTools } from '@/api/mcp'
import type { McpTool } from '@/types/mcp'

interface Props {
  serverId: number
  tools: McpTool[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  refresh: []
}>()

const schemaDialogVisible = ref(false)
const currentTool = ref<McpTool | undefined>(undefined)
const currentSchema = ref<Record<string, unknown> | undefined>(undefined)
const syncing = ref(false)

const handleSync = async (): Promise<void> => {
  syncing.value = true
  try {
    await syncTools(props.serverId)
    ElMessage.success('同步工具成功')
    emit('refresh')
  } catch (error) {
    console.error('Failed to sync tools:', error)
    ElMessage.error('同步工具失败')
  } finally {
    syncing.value = false
  }
}

const viewInputSchema = (tool: McpTool): void => {
  currentTool.value = tool
  currentSchema.value = tool.inputSchema
  schemaDialogVisible.value = true
}

const viewOutputSchema = (tool: McpTool): void => {
  currentTool.value = tool
  currentSchema.value = tool.outputSchema
  schemaDialogVisible.value = true
}

const formatSchema = (schema: Record<string, unknown> | undefined): string => {
  if (schema === undefined) {
    return '-'
  }
  return JSON.stringify(schema, null, 2)
}
</script>

<style scoped>
.mcp-tool-list {
  width: 100%;
}

.tool-list-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.schema-content {
  background-color: #f5f5f5;
  padding: 16px;
  border-radius: 4px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
}
</style>
