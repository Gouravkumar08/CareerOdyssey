import type { PlannerOccurrence } from '../../api/types'
import { RECURRENCE_LABELS, formatShortDate } from '../../utils/plannerLabels'

type PlannerTaskRowProps = {
  occurrence: PlannerOccurrence
  showDate?: boolean
  onToggle: (occurrence: PlannerOccurrence) => void
}

export function PlannerTaskRow({ occurrence, showDate = false, onToggle }: PlannerTaskRowProps) {
  return (
    <label className={`planner-task-row ${occurrence.completed ? 'is-done' : ''}`}>
      <input type="checkbox" checked={occurrence.completed} onChange={() => onToggle(occurrence)} />
      <span className="planner-task-title">{occurrence.title}</span>
      <span className="badge badge-topic planner-track-task">Task</span>
      {occurrence.priority === 'HIGH' && <span className="badge planner-badge-high">High</span>}
      {occurrence.recurrence !== 'NONE' && (
        <span className="badge badge-type">{RECURRENCE_LABELS[occurrence.recurrence]}</span>
      )}
      {showDate && <span className="planner-task-date">{formatShortDate(occurrence.date)}</span>}
    </label>
  )
}
