<template>
  <div class="organization-settings">
    <div class="header">
      <h3>组织设置</h3>
      <el-text type="info" size="small">管理组织基本信息</el-text>
    </div>

    <el-card v-loading="loading">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        :disabled="!isEditable"
      >
        <el-form-item label="组织名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入组织名称" />
        </el-form-item>

        <el-form-item label="组织描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入组织描述"
          />
        </el-form-item>

        <el-form-item label="创建时间">
          <el-text>{{
            organization?.createdAt ? formatDate(organization.createdAt) : '-'
          }}</el-text>
        </el-form-item>

        <el-form-item>
          <el-button v-if="!isEditable" type="primary" @click="isEditable = true"> 编辑 </el-button>
          <template v-else>
            <el-button type="primary" :loading="saving" @click="handleSave"> 保存 </el-button>
            <el-button @click="handleCancel">取消</el-button>
          </template>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { organizationApi } from '@/api/organization'
import type { Organization } from '@/types/user'
import dayjs from 'dayjs'

const formRef = ref<FormInstance>()
const organization = ref<Organization>()
const loading = ref(false)
const saving = ref(false)
const isEditable = ref(false)

const form = ref<Partial<Organization>>({
  name: '',
  description: ''
})

const originalForm = ref<Partial<Organization>>({
  name: '',
  description: ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入组织名称', trigger: 'blur' },
    { min: 2, max: 50, message: '组织名称长度为 2-50 个字符', trigger: 'blur' }
  ]
}

const loadOrganization = async () => {
  loading.value = true
  try {
    const data = await organizationApi.getMyOrganization()
    if (data) {
      organization.value = data as Organization
      form.value = {
        name: data.name,
        description: data.description
      }
      originalForm.value = { ...form.value }
    } else {
      // 用户还没有组织，显示默认值或提示信息
      organization.value = undefined
      form.value = {
        name: '',
        description: ''
      }
      originalForm.value = { ...form.value }
      ElMessage.info('您还没有加入任何组织')
    }
  } catch (error) {
    ElMessage.error('加载组织信息失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    saving.value = true
    try {
      await organizationApi.updateOrganization(organization.value!.id, {
        name: form.value.name!,
        description: form.value.description
      })
      ElMessage.success('保存成功')
      isEditable.value = false
      await loadOrganization()
    } catch (error) {
      ElMessage.error('保存失败')
    } finally {
      saving.value = false
    }
  })
}

const handleCancel = () => {
  form.value = { ...originalForm.value }
  isEditable.value = false
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  loadOrganization()
})
</script>

<style scoped>
.organization-settings {
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
