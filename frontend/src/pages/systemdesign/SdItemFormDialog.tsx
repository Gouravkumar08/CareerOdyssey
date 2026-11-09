import { useState } from 'react'
import type { SdCategory, SdItemRequest, SdItemResponse } from '../../api/types'

type SdItemFormDialogProps = {
  initial?: SdItemResponse | null
  topicOptions: string[]
  onSave: (payload: SdItemRequest) => Promise<void>
  onCancel: () => void
}

export function SdItemFormDialog({ initial, topicOptions, onSave, onCancel }: SdItemFormDialogProps) {
  const [topicName, setTopicName] = useState(initial?.topicName ?? '')
  const [subTopic, setSubTopic] = useState(initial?.subTopic ?? '')
  const [title, setTitle] = useState(initial?.title ?? '')
  const [category, setCategory] = useState<SdCategory>(initial?.category ?? 'HLD')
  const [important, setImportant] = useState(initial?.important ?? false)
  const [resourceLink, setResourceLink] = useState(initial?.resourceLink ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!topicName.trim() || !title.trim()) {
      setError('Topic and title are required.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onSave({
        topicName: topicName.trim(),
        subTopic: subTopic.trim() || undefined,
        title: title.trim(),
        category,
        important,
        resourceLink: resourceLink.trim() || undefined,
        description: description.trim() || undefined,
      })
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save item.')
      setSaving(false)
    }
  }

  return (
    <div className="modal-overlay" onMouseDown={onCancel}>
      <div className="modal-dialog" style={{ maxWidth: 480 }} onMouseDown={(e) => e.stopPropagation()}>
        <h3>{initial ? 'Edit item' : 'Add item'}</h3>
        <form onSubmit={handleSubmit}>
          {error && <div className="banner banner-error mt-1">{error}</div>}

          <div className="field mt-1">
            <label htmlFor="sd-topic">Topic</label>
            <input
              id="sd-topic"
              type="text"
              list="sd-topic-options"
              value={topicName}
              onChange={(e) => setTopicName(e.target.value)}
              autoFocus
            />
            <datalist id="sd-topic-options">
              {topicOptions.map((t) => (
                <option key={t} value={t} />
              ))}
            </datalist>
          </div>

          <div className="field">
            <label htmlFor="sd-subtopic">Sub-topic (optional)</label>
            <input id="sd-subtopic" type="text" value={subTopic} onChange={(e) => setSubTopic(e.target.value)} />
          </div>

          <div className="field">
            <label htmlFor="sd-title">Title</label>
            <input id="sd-title" type="text" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>

          <div className="field">
            <label htmlFor="sd-category">Category</label>
            <select id="sd-category" value={category} onChange={(e) => setCategory(e.target.value as SdCategory)}>
              <option value="HLD">HLD</option>
              <option value="LLD">LLD</option>
            </select>
          </div>

          <div className="checkbox-row">
            <input
              id="sd-important"
              type="checkbox"
              checked={important}
              onChange={(e) => setImportant(e.target.checked)}
            />
            <label htmlFor="sd-important" style={{ margin: 0 }}>
              Mark as important
            </label>
          </div>

          <div className="field mt-1">
            <label htmlFor="sd-resource">Resource link (optional)</label>
            <input
              id="sd-resource"
              type="url"
              value={resourceLink}
              onChange={(e) => setResourceLink(e.target.value)}
              placeholder="https://..."
            />
          </div>

          <div className="field mt-1">
            <label htmlFor="sd-description">Description (optional)</label>
            <textarea
              id="sd-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What this concept covers..."
              rows={3}
            />
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
