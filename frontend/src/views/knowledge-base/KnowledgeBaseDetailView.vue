<template>
  <div class="kb-detail">
    <div class="header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="title">{{ knowledgeBase?.name }}</span>
        </template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="文档" name="documents">
        <div class="tab-header">
          <el-button type="primary" :icon="Upload" @click="showUploadDialog = true">
            上传文档
          </el-button>
          <el-button :icon="Link" @click="showWebCrawlDialog = true"> 网页抓取 </el-button>
        </div>

        <el-table :data="documents" :loading="documentsLoading" stripe>
          <el-table-column prop="filename" label="文件名" min-width="200" />
          <el-table-column label="文件大小" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="分块数" width="100" align="center" />
          <el-table-column prop="createdAt" label="上传时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" :icon="Delete" @click="handleDeleteDocument(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="docPagination.page"
            v-model:page-size="docPagination.pageSize"
            :total="docPagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadDocuments"
            @current-change="loadDocuments"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="网页抓取" name="webcrawl">
        <WebCrawlConfig :kb-id="kbId" />
      </el-tab-pane>
    </el-tabs>

    <!-- Upload Dialog -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="500px">
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        accept=".pdf,.doc,.docx,.txt"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 PDF、Word、TXT 格式，文件大小不超过 100MB</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload"> 上传 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Link, Delete, UploadFilled } from '@element-plus/icons-vue'
import { getKnowledgeBase } from '@/api/knowledge-base'
import { getDocuments, uploadDocument, deleteDocument } from '@/api/document'
import type { KnowledgeBase, Document } from '@/types/knowledge-base'
import dayjs from 'dayjs'
import WebCrawlConfig from '@/components/knowledge-base/WebCrawlConfig.vue'

const router = useRouter()
const route = useRoute()

const kbId = ref<number>(parseInt(route.params.id as string))
const knowledgeBase = ref<KnowledgeBase>()
const documents = ref<Document[]>([])

const activeTab = ref('documents')
const documentsLoading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const showWebCrawlDialog = ref(false)
const uploadFile = ref<File | null>(null)

const docPagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// Watch for route parameter changes
watch(
  () => route.params.id,
  newId => {
    if (newId) {
      const id = parseInt(newId as string)
      if (!isNaN(id)) {
        kbId.value = id
        loadKnowledgeBase()
        loadDocuments()
      }
    }
  },
  { immediate: true }
)

async function loadKnowledgeBase() {
  knowledgeBase.value = await getKnowledgeBase(kbId.value)
}

async function loadDocuments() {
  documentsLoading.value = true
  try {
    const data = await getDocuments(kbId.value, {
      page: docPagination.page,
      pageSize: docPagination.pageSize
    })
    documents.value = data.records
    docPagination.total = data.total
  } finally {
    documentsLoading.value = false
  }
}

function handleFileChange(file: { raw: File }) {
  uploadFile.value = file.raw
}

async function handleUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  try {
    await uploadDocument(kbId.value, uploadFile.value)
    ElMessage.success('上传成功，正在处理中...')
    showUploadDialog.value = false
    uploadFile.value = null
    loadDocuments()
  } finally {
    uploading.value = false
  }
}

async function handleDeleteDocument(doc: Document) {
  try {
    await ElMessageBox.confirm(`确定要删除文档"${doc.filename}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDocument(kbId.value, doc.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
      ElMessage.error('操作失败，请稍后重试')
    }
  }
}

function getStatusType(status: string) {
  const map: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
    PENDING: 'info',
    PROCESSING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '等待中',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status] || status
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped lang="scss">
.kb-detail {
  .header {
    margin-bottom: 20px;

    .title {
      font-size: 20px;
      font-weight: 500;
    }
  }

  .tab-header {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
