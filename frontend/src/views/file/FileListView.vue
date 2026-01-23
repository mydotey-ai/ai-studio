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
              <el-button size="small" @click="openPreviewDialog(row)">
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
      :file="selectedPreviewFile"
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
const selectedPreviewFile = ref<FileMetadata | null>(null)

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
const openPreviewDialog = (file: FileMetadata) => {
  selectedPreviewFile.value = file
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
