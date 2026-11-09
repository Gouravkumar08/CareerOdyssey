import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { plannerApi } from '../../api/planner'
import type { DsaGoalItemResponse, PlannerOccurrence, PlannerOverview, SdGoalItemResponse } from '../../api/types'
import { Spinner } from '../../components/Spinner'
import { buildAgenda, isDone } from '../../utils/plannerAgenda'
import { formatProgress, progressPercent } from '../../utils/plannerLabels'
import { PlannerAgendaRow } from './PlannerAgendaRow'

const MAX_NOTES_SHOWN = 5

type DashboardPlannerCardProps = {
  dsaGoalItems: DsaGoalItemResponse[]
  sdGoalItems: SdGoalItemResponse[]
}

/** Everything planned for today — DSA, System Design and generic tasks — plus overdue, progress and notes. */
export function DashboardPlannerCard({ dsaGoalItems, sdGoalItems }: DashboardPlannerCardProps) {
  const [overview, setOverview] = useState<PlannerOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(
    () =>
      plannerApi
        .overview()
        .then((o) => {
          setOverview(o)
          setError(null)
        })
        .catch(() => setError("Couldn't load planner tasks."))
        .finally(() => setLoading(false)),
    [],
  )

  useEffect(() => {
    load()
  }, [load])

  // Only today and overdue are shown here, so the upcoming schedules aren't needed.
  const agenda = useMemo(
    () => (overview ? buildAgenda(overview, dsaGoalItems, sdGoalItems, { dsa: [], sd: [] }) : null),
    [overview, dsaGoalItems, sdGoalItems],
  )

  async function handleToggle(o: PlannerOccurrence) {
    try {
      await plannerApi.tasks.setCompleted(o.taskId, o.date, !o.completed)
      await load()
    } catch {
      setError("Couldn't update that task.")
    }
  }

  const todayDone = agenda?.today.filter(isDone).length ?? 0

  return (
    <div className="card">
      <div className="dashboard-card-header">
        <h3>{agenda ? `Due today · ${todayDone}/${agenda.today.length} done` : 'Due today'}</h3>
        <Link to="/planner">Open planner →</Link>
      </div>

      {loading ? (
        <Spinner size="sm" />
      ) : (
        <>
          {error && <p className="empty-state">{error}</p>}
          {overview && agenda && (
            <>
              {agenda.today.length === 0 && agenda.overdue.length === 0 ? (
                <p className="empty-state">Nothing planned for today.</p>
              ) : (
                <>
                  {agenda.today.map((e) => (
                    <PlannerAgendaRow key={e.key} entry={e} onToggle={handleToggle} />
                  ))}
                  {agenda.overdue.length > 0 && (
                    <>
                      <div className="dashboard-card-subheader">Overdue</div>
                      {agenda.overdue.map((e) => (
                        <PlannerAgendaRow key={e.key} entry={e} showDate onToggle={handleToggle} />
                      ))}
                    </>
                  )}
                </>
              )}

              {overview.progress.length > 0 && (
                <>
                  <div className="dashboard-card-subheader">In progress</div>
                  {overview.progress.map((task) => (
                    <div className="planner-progress-row" key={task.id}>
                      <div className="planner-progress-head">
                        <span className="planner-task-title">{task.title}</span>
                        <span className="planner-progress-figure">{formatProgress(task)}</span>
                      </div>
                      <div className="progress-bar-track">
                        <div className="progress-bar-fill" style={{ width: `${progressPercent(task)}%` }} />
                      </div>
                    </div>
                  ))}
                </>
              )}

              {overview.notes.length > 0 && (
                <>
                  <div className="dashboard-card-subheader">Notes</div>
                  <ul className="planner-dashboard-notes">
                    {overview.notes.slice(0, MAX_NOTES_SHOWN).map((n) => (
                      <li key={n.id}>{n.content}</li>
                    ))}
                  </ul>
                  {overview.notes.length > MAX_NOTES_SHOWN && (
                    <Link to="/planner">+{overview.notes.length - MAX_NOTES_SHOWN} more</Link>
                  )}
                </>
              )}
            </>
          )}
        </>
      )}
    </div>
  )
}
