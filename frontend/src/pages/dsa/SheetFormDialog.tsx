import { useState } from 'react'
import type { SheetRequest, SheetResponse } from '../../api/types'

type SheetFormDialogProps = {
  initial?: SheetResponse | null
  /** Overrides the dialog heading — defaults to "New sheet"/"Rename sheet" based on `initial`. */
  title?: string
  /** Overrides the prefilled name — defaults to `initial?.name`. Lets a caller suggest a name
   * (e.g. derived from an uploaded file) without this reading as a rename of an existing sheet. */
  initialName?: string
  onSave: (payload: SheetRequest) => Promise<void>
  onCancel: () => void
}

export function SheetFormDialog({ initial, title, initialName, onSave, onCancel }: SheetFormDialogProps) {
  const [name, setName] = useState(initialName ?? initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!name.trim()) {
      setError('Sheet name is required.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onSave({ name: name.trim(), description: description.trim() || undefined })
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save sheet.')
      setSaving(false)
    }
  }

  return (
    <div className="modal-overlay" onMouseDown={onCancel}>
      <div className="modal-dialog" onMouseDown={(e) => e.stopPropagation()}>
        <h3>{title ?? (initial ? 'Rename sheet' : 'New sheet')}</h3>
        <form onSubmit={handleSubmit}>
          {error && <div className="banner banner-error mt-1">{error}</div>}
          <div className="field mt-1">
            <label htmlFor="sheet-name">Name</label>
            <input id="sheet-name" type="text" value={name} onChange={(e) => setName(e.target.value)} autoFocus />
          </div>
          <div className="field">
            <label htmlFor="sheet-description">Description (optional)</label>
            <textarea
              id="sheet-description"
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
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
