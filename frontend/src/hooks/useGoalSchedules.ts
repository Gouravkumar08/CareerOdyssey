import { useEffect, useState } from 'react'
import { dsaApi } from '../api/dsa'
import { systemDesignApi } from '../api/systemDesign'
import type { DsaGoalItemResponse, SdGoalItemResponse } from '../api/types'
import { addDays, todayString } from '../utils/plannerLabels'
import type { GoalSchedules } from '../utils/plannerAgenda'

const NO_SCHEDULES: GoalSchedules = { dsa: [], sd: [] }

/** Planned DSA / System Design targets for the next two days; skips the requests when there are no goals. */
export function useGoalSchedules(dsaGoals: DsaGoalItemResponse[], sdGoals: SdGoalItemResponse[]): GoalSchedules {
  const [schedules, setSchedules] = useState<GoalSchedules>(NO_SCHEDULES)

  useEffect(() => {
    const today = todayString()
    const months = [...new Set([addDays(today, 1), addDays(today, 2)].map((d) => d.slice(0, 7)))].map((m) => {
      const [year, month] = m.split('-').map(Number)
      return { year, month }
    })
    let cancelled = false
    Promise.all([
      Promise.all(months.map(({ year, month }) => (dsaGoals.length ? dsaApi.planner.goalItemsSchedule(year, month).catch(() => []) : []))),
      Promise.all(months.map(({ year, month }) => (sdGoals.length ? systemDesignApi.planner.goalItemsSchedule(year, month).catch(() => []) : []))),
    ]).then(([dsa, sd]) => {
      if (!cancelled) setSchedules({ dsa: dsa.flat(), sd: sd.flat() })
    })
    return () => {
      cancelled = true
    }
  }, [dsaGoals, sdGoals])

  return schedules
}
