<template>
  <div class="storage-config">
    <div class="header">
      <h3>存储配置</h3>
      <el-text type="info" size="small">管理文件存储配置</el-text>
    </div>

    <div class="actions">
      <el-button type="primary" :icon="Plus" @click="handleCreate"> 新建配置 </el-button>
    </div>

    <el-table v-loading="loading" :data="configs" :loading="loading" stripe>
      <el-table-column prop="description" label="名称" width="200" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getTypeType(row.storageType)" size="small">
            {{ getTypeLabel(row.storageType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="endpoint" label="端点" min-width="200" show-overflow-tooltip />
      <el-table-column prop="bucketName" label="Bucket" width="150" />
      <el-table-column prop="region" label="区域" width="120" />
      <el-table-column label="默认" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault" type="success" size="small">是</el-tag>
          <el-text v-else type="info">否</el-text>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)"> 编辑 </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)"> 删除 </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑存储配置' : '新建存储配置'"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" placeholder="请输入配置描述" />
        </el-form-item>

        <el-form-item label="存储类型" prop="storageType">
          <el-select
            v-model="form.storageType"
            placeholder="请选择存储类型"
            :disabled="isEdit"
            @change="handleTypeChange"
          >
            <el-option label="本地存储" value="LOCAL" />
            <el-option label="阿里云 OSS" value="OSS" />
            <el-option label="AWS S3" value="S3" />
          </el-select>
        </el-form-item>

        <template v-if="form.storageType !== 'LOCAL'">
          <el-form-item label="端点" prop="endpoint">
            <el-input v-model="form.endpoint" placeholder="请输入端点地址" />
          </el-form-item>

          <el-form-item label="Bucket" prop="bucketName">
            <el-input v-model="form.bucketName" placeholder="请输入 Bucket 名称" />
          </el-form-item>

          <el-form-item label="区域" prop="region">
            <el-input v-model="form.region" placeholder="请输入区域" />
          </el-form-item>

          <el-form-item label="Access Key" prop="accessKey">
            <el-input v-model="form.accessKey" placeholder="请输入 Access Key" />
          </el-form-item>

          <el-form-item label="Secret" prop="secretKey">
            <el-input
              v-model="form.secretKey"
              type="password"
              placeholder="请输入 Secret"
              show-password
            />
          </el-form-item>
        </template>

        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave"> 保存 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { storageApi } from '@/api/storage'
import type { StorageConfig, CreateStorageConfigRequest } from '@/types/storage'
import dayjs from 'dayjs'

const configs = ref<StorageConfig[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingConfigId = ref<number>()
const formRef = ref<FormInstance>()

const form = ref<CreateStorageConfigRequest>({
  storageType: 'LOCAL',
  isDefault: false
})

const rules: FormRules = {
  description: [{ required: true, message: '请输入配置描述', trigger: 'blur' }],
  storageType: [{ required: true, message: '请选择存储类型', trigger: 'change' }],
  endpoint: [{ required: true, message: '请输入端点地址', trigger: 'blur' }],
  bucketName: [{ required: true, message: '请输入 Bucket 名称', trigger: 'blur' }],
  accessKey: [{ required: true, message: '请输入 Access Key', trigger: 'blur' }],
  secretKey: [{ required: true, message: '请输入 Secret', trigger: 'blur' }]
}

const loadConfigs = async () => {
  loading.value = true
  try {
    const data = await storageApi.getAllConfigs()
    configs.value = data as StorageConfig[]
  } catch (error) {
    ElMessage.error('加载存储配置失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  isEdit.value = false
  editingConfigId.value = undefined
  form.value = {
    storageType: 'LOCAL',
    isDefault: false
  }
  dialogVisible.value = true
}

const handleEdit = (config: StorageConfig) => {
  isEdit.value = true
  editingConfigId.value = config.id
  form.value = {
    storageType: config.storageType,
    description: config.description,
    endpoint: config.endpoint,
    bucketName: config.bucketName,
    region: config.region,
    accessKey: config.accessKey,
    secretKey: '', // 不回显 secret
    isDefault: config.isDefault
  }
  dialogVisible.value = true
}

const handleTypeChange = () => {
  // 清空类型特定字段
  if (form.value.storageType === 'LOCAL') {
    form.value.endpoint = undefined
    form.value.bucketName = undefined
    form.value.region = undefined
    form.value.accessKey = undefined
    form.value.secretKey = undefined
  }
}

const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    saving.value = true
    try {
      if (isEdit.value && editingConfigId.value) {
        await storageApi.updateConfig(editingConfigId.value, form.value)
        ElMessage.success('更新成功')
      } else {
        await storageApi.createConfig(form.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await loadConfigs()
    } catch (error) {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = async (config: StorageConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除存储配置 "${config.description || config.id}" 吗?`,
      '确认删除',
      {
        type: 'error'
      }
    )

    await storageApi.deleteConfig(config.id)
    ElMessage.success('删除成功')
    await loadConfigs()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const getTypeType = (type: string) => {
  const map: Record<string, any> = {
    LOCAL: 'info',
    OSS: 'success',
    S3: 'warning'
  }
  return map[type] || ''
}

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    LOCAL: '本地',
    OSS: 'OSS',
    S3: 'S3'
  }
  return map[type] || type
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.storage-config {
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

.actions {
  margin-bottom: 20px;
}
</style>
