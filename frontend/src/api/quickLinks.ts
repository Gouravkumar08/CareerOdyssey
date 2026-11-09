import { apiClient } from './client'
import type { QuickLinkRequest, QuickLinkResponse } from './types'

export const quickLinksApi = {
  list: () => apiClient.get<QuickLinkResponse[]>('/quick-links').then((r) => r.data),

  create: (payload: QuickLinkRequest) =>
    apiClient.post<QuickLinkResponse>('/quick-links', payload).then((r) => r.data),

  update: (id: number, payload: QuickLinkRequest) =>
    apiClient.put<QuickLinkResponse>(`/quick-links/${id}`, payload).then((r) => r.data),

  remove: (id: number) => apiClient.delete(`/quick-links/${id}`).then(() => undefined),
}
