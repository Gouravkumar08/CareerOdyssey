import { apiClient } from './client'
import type { CompanyRequest, CompanyResponse } from './types'

export const companiesApi = {
  list: (search?: string) =>
    apiClient.get<CompanyResponse[]>('/companies', { params: search ? { search } : undefined }).then((r) => r.data),

  get: (id: number) => apiClient.get<CompanyResponse>(`/companies/${id}`).then((r) => r.data),

  create: (payload: CompanyRequest) => apiClient.post<CompanyResponse>('/companies', payload).then((r) => r.data),

  update: (id: number, payload: CompanyRequest) =>
    apiClient.put<CompanyResponse>(`/companies/${id}`, payload).then((r) => r.data),

  remove: (id: number) => apiClient.delete(`/companies/${id}`).then(() => undefined),
}
