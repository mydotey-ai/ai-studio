<template>
  <el-dialog v-model="visible" title="数据导入" width="500px" @closed="handleClosed">
    <el-form :model="form" label-width="100px">
      <!-- 文件上传 -->
      <el-form-item label="选择文件">
        <el-upload
          class="upload-demo"
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          :before-upload="beforeUpload"
          accept=".json"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或<em>点击选择文件</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持 .json 格式文件，文件大小不超过 100MB</div>
          </template>
        </el-upload>

        <div v-if="selectedFile" class="selected-file">
          <el-icon><document /></el-icon>
          <span>{{ selectedFile.name }}</span>
          <el-button type="text" icon="Delete" @click="clearFile">删除</el-button>
        </div>
      </el-form-item>

      <!-- 导入策略 -->
      <el-form-item label="导入策略">
        <el-radio-group v-model="form.strategy">
          <el-radio :value="ImportStrategy.SKIP_EXISTING">跳过已存在</el-radio>
          <el-radio :value="ImportStrategy.OVERWRITE">覆盖已存在</el-radio>
          <el-radio :value="ImportStrategy.RENAME_CONFLICT">重命名冲突</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 验证选项 -->
      <el-form-item>
        <el-checkbox v-model="form.validateOnly"> 仅验证文件（不执行导入） </el-checkbox>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button
          type="primary"
          :loading="loading"
          :disabled="!selectedFile"
          @click="handleImport"
        >
          {{ form.validateOnly ? '验证文件' : '开始导入' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import { ImportStrategy } from '@/types/data-management'
import { createImportTask } from '@/api/data-management'

// 响应式数据
const visible = ref(false)
const loading = ref(false)
const selectedFile = ref<File | null>(null)

const form = reactive({
  strategy: ImportStrategy.SKIP_EXISTING,
  validateOnly: false
})

// 文件上传处理
const handleFileChange = (file: any) => {
  selectedFile.value = file.raw as File
}

const beforeUpload = (file: File) => {
  const isJSON = file.type === 'application/json' || file.name.endsWith('.json')
  const isLt100M = file.size / 1024 / 1024 < 100

  if (!isJSON) {
    ElMessage.error('请上传 JSON 格式文件')
    return false
  }

  if (!isLt100M) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }

  return true
}

const clearFile = () => {
  selectedFile.value = null
}

// 事件
const handleClosed = () => {
  // 重置表单
  Object.assign(form, {
    strategy: ImportStrategy.SKIP_EXISTING,
    validateOnly: false
  })
  selectedFile.value = null
}

const handleCancel = () => {
  visible.value = false
}

const handleImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要导入的文件')
    return
  }

  loading.value = true
  try {
    await createImportTask(selectedFile.value, form.strategy, form.validateOnly)
    const message = form.validateOnly ? '文件验证成功' : '导入任务创建成功'
    ElMessage.success(message)
    visible.value = false
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
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
.upload-demo {
  margin-bottom: 20px;
}

.selected-file {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
}

.selected-file :deep(.el-icon) {
  color: #409eff;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
