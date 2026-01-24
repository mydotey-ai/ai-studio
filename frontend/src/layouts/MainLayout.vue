<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar">
      <div class="logo-container">
        <span v-if="!isCollapse" class="logo-text">AI Studio</span>
        <span v-else class="logo-icon">AI</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        router
        class="sidebar-menu"
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-menu-item v-if="!route.meta?.hidden" :index="route.path" :route="route.path">
            <el-icon v-if="route.meta?.icon">
              <component :is="getIcon(String(route.meta.icon))" />
            </el-icon>
            <template #title>{{ route.meta?.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-icon" aria-label="Toggle sidebar" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-icon class="user-icon"><User /></el-icon>
              <span class="username">{{ userInfo?.username }}</span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人信息
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component: RouteComponent }">
          <transition name="fade-transform" mode="out-in">
            <component :is="RouteComponent" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import type { Component } from 'vue'
import {
  Fold,
  Expand,
  User,
  ArrowDown,
  SwitchButton,
  Odometer,
  FolderOpened,
  ChatDotSquare,
  Connection,
  Setting
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const currentRoute = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)

const userInfo = computed(() => userStore.userInfo)
const activeMenu = computed(() => currentRoute.path)

// Get menu routes from router configuration
const routerConfig = router.getRoutes()
const mainRoute = routerConfig.find(r => r.path === '/')
const menuRoutes = computed(() => {
  return mainRoute?.children?.filter(child => !child.meta?.hidden) || []
})

// Icon mapping
const iconMap: Record<string, Component> = {
  Odometer,
  FolderOpened,
  User,
  ChatDotSquare,
  Connection,
  Setting
}

function getIcon(iconName: string): Component {
  return iconMap[iconName] || User
}

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

async function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      } catch {
        // User cancelled - no action needed
        console.debug('Logout cancelled by user')
      }
      break
  }
}

onMounted(() => {
  // Initialize auth state from localStorage
  userStore.initAuth()
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4a;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.logo-icon {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.sidebar-menu {
  border-right: none;
  background-color: #304156;
}

:deep(.el-menu) {
  background-color: #304156;
}

:deep(.el-menu-item) {
  color: #bfcbd9;
}

:deep(.el-menu-item:hover) {
  background-color: #263445 !important;
  color: #fff;
}

:deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
  color: #fff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-icon {
  font-size: 20px;
  cursor: pointer;
  color: #5a5e66;
  transition: color 0.3s;
}

.collapse-icon:hover {
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 12px;
  height: 60px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-icon {
  font-size: 20px;
  color: #5a5e66;
}

.username {
  margin: 0 8px 0 12px;
  font-size: 14px;
  color: #5a5e66;
}

.arrow-icon {
  font-size: 12px;
  color: #5a5e66;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

/* Fade transform animation */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}
</style>
