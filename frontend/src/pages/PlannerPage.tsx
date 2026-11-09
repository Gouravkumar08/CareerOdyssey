import { useEffect, useMemo, useState } from 'react'
import { dsaApi } from '../api/dsa'
import { systemDesignApi } from '../api/systemDesign'
import type {
  Difficulty,
  DsaGoalItemRequest,
  DsaGoalItemResponse,
  DsaGoalTopicTargetRequest,
  SdGoalItemRequest,
  SdGoalItemResponse,
  SdSheetResponse,
  SdTopicResponse,
  SheetResponse,
  TopicResponse,
} from '../api/types'
import { CheckCircleIcon } from '../components/icons/NavIcons'
import { formatDsaTopicTargetLabel, formatSdTopicLabel } from '../utils/goalLabels'
import { GenericPlanner } from './planner/GenericPlanner'

const DIFFICULTIES: Difficulty[] = ['EASY', 'MEDIUM', 'HARD']
const DIFFICULTY_LABELS: Record<Difficulty, string> = { EASY: 'Easy', MEDIUM: 'Medium', HARD: 'Hard' }

const deadlineFormatter = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'long', year: 'numeric' })

function formatDeadline(dateStr: string) {
  return deadlineFormatter.format(new Date(`${dateStr}T00:00:00`))
}

type DsaGoalRowForm = {
  rowId: string
  topicId: number | ''
  mode: 'ALL' | 'COUNT'
  count: string
  difficulties: Difficulty[]
}

type SdGoalRowForm = {
  rowId: string
  topicId: number | ''
}

function daysUntil(dateStr: string, todayStr: string) {
  const end = new Date(`${dateStr}T00:00:00`)
  const today = new Date(`${todayStr}T00:00:00`)
  return Math.ceil((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
}

let rowIdCounter = 0
function nextRowId() {
  rowIdCounter += 1
  return `row-${rowIdCounter}`
}

function newDsaGoalRow(): DsaGoalRowForm {
  return { rowId: nextRowId(), topicId: '', mode: 'ALL', count: '10', difficulties: [] }
}

function newSdGoalRow(): SdGoalRowForm {
  return { rowId: nextRowId(), topicId: '' }
}

export function PlannerPage() {
  const [dsaTopics, setDsaTopics] = useState<TopicResponse[]>([])
  const [sdTopics, setSdTopics] = useState<SdTopicResponse[]>([])
  const [dsaSheets, setDsaSheets] = useState<SheetResponse[]>([])
  const [sdSheets, setSdSheets] = useState<SdSheetResponse[]>([])

  const [dsaGoalItems, setDsaGoalItems] = useState<DsaGoalItemResponse[]>([])
  const [sdGoalItems, setSdGoalItems] = useState<SdGoalItemResponse[]>([])

  const [dsaGoalRows, setDsaGoalRows] = useState<DsaGoalRowForm[]>([newDsaGoalRow()])
  const [dsaGoalSheetId, setDsaGoalSheetId] = useState<number | ''>('')
  const [dsaGoalEndDate, setDsaGoalEndDate] = useState('')
  const [creatingDsaGoal, setCreatingDsaGoal] = useState(false)
  const [dsaPreview, setDsaPreview] = useState<DsaGoalItemResponse | null>(null)
  const [editingDsaGoalId, setEditingDsaGoalId] = useState<number | null>(null)

  const [sdGoalRows, setSdGoalRows] = useState<SdGoalRowForm[]>([newSdGoalRow()])
  const [sdGoalSheetId, setSdGoalSheetId] = useState<number | ''>('')
  const [sdGoalEndDate, setSdGoalEndDate] = useState('')
  const [creatingSdGoal, setCreatingSdGoal] = useState(false)
  const [sdPreview, setSdPreview] = useState<SdGoalItemResponse | null>(null)
  const [editingSdGoalId, setEditingSdGoalId] = useState<number | null>(null)

  const [error, setError] = useState<string | null>(null)

  function loadDsaGoalItems() {
    return dsaApi.planner
      .listGoalItems()
      .then(setDsaGoalItems)
      .catch(() => setDsaGoalItems([]))
  }

  function loadSdGoalItems() {
    return systemDesignApi.planner
      .listGoalItems()
      .then(setSdGoalItems)
      .catch(() => setSdGoalItems([]))
  }

  useEffect(() => {
    loadDsaGoalItems()
    loadSdGoalItems()
    dsaApi.topics.list().then(setDsaTopics).catch(() => setDsaTopics([]))
    systemDesignApi.topics.list().then(setSdTopics).catch(() => setSdTopics([]))
    dsaApi.sheets.list().then(setDsaSheets).catch(() => setDsaSheets([]))
    systemDesignApi.sheets.list().then(setSdSheets).catch(() => setSdSheets([]))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // ---- Live preview: DSA ----
  useEffect(() => {
    const validTopics: DsaGoalTopicTargetRequest[] = dsaGoalRows
      .filter((r) => r.topicId !== '')
      .map((r) => ({
        topicId: r.topicId as number,
        targetQuestionCount: r.mode === 'COUNT' ? Number(r.count) || 0 : null,
        difficulties: r.difficulties.length > 0 ? r.difficulties : undefined,
      }))
    if (validTopics.length === 0 || !dsaGoalEndDate) {
      setDsaPreview(null)
      return
    }
    const payload: DsaGoalItemRequest = {
      topics: validTopics,
      sheetId: dsaGoalSheetId === '' ? null : dsaGoalSheetId,
      endDate: dsaGoalEndDate,
    }
    const timer = setTimeout(() => {
      dsaApi.planner
        .previewGoalItem(payload)
        .then(setDsaPreview)
        .catch(() => setDsaPreview(null))
    }, 400)
    return () => clearTimeout(timer)
  }, [dsaGoalRows, dsaGoalSheetId, dsaGoalEndDate])

  // ---- Live preview: System Design ----
  useEffect(() => {
    const validTopicIds = sdGoalRows.map((r) => r.topicId).filter((id): id is number => id !== '')
    if (validTopicIds.length === 0 || !sdGoalEndDate) {
      setSdPreview(null)
      return
    }
    const payload: SdGoalItemRequest = {
      topicIds: validTopicIds,
      sheetId: sdGoalSheetId === '' ? null : sdGoalSheetId,
      endDate: sdGoalEndDate,
    }
    const timer = setTimeout(() => {
      systemDesignApi.planner
        .previewGoalItem(payload)
        .then(setSdPreview)
        .catch(() => setSdPreview(null))
    }, 400)
    return () => clearTimeout(timer)
  }, [sdGoalRows, sdGoalSheetId, sdGoalEndDate])

  function updateDsaGoalRow(rowId: string, patch: Partial<DsaGoalRowForm>) {
    setDsaGoalRows((prev) => prev.map((r) => (r.rowId === rowId ? { ...r, ...patch } : r)))
  }

  function toggleDsaRowDifficulty(rowId: string, difficulty: Difficulty) {
    setDsaGoalRows((prev) =>
      prev.map((r) => {
        if (r.rowId !== rowId) return r
        const has = r.difficulties.includes(difficulty)
        return { ...r, difficulties: has ? r.difficulties.filter((d) => d !== difficulty) : [...r.difficulties, difficulty] }
      }),
    )
  }

  function addDsaGoalRow() {
    setDsaGoalRows((prev) => [...prev, newDsaGoalRow()])
  }

  function removeDsaGoalRow(rowId: string) {
    setDsaGoalRows((prev) => (prev.length > 1 ? prev.filter((r) => r.rowId !== rowId) : prev))
  }

  function addSdGoalRow() {
    setSdGoalRows((prev) => [...prev, newSdGoalRow()])
  }

  function removeSdGoalRow(rowId: string) {
    setSdGoalRows((prev) => (prev.length > 1 ? prev.filter((r) => r.rowId !== rowId) : prev))
  }

  function resetDsaGoalForm() {
    setDsaGoalRows([newDsaGoalRow()])
    setDsaGoalSheetId('')
    setDsaGoalEndDate('')
    setDsaPreview(null)
    setEditingDsaGoalId(null)
  }

  function resetSdGoalForm() {
    setSdGoalRows([newSdGoalRow()])
    setSdGoalSheetId('')
    setSdGoalEndDate('')
    setSdPreview(null)
    setEditingSdGoalId(null)
  }

  function startEditDsaGoal(item: DsaGoalItemResponse) {
    setDsaGoalRows(
      item.topics.map((t) => ({
        rowId: nextRowId(),
        topicId: t.topicId,
        mode: t.targetQuestionCount != null ? 'COUNT' : 'ALL',
        count: t.targetQuestionCount != null ? String(t.targetQuestionCount) : '10',
        difficulties: t.difficulties ?? [],
      })),
    )
    setDsaGoalSheetId(item.sheetId ?? '')
    setDsaGoalEndDate(item.endDate)
    setEditingDsaGoalId(item.id)
  }

  function startEditSdGoal(item: SdGoalItemResponse) {
    setSdGoalRows(item.topics.map((t) => ({ rowId: nextRowId(), topicId: t.topicId })))
    setSdGoalSheetId(item.sheetId ?? '')
    setSdGoalEndDate(item.endDate)
    setEditingSdGoalId(item.id)
  }

  async function handleCreateDsaGoal(event: React.FormEvent) {
    event.preventDefault()
    if (!dsaGoalEndDate) {
      setError('Set a deadline for the DSA goal')
      return
    }
    const topics: DsaGoalTopicTargetRequest[] = dsaGoalRows
      .filter((r) => r.topicId !== '')
      .map((r) => ({
        topicId: r.topicId as number,
        targetQuestionCount: r.mode === 'COUNT' ? Number(r.count) || 0 : null,
        difficulties: r.difficulties.length > 0 ? r.difficulties : undefined,
      }))
    if (topics.length === 0) {
      setError('Select at least one DSA topic')
      return
    }
    const payload: DsaGoalItemRequest = {
      topics,
      sheetId: dsaGoalSheetId === '' ? null : dsaGoalSheetId,
      endDate: dsaGoalEndDate,
    }
    setCreatingDsaGoal(true)
    try {
      if (editingDsaGoalId != null) {
        await dsaApi.planner.updateGoalItem(editingDsaGoalId, payload)
      } else {
        await dsaApi.planner.createGoalItem(payload)
      }
      resetDsaGoalForm()
      await loadDsaGoalItems()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save DSA goal')
    } finally {
      setCreatingDsaGoal(false)
    }
  }

  async function handleCreateSdGoal(event: React.FormEvent) {
    event.preventDefault()
    if (!sdGoalEndDate) {
      setError('Set a deadline for the System Design goal')
      return
    }
    const topicIds = sdGoalRows.map((r) => r.topicId).filter((id): id is number => id !== '')
    if (topicIds.length === 0) {
      setError('Select at least one System Design topic')
      return
    }
    const payload: SdGoalItemRequest = {
      topicIds,
      sheetId: sdGoalSheetId === '' ? null : sdGoalSheetId,
      endDate: sdGoalEndDate,
    }
    setCreatingSdGoal(true)
    try {
      if (editingSdGoalId != null) {
        await systemDesignApi.planner.updateGoalItem(editingSdGoalId, payload)
      } else {
        await systemDesignApi.planner.createGoalItem(payload)
      }
      resetSdGoalForm()
      await loadSdGoalItems()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save System Design goal')
    } finally {
      setCreatingSdGoal(false)
    }
  }

  async function handleDeleteDsaGoalItem(id: number) {
    try {
      await dsaApi.planner.deleteGoalItem(id)
      await loadDsaGoalItems()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to remove DSA goal')
    }
  }

  async function handleDeleteSdGoalItem(id: number) {
    try {
      await systemDesignApi.planner.deleteGoalItem(id)
      await loadSdGoalItems()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to remove System Design goal')
    }
  }

  const todayStr = useMemo(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }, [])

  return (
    <div className="dsa-shell">
      {error && <div className="banner banner-error">{error}</div>}

      <GenericPlanner dsaGoalItems={dsaGoalItems} sdGoalItems={sdGoalItems} />

      <h2 className="section-heading">Goals</h2>
      <div className="planner-goals">
      <form onSubmit={handleCreateDsaGoal} className="goal-form">
        <div className="stat-card goal-card">
          <div className="stat-label">Add DSA Goal</div>

          {dsaGoalRows.map((row) => (
            <div className="goal-row-card" key={row.rowId}>
              <div className="goal-row-header">
                <select
                  value={row.topicId}
                  onChange={(e) => updateDsaGoalRow(row.rowId, { topicId: e.target.value ? Number(e.target.value) : '' })}
                >
                  <option value="">Select a topic</option>
                  {dsaTopics.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} ({t.questionCount} questions)
                    </option>
                  ))}
                </select>
                {dsaGoalRows.length > 1 && (
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => removeDsaGoalRow(row.rowId)}
                  >
                    Remove row
                  </button>
                )}
              </div>

              <div className="goal-row-fields">
                <div className="goal-row-mode-toggle">
                  <button
                    type="button"
                    className={`btn btn-sm toggle-btn ${row.mode === 'ALL' ? 'active' : ''}`}
                    onClick={() => updateDsaGoalRow(row.rowId, { mode: 'ALL' })}
                  >
                    Complete all
                  </button>
                  <button
                    type="button"
                    className={`btn btn-sm toggle-btn ${row.mode === 'COUNT' ? 'active' : ''}`}
                    onClick={() => updateDsaGoalRow(row.rowId, { mode: 'COUNT' })}
                  >
                    Solve N questions
                  </button>
                </div>

                {row.mode === 'COUNT' && (
                  <input
                    type="number"
                    min={1}
                    value={row.count}
                    onChange={(e) => updateDsaGoalRow(row.rowId, { count: e.target.value })}
                    className="goal-total-input"
                    placeholder="Target question count"
                  />
                )}

                <div className="goal-row-difficulties">
                  {DIFFICULTIES.map((d) => (
                    <label key={d}>
                      <input
                        type="checkbox"
                        checked={row.difficulties.includes(d)}
                        onChange={() => toggleDsaRowDifficulty(row.rowId, d)}
                      />
                      {DIFFICULTY_LABELS[d]}
                    </label>
                  ))}
                </div>
              </div>
            </div>
          ))}

          <button type="button" className="btn btn-secondary btn-sm" onClick={addDsaGoalRow}>
            + Add another topic
          </button>

          <select
            value={dsaGoalSheetId}
            onChange={(e) => setDsaGoalSheetId(e.target.value ? Number(e.target.value) : '')}
          >
            <option value="">All sheets</option>
            {dsaSheets.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>

          <input
            type="date"
            value={dsaGoalEndDate}
            onChange={(e) => setDsaGoalEndDate(e.target.value)}
            className="goal-date-input"
          />

          {dsaPreview && (
            <div className="goal-preview-list">
              {dsaPreview.topics.map((t) => (
                <div className="goal-preview-row" key={t.topicId}>
                  <span>{t.topicName}</span>
                  <span>
                    today: {t.todayTargetCount}, total: {t.totalTarget}
                  </span>
                </div>
              ))}
              <div className="goal-preview-row">
                <span>Deadline</span>
                <span>{daysUntil(dsaPreview.endDate, todayStr)} day(s) left</span>
              </div>
            </div>
          )}

          <div className="goal-form-actions">
            <button className="btn btn-primary btn-sm" type="submit" disabled={creatingDsaGoal}>
              {creatingDsaGoal ? 'Saving…' : editingDsaGoalId != null ? 'Save changes' : 'Create Goal'}
            </button>
            {editingDsaGoalId != null && (
              <button type="button" className="btn btn-secondary btn-sm" onClick={resetDsaGoalForm}>
                Cancel edit
              </button>
            )}
          </div>

          {dsaGoalItems.length > 0 && (
            <div className="goal-item-list">
              <div className="stat-label">Your DSA Goals</div>
              {dsaGoalItems.map((item) => (
                <div className={`goal-item-card ${item.done ? 'goal-item-done' : ''}`} key={item.id}>
                  <div className="goal-item-card-header">
                    <span>
                      {item.sheetName ? `${item.sheetName} · ` : ''}
                      Deadline {formatDeadline(item.endDate)}
                    </span>
                    {item.done && <CheckCircleIcon />}
                  </div>
                  <div className="goal-item-progress">
                    {item.topics.map((t) => (
                      <div className="goal-item-topic-row" key={t.topicId}>
                        <span>
                          {formatDsaTopicTargetLabel(t)}
                          {t.done ? ' ✓' : ''}
                        </span>
                        <span>
                          {t.todayCompletedCount}/{t.todayTargetCount} today · {t.completedCount}/{t.totalTarget} total
                        </span>
                      </div>
                    ))}
                    <div>
                      {daysUntil(item.endDate, todayStr) >= 0
                        ? `${daysUntil(item.endDate, todayStr)} day(s) left`
                        : 'overdue'}
                    </div>
                  </div>
                  <div className="goal-item-card-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => startEditDsaGoal(item)}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      onClick={() => handleDeleteDsaGoalItem(item.id)}
                    >
                      Remove goal
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </form>

      <form onSubmit={handleCreateSdGoal} className="goal-form">
        <div className="stat-card goal-card">
          <div className="stat-label">Add System Design Goal</div>

          {sdGoalRows.map((row) => (
            <div className="goal-row-card" key={row.rowId}>
              <div className="goal-row-header">
                <select
                  value={row.topicId}
                  onChange={(e) => setSdGoalRows((prev) => prev.map((r) => (r.rowId === row.rowId ? { ...r, topicId: e.target.value ? Number(e.target.value) : '' } : r)))}
                >
                  <option value="">Select a topic</option>
                  {sdTopics.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} ({t.itemCount} items)
                    </option>
                  ))}
                </select>
                {sdGoalRows.length > 1 && (
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => removeSdGoalRow(row.rowId)}
                  >
                    Remove row
                  </button>
                )}
              </div>
            </div>
          ))}

          <button type="button" className="btn btn-secondary btn-sm" onClick={addSdGoalRow}>
            + Add another topic
          </button>

          <select
            value={sdGoalSheetId}
            onChange={(e) => setSdGoalSheetId(e.target.value ? Number(e.target.value) : '')}
          >
            <option value="">All sheets</option>
            {sdSheets.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>

          <input
            type="date"
            value={sdGoalEndDate}
            onChange={(e) => setSdGoalEndDate(e.target.value)}
            className="goal-date-input"
          />

          {sdPreview && (
            <div className="goal-preview-list">
              {sdPreview.topics.map((t) => (
                <div className="goal-preview-row" key={t.topicId}>
                  <span>{t.topicName}</span>
                  <span>
                    today: {t.todayTargetCount}, total: {t.totalTarget}
                  </span>
                </div>
              ))}
              <div className="goal-preview-row">
                <span>Deadline</span>
                <span>{daysUntil(sdPreview.endDate, todayStr)} day(s) left</span>
              </div>
            </div>
          )}

          <div className="goal-form-actions">
            <button className="btn btn-primary btn-sm" type="submit" disabled={creatingSdGoal}>
              {creatingSdGoal ? 'Saving…' : editingSdGoalId != null ? 'Save changes' : 'Create Goal'}
            </button>
            {editingSdGoalId != null && (
              <button type="button" className="btn btn-secondary btn-sm" onClick={resetSdGoalForm}>
                Cancel edit
              </button>
            )}
          </div>

          {sdGoalItems.length > 0 && (
            <div className="goal-item-list">
              <div className="stat-label">Your System Design Goals</div>
              {sdGoalItems.map((item) => (
                <div className={`goal-item-card ${item.done ? 'goal-item-done' : ''}`} key={item.id}>
                  <div className="goal-item-card-header">
                    <span>
                      {item.sheetName ? `${item.sheetName} · ` : ''}
                      Deadline {formatDeadline(item.endDate)}
                    </span>
                    {item.done && <CheckCircleIcon />}
                  </div>
                  <div className="goal-item-progress">
                    {item.topics.map((t) => (
                      <div className="goal-item-topic-row" key={t.topicId}>
                        <span>
                          {formatSdTopicLabel(t)}
                          {t.done ? ' ✓' : ''}
                        </span>
                        <span>
                          {t.todayCompletedCount}/{t.todayTargetCount} today · {t.completedCount}/{t.totalTarget} total
                        </span>
                      </div>
                    ))}
                    <div>
                      {daysUntil(item.endDate, todayStr) >= 0
                        ? `${daysUntil(item.endDate, todayStr)} day(s) left`
                        : 'overdue'}
                    </div>
                  </div>
                  <div className="goal-item-card-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => startEditSdGoal(item)}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      onClick={() => handleDeleteSdGoalItem(item.id)}
                    >
                      Remove goal
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </form>
      </div>
    </div>
  )
}
