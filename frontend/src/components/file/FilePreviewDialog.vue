<template>
  <el-dialog
    v-model="visible"
    :title="`预览: ${file?.originalFileName || ''}`"
    width="80%"
    top="5vh"
    @close="handleClose"
  >
    <div v-loading="loading" class="preview-container">
      <!-- 图片预览 -->
      <div v-if="isImage" class="image-preview">
        <img :src="previewUrl" alt="预览图片" />
      </div>

      <!-- PDF 预览 -->
      <div v-else-if="isPdf" class="pdf-preview">
        <iframe :src="previewUrl" frameborder="0"></iframe>
      </div>

      <!-- 文本预览 -->
      <div v-else-if="isText" class="text-preview">
        <pre>{{ textContent }}</pre>
      </div>

      <!-- 不支持预览 -->
      <div v-else class="no-preview">
        <el-icon><Document /></el-icon>
        <p>此文件类型不支持预览</p>
        <el-button type="primary" @click="downloadFile">
          下载文件
        </el-button>
      </div>

      <!-- 文件信息 -->
      <div v-if="file" class="file-info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名">
            {{ file.originalFileName }}
          </el-descriptions-item>
          <el-descriptions-item label="大小">
            {{ formatFileSize(file.fileSize) }}
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            {{ file.contentType }}
          </el-descriptions-item>
          <el-descriptions-item label="存储">
            {{ file.storageType }}
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">
            {{ formatDate(file.createdAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { fileApi } from '@/api/file'
import type { FileMetadata } from '@/types/file'
import { formatFileSize, formatDate } from '@/utils/file'

interface Props {
  modelValue: boolean
  file: FileMetadata | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const previewUrl = ref('')
const textContent = ref('')

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isImage = computed(() => {
  return props.file?.contentType.startsWith('image/')
})

const isPdf = computed(() => {
  return props.file?.contentType === 'application/pdf'
})

const isText = computed(() => {
  return props.file?.contentType.startsWith('text/')
})

// 加载预览
const loadPreview = async () => {
  if (!props.file) return

  loading.value = true

  try {
    // 获取文件 URL
    const response = await fileApi.getFileUrl(props.file.id, 3600)
    previewUrl.value = response.data

    // 如果是文本文件，加载内容
    if (isText.value) {
      const textResponse = await fetch(previewUrl.value)
      textContent.value = await textResponse.text()
    }
  } catch (error) {
    ElMessage.error('加载预览失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 下载文件
const downloadFile = () => {
  if (!props.file) return
  const url = fileApi.getDownloadUrl(props.file.id)
  window.open(url, '_blank')
}

// 关闭对话框
const handleClose = () => {
  previewUrl.value = ''
  textContent.value = ''
}

watch(() => props.file, (newFile) => {
  if (newFile && props.modelValue) {
    loadPreview()
  }
})
</script>

<style scoped>
.preview-container {
  min-height: 400px;
}

.image-preview img {
  max-width: 100%;
  max-height: 600px;
  display: block;
  margin: 0 auto;
}

.pdf-preview iframe {
  width: 100%;
  height: 600px;
  border: none;
}

.text-preview pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 600px;
  overflow-y: auto;
}

.no-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: #909399;
}

.no-preview .el-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.file-info {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
