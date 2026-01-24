import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { ErrorCode } from '@/types/common'
import { apiCache } from '@/utils/cache'

const baseURL = import.meta.env.VITE_API_BASE_URL

const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 60000
})

// Request interceptor
service.interceptors.request.use(
  config => {
    // Skip cache for non-GET requests
    if (config.method?.toLowerCase() !== 'get') {
      config.headers = config.headers || {}
      config.headers['X-Skip-Cache'] = 'true'
      const userStore = useUserStore()
      if (userStore.token) {
        config.headers.Authorization = `Bearer ${userStore.token}`
      }
      return config
    }

    // Check cache
    const cacheKey = `${config.url}?${JSON.stringify(config.params || {})}`
    const cachedData = apiCache.get(cacheKey)

    if (cachedData && !config.headers?.['X-Skip-Cache']) {
      // Return cached data immediately
      return Promise.reject({
        __cached: true,
        data: cachedData,
        config
      })
    }

    const userStore = useUserStore()
    if (userStore.token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
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

    const { code, message, result, data: apiData } = data

    // Cache GET requests
    if (
      response.config.method?.toLowerCase() === 'get' &&
      response.status === 200 &&
      !response.config.headers?.['X-Skip-Cache']
    ) {
      const cacheKey = `${response.config.url}?${JSON.stringify(response.config.params || {})}`
      const ttl = (response.config as any).cacheTTL || 5 * 60 * 1000 // 5 minutes default

      // Cache the result if it exists, otherwise cache the whole data
      const dataToCache = result !== undefined ? result : apiData !== undefined ? apiData : data
      apiCache.set(cacheKey, dataToCache, ttl)
    }

    // Check if response uses { code, message, result } or { code, message, data } format
    if (code === ErrorCode.SUCCESS || code === 0) {
      return result ?? apiData ?? data
    }

    // Otherwise assume direct data return
    if (code === undefined) {
      return data
    }

    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  error => {
    // Handle cached responses
    if (error.__cached) {
      return Promise.resolve({
        data: error.data,
        status: 200,
        statusText: 'OK (Cached)',
        config: error.config,
        headers: {}
      })
    }

    const { response } = error

    if (response) {
      const { status, data } = response

      switch (status) {
        case ErrorCode.UNAUTHORIZED: {
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        }
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
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config)
}

export function get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, config)
}

export function post<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> {
  return service.post(url, data, config)
}

export function put<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> {
  return service.put(url, data, config)
}

export function del<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config)
}
