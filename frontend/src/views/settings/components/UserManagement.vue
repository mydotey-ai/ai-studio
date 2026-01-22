<template>
  <div class="user-management">
    <div class="header">
      <h3>用户管理</h3>
      <el-text type="info" size="small">管理系统用户和权限</el-text>
    </div>

    <el-table v-loading="loading" :data="users" :loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column label="角色" width="150">
        <template #default="{ row }">
          <el-tag :type="getRoleType(row.role)" size="small">
            {{ getRoleLabel(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)"> 编辑 </el-button>
          <el-button
            v-if="row.status === UserStatus.ACTIVE"
            link
            type="warning"
            size="small"
            @click="handleUpdateStatus(row, UserStatus.INACTIVE)"
          >
            禁用
          </el-button>
          <el-button
            v-else
            link
            type="success"
            size="small"
            @click="handleUpdateStatus(row, UserStatus.ACTIVE)"
          >
            启用
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)"> 删除 </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave"> 保存 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { userApi } from '@/api/user'
import type { User } from '@/types/user'
import { UserStatus } from '@/types/user'
import dayjs from 'dayjs'

const users = ref<User[]>([])
const loading = ref(false)
const saving = ref(false)
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()

const editForm = ref<Partial<User>>({
  id: 0,
  username: '',
  email: ''
})

const editRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const loadUsers = async () => {
  loading.value = true
  try {
    const data = await userApi.getAllUsers()
    users.value = data as User[]
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (user: User) => {
  editForm.value = { ...user }
  editDialogVisible.value = true
}

const handleSave = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async valid => {
    if (!valid) return

    saving.value = true
    try {
      await userApi.updateUser(editForm.value.id!, {
        email: editForm.value.email
      })
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      await loadUsers()
    } catch (error) {
      ElMessage.error('更新失败')
    } finally {
      saving.value = false
    }
  })
}

const handleUpdateStatus = async (user: User, status: UserStatus) => {
  try {
    await ElMessageBox.confirm(
      `确定要${status === 'ACTIVE' ? '启用' : '禁用'}用户 ${user.username} 吗?`,
      '确认操作',
      { type: 'warning' }
    )

    await userApi.updateUserStatus(user.id, status)
    ElMessage.success('操作成功')
    await loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleDelete = async (user: User) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 ${user.username} 吗?此操作不可恢复!`, '确认删除', {
      type: 'error'
    })

    await userApi.deleteUser(user.id)
    ElMessage.success('删除成功')
    await loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const getRoleType = (role: string) => {
  const map: Record<string, any> = {
    USER: '',
    ADMIN: 'warning',
    SUPER_ADMIN: 'danger'
  }
  return map[role] || ''
}

const getRoleLabel = (role: string) => {
  const map: Record<string, string> = {
    USER: '普通用户',
    ADMIN: '管理员',
    SUPER_ADMIN: '超级管理员'
  }
  return map[role] || role
}

const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    ACTIVE: 'success',
    INACTIVE: 'info',
    LOCKED: 'danger'
  }
  return map[status] || ''
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    ACTIVE: '正常',
    INACTIVE: '禁用',
    LOCKED: '锁定'
  }
  return map[status] || status
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}
</style>
