import { useEffect, useMemo } from 'react'

export type CompletedEntry = { key: string; trackLabel: string; label: string }

type Props = { entries: CompletedEntry[]; onDismiss: () => void }

const CELEBRATION_MESSAGES = [
  'Target smashed! 🎯',
  'Another day, another win.',
  'Consistency is paying off.',
  'You showed up today — that\'s what matters.',
  'Small steps, big progress.',
  'That\'s the way to do it!',
  'Momentum: officially yours.',
  'Future you says thanks.',
]

export function GoalCelebrationModal({ entries, onDismiss }: Props) {
  const message = useMemo(
    () => CELEBRATION_MESSAGES[Math.floor(Math.random() * CELEBRATION_MESSAGES.length)],
    [],
  )

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
        className="modal-dialog goal-celebration-dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="goal-celebration-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <h3 id="goal-celebration-title">Target{entries.length > 1 ? 's' : ''} hit!</h3>
        <ul className="goal-popup-list">
          {entries.map((entry) => (
            <li className="goal-popup-row" key={entry.key}>
              <span className="goal-popup-track">{entry.trackLabel}</span> · {entry.label}
            </li>
          ))}
        </ul>
        <p className="goal-celebration-message">{message}</p>
        <div className="modal-actions">
          <button type="button" className="btn btn-primary" onClick={onDismiss}>
            Nice!
          </button>
        </div>
      </div>
    </div>
  )
}
