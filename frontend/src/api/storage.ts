import { get, post, put, del } from './request'
import type {
  StorageConfig,
  CreateStorageConfigRequest,
  UpdateStorageConfigRequest
} from '@/types/storage'

export const storageApi = {
  getAllConfigs() {
    return get<StorageConfig[]>('/storage-configs')
  },
  getConfigById(id: number) {
    return get<StorageConfig>(`/storage-configs/${id}`)
  },
  getDefaultConfig() {
    return get<StorageConfig>('/storage-configs/default')
  },
  createConfig(data: CreateStorageConfigRequest) {
    return post<StorageConfig>('/storage-configs', data)
  },
  updateConfig(id: number, data: UpdateStorageConfigRequest) {
    return put<StorageConfig>(`/storage-configs/${id}`, data)
  },
  deleteConfig(id: number) {
    return del(`/storage-configs/${id}`)
  }
}
