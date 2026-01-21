<template>
  <div class="conversations-list">
    <div class="header">
      <h3>对话历史</h3>
      <el-button type="primary" :icon="Plus" @click="handleNewConversation"> 新建对话 </el-button>
    </div>

    <el-table
      :data="conversations"
      :loading="loading"
      stripe
      style="cursor: pointer"
      @row-click="handleSelect"
    >
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          {{ row.title || '新对话' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
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
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        small
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { getConversations, createConversation, deleteConversation } from '@/api/conversation'
import type { Conversation } from '@/types/chatbot'
import dayjs from 'dayjs'

interface Props {
  chatbotId: number
}

interface Emits {
  (e: 'select', conversationId: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const conversations = ref<Conversation[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

async function loadConversations() {
  loading.value = true
  try {
    const data = await getConversations(props.chatbotId, {
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    conversations.value = data.records
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

async function handleNewConversation() {
  loading.value = true
  try {
    const conversation = await createConversation(props.chatbotId)
    ElMessage.success('对话创建成功')
    emit('select', conversation.id)
    loadConversations()
  } catch (error) {
    ElMessage.error('创建对话失败')
    console.error('Failed to create conversation:', error)
  } finally {
    loading.value = false
  }
}

function handleSelect(conversation: Conversation) {
  emit('select', conversation.id)
}

async function handleDelete(conversation: Conversation) {
  try {
    await ElMessageBox.confirm('确定要删除此对话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    loading.value = true
    try {
      await deleteConversation(conversation.id)
      ElMessage.success('删除成功')
      loadConversations()
    } catch (error) {
      ElMessage.error('删除失败')
      console.error('Failed to delete conversation:', error)
    } finally {
      loading.value = false
    }
  } catch {
    // User cancelled
  }
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadConversations()
})
</script>

<style scoped lang="scss">
.conversations-list {
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

  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
