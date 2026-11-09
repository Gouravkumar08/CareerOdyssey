import { useEffect, useMemo, useState } from 'react'
import { dsaApi } from '../../api/dsa'
import { plannerApi } from '../../api/planner'
import { systemDesignApi } from '../../api/systemDesign'
import type { DsaGoalItemResponse, PlannerOccurrence, SdGoalItemResponse } from '../../api/types'
import { Spinner } from '../../components/Spinner'
import { AlertCircleIcon, CheckCircleIcon, ClockIcon } from '../../components/icons/NavIcons'

const DOW = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

const dayDetailFormatter = new Intl.DateTimeFormat('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })

type DayItem = { id: number; title: string }

type TopicDayBreakdown = { topicId: number; topicName: string; plannedCount: number; actualCount: number }

type MergedDay = {
  date: string
  dsaCount: number
  dsaItems: DayItem[]
  sdTopicsCount: number
  sdItems: DayItem[]
  plannedDsa: number
  actualDsaPlanned: number
  plannedSd: number
  actualSdPlanned: number
  dsaTopicBreakdown: TopicDayBreakdown[]
  sdTopicBreakdown: TopicDayBreakdown[]
  tasks: PlannerOccurrence[]
}

function emptyDay(date: string): MergedDay {
  return {
    date,
    dsaCount: 0,
    dsaItems: [],
    sdTopicsCount: 0,
    sdItems: [],
    plannedDsa: 0,
    actualDsaPlanned: 0,
    plannedSd: 0,
    actualSdPlanned: 0,
    dsaTopicBreakdown: [],
    sdTopicBreakdown: [],
    tasks: [],
  }
}

function inAnyGoalRange(dateStr: string, items: Array<{ startDate: string; endDate: string }>) {
  return items.some((item) => dateStr >= item.startDate && dateStr <= item.endDate)
}

type PlannerCalendarProps = {
  dsaGoalItems: DsaGoalItemResponse[]
  sdGoalItems: SdGoalItemResponse[]
  onError: (message: string) => void
}

export function PlannerCalendar({ dsaGoalItems, sdGoalItems, onError }: PlannerCalendarProps) {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1) // 1-12

  const [days, setDays] = useState<MergedDay[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedDay, setSelectedDay] = useState<MergedDay | null>(null)

  function loadCalendar() {
    setLoading(true)
    Promise.all([
      dsaApi.planner.calendar(year, month),
      systemDesignApi.planner.calendar(year, month),
      dsaApi.planner.goalItemsSchedule(year, month).catch(() => []),
      systemDesignApi.planner.goalItemsSchedule(year, month).catch(() => []),
      plannerApi.calendar(year, month).catch(() => []),
    ])
      .then(([dsaDays, sdDays, dsaGoalDays, sdGoalDays, taskDays]) => {
        const map = new Map<string, MergedDay>()
        function ensure(date: string) {
          if (!map.has(date)) {
            map.set(date, emptyDay(date))
          }
          return map.get(date)!
        }
        dsaDays.forEach((d) => {
          const entry = ensure(d.date)
          entry.dsaCount = d.solvedCount
          entry.dsaItems = d.questions
        })
        sdDays.forEach((d) => {
          const entry = ensure(d.date)
          entry.sdTopicsCount = d.topicsCount
          entry.sdItems = d.items
        })
        dsaGoalDays.forEach((d) => {
          const entry = ensure(d.date)
          entry.plannedDsa = d.plannedCount
          entry.actualDsaPlanned = d.actualCount
          entry.dsaTopicBreakdown = d.topics
        })
        sdGoalDays.forEach((d) => {
          const entry = ensure(d.date)
          entry.plannedSd = d.plannedCount
          entry.actualSdPlanned = d.actualCount
          entry.sdTopicBreakdown = d.topics
        })
        taskDays.forEach((d) => {
          ensure(d.date).tasks = d.tasks
        })
        setDays(Array.from(map.values()))
      })
      .catch((err) => onError(err?.response?.data?.message ?? 'Failed to load calendar'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadCalendar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [year, month])

  const daysByDate = useMemo(() => {
    const map = new Map<string, MergedDay>()
    days.forEach((d) => map.set(d.date, d))
    return map
  }, [days])

  const cells = useMemo(() => {
    const firstOfMonth = new Date(year, month - 1, 1)
    const startWeekday = firstOfMonth.getDay()
    const daysInMonth = new Date(year, month, 0).getDate()
    const result: Array<{ day: number; date: string } | null> = []
    for (let i = 0; i < startWeekday; i++) result.push(null)
    for (let d = 1; d <= daysInMonth; d++) {
      const date = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
      result.push({ day: d, date })
    }
    return result
  }, [year, month])

  function goToPrevMonth() {
    if (month === 1) {
      setMonth(12)
      setYear((y) => y - 1)
    } else {
      setMonth((m) => m - 1)
    }
  }

  function goToNextMonth() {
    if (month === 12) {
      setMonth(1)
      setYear((y) => y + 1)
    } else {
      setMonth((m) => m + 1)
    }
  }

  const todayStr = useMemo(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }, [])

  /** 'missing' = past day, no entry at all. 'pending' = today, a target is set and not yet hit.
   * 'done' = target met (or, with no target set, any activity happened) that day. Future days
   * get no verdict yet — they haven't had the chance to be missed.
   * Whether a day carries a target is now derived from the goal-items schedule aggregate
   * (plannedDsa/plannedSd), since the old manual daily-target feature has been removed. */
  function dayStatus(dateStr: string, activity: MergedDay | undefined): 'missing' | 'pending' | 'done' | null {
    if (dateStr > todayStr) return null

    const dsaGoalCount = activity?.plannedDsa ?? 0
    const sdGoalCount = activity?.plannedSd ?? 0
    const dsaActualForGoal = activity?.actualDsaPlanned ?? 0
    const sdActualForGoal = activity?.actualSdPlanned ?? 0
    const dsaCount = activity?.dsaCount ?? 0
    const sdCount = activity?.sdTopicsCount ?? 0
    const hasAnyGoal = dsaGoalCount > 0 || sdGoalCount > 0
    const meetsGoal =
      (dsaGoalCount === 0 || dsaActualForGoal >= dsaGoalCount) && (sdGoalCount === 0 || sdActualForGoal >= sdGoalCount)
    const hasActivity = dsaCount > 0 || sdCount > 0

    if (dateStr === todayStr) {
      if (hasAnyGoal) return meetsGoal ? 'done' : 'pending'
      return hasActivity ? 'done' : null
    }

    if (hasAnyGoal) {
      return meetsGoal ? 'done' : hasActivity ? null : 'missing'
    }
    return hasActivity ? 'done' : 'missing'
  }

  return (
    <>
      <div className="card">
        <div className="calendar-header">
          <button className="btn btn-secondary btn-sm" type="button" onClick={goToPrevMonth}>
            ← Prev
          </button>
          <strong>
            {MONTH_NAMES[month - 1]} {year}
          </strong>
          <button className="btn btn-secondary btn-sm" type="button" onClick={goToNextMonth}>
            Next →
          </button>
        </div>

        {loading && days.length === 0 ? (
          <Spinner label="Loading calendar…" />
        ) : (
          <div className={`calendar-grid content-fade ${loading ? 'is-loading' : ''}`}>
            {DOW.map((dow) => (
              <div className="calendar-dow" key={dow}>
                {dow}
              </div>
            ))}
            {cells.map((cell, i) => {
              if (!cell) return <div className="calendar-cell empty" key={`empty-${i}`} />
              const activity = daysByDate.get(cell.date)
              const hasActivity = !!activity && (activity.dsaCount > 0 || activity.sdTopicsCount > 0)
              const dayTasks = activity?.tasks ?? []
              const plannedDsa = activity?.plannedDsa ?? 0
              const plannedSd = activity?.plannedSd ?? 0
              const status = dayStatus(cell.date, activity)
              const inDsaGoal = inAnyGoalRange(cell.date, dsaGoalItems)
              const inSdGoal = inAnyGoalRange(cell.date, sdGoalItems)
              const hasGoalPlan = inDsaGoal || inSdGoal
              return (
                <div
                  className={`calendar-cell ${hasActivity ? 'has-activity' : ''} ${dayTasks.length > 0 || hasGoalPlan ? 'has-tasks' : ''} ${cell.date === todayStr ? 'today' : ''}`}
                  style={{ animationDelay: `${Math.min(i * 12, 300)}ms` }}
                  key={cell.date}
                  onClick={() =>
                    (hasActivity || hasGoalPlan || dayTasks.length > 0) && setSelectedDay(activity ?? emptyDay(cell.date))
                  }
                >
                  <div className="cell-day-row">
                    <span className="cell-day">{cell.day}</span>
                    {hasGoalPlan && <span className="goal-indicator" title="Within an active goal window" />}
                    {status === 'missing' && (
                      <span className="cell-status-icon missing" title="Missing — no entry">
                        <AlertCircleIcon />
                      </span>
                    )}
                    {status === 'pending' && (
                      <span className="cell-status-icon pending" title="Pending — today's target not met yet">
                        <ClockIcon />
                      </span>
                    )}
                    {status === 'done' && (
                      <span className="cell-status-icon done" title="Done">
                        <CheckCircleIcon />
                      </span>
                    )}
                  </div>
                  {(hasActivity || dayTasks.length > 0 || plannedDsa > 0 || plannedSd > 0) && (
                    <div className="cell-count-list">
                      {plannedDsa > 0 ? (
                        <span className={`cell-count ${activity!.actualDsaPlanned >= plannedDsa ? '' : 'cell-count-pending'}`}>
                          DSA - {activity!.actualDsaPlanned}/{plannedDsa}
                        </span>
                      ) : (
                        activity!.dsaCount > 0 && <span className="cell-count">DSA Question - {activity!.dsaCount}</span>
                      )}
                      {plannedSd > 0 ? (
                        <span className={`cell-count ${activity!.actualSdPlanned >= plannedSd ? '' : 'cell-count-pending'}`}>
                          System Design - {activity!.actualSdPlanned}/{plannedSd}
                        </span>
                      ) : (
                        activity!.sdTopicsCount > 0 && (
                          <span className="cell-count">System Design - {activity!.sdTopicsCount}</span>
                        )
                      )}
                      {dayTasks.length > 0 && (
                        <span className="cell-count cell-count-task">
                          Tasks - {dayTasks.filter((t) => t.completed).length}/{dayTasks.length}
                        </span>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>

      {selectedDay && (
        <div className="modal-overlay" onMouseDown={() => setSelectedDay(null)}>
          <div className="modal-dialog day-detail-dialog" onMouseDown={(e) => e.stopPropagation()}>
            <h3>{dayDetailFormatter.format(new Date(`${selectedDay.date}T00:00:00`))}</h3>

            {(selectedDay.dsaCount > 0 || inAnyGoalRange(selectedDay.date, dsaGoalItems)) && (
              <div className="day-detail-section day-detail-section-dsa">
                <div className="day-detail-section-title">DSA</div>
                {selectedDay.dsaCount > 0 && (
                  <>
                    <p className="day-detail-subline">{selectedDay.dsaCount} question(s) solved:</p>
                    <ul>
                      {selectedDay.dsaItems.map((item) => (
                        <li key={`dsa-${item.id}`}>{item.title}</li>
                      ))}
                    </ul>
                  </>
                )}
                {inAnyGoalRange(selectedDay.date, dsaGoalItems) && (
                  <>
                    <p className="goal-day-stats">
                      Planned: {selectedDay.plannedDsa} · Done: {selectedDay.actualDsaPlanned} · Pending:{' '}
                      {Math.max(0, selectedDay.plannedDsa - selectedDay.actualDsaPlanned)}
                    </p>
                    {selectedDay.dsaTopicBreakdown.length > 1 && (
                      <ul className="goal-day-topic-breakdown">
                        {selectedDay.dsaTopicBreakdown.map((t) => (
                          <li key={t.topicId}>
                            {t.topicName} — Planned: {t.plannedCount} · Done: {t.actualCount} · Pending:{' '}
                            {Math.max(0, t.plannedCount - t.actualCount)}
                          </li>
                        ))}
                      </ul>
                    )}
                  </>
                )}
              </div>
            )}

            {(selectedDay.sdTopicsCount > 0 || inAnyGoalRange(selectedDay.date, sdGoalItems)) && (
              <div className="day-detail-section day-detail-section-sd">
                <div className="day-detail-section-title">System Design</div>
                {selectedDay.sdTopicsCount > 0 && (
                  <>
                    <p className="day-detail-subline">{selectedDay.sdTopicsCount} topic(s) covered:</p>
                    <ul>
                      {selectedDay.sdItems.map((item) => (
                        <li key={`sd-${item.id}`}>{item.title}</li>
                      ))}
                    </ul>
                  </>
                )}
                {inAnyGoalRange(selectedDay.date, sdGoalItems) && (
                  <>
                    <p className="goal-day-stats">
                      Planned: {selectedDay.plannedSd} · Done: {selectedDay.actualSdPlanned} · Pending:{' '}
                      {Math.max(0, selectedDay.plannedSd - selectedDay.actualSdPlanned)}
                    </p>
                    {selectedDay.sdTopicBreakdown.length > 1 && (
                      <ul className="goal-day-topic-breakdown">
                        {selectedDay.sdTopicBreakdown.map((t) => (
                          <li key={t.topicId}>
                            {t.topicName} — Planned: {t.plannedCount} · Done: {t.actualCount} · Pending:{' '}
                            {Math.max(0, t.plannedCount - t.actualCount)}
                          </li>
                        ))}
                      </ul>
                    )}
                  </>
                )}
              </div>
            )}

            {selectedDay.tasks.length > 0 && (
              <div className="day-detail-section day-detail-section-tasks">
                <div className="day-detail-section-title">Tasks</div>
                <ul>
                  {selectedDay.tasks.map((t) => (
                    <li key={t.taskId}>
                      {t.completed ? '✓' : '○'} {t.title}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <div className="modal-actions">
              <button className="btn btn-secondary" type="button" onClick={() => setSelectedDay(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
