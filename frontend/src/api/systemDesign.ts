import { apiClient } from './client'
import type {
  SdCalendarDayResponse,
  SdCategory,
  SdGoalItemDayResponse,
  SdGoalItemRequest,
  SdGoalItemResponse,
  SdImportResultResponse,
  SdItemRequest,
  SdItemResponse,
  SdProgressUpdateRequest,
  SdRecentCompletedResponse,
  SdSheetRequest,
  SdSheetResponse,
  SdSheetStatsResponse,
  SdSubTopicRenameRequest,
  SdSubTopicResponse,
  SdTargetPlanRequest,
  SdTargetPlanResponse,
  SdTopicRequest,
  SdTopicResponse,
} from './types'

export interface SdItemFilters {
  topic?: string
  category?: SdCategory
  important?: boolean
  revision?: boolean
  completed?: boolean
  search?: string
}

export const systemDesignApi = {
  sheets: {
    list: (category?: SdCategory) =>
      apiClient.get<SdSheetResponse[]>('/sysdesign/sheets', { params: { category } }).then((r) => r.data),

    get: (id: number) => apiClient.get<SdSheetResponse>(`/sysdesign/sheets/${id}`).then((r) => r.data),

    create: (payload: SdSheetRequest) =>
      apiClient.post<SdSheetResponse>('/sysdesign/sheets', payload).then((r) => r.data),

    update: (id: number, payload: SdSheetRequest) =>
      apiClient.put<SdSheetResponse>(`/sysdesign/sheets/${id}`, payload).then((r) => r.data),

    remove: (id: number) => apiClient.delete(`/sysdesign/sheets/${id}`).then(() => undefined),

    exportExcel: (id: number) =>
      apiClient.get<Blob>(`/sysdesign/sheets/${id}/export`, { responseType: 'blob' }).then((r) => r.data),

    importExcel: (id: number, file: File) => {
      const formData = new FormData()
      formData.append('file', file)
      return apiClient
        .post<SdImportResultResponse>(`/sysdesign/sheets/${id}/import`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        .then((r) => r.data)
    },

    addItem: (sheetId: number, payload: SdItemRequest) =>
      apiClient.post<SdItemResponse>(`/sysdesign/sheets/${sheetId}/items`, payload).then((r) => r.data),

    removeItem: (sheetId: number, itemId: number) =>
      apiClient.delete(`/sysdesign/sheets/${sheetId}/items/${itemId}`).then(() => undefined),
  },

  items: {
    listForSheet: (sheetId: number, filters: SdItemFilters = {}) =>
      apiClient
        .get<SdItemResponse[]>(`/sysdesign/sheets/${sheetId}/items`, { params: filters })
        .then((r) => r.data),

    stats: (sheetId: number) =>
      apiClient.get<SdSheetStatsResponse>(`/sysdesign/sheets/${sheetId}/stats`).then((r) => r.data),

    update: (id: number, payload: SdItemRequest) =>
      apiClient.put<SdItemResponse>(`/sysdesign/items/${id}`, payload).then((r) => r.data),

    updateProgress: (id: number, payload: SdProgressUpdateRequest) =>
      apiClient.put<SdItemResponse>(`/sysdesign/items/${id}/progress`, payload).then((r) => r.data),
  },

  topics: {
    list: () => apiClient.get<SdTopicResponse[]>('/sysdesign/topics').then((r) => r.data),

    create: (payload: SdTopicRequest) =>
      apiClient.post<SdTopicResponse>('/sysdesign/topics', payload).then((r) => r.data),

    rename: (id: number, payload: SdTopicRequest) =>
      apiClient.put<SdTopicResponse>(`/sysdesign/topics/${id}`, payload).then((r) => r.data),

    remove: (id: number) => apiClient.delete(`/sysdesign/topics/${id}`).then(() => undefined),

    reorder: (orderedTopicIds: number[]) =>
      apiClient.put('/sysdesign/topics/reorder', orderedTopicIds).then(() => undefined),

    listSubTopics: (topicId: number) =>
      apiClient.get<SdSubTopicResponse[]>(`/sysdesign/topics/${topicId}/subtopics`).then((r) => r.data),

    renameSubTopic: (topicId: number, payload: SdSubTopicRenameRequest) =>
      apiClient
        .put<SdSubTopicResponse[]>(`/sysdesign/topics/${topicId}/subtopics/rename`, payload)
        .then((r) => r.data),

    removeSubTopic: (topicId: number, name: string) =>
      apiClient
        .delete<SdSubTopicResponse[]>(`/sysdesign/topics/${topicId}/subtopics`, { params: { name } })
        .then((r) => r.data),
  },

  planner: {
    getTarget: () => apiClient.get<SdTargetPlanResponse>('/sysdesign/planner/target').then((r) => r.data),

    setTarget: (payload: SdTargetPlanRequest) =>
      apiClient.put<SdTargetPlanResponse>('/sysdesign/planner/target', payload).then((r) => r.data),

    calendar: (year: number, month: number) =>
      apiClient
        .get<SdCalendarDayResponse[]>('/sysdesign/planner/calendar', { params: { year, month } })
        .then((r) => r.data),

    recent: (days = 2, limit = 10) =>
      apiClient
        .get<SdRecentCompletedResponse[]>('/sysdesign/planner/recent', { params: { days, limit } })
        .then((r) => r.data),

    createGoalItem: (payload: SdGoalItemRequest) =>
      apiClient.post<SdGoalItemResponse>('/sysdesign/planner/goal-items', payload).then((r) => r.data),

    updateGoalItem: (id: number, payload: SdGoalItemRequest) =>
      apiClient.put<SdGoalItemResponse>(`/sysdesign/planner/goal-items/${id}`, payload).then((r) => r.data),

    listGoalItems: () => apiClient.get<SdGoalItemResponse[]>('/sysdesign/planner/goal-items').then((r) => r.data),

    deleteGoalItem: (id: number) => apiClient.delete(`/sysdesign/planner/goal-items/${id}`).then(() => undefined),

    goalItemsSchedule: (year: number, month: number) =>
      apiClient
        .get<SdGoalItemDayResponse[]>('/sysdesign/planner/goal-items/schedule', { params: { year, month } })
        .then((r) => r.data),

    previewGoalItem: (payload: SdGoalItemRequest) =>
      apiClient.post<SdGoalItemResponse>('/sysdesign/planner/goal-items/preview', payload).then((r) => r.data),
  },
}
