import { apiClient } from './client'
import type {
  CalendarDayResponse,
  Difficulty,
  DsaGoalItemDayResponse,
  DsaGoalItemRequest,
  DsaGoalItemResponse,
  ImportResultResponse,
  ProgressUpdateRequest,
  QuestionRequest,
  QuestionResponse,
  RecentSolvedResponse,
  SheetRequest,
  SheetResponse,
  SheetStatsResponse,
  TargetPlanRequest,
  TargetPlanResponse,
  TopicResponse,
} from './types'

export interface QuestionFilters {
  topic?: string
  difficulty?: Difficulty
  important?: boolean
  revision?: boolean
  solved?: boolean
  search?: string
}

export const dsaApi = {
  sheets: {
    list: () => apiClient.get<SheetResponse[]>('/dsa/sheets').then((r) => r.data),

    get: (id: number) => apiClient.get<SheetResponse>(`/dsa/sheets/${id}`).then((r) => r.data),

    create: (payload: SheetRequest) => apiClient.post<SheetResponse>('/dsa/sheets', payload).then((r) => r.data),

    update: (id: number, payload: SheetRequest) =>
      apiClient.put<SheetResponse>(`/dsa/sheets/${id}`, payload).then((r) => r.data),

    remove: (id: number) => apiClient.delete(`/dsa/sheets/${id}`).then(() => undefined),

    exportExcel: (id: number) =>
      apiClient.get<Blob>(`/dsa/sheets/${id}/export`, { responseType: 'blob' }).then((r) => r.data),

    importExcel: (id: number, file: File) => {
      const formData = new FormData()
      formData.append('file', file)
      return apiClient
        .post<ImportResultResponse>(`/dsa/sheets/${id}/import`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        .then((r) => r.data)
    },

    addQuestion: (sheetId: number, payload: QuestionRequest) =>
      apiClient.post<QuestionResponse>(`/dsa/sheets/${sheetId}/questions`, payload).then((r) => r.data),

    removeQuestion: (sheetId: number, questionId: number) =>
      apiClient.delete(`/dsa/sheets/${sheetId}/questions/${questionId}`).then(() => undefined),
  },

  questions: {
    listForSheet: (sheetId: number, filters: QuestionFilters = {}) =>
      apiClient
        .get<QuestionResponse[]>(`/dsa/sheets/${sheetId}/questions`, { params: filters })
        .then((r) => r.data),

    stats: (sheetId: number) => apiClient.get<SheetStatsResponse>(`/dsa/sheets/${sheetId}/stats`).then((r) => r.data),

    update: (id: number, payload: QuestionRequest) =>
      apiClient.put<QuestionResponse>(`/dsa/questions/${id}`, payload).then((r) => r.data),

    updateProgress: (id: number, payload: ProgressUpdateRequest) =>
      apiClient.put<QuestionResponse>(`/dsa/questions/${id}/progress`, payload).then((r) => r.data),
  },

  planner: {
    getTarget: () => apiClient.get<TargetPlanResponse>('/dsa/planner/target').then((r) => r.data),

    setTarget: (payload: TargetPlanRequest) =>
      apiClient.put<TargetPlanResponse>('/dsa/planner/target', payload).then((r) => r.data),

    calendar: (year: number, month: number) =>
      apiClient
        .get<CalendarDayResponse[]>('/dsa/planner/calendar', { params: { year, month } })
        .then((r) => r.data),

    recent: (days = 2, limit = 10) =>
      apiClient
        .get<RecentSolvedResponse[]>('/dsa/planner/recent', { params: { days, limit } })
        .then((r) => r.data),

    createGoalItem: (payload: DsaGoalItemRequest) =>
      apiClient.post<DsaGoalItemResponse>('/dsa/planner/goal-items', payload).then((r) => r.data),

    updateGoalItem: (id: number, payload: DsaGoalItemRequest) =>
      apiClient.put<DsaGoalItemResponse>(`/dsa/planner/goal-items/${id}`, payload).then((r) => r.data),

    listGoalItems: () => apiClient.get<DsaGoalItemResponse[]>('/dsa/planner/goal-items').then((r) => r.data),

    deleteGoalItem: (id: number) => apiClient.delete(`/dsa/planner/goal-items/${id}`).then(() => undefined),

    goalItemsSchedule: (year: number, month: number) =>
      apiClient
        .get<DsaGoalItemDayResponse[]>('/dsa/planner/goal-items/schedule', { params: { year, month } })
        .then((r) => r.data),

    previewGoalItem: (payload: DsaGoalItemRequest) =>
      apiClient.post<DsaGoalItemResponse>('/dsa/planner/goal-items/preview', payload).then((r) => r.data),
  },

  topics: {
    list: () => apiClient.get<TopicResponse[]>('/dsa/topics').then((r) => r.data),

    count: (topicId: number, params: { difficulties?: Difficulty[]; sheetId?: number | null }) =>
      apiClient
        .get<{ count: number }>(`/dsa/topics/${topicId}/question-count`, {
          params: { difficulties: params.difficulties?.join(','), sheetId: params.sheetId ?? undefined },
        })
        .then((r) => r.data.count),
  },
}
