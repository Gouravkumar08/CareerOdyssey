import { useEffect, useRef, useState } from 'react'
import type { CompletedEntry } from '../components/GoalCelebrationModal'
import type { PendingEntry } from '../components/GoalReminderModal'
import type { FlatGoalRow } from '../utils/goalLabels'

function todayDateKey() {
  return new Date().toLocaleDateString('en-CA')
}

/** Takes the flattened per-topic-target rows (see utils/goalLabels.ts flattenDsaGoals/flattenSdGoals),
 * combined across both tracks, and derives the reminder/celebration popup state — one entry per
 * topic-target rather than per goal item. Pass `null` while the rows haven't loaded yet. */
export function useGoalItemPopups(rows: FlatGoalRow[] | null) {
  const [pendingEntries, setPendingEntries] = useState<PendingEntry[]>([])
  const [showReminder, setShowReminder] = useState(false)
  const hasShownReminder = useRef(false)

  const [completedEntries, setCompletedEntries] = useState<CompletedEntry[]>([])
  const [showCelebration, setShowCelebration] = useState(false)
  const prevMet = useRef<Map<string, boolean>>(new Map())

  useEffect(() => {
    if (!rows) return

    const pending: PendingEntry[] = []
    const newlyCompleted: CompletedEntry[] = []

    for (const row of rows) {
      const key = row.key

      if (!row.done && row.todayTargetCount > row.todayCompletedCount) {
        pending.push({
          key,
          trackLabel: row.trackLabel,
          label: row.label,
          remaining: row.todayTargetCount - row.todayCompletedCount,
          yesterdayMet: row.yesterdayMet,
        })
      }

      const metNow = row.todayTargetCount > 0 && row.todayCompletedCount >= row.todayTargetCount
      const wasMet = prevMet.current.get(key)
      if (metNow && wasMet === false) {
        const storageKey = `goal-item-celebrated-${key}-${todayDateKey()}`
        let alreadyCelebrated = false
        try {
          alreadyCelebrated = !!localStorage.getItem(storageKey)
        } catch {
          alreadyCelebrated = false
        }
        if (!alreadyCelebrated) {
          newlyCompleted.push({ key, trackLabel: row.trackLabel, label: row.label })
          try {
            localStorage.setItem(storageKey, '1')
          } catch {
            // ignore storage failures (private mode, quota, etc.)
          }
        }
      }
      prevMet.current.set(key, metNow)
    }

    if (!hasShownReminder.current && pending.length > 0) {
      setPendingEntries(pending)
      setShowReminder(true)
      hasShownReminder.current = true
    }

    if (newlyCompleted.length > 0) {
      setCompletedEntries((prev) => [...prev, ...newlyCompleted])
      setShowCelebration(true)
    }
  }, [rows])

  return {
    pendingEntries,
    showReminder,
    dismissReminder: () => setShowReminder(false),
    completedEntries,
    showCelebration,
    dismissCelebration: () => setShowCelebration(false),
  }
}
