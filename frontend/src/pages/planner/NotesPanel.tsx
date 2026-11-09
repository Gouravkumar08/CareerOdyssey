import { useState } from 'react'
import { plannerApi } from '../../api/planner'
import type { PlannerNoteResponse } from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'

type NotesPanelProps = {
  notes: PlannerNoteResponse[]
  onChanged: () => Promise<void>
  onError: (message: string) => void
}

export function NotesPanel({ notes, onChanged, onError }: NotesPanelProps) {
  const [draft, setDraft] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editText, setEditText] = useState('')
  const [deleting, setDeleting] = useState<PlannerNoteResponse | null>(null)

  /** Returns whether the action succeeded, so callers only reset their input on success. */
  async function run(action: () => Promise<unknown>, failure: string) {
    try {
      await action()
      await onChanged()
      return true
    } catch (err: any) {
      onError(err?.response?.data?.message ?? failure)
      return false
    }
  }

  async function handleAdd(event: React.FormEvent) {
    event.preventDefault()
    if (!draft.trim()) return
    if (await run(() => plannerApi.notes.create(draft), 'Failed to add note')) setDraft('')
  }

  async function handleSaveEdit(id: number) {
    if (!editText.trim()) return
    if (await run(() => plannerApi.notes.update(id, editText), 'Failed to save note')) setEditingId(null)
  }

  return (
    <div className="card planner-card">
      <h3 className="planner-card-title">Notes &amp; reminders</h3>

      <form className="planner-note-add" onSubmit={handleAdd}>
        <textarea
          rows={2}
          value={draft}
          maxLength={2000}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Remember…"
          aria-label="New note"
        />
        <button className="btn btn-primary btn-sm" type="submit" disabled={!draft.trim()}>
          Add note
        </button>
      </form>

      {notes.length === 0 ? (
        <p className="empty-state">Nothing to remember yet.</p>
      ) : (
        <ul className="planner-note-list">
          {notes.map((note) => (
            <li className="planner-note" key={note.id}>
              {editingId === note.id ? (
                <>
                  <textarea
                    rows={3}
                    value={editText}
                    maxLength={2000}
                    onChange={(e) => setEditText(e.target.value)}
                    aria-label="Edit note"
                  />
                  <div className="goal-form-actions">
                    <button type="button" className="btn btn-primary btn-sm" onClick={() => handleSaveEdit(note.id)}>
                      Save
                    </button>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditingId(null)}>
                      Cancel
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <p className="planner-note-text">{note.content}</p>
                  <div className="planner-note-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => {
                        setEditingId(note.id)
                        setEditText(note.content)
                      }}
                    >
                      Edit
                    </button>
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => setDeleting(note)}>
                      Delete
                    </button>
                  </div>
                </>
              )}
            </li>
          ))}
        </ul>
      )}

      {deleting && (
        <ConfirmDialog
          title="Delete note?"
          message="This note will be permanently removed."
          onCancel={() => setDeleting(null)}
          onConfirm={() => {
            const id = deleting.id
            setDeleting(null)
            run(() => plannerApi.notes.remove(id), 'Failed to delete note')
          }}
        />
      )}
    </div>
  )
}
