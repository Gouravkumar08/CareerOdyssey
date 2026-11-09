import { apiClient } from './client'
import type { OutreachResponse, OutreachSendRequest, ReplyStatus } from './types'

export const outreachApi = {
  list: () => apiClient.get<OutreachResponse[]>('/outreach').then((r) => r.data),

  get: (id: number) => apiClient.get<OutreachResponse>(`/outreach/${id}`).then((r) => r.data),

  resendRequest: (id: number, followUp: boolean) =>
    apiClient
      .get<OutreachSendRequest>(`/outreach/${id}/resend-request`, { params: { followUp } })
      .then((r) => r.data),

  send: (payload: OutreachSendRequest, resumeFile: File | null, signal?: AbortSignal) => {
    const formData = new FormData()
    formData.append('userId', String(payload.userId))
    payload.companyIds.forEach((id) => formData.append('companyIds', String(id)))
    formData.append('templateId', String(payload.templateId))
    if (payload.jobTitle) formData.append('jobTitle', payload.jobTitle)
    if (payload.jobUrl) formData.append('jobUrl', payload.jobUrl)
    if (resumeFile) formData.append('resumeFile', resumeFile)
    return apiClient
      .post<OutreachResponse[]>('/outreach/send', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        signal,
      })
      .then((r) => r.data)
  },

  updateReplyStatus: (id: number, replyStatus: ReplyStatus) =>
    apiClient.patch<OutreachResponse>(`/outreach/${id}/reply-status`, { replyStatus }).then((r) => r.data),

  remove: (id: number) => apiClient.delete(`/outreach/${id}`).then(() => undefined),

  removeAll: () => apiClient.delete('/outreach').then(() => undefined),
}
