<template>
  <div class="profile-container">
    <el-card>
      <template #header>
        <span class="card-header">个人信息</span>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基础信息" name="basic">
          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="100px"
            style="max-width: 600px"
          >
            <el-form-item label="用户名">
              <el-input v-model="username" disabled />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item label="个人简介" prop="bio">
              <el-input
                v-model="profileForm.bio"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="请输入个人简介"
              />
            </el-form-item>
            <el-form-item label="语言" prop="language">
              <el-select v-model="profileForm.language" placeholder="请选择语言">
                <el-option label="简体中文" value="zh-CN" />
                <el-option label="English" value="en-US" />
              </el-select>
            </el-form-item>
            <el-form-item label="时区" prop="timezone">
              <el-select v-model="profileForm.timezone" placeholder="请选择时区" filterable>
                <el-option label="Asia/Shanghai" value="Asia/Shanghai" />
                <el-option label="Asia/Hong_Kong" value="Asia/Hong_Kong" />
                <el-option label="Asia/Tokyo" value="Asia/Tokyo" />
                <el-option label="America/New_York" value="America/New_York" />
                <el-option label="America/Los_Angeles" value="America/Los_Angeles" />
                <el-option label="Europe/London" value="Europe/London" />
                <el-option label="Europe/Paris" value="Europe/Paris" />
                <el-option label="UTC" value="UTC" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveProfile" :loading="saving">
                保存
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="账户安全" name="security">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            style="max-width: 600px"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（至少8位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
                @keyup.enter="handleChangePassword"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="changing">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { userApi, type UserProfile, type UpdateProfileRequest, type ChangePasswordRequest } from '@/api/user'

const activeTab = ref('basic')
const saving = ref(false)
const changing = ref(false)
const username = ref('')

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

const profileForm = reactive<UpdateProfileRequest>({
  email: '',
  bio: '',
  language: 'zh-CN',
  timezone: 'Asia/Shanghai'
})

const passwordForm = reactive<ChangePasswordRequest>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (_rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '新密码至少8位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

let userProfile: UserProfile | null = null

async function loadUserProfile() {
  try {
    userProfile = await userApi.getUserProfile()
    username.value = userProfile.username
    profileForm.email = userProfile.email
    profileForm.bio = userProfile.bio || ''
    profileForm.language = userProfile.language || 'zh-CN'
    profileForm.timezone = userProfile.timezone || 'Asia/Shanghai'
  } catch (error: any) {
    ElMessage.error(error.message || '加载个人信息失败')
  }
}

async function handleSaveProfile() {
  if (!profileFormRef.value) return

  await profileFormRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      await userApi.updateProfile(profileForm)
      ElMessage.success('保存成功')
      await loadUserProfile()
    } catch (error: any) {
      ElMessage.error(error.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    changing.value = true
    try {
      await userApi.changePassword(passwordForm)
      ElMessage.success('密码修改成功，请重新登录')
      passwordFormRef.value?.resetFields()
    } catch (error: any) {
      ElMessage.error(error.message || '密码修改失败')
    } finally {
      changing.value = false
    }
  })
}

onMounted(() => {
  loadUserProfile()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
}
</style>
