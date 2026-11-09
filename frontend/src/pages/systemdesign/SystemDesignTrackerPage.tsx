import { useEffect, useMemo, useState } from 'react'
import { systemDesignApi } from '../../api/systemDesign'
import type { SdCategory, SdGoalItemResponse, SdImportResultResponse, SdItemRequest, SdItemResponse, SdSheetResponse } from '../../api/types'
import { CardMenu } from '../../components/CardMenu'
import { PencilIcon, TrashIcon } from '../../components/icons/ActionIcons'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Spinner } from '../../components/Spinner'
import { SdSheetFormDialog } from './SdSheetFormDialog'
import { SdItemFormDialog } from './SdItemFormDialog'
import { SdTopicManagerDialog } from './SdTopicManagerDialog'
import { SdProgressRing } from './SdProgressRing'
import { SearchIcon } from '../../components/icons/SearchIcon'
import { GoalReminderModal } from '../../components/GoalReminderModal'
import { GoalCelebrationModal } from '../../components/GoalCelebrationModal'
import { useGoalItemPopups } from '../../hooks/useGoalItemPopups'
import { flattenSdGoals } from '../../utils/goalLabels'
import { excelFileName, saveBlob } from '../../utils/download'

const CATEGORIES: SdCategory[] = ['HLD', 'LLD']

function computeStats(items: SdItemResponse[]) {
  const total = items.length
  const completed = items.filter((i) => i.completed).length
  const byCategory = (c: SdCategory) => {
    const inCat = items.filter((i) => i.category === c)
    return { total: inCat.length, completed: inCat.filter((i) => i.completed).length }
  }
  return {
    total,
    completed,
    percent: total ? Math.round((completed * 100) / total) : 0,
    hld: byCategory('HLD'),
    lld: byCategory('LLD'),
  }
}

export function SystemDesignTrackerPage() {
  const [sheets, setSheets] = useState<SdSheetResponse[]>([])
  const [selectedSheetId, setSelectedSheetId] = useState<number | null>(null)
  const [items, setItems] = useState<SdItemResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [search, setSearch] = useState('')
  const [topicFilter, setTopicFilter] = useState('')
  const [category, setCategory] = useState<SdCategory>('HLD')
  const [revisionOnly, setRevisionOnly] = useState(false)
  const [completedOnly, setCompletedOnly] = useState(false)

  const [openTopics, setOpenTopics] = useState<Set<string>>(new Set())
  const [openGroups, setOpenGroups] = useState<Set<string>>(new Set())
  const [noteEditingId, setNoteEditingId] = useState<number | null>(null)
  const [noteDraft, setNoteDraft] = useState('')
  const [githubDraft, setGithubDraft] = useState('')

  const [showNewSheet, setShowNewSheet] = useState(false)
  const [showAddItem, setShowAddItem] = useState(false)
  const [showTopicManager, setShowTopicManager] = useState(false)
  const [editingItem, setEditingItem] = useState<SdItemResponse | null>(null)
  const [confirmingClearProgress, setConfirmingClearProgress] = useState(false)
  const [confirmingDeleteSheet, setConfirmingDeleteSheet] = useState(false)
  const [removingItemId, setRemovingItemId] = useState<number | null>(null)

  const [importing, setImporting] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [importResult, setImportResult] = useState<SdImportResultResponse | null>(null)
  const [importedSheetId, setImportedSheetId] = useState<number | null>(null)
  const [undoingImport, setUndoingImport] = useState(false)
  const [pendingImportFile, setPendingImportFile] = useState<File | null>(null)

  const [goalItems, setGoalItems] = useState<SdGoalItemResponse[]>([])

  function loadGoalItems() {
    systemDesignApi.planner.listGoalItems().then(setGoalItems).catch(() => setGoalItems([]))
  }

  useEffect(() => {
    loadGoalItems()
  }, [])

  const flatGoalRows = useMemo(() => flattenSdGoals(goalItems), [goalItems])
  const goalPopups = useGoalItemPopups(flatGoalRows)

  function loadSheets(selectId?: number) {
    systemDesignApi.sheets
      .list(category)
      .then((data) => {
        setSheets(data)
        if (data.length === 0) {
          setSelectedSheetId(null)
        } else if (selectId && data.some((s) => s.id === selectId)) {
          setSelectedSheetId(selectId)
        } else if (!data.some((s) => s.id === selectedSheetId)) {
          setSelectedSheetId(data[0].id)
        }
      })
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load sheets'))
  }

  useEffect(() => {
    loadSheets()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [category])

  function loadItems(sheetId: number) {
    setLoading(true)
    systemDesignApi.items
      .listForSheet(sheetId)
      .then(setItems)
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load items'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (selectedSheetId != null) {
      loadItems(selectedSheetId)
    } else {
      setItems([])
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedSheetId])

  const inCategory = useMemo(() => items.filter((i) => i.category === category), [items, category])

  const topicOptions = useMemo(() => Array.from(new Set(inCategory.map((i) => i.topicName))).sort(), [inCategory])

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    return inCategory.filter((i) => {
      if (topicFilter && i.topicName !== topicFilter) return false
      if (revisionOnly && !i.revisionStarred) return false
      if (completedOnly && !i.completed) return false
      if (needle) {
        const haystack = `${i.title} ${i.topicName} ${i.subTopic ?? ''}`.toLowerCase()
        if (!haystack.includes(needle)) return false
      }
      return true
    })
  }, [inCategory, search, topicFilter, revisionOnly, completedOnly])

  const grouped = useMemo(() => {
    const byTopic = new Map<string, Map<string, SdItemResponse[]>>()
    for (const i of filtered) {
      if (!byTopic.has(i.topicName)) byTopic.set(i.topicName, new Map())
      const bySub = byTopic.get(i.topicName)!
      const subKey = i.subTopic || 'General'
      if (!bySub.has(subKey)) bySub.set(subKey, [])
      bySub.get(subKey)!.push(i)
    }
    return byTopic
  }, [filtered])

  const stats = useMemo(() => computeStats(items), [items])

  const isFiltering = search.trim().length > 0 || topicFilter !== '' || revisionOnly || completedOnly

  function resetFilters() {
    setSearch('')
    setTopicFilter('')
    setRevisionOnly(false)
    setCompletedOnly(false)
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

  function replaceItem(updated: SdItemResponse) {
    setItems((prev) => prev.map((i) => (i.id === updated.id ? updated : i)))
  }

  async function toggleCompleted(item: SdItemResponse) {
    try {
      const updated = await systemDesignApi.items.updateProgress(item.id, { completed: !item.completed })
      replaceItem(updated)
      loadGoalItems()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to update progress')
    }
  }

  async function toggleStar(item: SdItemResponse) {
    try {
      const updated = await systemDesignApi.items.updateProgress(item.id, { revisionStarred: !item.revisionStarred })
      replaceItem(updated)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to update progress')
    }
  }

  function openNoteEditor(item: SdItemResponse) {
    setNoteEditingId(item.id)
    setNoteDraft(item.note ?? '')
    setGithubDraft(item.githubSolutionLink ?? '')
  }

  async function saveNote(item: SdItemResponse) {
    try {
      const updated = await systemDesignApi.items.updateProgress(item.id, {
        note: noteDraft,
        ...(item.category === 'LLD' ? { githubSolutionLink: githubDraft } : {}),
      })
      replaceItem(updated)
      setNoteEditingId(null)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save note')
    }
  }

  async function handleCreateSheet(payload: { name: string; description?: string }) {
    const sheet = await systemDesignApi.sheets.create(payload)
    setShowNewSheet(false)
    loadSheets(sheet.id)
  }

  async function handleAddItem(payload: SdItemRequest) {
    if (selectedSheetId == null) return
    await systemDesignApi.sheets.addItem(selectedSheetId, payload)
    setShowAddItem(false)
    loadItems(selectedSheetId)
    loadSheets(selectedSheetId)
  }

  async function handleEditItem(payload: SdItemRequest) {
    if (!editingItem) return
    const updated = await systemDesignApi.items.update(editingItem.id, payload)
    replaceItem(updated)
    setEditingItem(null)
  }

  async function handleRemoveItem() {
    if (selectedSheetId == null || removingItemId == null) return
    const id = removingItemId
    setRemovingItemId(null)
    try {
      await systemDesignApi.sheets.removeItem(selectedSheetId, id)
      setItems((prev) => prev.filter((i) => i.id !== id))
      loadSheets(selectedSheetId)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to remove item')
    }
  }

  async function handleDeleteSheet() {
    if (selectedSheetId == null) return
    const id = selectedSheetId
    setConfirmingDeleteSheet(false)
    try {
      await systemDesignApi.sheets.remove(id)
      loadSheets()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to delete sheet')
    }
  }

  // Import always creates a brand-new sheet rather than adding into whatever sheet happens to be
  // selected — an import that lands in the wrong existing sheet mixes its items in irreversibly
  // (there's no per-item "which import added this" record to undo from later). A dedicated new
  // sheet keeps the blast radius to "delete this one sheet" if the result isn't wanted, which is
  // exactly what the Undo import action below does.
  // Always the whole sheet with every column, whatever filters are active on screen.
  async function handleDownloadSheet() {
    const sheet = sheets.find((s) => s.id === selectedSheetId)
    if (!sheet) return
    setDownloading(true)
    try {
      saveBlob(await systemDesignApi.sheets.exportExcel(sheet.id), excelFileName(sheet.name))
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
    const sheet = await systemDesignApi.sheets.create(payload)
    setImporting(true)
    setImportResult(null)
    try {
      const result = await systemDesignApi.sheets.importExcel(sheet.id, file)
      setImportResult(result)
      setImportedSheetId(sheet.id)
      loadItems(sheet.id)
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
      await systemDesignApi.sheets.remove(importedSheetId)
      setImportResult(null)
      setImportedSheetId(null)
      loadSheets()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to undo import')
    } finally {
      setUndoingImport(false)
    }
  }

  async function handleClearProgress() {
    setConfirmingClearProgress(false)
    const toClear = items.filter((i) => i.completed || i.revisionStarred || i.note || i.githubSolutionLink)
    try {
      const updates = await Promise.all(
        toClear.map((i) =>
          systemDesignApi.items.updateProgress(i.id, {
            completed: false,
            revisionStarred: false,
            note: '',
            ...(i.category === 'LLD' ? { githubSolutionLink: '' } : {}),
          }),
        ),
      )
      setItems((prev) => prev.map((i) => updates.find((u) => u.id === i.id) ?? i))
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to clear progress')
    }
  }

  const selectedSheet = sheets.find((s) => s.id === selectedSheetId) ?? null

  return (
    <div className="dsa-shell sysdesign-shell">
      <div className="page-header">
        <div>
          <h1>System Design Tracker</h1>
          <p>Track your practice across sheets and topics — switch between HLD and LLD below.</p>
        </div>
        <div className="actions">
          <div className="sd-category-toggle">
            {CATEGORIES.map((c) => (
              <button
                key={c}
                type="button"
                className={`btn btn-sm toggle-btn ${category === c ? 'active' : ''}`}
                onClick={() => {
                  setCategory(c)
                  setTopicFilter('')
                }}
              >
                {c}
              </button>
            ))}
          </div>
          <button className="btn btn-secondary" type="button" onClick={resetFilters}>
            Reset Filters
          </button>
          {items.length > 0 && (
            <button className="btn btn-danger" type="button" onClick={() => setConfirmingClearProgress(true)}>
              Clear Progress
            </button>
          )}
        </div>
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {importResult && (
        <div className="banner banner-success">
          Imported {importResult.importedCount} item(s) into a new sheet, skipped {importResult.skippedCount}.
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
                {s.name} ({s.itemCount})
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
                <strong>Expected .xlsx format</strong> — required: <strong>Topic</strong>, <strong>Title</strong>,{' '}
                <strong>Category</strong> (HLD/LLD). Optional: <strong>Sub-topic</strong>, <strong>Link</strong>,{' '}
                <strong>Description</strong>. Column order doesn't matter, extra columns are ignored, one row per
                item starting on row 2.
                <table className="tooltip-sample-table">
                  <thead>
                    <tr>
                      <th>Topic</th>
                      <th>Sub-topic</th>
                      <th>Title</th>
                      <th>Category</th>
                      <th>Link</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>Databases</td>
                      <td>Sharding</td>
                      <td>Design a URL Shortener</td>
                      <td>HLD</td>
                      <td>https://...</td>
                    </tr>
                    <tr>
                      <td>Design Patterns</td>
                      <td></td>
                      <td>Design a Rate Limiter</td>
                      <td>LLD</td>
                      <td></td>
                    </tr>
                  </tbody>
                </table>
              </span>
            </span>
            {selectedSheetId != null && (
              <>
                <button className="btn btn-secondary btn-sm" type="button" onClick={() => setShowAddItem(true)}>
                  + Add Item
                </button>
                <button className="btn btn-danger btn-sm" type="button" onClick={() => setConfirmingDeleteSheet(true)}>
                  Delete Sheet
                </button>
              </>
            )}
            <button className="btn btn-secondary btn-sm" type="button" onClick={() => setShowTopicManager(true)}>
              Manage Topics
            </button>
          </div>
        </div>

        {selectedSheetId != null && (
          <div className="card dsa-progress-card">
            <div className="dsa-progress-header">
              <span className="dsa-progress-pill">System Design Progress</span>
              <span className="info-tooltip">
                i
                <span className="tooltip-text">
                  The ring shows how much of this sheet you've completed overall.
                </span>
              </span>
            </div>
            <div className="dsa-progress-body">
              <SdProgressRing completed={stats.completed} total={stats.total} />
              <ul className="dsa-progress-legend">
                <li>
                  <span className="dot hld" /> HLD {stats.hld.completed}/{stats.hld.total}
                </li>
                <li>
                  <span className="dot lld" /> LLD {stats.lld.completed}/{stats.lld.total}
                </li>
              </ul>
            </div>
          </div>
        )}
      </div>

      {selectedSheetId == null ? (
        <div className="card">
          <p className="empty-state">Create a sheet to start tracking items.</p>
        </div>
      ) : (
        <>
          <div className="dsa-filter-bar">
            <div className="search-input-wrap">
              <SearchIcon />
              <input
                type="search"
                placeholder="Search by title, topic, sub-topic..."
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
            <button
              type="button"
              className={`btn btn-sm toggle-btn ${revisionOnly ? 'active' : ''}`}
              onClick={() => setRevisionOnly((v) => !v)}
            >
              Revision only
            </button>
            <button
              type="button"
              className={`btn btn-sm toggle-btn ${completedOnly ? 'active' : ''}`}
              onClick={() => setCompletedOnly((v) => !v)}
            >
              Completed only
            </button>
          </div>

          {loading && items.length === 0 ? (
            <div className="card">
              <Spinner label="Loading items…" />
            </div>
          ) : filtered.length === 0 ? (
            <div className="card">
              <p className="empty-state">No items match these filters.</p>
            </div>
          ) : (
            <div className={`content-fade ${loading ? 'is-loading' : ''}`}>
              {Array.from(grouped.entries()).map(([topicName, bySub], topicIndex) => {
                const topicItems = Array.from(bySub.values()).flat()
                const topicCompleted = topicItems.filter((i) => i.completed).length
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
                        {topicCompleted} / {topicItems.length}
                      </span>
                    </div>
                    <div className={`accordion-collapse ${isOpen ? 'open' : ''}`}>
                      <div>
                        <div className="accordion-topic-body">
                          {Array.from(bySub.entries()).map(([subTopicName, groupItems]) => {
                            const groupKey = `${topicName}::${subTopicName}`
                            const groupCompleted = groupItems.filter((i) => i.completed).length
                            const groupOpen = isFiltering || openGroups.has(groupKey)
                            return (
                              <div className={`accordion-diff sd-category-${category}`} key={groupKey}>
                                <div
                                  className={`accordion-diff-header ${groupOpen ? 'open' : ''}`}
                                  onClick={() => toggleGroup(groupKey)}
                                >
                                  <span className="chevron">▶</span>
                                  <span className="diff-name">{subTopicName}</span>
                                  <span className="topic-count">
                                    {groupCompleted} / {groupItems.length}
                                  </span>
                                </div>
                                <div className={`accordion-collapse ${groupOpen ? 'open' : ''}`}>
                                  <div>
                                    <div className="accordion-diff-body">
                                      {groupItems.map((i) => (
                                        <div className="dsa-question-row" key={i.id}>
                                          <label className="dsa-checkbox">
                                            <input type="checkbox" checked={i.completed} onChange={() => toggleCompleted(i)} />
                                            <span className="mark" />
                                          </label>
                                          <div className="dsa-question-main">
                                            <span className="dsa-question-title">{i.title}</span>
                                            {i.important && <span className="important-badge">IMPORTANT</span>}
                                            {i.description && <p className="dsa-question-description">{i.description}</p>}

                                            {noteEditingId === i.id && (
                                              <div className="dsa-note-panel">
                                                <textarea
                                                  placeholder="Note..."
                                                  value={noteDraft}
                                                  onChange={(e) => setNoteDraft(e.target.value)}
                                                />
                                                {i.category === 'LLD' && (
                                                  <input
                                                    type="url"
                                                    placeholder="GitHub solution link (optional)"
                                                    value={githubDraft}
                                                    onChange={(e) => setGithubDraft(e.target.value)}
                                                  />
                                                )}
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
                                                    onClick={() => saveNote(i)}
                                                  >
                                                    Save
                                                  </button>
                                                </div>
                                              </div>
                                            )}
                                          </div>
                                          <div className="dsa-question-actions">
                                            {i.resourceLink && (
                                              <a
                                                className="btn btn-open btn-sm"
                                                href={i.resourceLink}
                                                target="_blank"
                                                rel="noreferrer"
                                              >
                                                Resource
                                              </a>
                                            )}
                                            {i.category === 'LLD' && i.githubSolutionLink && (
                                              <a
                                                className="btn btn-github btn-sm"
                                                href={i.githubSolutionLink}
                                                target="_blank"
                                                rel="noreferrer"
                                              >
                                                GitHub
                                              </a>
                                            )}
                                            <button
                                              className="btn btn-secondary btn-sm"
                                              type="button"
                                              title="Note / GitHub link"
                                              onClick={() => (noteEditingId === i.id ? setNoteEditingId(null) : openNoteEditor(i))}
                                            >
                                              {i.note ? '📝' : '+'}
                                            </button>
                                            <button
                                              className={`dsa-star-btn ${i.revisionStarred ? 'starred' : ''}`}
                                              type="button"
                                              title="Mark for revision"
                                              onClick={() => toggleStar(i)}
                                            >
                                              {i.revisionStarred ? '★' : '☆'}
                                            </button>
                                            <CardMenu
                                              actions={[
                                                { label: 'Edit', icon: <PencilIcon />, onClick: () => setEditingItem(i) },
                                                { label: 'Remove', icon: <TrashIcon />, onClick: () => setRemovingItemId(i.id), danger: true },
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

      {showNewSheet && <SdSheetFormDialog onSave={handleCreateSheet} onCancel={() => setShowNewSheet(false)} />}

      {pendingImportFile && (
        <SdSheetFormDialog
          title="Name the imported sheet"
          initialName={pendingImportFile.name.replace(/\.xlsx$/i, '')}
          onSave={handleCreateImportSheet}
          onCancel={() => setPendingImportFile(null)}
        />
      )}

      {showTopicManager && (
        <SdTopicManagerDialog
          onClose={() => setShowTopicManager(false)}
          onChanged={() => {
            if (selectedSheetId != null) {
              loadItems(selectedSheetId)
              loadSheets(selectedSheetId)
            }
          }}
        />
      )}

      {showAddItem && (
        <SdItemFormDialog topicOptions={topicOptions} onSave={handleAddItem} onCancel={() => setShowAddItem(false)} />
      )}

      {editingItem && (
        <SdItemFormDialog
          initial={editingItem}
          topicOptions={topicOptions}
          onSave={handleEditItem}
          onCancel={() => setEditingItem(null)}
        />
      )}

      {confirmingClearProgress && (
        <ConfirmDialog
          title="Clear progress"
          message={`Clear all completed/revision/note progress for "${selectedSheet?.name}"? This cannot be undone.`}
          onConfirm={handleClearProgress}
          onCancel={() => setConfirmingClearProgress(false)}
        />
      )}

      {confirmingDeleteSheet && (
        <ConfirmDialog
          title="Delete sheet"
          message={`Delete "${selectedSheet?.name}"? Items stay in the shared catalog but will be removed from this sheet.`}
          onConfirm={handleDeleteSheet}
          onCancel={() => setConfirmingDeleteSheet(false)}
        />
      )}

      {removingItemId != null && (
        <ConfirmDialog
          title="Remove item"
          message="Remove this item from the sheet? It stays in the shared catalog and any other sheet it belongs to."
          onConfirm={handleRemoveItem}
          onCancel={() => setRemovingItemId(null)}
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
