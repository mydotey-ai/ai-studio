import router from './index'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

// Set page title
router.beforeEach((to, _from, next) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - AI Studio`
  }
  next()
})

// Authentication guard
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !userStore.isLogin) {
    ElMessage.warning('请先登录')
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && userStore.isLogin) {
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

// Admin guard
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const requiresAdmin = to.meta.requiresAdmin === true

  if (requiresAdmin && !userStore.isAdmin) {
    ElMessage.error('需要管理员权限')
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})
