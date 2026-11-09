import { useEffect, useMemo, useState } from 'react'
import { dsaApi } from '../../api/dsa'
import type { Difficulty, DsaGoalItemResponse, ImportResultResponse, QuestionRequest, QuestionResponse, SheetResponse } from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Spinner } from '../../components/Spinner'
import { SheetFormDialog } from './SheetFormDialog'
import { QuestionFormDialog } from './QuestionFormDialog'
import { DsaProgressRing } from './DsaProgressRing'
import { SearchIcon } from '../../components/icons/SearchIcon'
import { CardMenu } from '../../components/CardMenu'
import { PencilIcon, TrashIcon } from '../../components/icons/ActionIcons'
import { GoalReminderModal } from '../../components/GoalReminderModal'
import { GoalCelebrationModal } from '../../components/GoalCelebrationModal'
import { useGoalItemPopups } from '../../hooks/useGoalItemPopups'
import { flattenDsaGoals } from '../../utils/goalLabels'
import { excelFileName, saveBlob } from '../../utils/download'

const DIFFICULTIES: Difficulty[] = ['EASY', 'MEDIUM', 'HARD']

const DIFF_ACCENT: Record<Difficulty, string> = {
  EASY: '#16a34a',
  MEDIUM: '#d97706',
  HARD: '#dc2626',
}

const LAST_SHEET_STORAGE_KEY = 'dsa-tracker-last-sheet-id'

function computeStats(questions: QuestionResponse[]) {
  const total = questions.length
  const solved = questions.filter((q) => q.solved).length
  const byDifficulty = (d: Difficulty) => {
    const inDiff = questions.filter((q) => q.difficulty === d)
    return { total: inDiff.length, solved: inDiff.filter((q) => q.solved).length }
  }
  return {
    total,
    solved,
    percent: total ? Math.round((solved * 100) / total) : 0,
    easy: byDifficulty('EASY'),
    medium: byDifficulty('MEDIUM'),
    hard: byDifficulty('HARD'),
  }
}

export function DsaTrackerPage() {
  const [sheets, setSheets] = useState<SheetResponse[]>([])
  const [selectedSheetId, setSelectedSheetId] = useState<number | null>(null)
  const [questions, setQuestions] = useState<QuestionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [search, setSearch] = useState('')
  const [topicFilter, setTopicFilter] = useState('')
  const [difficultyFilter, setDifficultyFilter] = useState<Difficulty | ''>('')
  const [revisionOnly, setRevisionOnly] = useState(false)
  const [solvedOnly, setSolvedOnly] = useState(false)

  const [openTopics, setOpenTopics] = useState<Set<string>>(new Set())
  const [openGroups, setOpenGroups] = useState<Set<string>>(new Set())
  const [noteEditingId, setNoteEditingId] = useState<number | null>(null)
  const [noteDraft, setNoteDraft] = useState('')
  const [githubDraft, setGithubDraft] = useState('')

  const [showNewSheet, setShowNewSheet] = useState(false)
  const [showAddQuestion, setShowAddQuestion] = useState(false)
  const [editingQuestion, setEditingQuestion] = useState<QuestionResponse | null>(null)
  const [confirmingClearProgress, setConfirmingClearProgress] = useState(false)
  const [confirmingDeleteSheet, setConfirmingDeleteSheet] = useState(false)
  const [removingQuestionId, setRemovingQuestionId] = useState<number | null>(null)

  const [importing, setImporting] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [importResult, setImportResult] = useState<ImportResultResponse | null>(null)
  const [importedSheetId, setImportedSheetId] = useState<number | null>(null)
  const [undoingImport, setUndoingImport] = useState(false)
  const [pendingImportFile, setPendingImportFile] = useState<File | null>(null)

  const [goalItems, setGoalItems] = useState<DsaGoalItemResponse[]>([])

  function loadGoalItems() {
    dsaApi.planner.listGoalItems().then(setGoalItems).catch(() => setGoalItems([]))
  }

  useEffect(() => {
    loadGoalItems()
  }, [])

  const flatGoalRows = useMemo(() => flattenDsaGoals(goalItems), [goalItems])
  const goalPopups = useGoalItemPopups(flatGoalRows)

  function loadSheets(selectId?: number) {
    dsaApi.sheets
      .list()
      .then((data) => {
        setSheets(data)
        if (data.length === 0) {
          setSelectedSheetId(null)
          localStorage.removeItem(LAST_SHEET_STORAGE_KEY)
          return
        }
        if (selectId && data.some((s) => s.id === selectId)) {
          setSelectedSheetId(selectId)
          return
        }
        if (data.some((s) => s.id === selectedSheetId)) {
          return
        }
        const lastUsedId = Number(localStorage.getItem(LAST_SHEET_STORAGE_KEY))
        if (lastUsedId && data.some((s) => s.id === lastUsedId)) {
          setSelectedSheetId(lastUsedId)
        } else {
          setSelectedSheetId(data[0].id)
        }
      })
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load sheets'))
  }

  useEffect(() => {
    loadSheets()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (selectedSheetId != null) {
      localStorage.setItem(LAST_SHEET_STORAGE_KEY, String(selectedSheetId))
    }
  }, [selectedSheetId])

  function loadQuestions(sheetId: number) {
    setLoading(true)
    dsaApi.questions
      .listForSheet(sheetId)
      .then(setQuestions)
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load questions'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (selectedSheetId != null) {
      loadQuestions(selectedSheetId)
    } else {
      setQuestions([])
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedSheetId])

  const topicOptions = useMemo(() => Array.from(new Set(questions.map((q) => q.topicName))).sort(), [questions])

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    return questions.filter((q) => {
      if (topicFilter && q.topicName !== topicFilter) return false
      if (difficultyFilter && q.difficulty !== difficultyFilter) return false
      if (revisionOnly && !q.revisionStarred) return false
      if (solvedOnly && !q.solved) return false
      if (needle) {
        const haystack = `${q.title} ${q.topicName} ${q.subTopic ?? ''}`.toLowerCase()
        if (!haystack.includes(needle)) return false
      }
      return true
    })
  }, [questions, search, topicFilter, difficultyFilter, revisionOnly, solvedOnly])

  const grouped = useMemo(() => {
    const byTopic = new Map<string, Map<Difficulty, QuestionResponse[]>>()
    for (const q of filtered) {
      if (!byTopic.has(q.topicName)) byTopic.set(q.topicName, new Map())
      const byDiff = byTopic.get(q.topicName)!
      if (!byDiff.has(q.difficulty)) byDiff.set(q.difficulty, [])
      byDiff.get(q.difficulty)!.push(q)
    }
    return byTopic
  }, [filtered])

  const stats = useMemo(() => computeStats(questions), [questions])

  const isFiltering = search.trim().length > 0 || topicFilter !== '' || difficultyFilter !== '' || revisionOnly || solvedOnly

  function resetFilters() {
    setSearch('')
    setTopicFilter('')
    setDifficultyFilter('')
    setRevisionOnly(false)
    setSolvedOnly(false)
  }

  function toggleTopic(topic: string) {
    setOpenTopics((prev) => {
      const next = new Set(prev)
      if (next.has(topic)) next.delete(topic)
      else next.add(topic)
      return next
    })
  }

  function toggleGroup(key: string) {
    setOpenGroups((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  function replaceQuestion(updated: QuestionResponse) {
    setQuestions((prev) => prev.map((q) => (q.id === updated.id ? updated : q)))
  }

  async function toggleSolved(question: QuestionResponse) {
    try {
      const updated = await dsaApi.questions.updateProgress(question.id, { solved: !question.solved })
      replaceQuestion(updated)
      loadGoalItems()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to update progress')
    }
  }

  async function toggleStar(question: QuestionResponse) {
    try {
      const updated = await dsaApi.questions.updateProgress(question.id, { revisionStarred: !question.revisionStarred })
      replaceQuestion(updated)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to update progress')
    }
  }

  function openNoteEditor(question: QuestionResponse) {
    setNoteEditingId(question.id)
    setNoteDraft(question.note ?? '')
    setGithubDraft(question.githubSolutionLink ?? '')
  }

  async function saveNote(question: QuestionResponse) {
    try {
      const updated = await dsaApi.questions.updateProgress(question.id, {
        note: noteDraft,
        githubSolutionLink: githubDraft,
      })
      replaceQuestion(updated)
      setNoteEditingId(null)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save note')
    }
  }

  async function handleCreateSheet(payload: { name: string; description?: string }) {
    const sheet = await dsaApi.sheets.create(payload)
    setShowNewSheet(false)
    loadSheets(sheet.id)
  }

  async function handleAddQuestion(payload: QuestionRequest) {
    if (selectedSheetId == null) return
    await dsaApi.sheets.addQuestion(selectedSheetId, payload)
    setShowAddQuestion(false)
    loadQuestions(selectedSheetId)
    loadSheets(selectedSheetId)
  }

  async function handleEditQuestion(payload: QuestionRequest) {
    if (!editingQuestion) return
    const updated = await dsaApi.questions.update(editingQuestion.id, payload)
    replaceQuestion(updated)
    setEditingQuestion(null)
  }

  async function handleRemoveQuestion() {
    if (selectedSheetId == null || removingQuestionId == null) return
    const id = removingQuestionId
    setRemovingQuestionId(null)
    try {
      await dsaApi.sheets.removeQuestion(selectedSheetId, id)
      setQuestions((prev) => prev.filter((q) => q.id !== id))
      loadSheets(selectedSheetId)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to remove question')
    }
  }

  async function handleDeleteSheet() {
    if (selectedSheetId == null) return
    const id = selectedSheetId
    setConfirmingDeleteSheet(false)
    try {
      await dsaApi.sheets.remove(id)
      loadSheets()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to delete sheet')
    }
  }

  async function handleClearProgress() {
    setConfirmingClearProgress(false)
    const toClear = questions.filter((q) => q.solved || q.revisionStarred || q.note || q.githubSolutionLink)
    try {
      const updates = await Promise.all(
        toClear.map((q) =>
          dsaApi.questions.updateProgress(q.id, { solved: false, revisionStarred: false, note: '', githubSolutionLink: '' }),
        ),
      )
      setQuestions((prev) => prev.map((q) => updates.find((u) => u.id === q.id) ?? q))
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to clear progress')
    }
  }

  // Import always creates a brand-new sheet rather than adding into whatever sheet happens to be
  // selected — an import that lands in the wrong existing sheet mixes its questions in
  // irreversibly (there's no per-question "which import added this" record to undo from later).
  // A dedicated new sheet keeps the blast radius to "delete this one sheet" if the result isn't
  // wanted, which is exactly what the Undo import action below does.
  // Always the whole sheet with every column, whatever filters are active on screen.
  async function handleDownloadSheet() {
    const sheet = sheets.find((s) => s.id === selectedSheetId)
    if (!sheet) return
    setDownloading(true)
    try {
      saveBlob(await dsaApi.sheets.exportExcel(sheet.id), excelFileName(sheet.name))
    } catch {
      setError('Download failed')
    } finally {
      setDownloading(false)
    }
  }

  function handleImportFile(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setPendingImportFile(file)
  }

  async function handleCreateImportSheet(payload: { name: string; description?: string }) {
    const file = pendingImportFile
    if (!file) return
    setPendingImportFile(null)
    const sheet = await dsaApi.sheets.create(payload)
    setImporting(true)
    setImportResult(null)
    try {
      const result = await dsaApi.sheets.importExcel(sheet.id, file)
      setImportResult(result)
      setImportedSheetId(sheet.id)
      loadQuestions(sheet.id)
      loadSheets(sheet.id)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Import failed')
    } finally {
      setImporting(false)
    }
  }

  async function handleUndoImport() {
    if (importedSheetId == null) return
    setUndoingImport(true)
    try {
      await dsaApi.sheets.remove(importedSheetId)
      setImportResult(null)
      setImportedSheetId(null)
      loadSheets()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to undo import')
    } finally {
      setUndoingImport(false)
    }
  }

  const selectedSheet = sheets.find((s) => s.id === selectedSheetId) ?? null

  return (
    <div className="dsa-shell">
      <div className="page-header">
        <div>
          <h1>DSA Tracker</h1>
          <p>Track your practice across sheets, topics and difficulty.</p>
        </div>
        <div className="actions">
          <button className="btn btn-secondary" type="button" onClick={resetFilters}>
            Reset Filters
          </button>
          {questions.length > 0 && (
            <button className="btn btn-danger" type="button" onClick={() => setConfirmingClearProgress(true)}>
              Clear Progress
            </button>
          )}
        </div>
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {importResult && (
        <div className="banner banner-success">
          Imported {importResult.importedCount} question(s) into a new sheet, skipped {importResult.skippedCount}.
          {importResult.skippedReasons.length > 0 && (
            <ul>
              {importResult.skippedReasons.map((reason, i) => (
                <li key={i}>{reason}</li>
              ))}
            </ul>
          )}
          <div className="mt-1" style={{ display: 'flex', gap: '0.5rem' }}>
            <button className="btn btn-secondary btn-sm" type="button" onClick={() => setImportResult(null)}>
              Dismiss
            </button>
            <button className="btn btn-danger btn-sm" type="button" onClick={handleUndoImport} disabled={undoingImport}>
              {undoingImport ? 'Undoing…' : 'Undo import (delete this sheet)'}
            </button>
          </div>
        </div>
      )}

      <div className="dsa-top-row">
        <div className="card dsa-sheet-toolbar">
          <select
            value={selectedSheetId ?? ''}
            onChange={(e) => setSelectedSheetId(e.target.value ? Number(e.target.value) : null)}
          >
            {sheets.length === 0 && <option value="">No sheets yet</option>}
            {sheets.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.questionCount})
              </option>
            ))}
          </select>
          <div className="dsa-sheet-toolbar-actions">
            <button className="btn btn-secondary btn-sm" type="button" onClick={() => setShowNewSheet(true)}>
              + New Sheet
            </button>
            <button
              className="btn btn-secondary btn-sm"
              type="button"
              onClick={handleDownloadSheet}
              disabled={selectedSheetId == null || downloading}
            >
              {downloading ? 'Preparing…' : 'Download Excel'}
            </button>
            <label className="btn btn-secondary btn-sm" style={{ cursor: importing ? 'wait' : 'pointer' }}>
              {importing ? 'Importing…' : 'Import Excel (new sheet)'}
              <input type="file" accept=".xlsx" onChange={handleImportFile} disabled={importing} hidden />
            </label>
            <span className="info-tooltip">
              i
              <span className="tooltip-text tooltip-text-wide tooltip-below">
                Import always creates a brand-new sheet — it never adds into the sheet you have
                selected, so a bad import is just "delete this one sheet" to undo.
                <br />
                <br />
                <strong>Expected .xlsx format</strong> — required: <strong>Topic</strong>, <strong>Question</strong>,{' '}
                <strong>Difficulty</strong> (Easy/Medium/Hard). Optional: <strong>Sub-topic</strong>,{' '}
                <strong>Link</strong>. Column order doesn't matter, extra columns are ignored, one row per
                question starting on row 2.
                    <table className="tooltip-sample-table">
                      <thead>
                        <tr>
                          <th>Topic</th>
                          <th>Sub-topic</th>
                          <th>Question</th>
                          <th>Difficulty</th>
                          <th>Link</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>Arrays</td>
                          <td>Two Pointers</td>
                          <td>Two Sum</td>
                          <td>Easy</td>
                          <td>https://...</td>
                        </tr>
                        <tr>
                          <td>Trees</td>
                          <td></td>
                          <td>Binary Tree Traversal</td>
                          <td>Medium</td>
                          <td></td>
                        </tr>
                      </tbody>
                    </table>
                  </span>
                </span>
            {selectedSheetId != null && (
              <>
                <button className="btn btn-secondary btn-sm" type="button" onClick={() => setShowAddQuestion(true)}>
                  + Add Question
                </button>
                <button className="btn btn-danger btn-sm" type="button" onClick={() => setConfirmingDeleteSheet(true)}>
                  Delete Sheet
                </button>
              </>
            )}
          </div>
        </div>

        {selectedSheetId != null && (
          <div className="card dsa-progress-card">
            <div className="dsa-progress-header">
              <span className="dsa-progress-pill">DSA Progress</span>
              <span className="info-tooltip">
                i
                <span className="tooltip-text">
                  The ring splits into an Easy / Medium / Hard segment sized by how many questions
                  of that difficulty are in this sheet. The glowing portion of each segment is how
                  much of it you've solved.
                </span>
              </span>
            </div>
            <div className="dsa-progress-body">
              <DsaProgressRing solved={stats.solved} total={stats.total} easy={stats.easy} medium={stats.medium} hard={stats.hard} />
              <ul className="dsa-progress-legend">
                <li>
                  <span className="dot easy" /> Easy {stats.easy.solved}/{stats.easy.total}
                </li>
                <li>
                  <span className="dot medium" /> Medium {stats.medium.solved}/{stats.medium.total}
                </li>
                <li>
                  <span className="dot hard" /> Hard {stats.hard.solved}/{stats.hard.total}
                </li>
              </ul>
            </div>
          </div>
        )}

      </div>

      {selectedSheetId == null ? (
        <div className="card">
          <p className="empty-state">Create a sheet to start tracking questions.</p>
        </div>
      ) : (
        <>
              <div className="dsa-filter-bar">
                <div className="search-input-wrap">
                  <SearchIcon />
                  <input
                    type="search"
                    placeholder="Search by title, topic, pattern..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                  />
                </div>
                <select value={topicFilter} onChange={(e) => setTopicFilter(e.target.value)}>
                  <option value="">All topics</option>
                  {topicOptions.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
                <select value={difficultyFilter} onChange={(e) => setDifficultyFilter(e.target.value as Difficulty | '')}>
                  <option value="">All difficulties</option>
                  {DIFFICULTIES.map((d) => (
                    <option key={d} value={d}>
                      {d.charAt(0) + d.slice(1).toLowerCase()}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className={`btn btn-sm toggle-btn ${revisionOnly ? 'active' : ''}`}
                  onClick={() => setRevisionOnly((v) => !v)}
                >
                  Revision only
                </button>
                <button
                  type="button"
                  className={`btn btn-sm toggle-btn ${solvedOnly ? 'active' : ''}`}
                  onClick={() => setSolvedOnly((v) => !v)}
                >
                  Solved only
                </button>
              </div>

              {loading && questions.length === 0 ? (
                <div className="card">
                  <Spinner label="Loading questions…" />
                </div>
              ) : filtered.length === 0 ? (
                <div className="card">
                  <p className="empty-state">No questions match these filters.</p>
                </div>
              ) : (
                <div className={`content-fade ${loading ? 'is-loading' : ''}`}>
                {Array.from(grouped.entries()).map(([topicName, byDiff], topicIndex) => {
                  const topicQuestions = Array.from(byDiff.values()).flat()
                  const topicSolved = topicQuestions.filter((q) => q.solved).length
                  const isOpen = isFiltering || openTopics.has(topicName)
                  return (
                    <div
                      className="accordion-topic stagger-in"
                      style={{ '--stagger-index': topicIndex } as React.CSSProperties}
                      key={topicName}
                    >
                      <div
                        className={`accordion-topic-header ${isOpen ? 'open' : ''}`}
                        onClick={() => toggleTopic(topicName)}
                      >
                        <span className="chevron">▶</span>
                        <span className="topic-name">{topicName}</span>
                        <span className="topic-count">
                          {topicSolved} / {topicQuestions.length}
                        </span>
                      </div>
                      <div className={`accordion-collapse ${isOpen ? 'open' : ''}`}>
                        <div>
                        <div className="accordion-topic-body">
                          {DIFFICULTIES.filter((d) => byDiff.has(d)).map((difficulty) => {
                            const groupKey = `${topicName}::${difficulty}`
                            const groupQuestions = byDiff.get(difficulty)!
                            const groupSolved = groupQuestions.filter((q) => q.solved).length
                            const groupOpen = isFiltering || openGroups.has(groupKey)
                            return (
                              <div
                                className="accordion-diff"
                                style={{ '--diff-accent': DIFF_ACCENT[difficulty] } as React.CSSProperties}
                                key={groupKey}
                              >
                                <div
                                  className={`accordion-diff-header ${groupOpen ? 'open' : ''}`}
                                  onClick={() => toggleGroup(groupKey)}
                                >
                                  <span className="chevron">▶</span>
                                  <span className="diff-name">
                                    {difficulty.charAt(0) + difficulty.slice(1).toLowerCase()}
                                  </span>
                                  <span className="topic-count">
                                    {groupSolved} / {groupQuestions.length}
                                  </span>
                                </div>
                                <div className={`accordion-collapse ${groupOpen ? 'open' : ''}`}>
                                  <div>
                                  <div className="accordion-diff-body">
                                    {groupQuestions.map((q) => (
                                      <div className="dsa-question-row" key={q.id}>
                                        <label className="dsa-checkbox">
                                          <input type="checkbox" checked={q.solved} onChange={() => toggleSolved(q)} />
                                          <span className="mark" />
                                        </label>
                                        <div className="dsa-question-main">
                                          <span className="dsa-question-title">{q.title}</span>
                                          {q.important && <span className="important-badge">IMPORTANT</span>}
                                          {q.subTopic && <span className="dsa-question-subtopic">{q.subTopic}</span>}

                                          {noteEditingId === q.id && (
                                            <div className="dsa-note-panel">
                                              <textarea
                                                placeholder="Note..."
                                                value={noteDraft}
                                                onChange={(e) => setNoteDraft(e.target.value)}
                                              />
                                              <input
                                                type="url"
                                                placeholder="GitHub solution link (optional)"
                                                value={githubDraft}
                                                onChange={(e) => setGithubDraft(e.target.value)}
                                              />
                                              <div className="actions">
                                                <button
                                                  className="btn btn-secondary btn-sm"
                                                  type="button"
                                                  onClick={() => setNoteEditingId(null)}
                                                >
                                                  Cancel
                                                </button>
                                                <button
                                                  className="btn btn-primary btn-sm"
                                                  type="button"
                                                  onClick={() => saveNote(q)}
                                                >
                                                  Save
                                                </button>
                                              </div>
                                            </div>
                                          )}
                                        </div>
                                        <div className="dsa-question-actions">
                                          {q.practiceLink && (
                                            <a className="btn btn-open btn-sm" href={q.practiceLink} target="_blank" rel="noreferrer">
                                              Open
                                            </a>
                                          )}
                                          {q.youtubeLink && (
                                            <a className="btn btn-secondary btn-sm" href={q.youtubeLink} target="_blank" rel="noreferrer">
                                              ▶
                                            </a>
                                          )}
                                          {q.articleLink && (
                                            <a className="btn btn-secondary btn-sm" href={q.articleLink} target="_blank" rel="noreferrer">
                                              📄
                                            </a>
                                          )}
                                          {q.githubSolutionLink && (
                                            <a className="btn btn-github btn-sm" href={q.githubSolutionLink} target="_blank" rel="noreferrer">
                                              GitHub
                                            </a>
                                          )}
                                          <button
                                            className="btn btn-secondary btn-sm"
                                            type="button"
                                            title="Note / GitHub link"
                                            onClick={() => (noteEditingId === q.id ? setNoteEditingId(null) : openNoteEditor(q))}
                                          >
                                            {q.note ? '📝' : '+'}
                                          </button>
                                          <button
                                            className={`dsa-star-btn ${q.revisionStarred ? 'starred' : ''}`}
                                            type="button"
                                            title="Mark for revision"
                                            onClick={() => toggleStar(q)}
                                          >
                                            {q.revisionStarred ? '★' : '☆'}
                                          </button>
                                          <span className={`difficulty-badge ${q.difficulty}`}>
                                            {q.difficulty.charAt(0) + q.difficulty.slice(1).toLowerCase()}
                                          </span>
                                          <CardMenu
                                            actions={[
                                              { label: 'Edit', icon: <PencilIcon />, onClick: () => setEditingQuestion(q) },
                                              {
                                                label: 'Remove',
                                                icon: <TrashIcon />,
                                                danger: true,
                                                onClick: () => setRemovingQuestionId(q.id),
                                              },
                                            ]}
                                          />
                                        </div>
                                      </div>
                                    ))}
                                  </div>
                                  </div>
                                </div>
                              </div>
                            )
                          })}
                        </div>
                        </div>
                      </div>
                    </div>
                  )
                })}
                </div>
              )}
            </>
          )}

      {showNewSheet && <SheetFormDialog onSave={handleCreateSheet} onCancel={() => setShowNewSheet(false)} />}

      {pendingImportFile && (
        <SheetFormDialog
          title="Name the imported sheet"
          initialName={pendingImportFile.name.replace(/\.xlsx$/i, '')}
          onSave={handleCreateImportSheet}
          onCancel={() => setPendingImportFile(null)}
        />
      )}

      {showAddQuestion && (
        <QuestionFormDialog
          topicOptions={topicOptions}
          onSave={handleAddQuestion}
          onCancel={() => setShowAddQuestion(false)}
        />
      )}

      {editingQuestion && (
        <QuestionFormDialog
          initial={editingQuestion}
          topicOptions={topicOptions}
          onSave={handleEditQuestion}
          onCancel={() => setEditingQuestion(null)}
        />
      )}

      {confirmingClearProgress && (
        <ConfirmDialog
          title="Clear progress"
          message={`Clear all solved/revision/note progress for "${selectedSheet?.name}"? This cannot be undone.`}
          onConfirm={handleClearProgress}
          onCancel={() => setConfirmingClearProgress(false)}
        />
      )}

      {confirmingDeleteSheet && (
        <ConfirmDialog
          title="Delete sheet"
          message={`Delete "${selectedSheet?.name}"? Questions stay in the shared catalog but will be removed from this sheet.`}
          onConfirm={handleDeleteSheet}
          onCancel={() => setConfirmingDeleteSheet(false)}
        />
      )}

      {removingQuestionId != null && (
        <ConfirmDialog
          title="Remove question"
          message="Remove this question from the sheet? It stays in the shared catalog and any other sheet it belongs to."
          onConfirm={handleRemoveQuestion}
          onCancel={() => setRemovingQuestionId(null)}
        />
      )}

      {goalPopups.showReminder && (
        <GoalReminderModal entries={goalPopups.pendingEntries} onDismiss={goalPopups.dismissReminder} />
      )}

      {goalPopups.showCelebration && (
        <GoalCelebrationModal entries={goalPopups.completedEntries} onDismiss={goalPopups.dismissCelebration} />
      )}
    </div>
  )
}
