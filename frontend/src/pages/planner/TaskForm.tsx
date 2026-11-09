import { useState } from 'react'
import type { PlannerTaskRequest, PlannerTaskResponse, TaskPriority, TaskRecurrence, Weekday } from '../../api/types'
import { PRIORITY_LABELS, RECURRENCE_LABELS, WEEKDAYS, todayString } from '../../utils/plannerLabels'

const RECURRENCES: TaskRecurrence[] = ['NONE', 'DAILY', 'WEEKLY', 'MONTHLY']
const PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH']

type TaskFormProps = {
  initial?: PlannerTaskResponse
  submitLabel: string
  onSubmit: (payload: PlannerTaskRequest) => Promise<void>
  onCancel?: () => void
}

export function TaskForm({ initial, submitLabel, onSubmit, onCancel }: TaskFormProps) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [recurrence, setRecurrence] = useState<TaskRecurrence>(initial?.recurrence ?? 'NONE')
  const [weekdays, setWeekdays] = useState<Weekday[]>(initial?.weekdays ?? [])
  const [startDate, setStartDate] = useState(initial?.startDate ?? todayString())
  const [endDate, setEndDate] = useState(initial?.endDate ?? '')
  const [showMore, setShowMore] = useState(initial != null)
  const [priority, setPriority] = useState<TaskPriority>(initial?.priority ?? 'MEDIUM')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [trackProgress, setTrackProgress] = useState(initial?.progressTarget != null)
  const [progressTarget, setProgressTarget] = useState(initial?.progressTarget?.toString() ?? '')
  const [progressCurrent, setProgressCurrent] = useState(initial?.progressCurrent?.toString() ?? '0')
  const [progressUnit, setProgressUnit] = useState(initial?.progressUnit ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function toggleWeekday(day: Weekday) {
    setWeekdays((prev) => (prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]))
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!title.trim()) {
      setError('Give the task a title')
      return
    }
    if (recurrence === 'WEEKLY' && weekdays.length === 0) {
      setError('Pick at least one weekday')
      return
    }
    if (trackProgress && !(Number(progressTarget) > 0)) {
      setError('Set a progress target greater than 0')
      return
    }
    const payload: PlannerTaskRequest = {
      title: title.trim(),
      description: description.trim() || undefined,
      priority,
      recurrence,
      weekdays: recurrence === 'WEEKLY' ? weekdays : undefined,
      startDate,
      endDate: recurrence !== 'NONE' && endDate ? endDate : undefined,
      ...(trackProgress && {
        progressTarget: Number(progressTarget),
        progressCurrent: Number(progressCurrent) || 0,
        progressUnit: progressUnit.trim() || undefined,
      }),
    }
    setBusy(true)
    setError(null)
    try {
      await onSubmit(payload)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save task')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="planner-form" onSubmit={handleSubmit}>
      {error && <div className="banner banner-error">{error}</div>}

      <input
        type="text"
        value={title}
        maxLength={200}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="What needs doing? e.g. Workout"
        aria-label="Task title"
      />

      <div className="planner-form-chips" role="group" aria-label="Repeat">
        {RECURRENCES.map((r) => (
          <button
            key={r}
            type="button"
            className={`btn btn-sm toggle-btn ${recurrence === r ? 'active' : ''}`}
            aria-pressed={recurrence === r}
            onClick={() => setRecurrence(r)}
          >
            {RECURRENCE_LABELS[r]}
          </button>
        ))}
      </div>

      {recurrence === 'WEEKLY' && (
        <div className="planner-form-chips" role="group" aria-label="Weekdays">
          {WEEKDAYS.map((d) => (
            <button
              key={d.value}
              type="button"
              className={`btn btn-sm toggle-btn ${weekdays.includes(d.value) ? 'active' : ''}`}
              aria-pressed={weekdays.includes(d.value)}
              title={d.label}
              onClick={() => toggleWeekday(d.value)}
            >
              {d.short}
            </button>
          ))}
        </div>
      )}

      <div className="planner-form-dates">
        <label>
          <span>{recurrence === 'NONE' ? 'Due date' : 'Starts'}</span>
          <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
        </label>
        {recurrence !== 'NONE' && (
          <label>
            <span>Ends (optional)</span>
            <input type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} />
          </label>
        )}
      </div>

      <button type="button" className="planner-form-more" aria-expanded={showMore} onClick={() => setShowMore((v) => !v)}>
        {showMore ? '− Fewer options' : '+ More options'}
      </button>

      {showMore && (
        <div className="planner-form-more-fields">
          <label>
            <span>Priority</span>
            <select value={priority} onChange={(e) => setPriority(e.target.value as TaskPriority)}>
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>
                  {PRIORITY_LABELS[p]}
                </option>
              ))}
            </select>
          </label>

          <label>
            <span>Notes</span>
            <textarea
              rows={2}
              value={description}
              maxLength={1000}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional details"
            />
          </label>

          <label className="checkbox-row">
            <input type="checkbox" checked={trackProgress} onChange={(e) => setTrackProgress(e.target.checked)} />
            Track progress (e.g. pages read)
          </label>

          {trackProgress && (
            <div className="planner-form-progress">
              <label>
                <span>Current</span>
                <input type="number" min={0} value={progressCurrent} onChange={(e) => setProgressCurrent(e.target.value)} />
              </label>
              <label>
                <span>Target</span>
                <input type="number" min={1} value={progressTarget} onChange={(e) => setProgressTarget(e.target.value)} />
              </label>
              <label>
                <span>Unit</span>
                <input
                  type="text"
                  value={progressUnit}
                  maxLength={30}
                  onChange={(e) => setProgressUnit(e.target.value)}
                  placeholder="pages"
                />
              </label>
            </div>
          )}
        </div>
      )}

      <div className="goal-form-actions">
        <button className="btn btn-primary btn-sm" type="submit" disabled={busy}>
          {busy ? 'Saving…' : submitLabel}
        </button>
        {onCancel && (
          <button type="button" className="btn btn-secondary btn-sm" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}
