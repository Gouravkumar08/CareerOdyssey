import { useState } from 'react'
import type { ResourceItemRequest, ResourceItemResponse } from '../../api/types'

type ResourceItemFormDialogProps = {
  initial?: ResourceItemResponse | null
  categoryOptions: string[]
  onSave: (payload: ResourceItemRequest) => Promise<void>
  onCancel: () => void
}

export function ResourceItemFormDialog({ initial, categoryOptions, onSave, onCancel }: ResourceItemFormDialogProps) {
  const [categoryName, setCategoryName] = useState(initial?.categoryName ?? '')
  const [subCategory, setSubCategory] = useState(initial?.subCategory ?? '')
  const [name, setName] = useState(initial?.name ?? '')
  const [link, setLink] = useState(initial?.link ?? '')
  const [important, setImportant] = useState(initial?.important ?? false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!categoryName.trim() || !name.trim() || !link.trim()) {
      setError('Category, name and link are required.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onSave({
        categoryName: categoryName.trim(),
        subCategory: subCategory.trim() || undefined,
        name: name.trim(),
        link: link.trim(),
        important,
      })
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save resource.')
      setSaving(false)
    }
  }

  return (
    <div className="modal-overlay" onMouseDown={onCancel}>
      <div className="modal-dialog" style={{ maxWidth: 480 }} onMouseDown={(e) => e.stopPropagation()}>
        <h3>{initial ? 'Edit resource' : 'Add resource'}</h3>
        <form onSubmit={handleSubmit}>
          {error && <div className="banner banner-error mt-1">{error}</div>}

          <div className="field mt-1">
            <label htmlFor="res-category">Category</label>
            <input
              id="res-category"
              type="text"
              list="res-category-options"
              value={categoryName}
              onChange={(e) => setCategoryName(e.target.value)}
              autoFocus
            />
            <datalist id="res-category-options">
              {categoryOptions.map((c) => (
                <option key={c} value={c} />
              ))}
            </datalist>
          </div>

          <div className="field">
            <label htmlFor="res-subcategory">Sub-category (optional)</label>
            <input
              id="res-subcategory"
              type="text"
              value={subCategory}
              onChange={(e) => setSubCategory(e.target.value)}
            />
          </div>

          <div className="field">
            <label htmlFor="res-name">Name</label>
            <input id="res-name" type="text" value={name} onChange={(e) => setName(e.target.value)} />
          </div>

          <div className="field mt-1">
            <label htmlFor="res-link">Link</label>
            <input
              id="res-link"
              type="url"
              value={link}
              onChange={(e) => setLink(e.target.value)}
              placeholder="https://..."
            />
          </div>

          <div className="checkbox-row">
            <input
              id="res-important"
              type="checkbox"
              checked={important}
              onChange={(e) => setImportant(e.target.checked)}
            />
            <label htmlFor="res-important" style={{ margin: 0 }}>
              Mark as important
            </label>
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
