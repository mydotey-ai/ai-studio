# 文件管理界面实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 实现完整的文件管理界面，支持文件的上传、下载、预览、删除、批量操作和存储配置管理。

**架构：** 基于 Vue 3 + TypeScript + Element Plus 的前端界面，调用已完成的后端文件存储 API（/api/files/*），复用现有的认证、路由和状态管理基础设施。

**技术栈：**
- Vue 3.5+ (Composition API) + TypeScript 5.3+
- Element Plus 2.13+ (UI 组件库)
- Axios 1.13+ (HTTP 客户端)
- Pinia 2.3+ (状态管理)
- Dayjs (日期格式化)

---

## 前置条件

- ✅ 后端文件存储系统已完成（Phase 8）
- ✅ 前端基础架构已完成（Phase 11）
- ✅ 后端 API 端点已实现：
  - `POST /api/files/upload` - 上传文件
  - `GET /api/files/download/{id}` - 下载文件
  - `GET /api/files/{id}/url` - 获取文件访问 URL
  - `GET /api/files/{id}` - 获取文件元数据
  - `GET /api/files/my` - 获取我的文件列表
  - `GET /api/files/related/{entityType}/{entityId}` - 获取关联实体文件
  - `DELETE /api/files/{id}` - 删除文件
  - `GET /api/storage-configs/*` - 存储配置管理 API

---

## 实施计划

### Task 1: 创建文件管理类型定义

**文件：**
- 创建：`frontend/src/types/file.ts`

**Step 1: 创建文件类型定义**

```typescript
/**
 * File metadata representation
 */
export interface FileMetadata {
  /** Unique identifier for the file metadata */
  id: number
  /** Stored file name */
  fileName: string
  /** Original uploaded file name */
  originalFileName: string
  /** Full file path in storage */
  filePath: string
  /** File size in bytes */
  fileSize: number
  /** Content type (MIME type) */
  contentType: string
  /** Storage type (LOCAL, OSS, S3) */
  storageType: string
  /** ID of the user who uploaded the file */
  uploadedBy: number
  /** Related entity type (e.g., "KNOWLEDGE_BASE", "CONVERSATION") */
  relatedEntityType?: string
  /** Related entity ID */
  relatedEntityId?: number
  /** Creation timestamp (ISO 8601 format) */
  createdAt: string
}

/**
 * File upload response
 */
export interface FileUploadResponse {
  /** File metadata ID */
  id: number
  /** Stored file name */
  fileName: string
  /** Original uploaded file name */
  originalFileName: string
  /** Full file path in storage */
  filePath: string
  /** File size in bytes */
  fileSize: number
  /** Content type (MIME type) */
  contentType: string
  /** Storage type (LOCAL, OSS, S3) */
  storageType: string
  /** URL to access the file (presigned or direct) */
  fileUrl: string
  /** ID of the user who uploaded the file */
  uploadedBy: number
  /** Upload timestamp (ISO 8601 format) */
  createdAt: string
}

/**
 * File upload request parameters
 */
export interface FileUploadRequest {
  /** File to upload */
  file: File
  /** Related entity type (optional) */
  relatedEntityType?: string
  /** Related entity ID (optional) */
  relatedEntityId?: number
}

/**
 * File list query parameters
 */
export interface FileListQuery {
  /** Page number (1-indexed) */
  page?: number
  /** Page size */
  pageSize?: number
  /** Search keyword (filename) */
  search?: string
  /** Filter by content type */
  contentType?: string
  /** Filter by storage type */
  storageType?: string
}
```

**Step 2: 保存文件**

保存文件到 `frontend/src/types/file.ts`

**Step 3: 验证类型编译**

运行：`cd frontend && npm run type-check`
预期：无类型错误

**Step 4: 提交**

```bash
git add frontend/src/types/file.ts
git commit -m "feat(file): add file management type definitions"
```

---

### Task 2: 创建文件管理 API 客户端

**文件：**
- 创建：`frontend/src/api/file.ts`

**Step 1: 创建文件 API 函数**

```typescript
import { get, post, del } from './request'
import type { FileMetadata, FileUploadResponse } from '@/types/file'

export const fileApi = {
  /**
   * Upload a file
   * @param file File to upload
   * @param relatedEntityType Optional related entity type
   * @param relatedEntityId Optional related entity ID
   * @returns Upload response with file metadata and URL
   */
  uploadFile(
    file: File,
    relatedEntityType?: string,
    relatedEntityId?: number
  ) {
    const formData = new FormData()
    formData.append('file', file)
    if (relatedEntityType) {
      formData.append('relatedEntityType', relatedEntityType)
    }
    if (relatedEntityId) {
      formData.append('relatedEntityId', relatedEntityId.toString())
    }

    return post<FileUploadResponse>('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * Get file metadata by ID
   * @param id File metadata ID
   * @returns File metadata
   */
  getFileMetadata(id: number) {
    return get<FileMetadata>(`/files/${id}`)
  },

  /**
   * Get current user's files
   * @returns List of file metadata
   */
  getMyFiles() {
    return get<FileMetadata[]>('/files/my')
  },

  /**
   * Get files related to an entity
   * @param entityType Entity type (e.g., "KNOWLEDGE_BASE")
   * @param entityId Entity ID
   * @returns List of file metadata
   */
  getRelatedFiles(entityType: string, entityId: number) {
    return get<FileMetadata[]>(`/files/related/${entityType}/${entityId}`)
  },

  /**
   * Get file download URL
   * @param id File metadata ID
   * @param expirationSeconds URL expiration time in seconds (default: 3600)
   * @returns Signed URL for file access
   */
  getFileUrl(id: number, expirationSeconds: number = 3600) {
    return get<string>(`/files/${id}/url?expirationSeconds=${expirationSeconds}`)
  },

  /**
   * Get direct download link for browser
   * @param id File metadata ID
   * @returns Direct download URL
   */
  getDownloadUrl(id: number) {
    return `${import.meta.env.VITE_API_BASE_URL}/files/download/${id}`
  },

  /**
   * Delete a file
   * @param id File metadata ID
   */
  deleteFile(id: number) {
    return del(`/files/${id}`)
  }
}
```

**Step 2: 保存文件**

保存文件到 `frontend/src/api/file.ts`

**Step 3: 验证 API 编译**

运行：`cd frontend && npm run type-check`
预期：无类型错误

**Step 4: 提交**

```bash
git add frontend/src/api/file.ts
git commit -m "feat(file): add file management API client"
```

---

### Task 3: 创建文件列表视图组件

**文件：**
- 创建：`frontend/src/views/file/FileListView.vue`

**Step 1: 创建文件列表视图**

```vue
<template>
  <div class="file-list-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件管理</span>
          <el-button type="primary" @click="showUploadDialog = true">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
        </div>
      </template>

      <!-- 搜索和过滤 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="搜索">
          <el-input
            v-model="searchQuery"
            placeholder="输入文件名搜索"
            clearable
            @clear="loadFiles"
            @keyup.enter="loadFiles"
          >
            <template #append>
              <el-button :icon="Search" @click="loadFiles" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="存储类型">
          <el-select
            v-model="storageTypeFilter"
            placeholder="全部"
            clearable
            @change="loadFiles"
          >
            <el-option label="本地存储" value="LOCAL" />
            <el-option label="阿里云 OSS" value="OSS" />
            <el-option label="AWS S3" value="S3" />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 文件列表 -->
      <el-table
        :data="filteredFiles"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="originalFileName" label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon><Document /></el-icon>
              <span>{{ row.originalFileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="contentType" label="类型" width="150" />
        <el-table-column prop="storageType" label="存储" width="100">
          <template #default="{ row }">
            <el-tag :type="getStorageTypeTag(row.storageType)">
              {{ row.storageType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="previewFile(row)">
                预览
              </el-button>
              <el-button size="small" @click="downloadFile(row)">
                下载
              </el-button>
              <el-button
                size="small"
                type="danger"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 批量操作 -->
      <div v-if="selectedFiles.length > 0" class="batch-actions">
        <span>已选择 {{ selectedFiles.length }} 个文件</span>
        <el-button type="danger" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>
    </el-card>

    <!-- 上传对话框 -->
    <FileUploadDialog
      v-model="showUploadDialog"
      @uploaded="loadFiles"
    />

    <!-- 预览对话框 -->
    <FilePreviewDialog
      v-model="showPreviewDialog"
      :file="previewFile"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Search, Document } from '@element-plus/icons-vue'
import { fileApi } from '@/api/file'
import type { FileMetadata } from '@/types/file'
import { formatFileSize, formatDate } from '@/utils/file'
import FileUploadDialog from '@/components/file/FileUploadDialog.vue'
import FilePreviewDialog from '@/components/file/FilePreviewDialog.vue'

const loading = ref(false)
const files = ref<FileMetadata[]>([])
const searchQuery = ref('')
const storageTypeFilter = ref('')
const selectedFiles = ref<FileMetadata[]>([])
const showUploadDialog = ref(false)
const showPreviewDialog = ref(false)
const previewFile = ref<FileMetadata | null>(null)

// 过滤后的文件列表
const filteredFiles = computed(() => {
  let result = files.value

  if (searchQuery.value) {
    result = result.filter(f =>
      f.originalFileName.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
  }

  if (storageTypeFilter.value) {
    result = result.filter(f => f.storageType === storageTypeFilter.value)
  }

  return result
})

// 加载文件列表
const loadFiles = async () => {
  loading.value = true
  try {
    const response = await fileApi.getMyFiles()
    files.value = response.data
  } catch (error) {
    ElMessage.error('加载文件列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 处理选择变化
const handleSelectionChange = (selection: FileMetadata[]) => {
  selectedFiles.value = selection
}

// 预览文件
const previewFile = (file: FileMetadata) => {
  previewFile.value = file
  showPreviewDialog.value = true
}

// 下载文件
const downloadFile = async (file: FileMetadata) => {
  try {
    const url = fileApi.getDownloadUrl(file.id)
    window.open(url, '_blank')
  } catch (error) {
    ElMessage.error('下载失败')
    console.error(error)
  }
}

// 删除文件
const handleDelete = async (file: FileMetadata) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.originalFileName}" 吗？`,
      '确认删除',
      { type: 'warning' }
    )

    await fileApi.deleteFile(file.id)
    ElMessage.success('删除成功')
    loadFiles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedFiles.value.length} 个文件吗？`,
      '确认删除',
      { type: 'warning' }
    )

    await Promise.all(
      selectedFiles.value.map(f => fileApi.deleteFile(f.id))
    )

    ElMessage.success('批量删除成功')
    selectedFiles.value = []
    loadFiles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
      console.error(error)
    }
  }
}

// 获取存储类型标签类型
const getStorageTypeTag = (type: string) => {
  const map: Record<string, string> = {
    LOCAL: '',
    OSS: 'success',
    S3: 'warning'
  }
  return map[type] || ''
}

onMounted(() => {
  loadFiles()
})
</script>

<style scoped>
.file-list-view {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  margin-bottom: 20px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-actions {
  margin-top: 20px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
```

**Step 2: 保存文件**

保存文件到 `frontend/src/views/file/FileListView.vue`

**Step 3: 提交**

```bash
git add frontend/src/views/file/FileListView.vue
git commit -m "feat(file): add file list view component"
```

---

### Task 4: 创建文件上传对话框组件

**文件：**
- 创建：`frontend/src/components/file/FileUploadDialog.vue`

**Step 1: 创建文件上传对话框**

```vue
<template>
  <el-dialog
    v-model="visible"
    title="上传文件"
    width="600px"
    @close="handleClose"
  >
    <el-upload
      ref="uploadRef"
      class="upload-demo"
      drag
      :action="uploadUrl"
      :headers="uploadHeaders"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-progress="handleProgress"
      :before-upload="beforeUpload"
      :file-list="fileList"
      :auto-upload="false"
      multiple
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        拖拽文件到此处或 <em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持任意类型文件，单个文件不超过 100MB
        </div>
      </template>
    </el-upload>

    <!-- 关联实体选择（可选） -->
    <el-form v-if="showEntitySelector" :model="entityForm" label-width="100px">
      <el-form-item label="关联类型">
        <el-select v-model="entityForm.type" placeholder="选择关联类型">
          <el-option label="知识库" value="KNOWLEDGE_BASE" />
          <el-option label="对话" value="CONVERSATION" />
          <el-option label="Agent" value="AGENT" />
        </el-select>
      </el-form-item>
      <el-form-item label="关联ID">
        <el-input v-model.number="entityForm.id" placeholder="输入关联实体ID" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleUpload" :loading="uploading">
        开始上传
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import type { UploadInstance, UploadUserFile, UploadProps } from 'element-plus'
import { useUserStore } from '@/stores/user'

interface Props {
  modelValue: boolean
  showEntitySelector?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'uploaded'): void
}

const props = withDefaults(defineProps<Props>(), {
  showEntitySelector: false
})

const emit = defineEmits<Emits>()

const userStore = useUserStore()
const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const uploading = ref(false)
const entityForm = ref({
  type: '',
  id: undefined as number | undefined
})

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const uploadUrl = computed(() => {
  return `${import.meta.env.VITE_API_BASE_URL}/files/upload`
})

const uploadHeaders = computed(() => {
  return {
    Authorization: `Bearer ${userStore.token}`
  }
})

// 上传前验证
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const maxSize = 100 * 1024 * 1024 // 100MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }
  return true
}

// 处理上传
const handleUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true

  try {
    // 如果有关联实体，需要逐个上传并添加参数
    if (entityForm.value.type && entityForm.value.id) {
      for (const file of fileList.value) {
        const formData = new FormData()
        formData.append('file', file.raw as File)
        formData.append('relatedEntityType', entityForm.value.type)
        formData.append('relatedEntityId', entityForm.value.id.toString())

        // 这里需要手动调用上传 API
        // 可以使用之前定义的 fileApi.uploadFile
      }
    }

    await uploadRef.value?.submit()
  } catch (error) {
    ElMessage.error('上传失败')
    console.error(error)
  } finally {
    uploading.value = false
  }
}

// 上传成功
const handleSuccess = (response: any) => {
  ElMessage.success('上传成功')
  emit('uploaded')
  visible.value = false
  fileList.value = []
}

// 上传失败
const handleError = (error: any) => {
  ElMessage.error('上传失败')
  console.error(error)
}

// 上传进度
const handleProgress = (percent: number) => {
  console.log(`Upload progress: ${percent}%`)
}

// 关闭对话框
const handleClose = () => {
  fileList.value = []
  entityForm.value = { type: '', id: undefined }
}

watch(visible, (newVal) => {
  if (!newVal) {
    handleClose()
  }
})
</script>

<style scoped>
.upload-demo {
  margin-bottom: 20px;
}

.el-icon--upload {
  font-size: 67px;
  color: #c0c4cc;
  margin: 20px 0;
}

.el-upload__text {
  color: #606266;
  font-size: 14px;
}

.el-upload__text em {
  color: #409eff;
  font-style: normal;
}
</style>
```

**Step 2: 保存文件**

保存文件到 `frontend/src/components/file/FileUploadDialog.vue`

**Step 3: 提交**

```bash
git add frontend/src/components/file/FileUploadDialog.vue
git commit -m "feat(file): add file upload dialog component"
```

---

### Task 5: 创建文件预览对话框组件

**文件：**
- 创建：`frontend/src/components/file/FilePreviewDialog.vue`

**Step 1: 创建文件预览对话框**

```vue
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
```

**Step 2: 保存文件**

保存文件到 `frontend/src/components/file/FilePreviewDialog.vue`

**Step 3: 提交**

```bash
git add frontend/src/components/file/FilePreviewDialog.vue
git commit -m "feat(file): add file preview dialog component"
```

---

### Task 6: 创建文件工具函数

**文件：**
- 创建：`frontend/src/utils/file.ts`

**Step 1: 创建文件工具函数**

```typescript
import dayjs from 'dayjs'

/**
 * Format file size to human readable string
 * @param bytes File size in bytes
 * @returns Formatted string (e.g., "1.5 MB")
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

/**
 * Format date to readable string
 * @param dateString ISO 8601 date string
 * @returns Formatted date string (e.g., "2026-01-24 12:34")
 */
export function formatDate(dateString: string): string {
  return dayjs(dateString).format('YYYY-MM-DD HH:mm')
}

/**
 * Get file icon based on content type
 * @param contentType MIME type
 * @returns Element Plus icon name
 */
export function getFileIcon(contentType: string): string {
  if (contentType.startsWith('image/')) return 'Picture'
  if (contentType === 'application/pdf') return 'Document'
  if (contentType.includes('word')) return 'Document'
  if (contentType.includes('excel') || contentType.includes('spreadsheet')) return 'Tickets'
  if (contentType.includes('powerpoint') || contentType.includes('presentation')) return 'Notebook'
  if (contentType.startsWith('text/')) return 'Document'
  if (contentType.includes('zip') || contentType.includes('rar')) return 'Folder'
  if (contentType.startsWith('video/')) return 'VideoCamera'
  if (contentType.startsWith('audio/')) return 'Headset'
  return 'Document'
}

/**
 * Check if file can be previewed
 * @param contentType MIME type
 * @returns true if file can be previewed
 */
export function canPreview(contentType: string): boolean {
  return (
    contentType.startsWith('image/') ||
    contentType === 'application/pdf' ||
    contentType.startsWith('text/')
  )
}

/**
 * Validate file size
 * @param file File to validate
 * @param maxSize Maximum size in bytes
 * @returns true if file size is valid
 */
export function validateFileSize(file: File, maxSize: number): boolean {
  return file.size <= maxSize
}

/**
 * Validate file type
 * @param file File to validate
 * @param allowedTypes Array of allowed MIME types
 * @returns true if file type is allowed
 */
export function validateFileType(file: File, allowedTypes: string[]): boolean {
  return allowedTypes.includes(file.type)
}
```

**Step 2: 保存文件**

保存文件到 `frontend/src/utils/file.ts`

**Step 3: 验证工具函数编译**

运行：`cd frontend && npm run type-check`
预期：无类型错误

**Step 4: 提交**

```bash
git add frontend/src/utils/file.ts
git commit -m "feat(file): add file utility functions"
```

---

### Task 7: 添加文件管理路由

**文件：**
- 修改：`frontend/src/router/index.ts`

**Step 1: 添加文件管理路由**

在路由配置的 children 数组中添加文件管理路由（在 settings 路由之前）：

```typescript
{
  path: 'files',
  name: 'Files',
  component: () => import('@/views/file/FileListView.vue'),
  meta: { title: '文件管理', icon: 'Folder' }
},
```

**Step 2: 保存文件**

保存修改到 `frontend/src/router/index.ts`

**Step 3: 验证路由编译**

运行：`cd frontend && npm run type-check`
预期：无类型错误

**Step 4: 提交**

```bash
git add frontend/src/router/index.ts
git commit -m "feat(file): add file management route"
```

---

### Task 8: 创建存储配置管理组件

**文件：**
- 创建：`frontend/src/views/settings/StorageConfigView.vue`

**Step 1: 创建存储配置管理视图**

```vue
<template>
  <div class="storage-config-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>存储配置管理</span>
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            添加配置
          </el-button>
        </div>
      </template>

      <!-- 存储配置列表 -->
      <el-table :data="configs" v-loading="loading">
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="storageType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getStorageTypeTag(row.storageType)">
              {{ row.storageType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="endpoint" label="端点" min-width="200" show-overflow-tooltip />
        <el-table-column prop="bucketName" label="Bucket" width="150" />
        <el-table-column prop="isDefault" label="默认" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success">是</el-tag>
            <el-tag v-else type="info">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="editConfig(row)">
                编辑
              </el-button>
              <el-button
                size="small"
                type="danger"
                :disabled="row.isDefault"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <StorageConfigDialog
      v-model="showCreateDialog"
      :config="editingConfig"
      @saved="loadConfigs"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { storageApi } from '@/api/storage'
import type { StorageConfig } from '@/types/storage'
import { formatDate } from '@/utils/file'
import StorageConfigDialog from '@/components/storage/StorageConfigDialog.vue'

const loading = ref(false)
const configs = ref<StorageConfig[]>([])
const showCreateDialog = ref(false)
const editingConfig = ref<StorageConfig | null>(null)

// 加载存储配置列表
const loadConfigs = async () => {
  loading.value = true
  try {
    const response = await storageApi.getAllConfigs()
    configs.value = response.data
  } catch (error) {
    ElMessage.error('加载存储配置失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 编辑配置
const editConfig = (config: StorageConfig) => {
  editingConfig.value = config
  showCreateDialog.value = true
}

// 删除配置
const handleDelete = async (config: StorageConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除存储配置 "${config.description}" 吗？`,
      '确认删除',
      { type: 'warning' }
    )

    await storageApi.deleteConfig(config.id)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

// 获取存储类型标签类型
const getStorageTypeTag = (type: string) => {
  const map: Record<string, string> = {
    LOCAL: '',
    OSS: 'success',
    S3: 'warning'
  }
  return map[type] || ''
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.storage-config-view {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
```

**Step 2: 保存文件**

保存文件到 `frontend/src/views/settings/StorageConfigView.vue`

**Step 3: 提交**

```bash
git add frontend/src/views/settings/StorageConfigView.vue
git commit -m "feat(file): add storage config management view"
```

---

### Task 9: 创建存储配置对话框组件

**文件：**
- 创建：`frontend/src/components/storage/StorageConfigDialog.vue`

**Step 1: 创建存储配置对话框**

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="config ? '编辑存储配置' : '创建存储配置'"
    width="600px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="存储类型" prop="storageType">
        <el-select
          v-model="formData.storageType"
          :disabled="!!config"
          placeholder="选择存储类型"
        >
          <el-option label="本地存储" value="LOCAL" />
          <el-option label="阿里云 OSS" value="OSS" />
          <el-option label="AWS S3" value="S3" />
        </el-select>
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input v-model="formData.description" placeholder="输入配置描述" />
      </el-form-item>

      <!-- OSS/S3 配置 -->
      <template v-if="formData.storageType !== 'LOCAL'">
        <el-form-item label="端点地址" prop="endpoint">
          <el-input v-model="formData.endpoint" placeholder="输入存储端点地址" />
        </el-form-item>

        <el-form-item label="Bucket 名称" prop="bucketName">
          <el-input v-model="formData.bucketName" placeholder="输入 Bucket 名称" />
        </el-form-item>

        <el-form-item label="区域" prop="region">
          <el-input v-model="formData.region" placeholder="输入存储区域（可选）" />
        </el-form-item>

        <el-form-item label="Access Key" prop="accessKey">
          <el-input v-model="formData.accessKey" placeholder="输入 Access Key" />
        </el-form-item>

        <el-form-item label="Secret Key" prop="secretKey">
          <el-input
            v-model="formData.secretKey"
            type="password"
            placeholder="输入 Secret Key"
            show-password
          />
        </el-form-item>
      </template>

      <el-form-item label="设为默认">
        <el-switch v-model="formData.isDefault" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { storageApi } from '@/api/storage'
import type { StorageConfig, CreateStorageConfigRequest } from '@/types/storage'

interface Props {
  modelValue: boolean
  config?: StorageConfig | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = ref<CreateStorageConfigRequest>({
  storageType: 'LOCAL',
  description: '',
  isDefault: false
})

const formRules: FormRules = {
  storageType: [
    { required: true, message: '请选择存储类型', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入配置描述', trigger: 'blur' }
  ],
  endpoint: [
    { required: true, message: '请输入端点地址', trigger: 'blur' }
  ],
  bucketName: [
    { required: true, message: '请输入 Bucket 名称', trigger: 'blur' }
  ],
  accessKey: [
    { required: true, message: '请输入 Access Key', trigger: 'blur' }
  ],
  secretKey: [
    { required: true, message: '请输入 Secret Key', trigger: 'blur' }
  ]
}

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 监听 config 变化，初始化表单
watch(() => props.config, (newConfig) => {
  if (newConfig) {
    formData.value = {
      storageType: newConfig.storageType,
      description: newConfig.description || '',
      endpoint: newConfig.endpoint,
      bucketName: newConfig.bucketName,
      region: newConfig.region,
      accessKey: newConfig.accessKey,
      secretKey: newConfig.secretKey,
      isDefault: newConfig.isDefault
    }
  } else {
    formData.value = {
      storageType: 'LOCAL',
      description: '',
      isDefault: false
    }
  }
})

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true

    try {
      if (props.config) {
        // 更新
        await storageApi.updateConfig(props.config.id, {
          description: formData.value.description,
          endpoint: formData.value.endpoint,
          bucketName: formData.value.bucketName,
          region: formData.value.region,
          accessKey: formData.value.accessKey,
          secretKey: formData.value.secretKey
        })
      } else {
        // 创建
        await storageApi.createConfig(formData.value)
      }

      ElMessage.success(props.config ? '更新成功' : '创建成功')
      emit('saved')
      visible.value = false
    } catch (error) {
      ElMessage.error('操作失败')
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

// 关闭对话框
const handleClose = () => {
  formRef.value?.resetFields()
  formData.value = {
    storageType: 'LOCAL',
    description: '',
    isDefault: false
  }
}
</script>

<style scoped>
.el-select {
  width: 100%;
}
</style>
```

**Step 2: 保存文件**

保存文件到 `frontend/src/components/storage/StorageConfigDialog.vue`

**Step 3: 提交**

```bash
git add frontend/src/components/storage/StorageConfigDialog.vue
git commit -m "feat(file): add storage config dialog component"
```

---

### Task 10: 集成到系统设置界面

**文件：**
- 修改：`frontend/src/views/settings/SettingsView.vue`

**Step 1: 在设置界面添加存储配置标签页**

读取现有的 SettingsView.vue 文件，在 el-tabs 中添加新的标签页：

```vue
<el-tab-pane label="存储配置" name="storage">
  <StorageConfigView />
</el-tab-pane>
```

在 script 部分添加导入：

```typescript
import StorageConfigView from './StorageConfigView.vue'
```

**Step 2: 保存文件**

保存修改到 `frontend/src/views/settings/SettingsView.vue`

**Step 3: 验证设置界面编译**

运行：`cd frontend && npm run type-check`
预期：无类型错误

**Step 4: 提交**

```bash
git add frontend/src/views/settings/SettingsView.vue
git commit -m "feat(file): integrate storage config into settings"
```

---

### Task 11: 构建测试和验证

**Step 1: 运行前端类型检查**

运行：`cd frontend && npm run type-check`
预期：零类型错误

**Step 2: 运行前端构建**

运行：`cd frontend && npm run build`
预期：构建成功，无错误

**Step 3: 运行代码规范检查**

运行：`cd frontend && npm run lint`
预期：无 ESLint 错误

**Step 4: 启动开发服务器验证**

运行：`cd frontend && npm run dev`
预期：开发服务器启动成功

**Step 5: 手动验证功能**

访问 `http://localhost:5173/files` 验证以下功能：
1. 文件列表加载
2. 文件上传（拖拽和点击）
3. 文件预览（图片、PDF、文本）
4. 文件下载
5. 文件删除（单个和批量）
6. 搜索和过滤

访问 `http://localhost:5173/settings` 并切换到"存储配置"标签验证：
1. 存储配置列表
2. 创建存储配置
3. 编辑存储配置
4. 删除存储配置

**Step 6: 提交**

```bash
git add .
git commit -m "feat(file): complete file management UI implementation"
```

---

## 测试清单

- [ ] 文件列表正确加载并显示
- [ ] 搜索功能正常工作
- [ ] 过滤功能（存储类型）正常工作
- [ ] 文件上传对话框可以打开和关闭
- [ ] 拖拽上传文件成功
- [ ] 点击上传文件成功
- [ ] 上传进度正确显示
- [ ] 上传后列表自动刷新
- [ ] 文件预览功能正常（图片、PDF、文本）
- [ ] 文件下载功能正常
- [ ] 单个文件删除功能正常
- [ ] 批量选择功能正常
- [ ] 批量删除功能正常
- [ ] 存储配置列表正确加载
- [ ] 创建存储配置功能正常
- [ ] 编辑存储配置功能正常
- [ ] 删除存储配置功能正常
- [ ] 所有错误情况都有友好的错误提示
- [ ] 所有操作都有加载状态指示
- [ ] TypeScript 编译无错误
- [ ] ESLint 检查无错误
- [ ] 生产构建成功

---

## 完成标准

1. ✅ 所有文件管理功能可用（上传、下载、预览、删除）
2. ✅ 存储配置管理功能可用（CRUD）
3. ✅ TypeScript 编译通过，零错误
4. ✅ ESLint 检查通过
5. ✅ 生产构建成功
6. ✅ 手动测试所有功能正常
7. ✅ 代码符合项目规范（Vue 3 最佳实践）
8. ✅ 所有 API 调用都有错误处理
9. ✅ 所有用户操作都有反馈（加载状态、成功/失败提示）
10. ✅ 组件可复用性良好

---

## 后续优化

- 添加文件分页功能（当前返回全部文件）
- 添加文件共享功能
- 添加文件版本管理
- 添加文件压缩下载
- 添加文件夹管理
- 添加文件搜索高级过滤
- 添加文件统计分析
