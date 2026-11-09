import type { PlannerTaskResponse, TaskPriority, TaskRecurrence, Weekday } from '../api/types'

export const WEEKDAYS: { value: Weekday; short: string; label: string }[] = [
  { value: 'MONDAY', short: 'Mon', label: 'Monday' },
  { value: 'TUESDAY', short: 'Tue', label: 'Tuesday' },
  { value: 'WEDNESDAY', short: 'Wed', label: 'Wednesday' },
  { value: 'THURSDAY', short: 'Thu', label: 'Thursday' },
  { value: 'FRIDAY', short: 'Fri', label: 'Friday' },
  { value: 'SATURDAY', short: 'Sat', label: 'Saturday' },
  { value: 'SUNDAY', short: 'Sun', label: 'Sunday' },
]

export const RECURRENCE_LABELS: Record<TaskRecurrence, string> = {
  NONE: 'Once',
  DAILY: 'Daily',
  WEEKLY: 'Weekly',
  MONTHLY: 'Monthly',
}

export const PRIORITY_LABELS: Record<TaskPriority, string> = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High' }

const shortDateFormatter = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' })

export function formatShortDate(dateStr: string) {
  return shortDateFormatter.format(new Date(`${dateStr}T00:00:00`))
}

export function todayString() {
  return new Date().toLocaleDateString('en-CA')
}

export function progressPercent(task: Pick<PlannerTaskResponse, 'progressCurrent' | 'progressTarget'>) {
  if (!task.progressTarget) return 0
  return Math.min(100, Math.round(((task.progressCurrent ?? 0) / task.progressTarget) * 1000) / 10)
}

export function formatProgress(task: Pick<PlannerTaskResponse, 'progressCurrent' | 'progressTarget' | 'progressUnit'>) {
  return `${task.progressCurrent ?? 0} / ${task.progressTarget}${task.progressUnit ? ` ${task.progressUnit}` : ''}`
}

export function describeSchedule(task: PlannerTaskResponse): string {
  if (task.recurrence === 'NONE') return `Due ${formatShortDate(task.startDate)}`
  const days = WEEKDAYS.filter((d) => task.weekdays.includes(d.value)).map((d) => d.short).join(', ')
  const cadence =
    task.recurrence === 'DAILY'
      ? 'Every day'
      : task.recurrence === 'WEEKLY'
        ? `Every ${days}`
        : `Monthly on day ${Number(task.startDate.slice(8, 10))}`
  const range = task.endDate
    ? `${formatShortDate(task.startDate)} – ${formatShortDate(task.endDate)}`
    : `from ${formatShortDate(task.startDate)}`
  return `${cadence} · ${range}`
}

export function addDays(dateStr: string, days: number) {
  const d = new Date(`${dateStr}T00:00:00`)
  d.setDate(d.getDate() + days)
  return d.toLocaleDateString('en-CA')
}
