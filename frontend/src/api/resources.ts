import { apiClient } from './client'
import type {
  ResourceCategoryRequest,
  ResourceCategoryResponse,
  ResourceItemRequest,
  ResourceItemResponse,
  ResourceSubCategoryRenameRequest,
  ResourceSubCategoryResponse,
} from './types'

export interface ResourceItemFilters {
  category?: string
  subCategory?: string
  important?: boolean
  search?: string
}

export const resourcesApi = {
  categories: {
    list: () => apiClient.get<ResourceCategoryResponse[]>('/resources/categories').then((r) => r.data),

    create: (payload: ResourceCategoryRequest) =>
      apiClient.post<ResourceCategoryResponse>('/resources/categories', payload).then((r) => r.data),

    rename: (id: number, payload: ResourceCategoryRequest) =>
      apiClient.put<ResourceCategoryResponse>(`/resources/categories/${id}`, payload).then((r) => r.data),

    remove: (id: number) => apiClient.delete(`/resources/categories/${id}`).then(() => undefined),

    reorder: (orderedCategoryIds: number[]) =>
      apiClient.put('/resources/categories/reorder', orderedCategoryIds).then(() => undefined),

    listSubCategories: (categoryId: number) =>
      apiClient
        .get<ResourceSubCategoryResponse[]>(`/resources/categories/${categoryId}/subcategories`)
        .then((r) => r.data),

    renameSubCategory: (categoryId: number, payload: ResourceSubCategoryRenameRequest) =>
      apiClient
        .put<ResourceSubCategoryResponse[]>(`/resources/categories/${categoryId}/subcategories/rename`, payload)
        .then((r) => r.data),

    removeSubCategory: (categoryId: number, name: string) =>
      apiClient
        .delete<ResourceSubCategoryResponse[]>(`/resources/categories/${categoryId}/subcategories`, {
          params: { name },
        })
        .then((r) => r.data),
  },

  items: {
    list: (filters: ResourceItemFilters = {}) =>
      apiClient.get<ResourceItemResponse[]>('/resources/items', { params: filters }).then((r) => r.data),

    exportExcel: () => apiClient.get<Blob>('/resources/items/export', { responseType: 'blob' }).then((r) => r.data),

    create: (payload: ResourceItemRequest) =>
      apiClient.post<ResourceItemResponse>('/resources/items', payload).then((r) => r.data),

    update: (id: number, payload: ResourceItemRequest) =>
      apiClient.put<ResourceItemResponse>(`/resources/items/${id}`, payload).then((r) => r.data),

    remove: (id: number) => apiClient.delete(`/resources/items/${id}`).then(() => undefined),

    toggleImportant: (id: number, important: boolean) =>
      apiClient.put<ResourceItemResponse>(`/resources/items/${id}/important`, { important }).then((r) => r.data),
  },
}
