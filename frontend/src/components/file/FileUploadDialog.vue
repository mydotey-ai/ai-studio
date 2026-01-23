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
