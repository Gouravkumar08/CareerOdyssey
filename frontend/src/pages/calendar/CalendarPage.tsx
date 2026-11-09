import { useEffect, useState } from 'react'
import { dsaApi } from '../../api/dsa'
import { systemDesignApi } from '../../api/systemDesign'
import type { DsaGoalItemResponse, SdGoalItemResponse } from '../../api/types'
import { PlannerCalendar } from '../planner/PlannerCalendar'

export function CalendarPage() {
  const [dsaGoalItems, setDsaGoalItems] = useState<DsaGoalItemResponse[]>([])
  const [sdGoalItems, setSdGoalItems] = useState<SdGoalItemResponse[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    dsaApi.planner.listGoalItems().then(setDsaGoalItems).catch(() => setDsaGoalItems([]))
    systemDesignApi.planner.listGoalItems().then(setSdGoalItems).catch(() => setSdGoalItems([]))
  }, [])

  return (
    <div className="dsa-shell">
      {error && <div className="banner banner-error">{error}</div>}
      <h2 className="section-heading">Calendar</h2>
      <PlannerCalendar dsaGoalItems={dsaGoalItems} sdGoalItems={sdGoalItems} onError={setError} />
    </div>
  )
}
