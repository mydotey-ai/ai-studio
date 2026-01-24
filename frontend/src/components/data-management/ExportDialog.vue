<template>
  <el-dialog v-model="visible" title="数据导出" width="600px" @closed="handleClosed">
    <el-form :model="form" label-width="100px">
      <!-- 导出范围 -->
      <el-form-item label="导出范围">
        <el-radio-group v-model="form.scope">
          <el-radio :value="ExportScope.ALL">全部数据</el-radio>
          <el-radio :value="ExportScope.KNOWLEDGE_BASES">知识库</el-radio>
          <el-radio :value="ExportScope.AGENTS">Agent</el-radio>
          <el-radio :value="ExportScope.CHATBOTS">聊天机器人</el-radio>
          <el-radio :value="ExportScope.MCP_SERVERS">MCP服务器</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 导出选项 -->
      <el-form-item label="导出选项">
        <el-checkbox v-model="form.includeDocumentContent"> 包含文档内容 </el-checkbox>
        <el-checkbox v-model="form.includeConversations"> 包含对话记录 </el-checkbox>
      </el-form-item>

      <!-- 高级选项 -->
      <el-form-item>
        <el-collapse v-model="activeNames">
          <el-collapse-item title="高级选项" name="advanced">
            <el-form-item label="导出方式">
              <el-radio-group v-model="form.exportType">
                <el-radio :value="'sync'">同步导出（直接下载）</el-radio>
                <el-radio :value="'async'">异步导出（后台任务）</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleExport">
          {{ form.exportType === 'sync' ? '立即导出' : '创建导出任务' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { ExportScope } from '@/types/data-management'
import { createExportTask, exportDataSync } from '@/api/data-management'

// 响应式数据
const visible = ref(false)
const loading = ref(false)
const activeNames = ref(['advanced'])

const form = reactive({
  scope: ExportScope.ALL,
  includeDocumentContent: true,
  includeConversations: true,
  exportType: 'sync' // sync: 同步导出, async: 异步导出
})

const handleClosed = () => {
  // 重置表单
  Object.assign(form, {
    scope: ExportScope.ALL,
    includeDocumentContent: true,
    includeConversations: true,
    exportType: 'sync'
  })
}

const handleCancel = () => {
  visible.value = false
}

const handleExport = async () => {
  loading.value = true
  try {
    if (form.exportType === 'sync') {
      // 同步导出
      await handleSyncExport()
    } else {
      // 异步导出
      await handleAsyncExport()
    }
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    loading.value = false
  }
}

const handleSyncExport = async () => {
  try {
    const response = await exportDataSync({
      scope: form.scope,
      includeDocumentContent: form.includeDocumentContent,
      includeConversations: form.includeConversations
    })

    if (!response.ok) {
      throw new Error('导出失败')
    }

    const blob = await response.blob()
    const contentDisposition = response.headers.get('Content-Disposition')
    let filename = 'data-export.json'

    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(
        /filename\*?=['"]?(?:UTF-8'')?([^;\r\n"']*)['"]?/
      )
      if (filenameMatch) {
        filename = decodeURIComponent(filenameMatch[1])
      }
    }

    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
    visible.value = false
  } catch (error) {
    console.error('同步导出失败:', error)
    ElMessage.error('导出失败')
  }
}

const handleAsyncExport = async () => {
  try {
    await createExportTask({
      scope: form.scope,
      includeDocumentContent: form.includeDocumentContent,
      includeConversations: form.includeConversations
    })
    ElMessage.success('导出任务创建成功')
    visible.value = false
  } catch (error) {
    console.error('异步导出失败:', error)
    ElMessage.error('创建导出任务失败')
  }
}

// 暴露方法
defineExpose({
  open() {
    visible.value = true
  }
})
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
