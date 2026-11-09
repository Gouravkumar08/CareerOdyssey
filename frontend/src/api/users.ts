import { apiClient } from './client'
import type { UserRequest, UserResponse } from './types'

export const usersApi = {
  list: (search?: string) =>
    apiClient.get<UserResponse[]>('/users', { params: search ? { search } : undefined }).then((r) => r.data),

  get: (id: number) => apiClient.get<UserResponse>(`/users/${id}`).then((r) => r.data),

  create: (payload: UserRequest) => apiClient.post<UserResponse>('/users', payload).then((r) => r.data),

  update: (id: number, payload: UserRequest) =>
    apiClient.put<UserResponse>(`/users/${id}`, payload).then((r) => r.data),

  remove: (id: number) => apiClient.delete(`/users/${id}`).then(() => undefined),

  parseResume: (resumeFile: File) => {
    const formData = new FormData()
    formData.append('resumeFile', resumeFile)
    return apiClient
      .post<UserRequest>('/users/parse-resume', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data)
  },
}
