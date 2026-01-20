import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { ErrorCode } from '@/types/common'

const baseURL = import.meta.env.VITE_API_BASE_URL

const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 60000
})

// Request interceptor
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response

    // Validate response structure
    if (!data || typeof data !== 'object') {
      ElMessage.error('无效的响应数据')
      return Promise.reject(new Error('Invalid response'))
    }

    const { code, message, result } = data

    // Check if response uses { code, message, result } format
    if (code === ErrorCode.SUCCESS || code === 0) {
      return result ?? data
    }

    // Otherwise assume direct data return
    if (code === undefined) {
      return data
    }

    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    const { response } = error

    if (response) {
      const { status, data } = response

      switch (status) {
        case ErrorCode.UNAUTHORIZED:
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case ErrorCode.FORBIDDEN:
          ElMessage.error('没有权限访问')
          break
        case ErrorCode.NOT_FOUND:
          ElMessage.error('请求的资源不存在')
          break
        case ErrorCode.INTERNAL_ERROR:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default service

// Export generic request method
export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config)
}

export function get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, config)
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config)
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config)
}

export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config)
}
