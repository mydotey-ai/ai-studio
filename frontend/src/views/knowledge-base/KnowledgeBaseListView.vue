<template>
  <div class="kb-list">
    <div class="header">
      <h2>知识库</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        创建知识库
      </el-button>
    </div>

    <el-table
      :data="knowledgeBases"
      :loading="loading"
      stripe
      style="cursor: pointer"
      @row-click="handleRowClick"
    >
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column label="文档数" width="100" align="center">
        <template #default="{ row }">
          {{ row.documentCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'" size="small">
            {{ row.isPublic ? '公开' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click.stop="handleView(row)">
            查看
          </el-button>
          <el-button link type="danger" :icon="Delete" @click.stop="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadKnowledgeBases"
        @current-change="loadKnowledgeBases"
      />
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建知识库" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="向量模型" prop="embeddingModelId">
          <el-select
            v-model="form.embeddingModelId"
            placeholder="请选择向量模型"
            :loading="loadingModels"
            style="width: 100%"
          >
            <el-option
              v-for="model in embeddingModels"
              :key="model.id"
              :label="model.name"
              :value="model.id"
            >
              <span style="float: left">{{ model.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">{{ model.model }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="LLM模型" prop="llmModelId">
          <el-select
            v-model="form.llmModelId"
            placeholder="请选择LLM模型"
            :loading="loadingModels"
            style="width: 100%"
          >
            <el-option
              v-for="model in llmModels"
              :key="model.id"
              :label="model.name"
              :value="model.id"
            >
              <span style="float: left">{{ model.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">{{ model.model }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="可见性" prop="isPublic">
          <el-switch v-model="form.isPublic" active-text="公开" inactive-text="私有" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate"> 创建 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, View, Delete } from '@element-plus/icons-vue'
import {
  getKnowledgeBases,
  createKnowledgeBase,
  deleteKnowledgeBase as deleteKbApi
} from '@/api/knowledge-base'
import { getModelConfigs } from '@/api/modelConfig'
import { ModelConfigType } from '@/api/modelConfig'
import type { KnowledgeBase } from '@/types/knowledge-base'
import type { ModelConfig } from '@/api/modelConfig'
import dayjs from 'dayjs'

const router = useRouter()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitting = ref(false)
const showCreateDialog = ref(false)
const loadingModels = ref(false)
const knowledgeBases = ref<KnowledgeBase[]>([])
const embeddingModels = ref<ModelConfig[]>([])
const llmModels = ref<ModelConfig[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const form = reactive({
  name: '',
  description: '',
  embeddingModelId: undefined as number | undefined,
  llmModelId: undefined as number | undefined,
  isPublic: false
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
}

async function loadModels() {
  loadingModels.value = true
  try {
    const [embedding, llm] = await Promise.all([
      getModelConfigs(ModelConfigType.EMBEDDING),
      getModelConfigs(ModelConfigType.LLM)
    ])
    embeddingModels.value = embedding
    llmModels.value = llm

    // Auto-select first available model if not selected
    if (!form.embeddingModelId && embedding.length > 0) {
      const defaultModel = embedding.find(m => m.isDefault)
      form.embeddingModelId = defaultModel?.id || embedding[0].id
    }
    if (!form.llmModelId && llm.length > 0) {
      const defaultModel = llm.find(m => m.isDefault)
      form.llmModelId = defaultModel?.id || llm[0].id
    }
  } catch (error) {
    console.error('Failed to load models:', error)
    ElMessage.error('加载模型配置失败')
  } finally {
    loadingModels.value = false
  }
}

async function loadKnowledgeBases() {
  loading.value = true
  try {
    const data = await getKnowledgeBases({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    knowledgeBases.value = data.records
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    submitting.value = true
    try {
      await createKnowledgeBase(form)
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      resetForm()
      // Skip cache to get fresh data after creation
      const data = await getKnowledgeBases(
        { page: pagination.page, pageSize: pagination.pageSize },
        true
      )
      knowledgeBases.value = data.records
      pagination.total = data.total
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.embeddingModelId = undefined
  form.llmModelId = undefined
  form.isPublic = false
  formRef.value?.resetFields()

  // Auto-select default models
  if (embeddingModels.value.length > 0) {
    const defaultModel = embeddingModels.value.find(m => m.isDefault)
    form.embeddingModelId = defaultModel?.id || embeddingModels.value[0].id
  }
  if (llmModels.value.length > 0) {
    const defaultModel = llmModels.value.find(m => m.isDefault)
    form.llmModelId = defaultModel?.id || llmModels.value[0].id
  }
}

function handleRowClick(row: KnowledgeBase) {
  router.push(`/knowledge-bases/${row.id}`)
}

function handleView(row: KnowledgeBase) {
  router.push(`/knowledge-bases/${row.id}`)
}

async function handleDelete(row: KnowledgeBase) {
  try {
    await ElMessageBox.confirm(`确定要删除知识库"${row.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteKbApi(row.id)
    ElMessage.success('删除成功')
    // Skip cache to get fresh data after deletion
    const data = await getKnowledgeBases(
      { page: pagination.page, pageSize: pagination.pageSize },
      true
    )
    knowledgeBases.value = data.records
    pagination.total = data.total
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
      ElMessage.error('操作失败，请稍后重试')
    }
  }
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadModels()
  loadKnowledgeBases()
})
</script>

<style scoped lang="scss">
.kb-list {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 24px;
      color: #303133;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
