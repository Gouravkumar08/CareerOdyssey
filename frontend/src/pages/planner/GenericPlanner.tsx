import { useCallback, useEffect, useMemo, useState } from 'react'
import { plannerApi } from '../../api/planner'
import type {
  DsaGoalItemResponse,
  PlannerOccurrence,
  PlannerOverview,
  PlannerTaskRequest,
  PlannerTaskResponse,
  SdGoalItemResponse,
} from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Spinner } from '../../components/Spinner'
import { useGoalSchedules } from '../../hooks/useGoalSchedules'
import { buildAgenda, isDone } from '../../utils/plannerAgenda'
import { describeSchedule, formatProgress, progressPercent } from '../../utils/plannerLabels'
import { NotesPanel } from './NotesPanel'
import { PlannerAgendaRow } from './PlannerAgendaRow'
import { TaskForm } from './TaskForm'

function ProgressRow({ task, onSave }: { task: PlannerTaskResponse; onSave: (current: number) => Promise<void> }) {
  const [value, setValue] = useState(String(task.progressCurrent ?? 0))
  const [saving, setSaving] = useState(false)

  async function handleSave(event: React.FormEvent) {
    event.preventDefault()
    setSaving(true)
    try {
      await onSave(Number(value) || 0)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="planner-progress-row">
      <div className="planner-progress-head">
        <span className="planner-task-title">{task.title}</span>
        <span className="planner-progress-figure">
          {formatProgress(task)} · {progressPercent(task)}%
        </span>
      </div>
      <div className="progress-bar-track">
        <div className="progress-bar-fill" style={{ width: `${progressPercent(task)}%` }} />
      </div>
      <form className="planner-progress-update" onSubmit={handleSave}>
        <input
          type="number"
          min={0}
          max={task.progressTarget ?? undefined}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          aria-label={`Current progress for ${task.title}`}
        />
        <button className="btn btn-secondary btn-sm" type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Update'}
        </button>
      </form>
    </div>
  )
}

type GenericPlannerProps = {
  dsaGoalItems: DsaGoalItemResponse[]
  sdGoalItems: SdGoalItemResponse[]
}

export function GenericPlanner({ dsaGoalItems, sdGoalItems }: GenericPlannerProps) {
  const [overview, setOverview] = useState<PlannerOverview | null>(null)
  const [tasks, setTasks] = useState<PlannerTaskResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [addFormKey, setAddFormKey] = useState(0)
  const [editing, setEditing] = useState<PlannerTaskResponse | null>(null)
  const [deleting, setDeleting] = useState<PlannerTaskResponse | null>(null)

  const reload = useCallback(
    () =>
      Promise.all([plannerApi.overview(), plannerApi.tasks.list()])
        .then(([o, t]) => {
          setOverview(o)
          setTasks(t)
          setError(null)
        })
        .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load the planner'))
        .finally(() => setLoading(false)),
    [],
  )

  useEffect(() => {
    reload()
  }, [reload])


  async function guarded(action: () => Promise<unknown>, failure: string) {
    try {
      await action()
      await reload()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? failure)
    }
  }

  function handleToggle(o: PlannerOccurrence) {
    return guarded(() => plannerApi.tasks.setCompleted(o.taskId, o.date, !o.completed), 'Failed to update task')
  }

  async function handleCreate(payload: PlannerTaskRequest) {
    await plannerApi.tasks.create(payload)
    setAddFormKey((k) => k + 1)
    await reload()
  }

  async function handleEdit(payload: PlannerTaskRequest) {
    await plannerApi.tasks.update(editing!.id, payload)
    setEditing(null)
    await reload()
  }

  async function handleProgress(task: PlannerTaskResponse, current: number) {
    await guarded(() => plannerApi.tasks.updateProgress(task.id, current), 'Failed to update progress')
  }

  const schedules = useGoalSchedules(dsaGoalItems, sdGoalItems)
  const agenda = useMemo(
    () => (overview ? buildAgenda(overview, dsaGoalItems, sdGoalItems, schedules) : null),
    [overview, dsaGoalItems, sdGoalItems, schedules],
  )
  const todayDone = agenda?.today.filter(isDone).length ?? 0

  return (
    <section className="planner-generic">
      <h2 className="section-heading">Planner</h2>

      {error && <div className="banner banner-error">{error}</div>}

      {loading ? (
        <Spinner label="Loading planner…" />
      ) : (
        overview &&
        agenda && (
          <>
            <div className="page-stats">
              <div className="stat-card">
                <div className="stat-label">Today</div>
                <div className="stat-value">
                  {todayDone}/{agenda.today.length}
                </div>
                <div className="stat-sub">
                  {agenda.today.length === 0 ? 'nothing planned' : `${agenda.today.length - todayDone} remaining`}
                </div>
              </div>
              <div className={`stat-card ${agenda.overdue.length > 0 ? 'accent-medium' : ''}`}>
                <div className="stat-label">Overdue</div>
                <div className="stat-value">{agenda.overdue.length}</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">Recurring today</div>
                <div className="stat-value">{overview.today.filter((o) => o.recurrence !== 'NONE').length}</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">Upcoming</div>
                <div className="stat-value">{agenda.upcoming.length}</div>
              </div>
            </div>

            <div className="planner-workspace">
              <div className="planner-workspace-main">
                <div className="card planner-card">
                  <h3 className="planner-card-title">Today</h3>
                  {agenda.today.length === 0 ? (
                    <p className="empty-state">Nothing planned for today.</p>
                  ) : (
                    agenda.today.map((e) => <PlannerAgendaRow key={e.key} entry={e} onToggle={handleToggle} />)
                  )}
                </div>

                {agenda.overdue.length > 0 && (
                  <div className="card planner-card planner-card-overdue">
                    <h3 className="planner-card-title">Overdue</h3>
                    {agenda.overdue.map((e) => (
                      <PlannerAgendaRow key={e.key} entry={e} showDate onToggle={handleToggle} />
                    ))}
                  </div>
                )}

                <div className="card planner-card">
                  <h3 className="planner-card-title">Upcoming</h3>
                  {agenda.upcoming.length === 0 ? (
                    <p className="empty-state">Nothing planned for tomorrow.</p>
                  ) : (
                    agenda.upcoming.map((e) => <PlannerAgendaRow key={e.key} entry={e} showDate onToggle={handleToggle} />)
                  )}
                </div>

                {overview.progress.length > 0 && (
                  <div className="card planner-card">
                    <h3 className="planner-card-title">In progress</h3>
                    {overview.progress.map((task) => (
                      <ProgressRow key={task.id} task={task} onSave={(current) => handleProgress(task, current)} />
                    ))}
                  </div>
                )}

                <div className="card planner-card">
                  <h3 className="planner-card-title">All tasks</h3>
                  {tasks.length === 0 ? (
                    <p className="empty-state">No tasks yet — add your first one.</p>
                  ) : (
                    tasks.map((task) => (
                      <div className="planner-definition-row" key={task.id}>
                        <div className="planner-definition-main">
                          <div className="planner-task-title">{task.title}</div>
                          <div className="planner-definition-sub">
                            {describeSchedule(task)}
                            {task.progressTarget != null && ` · ${formatProgress(task)}`}
                          </div>
                        </div>
                        <div className="planner-note-actions">
                          <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditing(task)}>
                            Edit
                          </button>
                          <button type="button" className="btn btn-danger btn-sm" onClick={() => setDeleting(task)}>
                            Delete
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <aside className="planner-workspace-side">
                <div className="card planner-card">
                  <h3 className="planner-card-title">Add task</h3>
                  <TaskForm key={addFormKey} submitLabel="Add task" onSubmit={handleCreate} />
                </div>

                <NotesPanel notes={overview.notes} onChanged={reload} onError={setError} />
              </aside>
            </div>
          </>
        )
      )}

      {editing && (
        <div className="modal-overlay" onMouseDown={() => setEditing(null)}>
          <div className="modal-dialog planner-edit-dialog" onMouseDown={(e) => e.stopPropagation()}>
            <h3>Edit task</h3>
            <TaskForm initial={editing} submitLabel="Save changes" onSubmit={handleEdit} onCancel={() => setEditing(null)} />
          </div>
        </div>
      )}

      {deleting && (
        <ConfirmDialog
          title="Delete task?"
          message={`"${deleting.title}" and its completion history will be permanently removed.`}
          onCancel={() => setDeleting(null)}
          onConfirm={() => {
            const id = deleting.id
            setDeleting(null)
            guarded(() => plannerApi.tasks.remove(id), 'Failed to delete task')
          }}
        />
      )}
    </section>
  )
}
