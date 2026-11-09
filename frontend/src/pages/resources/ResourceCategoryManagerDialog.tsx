import { useEffect, useRef, useState } from 'react'
import { resourcesApi } from '../../api/resources'
import type { ResourceCategoryResponse, ResourceSubCategoryResponse } from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Spinner } from '../../components/Spinner'

type ResourceCategoryManagerDialogProps = {
  onClose: () => void
  onChanged: () => void
}

function DragHandle() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
      <circle cx="5" cy="3" r="1.3" />
      <circle cx="11" cy="3" r="1.3" />
      <circle cx="5" cy="8" r="1.3" />
      <circle cx="11" cy="8" r="1.3" />
      <circle cx="5" cy="13" r="1.3" />
      <circle cx="11" cy="13" r="1.3" />
    </svg>
  )
}

export function ResourceCategoryManagerDialog({ onClose, onChanged }: ResourceCategoryManagerDialogProps) {
  const [categories, setCategories] = useState<ResourceCategoryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [newCategoryName, setNewCategoryName] = useState('')
  const [adding, setAdding] = useState(false)

  const [renamingId, setRenamingId] = useState<number | null>(null)
  const [renameDraft, setRenameDraft] = useState('')

  const [confirmingDelete, setConfirmingDelete] = useState<ResourceCategoryResponse | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const changedRef = useRef(false)
  const draggedCategoryId = useRef<number | null>(null)
  const [dragOverCategoryId, setDragOverCategoryId] = useState<number | null>(null)

  const [expandedCategoryId, setExpandedCategoryId] = useState<number | null>(null)
  const [subCategories, setSubCategories] = useState<ResourceSubCategoryResponse[]>([])
  const [subCategoriesLoading, setSubCategoriesLoading] = useState(false)
  const [renamingSubCategory, setRenamingSubCategory] = useState<string | null>(null)
  const [subCategoryRenameDraft, setSubCategoryRenameDraft] = useState('')
  const [confirmingDeleteSubCategory, setConfirmingDeleteSubCategory] = useState<ResourceSubCategoryResponse | null>(
    null,
  )

  function load() {
    setLoading(true)
    resourcesApi.categories
      .list()
      .then(setCategories)
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load categories'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  function handleClose() {
    if (changedRef.current) onChanged()
    onClose()
  }

  async function handleAdd(event: React.FormEvent) {
    event.preventDefault()
    if (!newCategoryName.trim()) return
    setAdding(true)
    try {
      await resourcesApi.categories.create({ name: newCategoryName.trim() })
      setNewCategoryName('')
      changedRef.current = true
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to add category')
    } finally {
      setAdding(false)
    }
  }

  function startRename(category: ResourceCategoryResponse) {
    setRenamingId(category.id)
    setRenameDraft(category.name)
  }

  async function saveRename(category: ResourceCategoryResponse) {
    if (!renameDraft.trim() || renameDraft.trim() === category.name) {
      setRenamingId(null)
      return
    }
    setBusyId(category.id)
    try {
      await resourcesApi.categories.rename(category.id, { name: renameDraft.trim() })
      setRenamingId(null)
      changedRef.current = true
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to rename category')
    } finally {
      setBusyId(null)
    }
  }

  function handleCategoryDragStart(category: ResourceCategoryResponse) {
    draggedCategoryId.current = category.id
  }

  function handleCategoryDragOver(event: React.DragEvent, category: ResourceCategoryResponse) {
    event.preventDefault()
    if (draggedCategoryId.current != null && draggedCategoryId.current !== category.id) {
      setDragOverCategoryId(category.id)
    }
  }

  async function handleCategoryDrop(target: ResourceCategoryResponse) {
    const fromId = draggedCategoryId.current
    draggedCategoryId.current = null
    setDragOverCategoryId(null)
    if (fromId == null || fromId === target.id) return
    const fromIndex = categories.findIndex((c) => c.id === fromId)
    const toIndex = categories.findIndex((c) => c.id === target.id)
    if (fromIndex === -1 || toIndex === -1) return
    const reordered = [...categories]
    const [moved] = reordered.splice(fromIndex, 1)
    reordered.splice(toIndex, 0, moved)
    setCategories(reordered)
    try {
      await resourcesApi.categories.reorder(reordered.map((c) => c.id))
      changedRef.current = true
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to reorder categories')
      load()
    }
  }

  function toggleExpand(category: ResourceCategoryResponse) {
    if (expandedCategoryId === category.id) {
      setExpandedCategoryId(null)
      return
    }
    setExpandedCategoryId(category.id)
    setRenamingSubCategory(null)
    setSubCategoriesLoading(true)
    resourcesApi.categories
      .listSubCategories(category.id)
      .then(setSubCategories)
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load sub-categories'))
      .finally(() => setSubCategoriesLoading(false))
  }

  function startSubCategoryRename(name: string) {
    setRenamingSubCategory(name)
    setSubCategoryRenameDraft(name)
  }

  async function saveSubCategoryRename(oldName: string) {
    if (expandedCategoryId == null) return
    if (!subCategoryRenameDraft.trim() || subCategoryRenameDraft.trim() === oldName) {
      setRenamingSubCategory(null)
      return
    }
    try {
      const updated = await resourcesApi.categories.renameSubCategory(expandedCategoryId, {
        oldName,
        newName: subCategoryRenameDraft.trim(),
      })
      setSubCategories(updated)
      setRenamingSubCategory(null)
      changedRef.current = true
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to rename sub-category')
    }
  }

  async function handleDeleteSubCategory() {
    if (expandedCategoryId == null || !confirmingDeleteSubCategory) return
    const sub = confirmingDeleteSubCategory
    setConfirmingDeleteSubCategory(null)
    try {
      const updated = await resourcesApi.categories.removeSubCategory(expandedCategoryId, sub.name)
      setSubCategories(updated)
      changedRef.current = true
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to delete sub-category')
    }
  }

  async function handleDelete() {
    if (!confirmingDelete) return
    const category = confirmingDelete
    setConfirmingDelete(null)
    setBusyId(category.id)
    try {
      await resourcesApi.categories.remove(category.id)
      changedRef.current = true
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to delete category')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="modal-overlay" onMouseDown={handleClose}>
      <div className="modal-dialog sd-topic-manager-dialog" onMouseDown={(e) => e.stopPropagation()}>
        <h3>Manage Categories</h3>
        <p className="stat-sub" style={{ marginTop: '-0.5rem', marginBottom: '1rem' }}>
          Drag the handle to reorder categories; deleting a category or sub-category removes every
          resource under it.
        </p>

        {error && <div className="banner banner-error">{error}</div>}

        <form onSubmit={handleAdd} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
          <input
            type="text"
            placeholder="New category name..."
            value={newCategoryName}
            onChange={(e) => setNewCategoryName(e.target.value)}
            style={{ flex: 1 }}
          />
          <button className="btn btn-primary btn-sm" type="submit" disabled={adding || !newCategoryName.trim()}>
            {adding ? 'Adding…' : '+ Add Category'}
          </button>
        </form>

        {loading ? (
          <Spinner label="Loading categories…" />
        ) : categories.length === 0 ? (
          <p className="empty-state">No categories yet.</p>
        ) : (
          <div className="sd-topic-manager-list">
            {categories.map((category) => (
              <div key={category.id}>
                <div
                  className={`sd-topic-manager-row ${dragOverCategoryId === category.id ? 'drag-over' : ''}`}
                  onDragOver={(e) => handleCategoryDragOver(e, category)}
                  onDragLeave={() => setDragOverCategoryId((id) => (id === category.id ? null : id))}
                  onDrop={() => handleCategoryDrop(category)}
                >
                  <span
                    className="sd-drag-handle"
                    draggable
                    onDragStart={() => handleCategoryDragStart(category)}
                    title="Drag to reorder"
                  >
                    <DragHandle />
                  </span>

                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => toggleExpand(category)}
                    title="Show sub-categories"
                  >
                    {expandedCategoryId === category.id ? '▾' : '▸'}
                  </button>

                  {renamingId === category.id ? (
                    <input
                      type="text"
                      value={renameDraft}
                      autoFocus
                      onChange={(e) => setRenameDraft(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') saveRename(category)
                        if (e.key === 'Escape') setRenamingId(null)
                      }}
                      style={{ flex: 1 }}
                    />
                  ) : (
                    <span className="sd-topic-manager-name">{category.name}</span>
                  )}

                  <span className="stat-sub sd-topic-manager-count">{category.itemCount} item(s)</span>

                  <div className="sd-topic-manager-row-actions">
                    {renamingId === category.id ? (
                      <>
                        <button className="btn btn-primary btn-sm" type="button" onClick={() => saveRename(category)}>
                          Save
                        </button>
                        <button
                          className="btn btn-secondary btn-sm"
                          type="button"
                          onClick={() => setRenamingId(null)}
                        >
                          Cancel
                        </button>
                      </>
                    ) : (
                      <>
                        <button
                          className="btn btn-secondary btn-sm"
                          type="button"
                          onClick={() => startRename(category)}
                        >
                          Rename
                        </button>
                        <button
                          className="btn btn-danger btn-sm"
                          type="button"
                          disabled={busyId === category.id}
                          onClick={() => setConfirmingDelete(category)}
                        >
                          Delete
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {expandedCategoryId === category.id && (
                  <div className="sd-subtopic-manager-list">
                    {subCategoriesLoading ? (
                      <Spinner label="Loading sub-categories…" />
                    ) : subCategories.length === 0 ? (
                      <p className="empty-state">No sub-categories on this category's items yet.</p>
                    ) : (
                      subCategories.map((sub) => (
                        <div className="sd-subtopic-manager-row" key={sub.name}>
                          {renamingSubCategory === sub.name ? (
                            <input
                              type="text"
                              value={subCategoryRenameDraft}
                              autoFocus
                              onChange={(e) => setSubCategoryRenameDraft(e.target.value)}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter') saveSubCategoryRename(sub.name)
                                if (e.key === 'Escape') setRenamingSubCategory(null)
                              }}
                              style={{ flex: 1 }}
                            />
                          ) : (
                            <span className="sd-topic-manager-name">{sub.name}</span>
                          )}
                          <span className="stat-sub sd-topic-manager-count">{sub.itemCount} item(s)</span>
                          <div className="sd-topic-manager-row-actions">
                            {renamingSubCategory === sub.name ? (
                              <>
                                <button
                                  className="btn btn-primary btn-sm"
                                  type="button"
                                  onClick={() => saveSubCategoryRename(sub.name)}
                                >
                                  Save
                                </button>
                                <button
                                  className="btn btn-secondary btn-sm"
                                  type="button"
                                  onClick={() => setRenamingSubCategory(null)}
                                >
                                  Cancel
                                </button>
                              </>
                            ) : (
                              <>
                                <button
                                  className="btn btn-secondary btn-sm"
                                  type="button"
                                  onClick={() => startSubCategoryRename(sub.name)}
                                >
                                  Rename
                                </button>
                                <button
                                  className="btn btn-danger btn-sm"
                                  type="button"
                                  onClick={() => setConfirmingDeleteSubCategory(sub)}
                                >
                                  Delete
                                </button>
                              </>
                            )}
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        <div className="modal-actions">
          <button type="button" className="btn btn-secondary" onClick={handleClose}>
            Close
          </button>
        </div>
      </div>

      {confirmingDelete && (
        <ConfirmDialog
          title="Delete category"
          message={`Delete "${confirmingDelete.name}" and all ${confirmingDelete.itemCount} item(s) under it? This cannot be undone.`}
          onConfirm={handleDelete}
          onCancel={() => setConfirmingDelete(null)}
        />
      )}

      {confirmingDeleteSubCategory && (
        <ConfirmDialog
          title="Delete sub-category"
          message={`Delete "${confirmingDeleteSubCategory.name}" and all ${confirmingDeleteSubCategory.itemCount} item(s) under it? This cannot be undone.`}
          onConfirm={handleDeleteSubCategory}
          onCancel={() => setConfirmingDeleteSubCategory(null)}
        />
      )}
    </div>
  )
}
