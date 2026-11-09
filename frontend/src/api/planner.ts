import { apiClient } from './client'
import type {
  PlannerCalendarDay,
  PlannerNoteResponse,
  PlannerOverview,
  PlannerTaskRequest,
  PlannerTaskResponse,
} from './types'

export const plannerApi = {
  overview: () => apiClient.get<PlannerOverview>('/planner/overview').then((r) => r.data),

  calendar: (year: number, month: number) =>
    apiClient.get<PlannerCalendarDay[]>('/planner/calendar', { params: { year, month } }).then((r) => r.data),

  tasks: {
    list: () => apiClient.get<PlannerTaskResponse[]>('/planner/tasks').then((r) => r.data),

    create: (payload: PlannerTaskRequest) =>
      apiClient.post<PlannerTaskResponse>('/planner/tasks', payload).then((r) => r.data),

    update: (id: number, payload: PlannerTaskRequest) =>
      apiClient.put<PlannerTaskResponse>(`/planner/tasks/${id}`, payload).then((r) => r.data),

    remove: (id: number) => apiClient.delete(`/planner/tasks/${id}`).then(() => undefined),

    setCompleted: (id: number, date: string, completed: boolean) =>
      (completed
        ? apiClient.put(`/planner/tasks/${id}/completions/${date}`)
        : apiClient.delete(`/planner/tasks/${id}/completions/${date}`)
      ).then(() => undefined),

    updateProgress: (id: number, current: number) =>
      apiClient.patch<PlannerTaskResponse>(`/planner/tasks/${id}/progress`, { current }).then((r) => r.data),
  },

  notes: {
    create: (content: string) =>
      apiClient.post<PlannerNoteResponse>('/planner/notes', { content }).then((r) => r.data),

    update: (id: number, content: string) =>
      apiClient.put<PlannerNoteResponse>(`/planner/notes/${id}`, { content }).then((r) => r.data),

    remove: (id: number) => apiClient.delete(`/planner/notes/${id}`).then(() => undefined),
  },
}
