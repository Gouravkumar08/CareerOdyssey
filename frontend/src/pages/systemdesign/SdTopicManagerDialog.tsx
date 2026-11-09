import { useEffect, useRef, useState } from 'react'
import { systemDesignApi } from '../../api/systemDesign'
import type { SdSubTopicResponse, SdTopicResponse } from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Spinner } from '../../components/Spinner'

type SdTopicManagerDialogProps = {
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

export function SdTopicManagerDialog({ onClose, onChanged }: SdTopicManagerDialogProps) {
  const [topics, setTopics] = useState<SdTopicResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [newTopicName, setNewTopicName] = useState('')
  const [adding, setAdding] = useState(false)

  const [renamingId, setRenamingId] = useState<number | null>(null)
  const [renameDraft, setRenameDraft] = useState('')

  const [confirmingDelete, setConfirmingDelete] = useState<SdTopicResponse | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const changedRef = useRef(false)
  const draggedTopicId = useRef<number | null>(null)
  const [dragOverTopicId, setDragOverTopicId] = useState<number | null>(null)

  const [expandedTopicId, setExpandedTopicId] = useState<number | null>(null)
  const [subTopics, setSubTopics] = useState<SdSubTopicResponse[]>([])
  const [subTopicsLoading, setSubTopicsLoading] = useState(false)
  const [renamingSubTopic, setRenamingSubTopic] = useState<string | null>(null)
  const [subTopicRenameDraft, setSubTopicRenameDraft] = useState('')
  const [confirmingDeleteSubTopic, setConfirmingDeleteSubTopic] = useState<SdSubTopicResponse | null>(null)

  function load() {
    setLoading(true)
    systemDesignApi.topics
      .list()
      .then(setTopics)
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load topics'))
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
    if (!newTopicName.trim()) return
    setAdding(true)
    try {
      await systemDesignApi.topics.create({ name: newTopicName.trim() })
      setNewTopicName('')
      changedRef.current = true
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to add topic')
    } finally {
      setAdding(false)
    }
  }

  function startRename(topic: SdTopicResponse) {
    setRenamingId(topic.id)
    setRenameDraft(topic.name)
  }

  async function saveRename(topic: SdTopicResponse) {
    if (!renameDraft.trim() || renameDraft.trim() === topic.name) {
      setRenamingId(null)
      return
    }
    setBusyId(topic.id)
    try {
      await systemDesignApi.topics.rename(topic.id, { name: renameDraft.trim() })
      setRenamingId(null)
      changedRef.current = true
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to rename topic')
    } finally {
      setBusyId(null)
    }
  }

  function handleTopicDragStart(topic: SdTopicResponse) {
    draggedTopicId.current = topic.id
  }

  function handleTopicDragOver(event: React.DragEvent, topic: SdTopicResponse) {
    event.preventDefault()
    if (draggedTopicId.current != null && draggedTopicId.current !== topic.id) {
      setDragOverTopicId(topic.id)
    }
  }

  async function handleTopicDrop(target: SdTopicResponse) {
    const fromId = draggedTopicId.current
    draggedTopicId.current = null
    setDragOverTopicId(null)
    if (fromId == null || fromId === target.id) return
    const fromIndex = topics.findIndex((t) => t.id === fromId)
    const toIndex = topics.findIndex((t) => t.id === target.id)
    if (fromIndex === -1 || toIndex === -1) return
    const reordered = [...topics]
    const [moved] = reordered.splice(fromIndex, 1)
    reordered.splice(toIndex, 0, moved)
    setTopics(reordered)
    try {
      await systemDesignApi.topics.reorder(reordered.map((t) => t.id))
      changedRef.current = true
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to reorder topics')
      load()
    }
  }

  function toggleExpand(topic: SdTopicResponse) {
    if (expandedTopicId === topic.id) {
      setExpandedTopicId(null)
      return
    }
    setExpandedTopicId(topic.id)
    setRenamingSubTopic(null)
    setSubTopicsLoading(true)
    systemDesignApi.topics
      .listSubTopics(topic.id)
      .then(setSubTopics)
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load sub-topics'))
      .finally(() => setSubTopicsLoading(false))
  }

  function startSubTopicRename(name: string) {
    setRenamingSubTopic(name)
    setSubTopicRenameDraft(name)
  }

  async function saveSubTopicRename(oldName: string) {
    if (expandedTopicId == null) return
    if (!subTopicRenameDraft.trim() || subTopicRenameDraft.trim() === oldName) {
      setRenamingSubTopic(null)
      return
    }
    try {
      const updated = await systemDesignApi.topics.renameSubTopic(expandedTopicId, {
        oldName,
        newName: subTopicRenameDraft.trim(),
      })
      setSubTopics(updated)
      setRenamingSubTopic(null)
      changedRef.current = true
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to rename sub-topic')
    }
  }

  async function handleDeleteSubTopic() {
    if (expandedTopicId == null || !confirmingDeleteSubTopic) return
    const sub = confirmingDeleteSubTopic
    setConfirmingDeleteSubTopic(null)
    try {
      const updated = await systemDesignApi.topics.removeSubTopic(expandedTopicId, sub.name)
      setSubTopics(updated)
      changedRef.current = true
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to delete sub-topic')
    }
  }

  async function handleDelete() {
    if (!confirmingDelete) return
    const topic = confirmingDelete
    setConfirmingDelete(null)
    setBusyId(topic.id)
    try {
      await systemDesignApi.topics.remove(topic.id)
      changedRef.current = true
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to delete topic')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="modal-overlay" onMouseDown={handleClose}>
      <div className="modal-dialog sd-topic-manager-dialog" onMouseDown={(e) => e.stopPropagation()}>
        <h3>Manage Topics</h3>
        <p className="stat-sub" style={{ marginTop: '-0.5rem', marginBottom: '1rem' }}>
          Topics are shared across every sheet. Drag the handle to reorder; deleting a topic or sub-topic
          removes every item under it, across all sheets.
        </p>

        {error && <div className="banner banner-error">{error}</div>}

        <form onSubmit={handleAdd} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
          <input
            type="text"
            placeholder="New topic name..."
            value={newTopicName}
            onChange={(e) => setNewTopicName(e.target.value)}
            style={{ flex: 1 }}
          />
          <button className="btn btn-primary btn-sm" type="submit" disabled={adding || !newTopicName.trim()}>
            {adding ? 'Adding…' : '+ Add Topic'}
          </button>
        </form>

        {loading ? (
          <Spinner label="Loading topics…" />
        ) : topics.length === 0 ? (
          <p className="empty-state">No topics yet.</p>
        ) : (
          <div className="sd-topic-manager-list">
            {topics.map((topic) => (
              <div key={topic.id}>
                <div
                  className={`sd-topic-manager-row ${dragOverTopicId === topic.id ? 'drag-over' : ''}`}
                  onDragOver={(e) => handleTopicDragOver(e, topic)}
                  onDragLeave={() => setDragOverTopicId((id) => (id === topic.id ? null : id))}
                  onDrop={() => handleTopicDrop(topic)}
                >
                  <span
                    className="sd-drag-handle"
                    draggable
                    onDragStart={() => handleTopicDragStart(topic)}
                    title="Drag to reorder"
                  >
                    <DragHandle />
                  </span>

                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => toggleExpand(topic)}
                    title="Show sub-topics"
                  >
                    {expandedTopicId === topic.id ? '▾' : '▸'}
                  </button>

                  {renamingId === topic.id ? (
                    <input
                      type="text"
                      value={renameDraft}
                      autoFocus
                      onChange={(e) => setRenameDraft(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') saveRename(topic)
                        if (e.key === 'Escape') setRenamingId(null)
                      }}
                      style={{ flex: 1 }}
                    />
                  ) : (
                    <span className="sd-topic-manager-name">{topic.name}</span>
                  )}

                  <span className="stat-sub sd-topic-manager-count">{topic.itemCount} item(s)</span>

                  <div className="sd-topic-manager-row-actions">
                    {renamingId === topic.id ? (
                      <>
                        <button className="btn btn-primary btn-sm" type="button" onClick={() => saveRename(topic)}>
                          Save
                        </button>
                        <button className="btn btn-secondary btn-sm" type="button" onClick={() => setRenamingId(null)}>
                          Cancel
                        </button>
                      </>
                    ) : (
                      <>
                        <button className="btn btn-secondary btn-sm" type="button" onClick={() => startRename(topic)}>
                          Rename
                        </button>
                        <button
                          className="btn btn-danger btn-sm"
                          type="button"
                          disabled={busyId === topic.id}
                          onClick={() => setConfirmingDelete(topic)}
                        >
                          Delete
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {expandedTopicId === topic.id && (
                  <div className="sd-subtopic-manager-list">
                    {subTopicsLoading ? (
                      <Spinner label="Loading sub-topics…" />
                    ) : subTopics.length === 0 ? (
                      <p className="empty-state">No sub-topics on this topic's items yet.</p>
                    ) : (
                      subTopics.map((sub) => (
                        <div className="sd-subtopic-manager-row" key={sub.name}>
                          {renamingSubTopic === sub.name ? (
                            <input
                              type="text"
                              value={subTopicRenameDraft}
                              autoFocus
                              onChange={(e) => setSubTopicRenameDraft(e.target.value)}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter') saveSubTopicRename(sub.name)
                                if (e.key === 'Escape') setRenamingSubTopic(null)
                              }}
                              style={{ flex: 1 }}
                            />
                          ) : (
                            <span className="sd-topic-manager-name">{sub.name}</span>
                          )}
                          <span className="stat-sub sd-topic-manager-count">{sub.itemCount} item(s)</span>
                          <div className="sd-topic-manager-row-actions">
                            {renamingSubTopic === sub.name ? (
                              <>
                                <button
                                  className="btn btn-primary btn-sm"
                                  type="button"
                                  onClick={() => saveSubTopicRename(sub.name)}
                                >
                                  Save
                                </button>
                                <button
                                  className="btn btn-secondary btn-sm"
                                  type="button"
                                  onClick={() => setRenamingSubTopic(null)}
                                >
                                  Cancel
                                </button>
                              </>
                            ) : (
                              <>
                                <button
                                  className="btn btn-secondary btn-sm"
                                  type="button"
                                  onClick={() => startSubTopicRename(sub.name)}
                                >
                                  Rename
                                </button>
                                <button
                                  className="btn btn-danger btn-sm"
                                  type="button"
                                  onClick={() => setConfirmingDeleteSubTopic(sub)}
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
          title="Delete topic"
          message={`Delete "${confirmingDelete.name}" and all ${confirmingDelete.itemCount} item(s) under it, across every sheet? This cannot be undone.`}
          onConfirm={handleDelete}
          onCancel={() => setConfirmingDelete(null)}
        />
      )}

      {confirmingDeleteSubTopic && (
        <ConfirmDialog
          title="Delete sub-topic"
          message={`Delete "${confirmingDeleteSubTopic.name}" and all ${confirmingDeleteSubTopic.itemCount} item(s) under it, across every sheet? This cannot be undone.`}
          onConfirm={handleDeleteSubTopic}
          onCancel={() => setConfirmingDeleteSubTopic(null)}
        />
      )}
    </div>
  )
}
