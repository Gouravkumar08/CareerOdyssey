import { useEffect } from 'react'

export type PendingEntry = {
  key: string // unique, e.g. `dsa-${item.id}` or `sd-${item.id}`
  trackLabel: string // "DSA" or "System Design"
  label: string // human description, e.g. "Arrays (15 questions)" or "Databases, Caching (topics)"
  remaining: number
  yesterdayMet: boolean | null
}

type Props = {
  entries: PendingEntry[] // one row per pending goal item, across both tracks, rendered together
  onDismiss: () => void
}

export function GoalReminderModal({ entries, onDismiss }: Props) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onDismiss()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onDismiss])

  return (
    <div className="modal-overlay" onMouseDown={onDismiss}>
      <div
        className="modal-dialog goal-popup-dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="goal-reminder-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <h3 id="goal-reminder-title">Today's goals</h3>
        <ul className="goal-popup-list">
          {entries.map((entry) => (
            <li className="goal-popup-row" key={entry.key}>
              <div>
                <span className="goal-popup-track">{entry.trackLabel}</span> · {entry.label} — {entry.remaining} left
                today
              </div>
              {entry.yesterdayMet === false && <div className="goal-popup-missed">missed yesterday's target</div>}
            </li>
          ))}
        </ul>
        <div className="modal-actions">
          <button type="button" className="btn btn-primary" onClick={onDismiss}>
            Got it
          </button>
        </div>
      </div>
    </div>
  )
}
