import { get, put } from './request'
import type { Organization } from '@/types/user'

export interface CreateOrganizationRequest {
  name: string
  description?: string
}

export const organizationApi = {
  getMyOrganization() {
    return get<Organization>('/organizations/my')
  },
  getOrganizationById(id: number) {
    return get<Organization>(`/organizations/${id}`)
  },
  updateOrganization(id: number, data: CreateOrganizationRequest) {
    return put<void>(`/organizations/${id}`, data)
  }
}
