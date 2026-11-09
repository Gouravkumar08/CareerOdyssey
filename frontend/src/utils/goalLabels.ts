import type {
  DsaGoalItemResponse,
  DsaGoalTopicTargetResponse,
  SdGoalItemResponse,
  SdGoalTopicStatus,
} from '../api/types'

const DIFFICULTY_LABELS: Record<string, string> = { EASY: 'Easy', MEDIUM: 'Medium', HARD: 'Hard' }

function difficultySuffix(difficulties: string[]): string {
  if (!difficulties || difficulties.length === 0) return ''
  return ` (${difficulties.map((d) => DIFFICULTY_LABELS[d] ?? d).join(', ')})`
}

export function formatDsaTopicTargetLabel(t: DsaGoalTopicTargetResponse): string {
  if (t.targetQuestionCount != null) {
    return `${t.targetQuestionCount} questions from ${t.topicName}${difficultySuffix(t.difficulties)}`
  }
  return `Complete ${t.topicName}${difficultySuffix(t.difficulties)}`
}

export function formatSdTopicLabel(t: SdGoalTopicStatus): string {
  return `Complete ${t.topicName}`
}

export type FlatGoalRow = {
  key: string
  trackLabel: string
  label: string
  /** What the goal covers, without a count — e.g. "Array (Medium, Easy)". */
  scope: string
  sheetName: string | null
  endDate: string
  remainingCount: number
  todayTargetCount: number
  todayCompletedCount: number
  yesterdayMet: boolean | null
  done: boolean
}

export function flattenDsaGoals(items: DsaGoalItemResponse[]): FlatGoalRow[] {
  return items.flatMap((item) =>
    item.topics.map((t) => ({
      key: `dsa-${item.id}-${t.topicId}`,
      trackLabel: 'DSA',
      label: formatDsaTopicTargetLabel(t),
      scope: `${t.topicName}${difficultySuffix(t.difficulties)}`,
      sheetName: item.sheetName,
      endDate: item.endDate,
      remainingCount: t.remainingCount,
      todayTargetCount: t.todayTargetCount,
      todayCompletedCount: t.todayCompletedCount,
      yesterdayMet: t.yesterdayMet,
      done: t.done,
    })),
  )
}

export function flattenSdGoals(items: SdGoalItemResponse[]): FlatGoalRow[] {
  return items.flatMap((item) =>
    item.topics.map((t) => ({
      key: `sd-${item.id}-${t.topicId}`,
      trackLabel: 'System Design',
      label: formatSdTopicLabel(t),
      scope: t.topicName,
      sheetName: item.sheetName,
      endDate: item.endDate,
      remainingCount: t.remainingCount,
      todayTargetCount: t.todayTargetCount,
      todayCompletedCount: t.todayCompletedCount,
      yesterdayMet: t.yesterdayMet,
      done: t.done,
    })),
  )
}
