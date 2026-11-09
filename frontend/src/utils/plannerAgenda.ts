import type {
  DsaGoalItemDayResponse,
  DsaGoalItemResponse,
  PlannerOccurrence,
  PlannerOverview,
  SdGoalItemDayResponse,
  SdGoalItemResponse,
} from '../api/types'
import { flattenDsaGoals, flattenSdGoals, type FlatGoalRow } from './goalLabels'
import { addDays } from './plannerLabels'

export type GoalTrack = 'DSA' | 'System Design'

/** One planned thing on the agenda: a generic task occurrence or one DSA / System Design goal target. */
export type AgendaEntry =
  | { kind: 'task'; key: string; date: string; occurrence: PlannerOccurrence }
  | {
      kind: 'goal'
      key: string
      date: string
      track: GoalTrack
      to: string
      label: string
      detail: string
      done: boolean
    }

export type Agenda = {
  today: AgendaEntry[]
  overdue: AgendaEntry[]
  upcoming: AgendaEntry[]
}

export type GoalSchedules = {
  dsa: DsaGoalItemDayResponse[]
  sd: SdGoalItemDayResponse[]
}

/** Tomorrow is shown alone when it already has this many entries; otherwise the day after is included too. */
const UPCOMING_BUSY_THRESHOLD = 3

const TRACKS = {
  dsa: { track: 'DSA' as const, to: '/dsa-tracker', verb: 'Solve', noun: 'question' },
  sd: { track: 'System Design' as const, to: '/system-design', verb: 'Study', noun: 'item' },
}

/** e.g. "Solve 3 questions from Array (Medium, Easy)" — the day's share of a goal, not its total. */
function plannedLabel(source: keyof typeof TRACKS, count: number, scope: string) {
  const { verb, noun } = TRACKS[source]
  return `${verb} ${count} ${noun}${count === 1 ? '' : 's'} from ${scope}`
}

function taskEntry(occurrence: PlannerOccurrence): AgendaEntry {
  return { kind: 'task', key: `task-${occurrence.taskId}-${occurrence.date}`, date: occurrence.date, occurrence }
}

function goalEntries(rows: FlatGoalRow[], source: keyof typeof TRACKS, todayStr: string) {
  const { track, to } = TRACKS[source]
  const today: AgendaEntry[] = []
  const overdue: AgendaEntry[] = []
  for (const row of rows) {
    if (row.endDate < todayStr) {
      if (!row.done) {
        overdue.push({
          kind: 'goal', key: row.key, date: row.endDate, track, to, label: row.label,
          detail: `${row.remainingCount} remaining`, done: false,
        })
      }
    } else if (row.todayTargetCount > 0 || row.todayCompletedCount > 0) {
      today.push({
        kind: 'goal', key: row.key, date: todayStr, track, to,
        label: plannedLabel(source, Math.max(row.todayTargetCount, row.todayCompletedCount), row.scope),
        detail: `${row.todayCompletedCount}/${row.todayTargetCount} done · ${row.remainingCount} left in goal`,
        done: row.todayCompletedCount >= row.todayTargetCount,
      })
    }
  }
  return { today, overdue }
}

function scheduleEntries(
  days: Array<{ date: string; topics: Array<{ topicId: number; topicName: string; plannedCount: number }> }>,
  source: keyof typeof TRACKS,
  dates: Set<string>,
): AgendaEntry[] {
  const { track, to } = TRACKS[source]
  return days
    .filter((day) => dates.has(day.date))
    .flatMap((day) =>
      day.topics
        .filter((t) => t.plannedCount > 0)
        .map((t): AgendaEntry => ({
          kind: 'goal', key: `${source}-${day.date}-${t.topicId}`, date: day.date, track, to,
          label: plannedLabel(source, t.plannedCount, t.topicName), detail: 'planned', done: false,
        })),
    )
}

function byDateThenKind(a: AgendaEntry, b: AgendaEntry) {
  return a.date.localeCompare(b.date) || (a.kind === b.kind ? 0 : a.kind === 'goal' ? -1 : 1)
}

/**
 * Merges generic tasks with the existing DSA / System Design goal data into one agenda. Goal logic
 * (targets, progress, schedule) stays in the goal services; this only reshapes their output.
 * Upcoming covers tomorrow, plus the day after only when tomorrow is light.
 */
export function buildAgenda(
  overview: PlannerOverview,
  dsaGoals: DsaGoalItemResponse[],
  sdGoals: SdGoalItemResponse[],
  schedules: GoalSchedules,
): Agenda {
  const todayStr = overview.date
  const dsa = goalEntries(flattenDsaGoals(dsaGoals), 'dsa', todayStr)
  const sd = goalEntries(flattenSdGoals(sdGoals), 'sd', todayStr)

  const tomorrow = addDays(todayStr, 1)
  const nextTwoDays = new Set([tomorrow, addDays(todayStr, 2)])
  const candidates = [
    ...scheduleEntries(schedules.dsa, 'dsa', nextTwoDays),
    ...scheduleEntries(schedules.sd, 'sd', nextTwoDays),
    ...overview.upcoming.filter((o) => nextTwoDays.has(o.date)).map(taskEntry),
  ]
  const tomorrowIsBusy = candidates.filter((e) => e.date === tomorrow).length >= UPCOMING_BUSY_THRESHOLD
  const upcoming = candidates.filter((e) => !tomorrowIsBusy || e.date === tomorrow).sort(byDateThenKind)

  return {
    today: [...dsa.today, ...sd.today, ...overview.today.map(taskEntry)],
    overdue: [...dsa.overdue, ...sd.overdue, ...overview.overdue.map(taskEntry)],
    upcoming,
  }
}

export function isDone(entry: AgendaEntry) {
  return entry.kind === 'task' ? entry.occurrence.completed : entry.done
}
