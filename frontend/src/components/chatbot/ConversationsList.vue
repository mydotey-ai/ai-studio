<template>
  <div class="conversations-list">
    <div class="header">
      <h3>会话历史</h3>
      <el-button type="primary" :icon="Plus" @click="handleCreate">新建会话</el-button>
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
          {{ row.title || '未命名会话' }}
        </template>
      </el-table-column>
      <el-table-column prop="messageCount" label="消息数" width="100" align="center" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
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

    <div v-if="pagination.total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadConversations"
        @current-change="loadConversations"
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
  (e: 'select', conversation: Conversation): void
  (e: 'created', conversation: Conversation): void
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
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '加载会话列表失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  try {
    const conversation = await createConversation(props.chatbotId)
    ElMessage.success('创建会话成功')
    emit('created', conversation)
    await loadConversations()
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '创建会话失败'
    ElMessage.error(errorMessage)
  }
}

function handleSelect(conversation: Conversation) {
  emit('select', conversation)
}

async function handleDelete(conversation: Conversation) {
  try {
    await ElMessageBox.confirm(
      `确定要删除会话"${conversation.title || '未命名会话'}"吗？此操作不可恢复。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteConversation(conversation.id)
    ElMessage.success('删除成功')
    await loadConversations()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const errorMessage = error instanceof Error ? error.message : '删除失败，请稍后重试'
      ElMessage.error(errorMessage)
    }
  }
}

function formatDateTime(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadConversations()
})

defineExpose({
  loadConversations
})
</script>

<style scoped lang="scss">
.conversations-list {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
      font-size: 18px;
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
