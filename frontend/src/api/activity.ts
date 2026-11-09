import { apiClient } from './client'
import type { StreakResponse } from './types'

export const activityApi = {
  streak: () => apiClient.get<StreakResponse>('/activity/streak').then((r) => r.data),
}
