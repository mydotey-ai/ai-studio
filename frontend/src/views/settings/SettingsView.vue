<template>
  <div class="settings-view">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="用户管理" name="users">
        <UserManagement />
      </el-tab-pane>

      <el-tab-pane label="组织设置" name="organization">
        <OrganizationSettings />
      </el-tab-pane>

      <el-tab-pane label="审计日志" name="audit">
        <AuditLogs />
      </el-tab-pane>

      <el-tab-pane label="存储配置" name="storage">
        <StorageConfig />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import UserManagement from './components/UserManagement.vue'
import OrganizationSettings from './components/OrganizationSettings.vue'
import AuditLogs from './components/AuditLogs.vue'
import StorageConfig from './components/StorageConfig.vue'

const activeTab = ref('users')
</script>

<style scoped>
.settings-view {
  height: 100%;
}

.settings-view :deep(.el-tabs__content) {
  height: calc(100vh - 200px);
  overflow-y: auto;
}
</style>
