import { apiClient } from './client'
import type { EmailTemplateResponse } from './types'

export const templatesApi = {
  list: () => apiClient.get<EmailTemplateResponse[]>('/email-templates').then((r) => r.data),
}
