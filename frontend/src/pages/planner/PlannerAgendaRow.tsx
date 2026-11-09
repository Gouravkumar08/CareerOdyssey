import { Link } from 'react-router-dom'
import type { PlannerOccurrence } from '../../api/types'
import { CheckCircleIcon, ClockIcon } from '../../components/icons/NavIcons'
import { formatShortDate } from '../../utils/plannerLabels'
import type { AgendaEntry } from '../../utils/plannerAgenda'
import { PlannerTaskRow } from './PlannerTaskRow'

type PlannerAgendaRowProps = {
  entry: AgendaEntry
  showDate?: boolean
  onToggle: (occurrence: PlannerOccurrence) => void
}

/** A generic task is ticked off here; a DSA / System Design target completes through its tracker. */
export function PlannerAgendaRow({ entry, showDate = false, onToggle }: PlannerAgendaRowProps) {
  if (entry.kind === 'task') {
    return <PlannerTaskRow occurrence={entry.occurrence} showDate={showDate} onToggle={onToggle} />
  }
  return (
    <Link className={`planner-task-row planner-goal-row ${entry.done ? 'is-done' : ''}`} to={entry.to}>
      <span className={`planner-goal-status ${entry.done ? 'done' : 'pending'}`} title={entry.done ? 'Done' : 'Pending'}>
        {entry.done ? <CheckCircleIcon /> : <ClockIcon />}
      </span>
      <span className="planner-task-title">{entry.label}</span>
      <span className="planner-task-date">{entry.detail}</span>
      <span className={`badge badge-topic dashboard-goal-track dashboard-goal-track-${entry.track === 'DSA' ? 'dsa' : 'sd'}`}>
        {entry.track}
      </span>
      {showDate && <span className="planner-task-date">{formatShortDate(entry.date)}</span>}
    </Link>
  )
}
