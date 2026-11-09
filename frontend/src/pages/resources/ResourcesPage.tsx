import { useEffect, useMemo, useState } from 'react'
import { resourcesApi } from '../../api/resources'
import type { ResourceCategoryResponse, ResourceItemRequest, ResourceItemResponse } from '../../api/types'
import { CardMenu } from '../../components/CardMenu'
import { PencilIcon, TrashIcon, ExternalLinkIcon } from '../../components/icons/ActionIcons'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Spinner } from '../../components/Spinner'
import { SearchIcon } from '../../components/icons/SearchIcon'
import { ResourceCategoryManagerDialog } from './ResourceCategoryManagerDialog'
import { ResourceItemFormDialog } from './ResourceItemFormDialog'
import { excelFileName, saveBlob } from '../../utils/download'

export function ResourcesPage() {
  const [categories, setCategories] = useState<ResourceCategoryResponse[]>([])
  const [items, setItems] = useState<ResourceItemResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [downloading, setDownloading] = useState(false)

  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [subCategoryFilter, setSubCategoryFilter] = useState('')
  const [importantOnly, setImportantOnly] = useState(false)

  const [openCategories, setOpenCategories] = useState<Set<string>>(new Set())
  const [openGroups, setOpenGroups] = useState<Set<string>>(new Set())

  const [showAddItem, setShowAddItem] = useState(false)
  const [showCategoryManager, setShowCategoryManager] = useState(false)
  const [editingItem, setEditingItem] = useState<ResourceItemResponse | null>(null)
  const [removingItemId, setRemovingItemId] = useState<number | null>(null)

  function loadCategories() {
    resourcesApi.categories
      .list()
      .then(setCategories)
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load categories'))
  }

  function loadItems() {
    setLoading(true)
    resourcesApi.items
      .list()
      .then(setItems)
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load resources'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadCategories()
    loadItems()
  }, [])

  const categoryOptions = useMemo(() => categories.map((c) => c.name), [categories])

  const subCategoryOptions = useMemo(() => {
    if (!categoryFilter) return []
    return Array.from(
      new Set(
        items
          .filter((i) => i.categoryName === categoryFilter && i.subCategory)
          .map((i) => i.subCategory as string),
      ),
    ).sort()
  }, [items, categoryFilter])

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    return items.filter((i) => {
      if (categoryFilter && i.categoryName !== categoryFilter) return false
      if (subCategoryFilter && i.subCategory !== subCategoryFilter) return false
      if (importantOnly && !i.important) return false
      if (needle) {
        const haystack = `${i.name} ${i.categoryName ?? ''} ${i.subCategory ?? ''}`.toLowerCase()
        if (!haystack.includes(needle)) return false
      }
      return true
    })
  }, [items, search, categoryFilter, subCategoryFilter, importantOnly])

  const grouped = useMemo(() => {
    const byCategory = new Map<string, Map<string, ResourceItemResponse[]>>()
    for (const i of filtered) {
      const categoryName = i.categoryName ?? 'Uncategorized'
      if (!byCategory.has(categoryName)) byCategory.set(categoryName, new Map())
      const bySub = byCategory.get(categoryName)!
      const subKey = i.subCategory || 'General'
      if (!bySub.has(subKey)) bySub.set(subKey, [])
      bySub.get(subKey)!.push(i)
    }
    return byCategory
  }, [filtered])

  const isFiltering = search.trim().length > 0 || categoryFilter !== '' || subCategoryFilter !== '' || importantOnly

  function resetFilters() {
    setSearch('')
    setCategoryFilter('')
    setSubCategoryFilter('')
    setImportantOnly(false)
  }

  function toggleCategory(name: string) {
    setOpenCategories((prev) => {
      const next = new Set(prev)
      if (next.has(name)) next.delete(name)
      else next.add(name)
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

  function replaceItem(updated: ResourceItemResponse) {
    setItems((prev) => prev.map((i) => (i.id === updated.id ? updated : i)))
  }

  async function toggleImportant(item: ResourceItemResponse) {
    try {
      const updated = await resourcesApi.items.toggleImportant(item.id, !item.important)
      replaceItem(updated)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to update resource')
    }
  }

  async function handleAddItem(payload: ResourceItemRequest) {
    await resourcesApi.items.create(payload)
    setShowAddItem(false)
    loadItems()
    loadCategories()
  }

  async function handleEditItem(payload: ResourceItemRequest) {
    if (!editingItem) return
    const updated = await resourcesApi.items.update(editingItem.id, payload)
    replaceItem(updated)
    setEditingItem(null)
    loadCategories()
  }

  async function handleRemoveItem() {
    if (removingItemId == null) return
    const id = removingItemId
    setRemovingItemId(null)
    try {
      await resourcesApi.items.remove(id)
      setItems((prev) => prev.filter((i) => i.id !== id))
      loadCategories()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to remove resource')
    }
  }

  // The whole library, whatever search or category filter is active on screen.
  async function handleDownload() {
    setDownloading(true)
    try {
      saveBlob(await resourcesApi.items.exportExcel(), excelFileName('Free Resources'))
    } catch {
      setError('Download failed')
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div className="dsa-shell resources-shell">
      <div className="page-header">
        <div>
          <h1>Free Resources</h1>
          <p>A curated library of videos, articles and playlists — organized by topic.</p>
        </div>
        <div className="actions">
          <button className="btn btn-secondary" type="button" onClick={handleDownload} disabled={downloading}>
            {downloading ? 'Preparing…' : 'Download Excel'}
          </button>
          <button className="btn btn-secondary" type="button" onClick={() => setShowCategoryManager(true)}>
            Manage Categories
          </button>
          <button className="btn btn-primary" type="button" onClick={() => setShowAddItem(true)}>
            + Add Resource
          </button>
        </div>
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      <div className="dsa-filter-bar">
        <div className="search-input-wrap">
          <SearchIcon />
          <input
            type="search"
            placeholder="Search by name, category, sub-category..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <select
          value={categoryFilter}
          onChange={(e) => {
            setCategoryFilter(e.target.value)
            setSubCategoryFilter('')
          }}
        >
          <option value="">All categories</option>
          {categoryOptions.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
        {categoryFilter && (
          <select value={subCategoryFilter} onChange={(e) => setSubCategoryFilter(e.target.value)}>
            <option value="">All sub-categories</option>
            {subCategoryOptions.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        )}
        <button
          type="button"
          className={`btn btn-sm toggle-btn ${importantOnly ? 'active' : ''}`}
          onClick={() => setImportantOnly((v) => !v)}
        >
          Important only
        </button>
        {isFiltering && (
          <button type="button" className="btn btn-secondary btn-sm" onClick={resetFilters}>
            Reset Filters
          </button>
        )}
      </div>

      {loading && items.length === 0 ? (
        <div className="card">
          <Spinner label="Loading resources…" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="card">
          <p className="empty-state">No resources match these filters.</p>
        </div>
      ) : (
        <div className={`content-fade ${loading ? 'is-loading' : ''}`}>
          {Array.from(grouped.entries()).map(([categoryName, bySub], categoryIndex) => {
            const categoryItems = Array.from(bySub.values()).flat()
            const isOpen = isFiltering || openCategories.has(categoryName)
            return (
              <div
                className="accordion-topic stagger-in"
                style={{ '--stagger-index': categoryIndex } as React.CSSProperties}
                key={categoryName}
              >
                <div
                  className={`accordion-topic-header ${isOpen ? 'open' : ''}`}
                  onClick={() => toggleCategory(categoryName)}
                >
                  <span className="chevron">▶</span>
                  <span className="topic-name">{categoryName}</span>
                  <span className="topic-count">{categoryItems.length} item(s)</span>
                </div>
                <div className={`accordion-collapse ${isOpen ? 'open' : ''}`}>
                  <div>
                    <div className="accordion-topic-body">
                      {Array.from(bySub.entries()).map(([subCategoryName, groupItems]) => {
                        const groupKey = `${categoryName}::${subCategoryName}`
                        const groupOpen = isFiltering || openGroups.has(groupKey)
                        return (
                          <div className="accordion-diff" key={groupKey}>
                            <div
                              className={`accordion-diff-header ${groupOpen ? 'open' : ''}`}
                              onClick={() => toggleGroup(groupKey)}
                            >
                              <span className="chevron">▶</span>
                              <span className="diff-name">{subCategoryName}</span>
                              <span className="topic-count">{groupItems.length} item(s)</span>
                            </div>
                            <div className={`accordion-collapse ${groupOpen ? 'open' : ''}`}>
                              <div>
                                <div className="accordion-diff-body">
                                  {groupItems.map((i) => (
                                    <div className="dsa-question-row" key={i.id}>
                                      <div className="dsa-question-main">
                                        <a
                                          className="dsa-question-title resource-link-title"
                                          href={i.link}
                                          target="_blank"
                                          rel="noreferrer"
                                        >
                                          {i.name}
                                          <ExternalLinkIcon className="resource-link-icon" />
                                        </a>
                                      </div>
                                      <div className="dsa-question-actions">
                                        <button
                                          className={`dsa-star-btn ${i.important ? 'starred' : ''}`}
                                          type="button"
                                          title="Mark as important"
                                          onClick={() => toggleImportant(i)}
                                        >
                                          {i.important ? '★' : '☆'}
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

      {showAddItem && (
        <ResourceItemFormDialog
          categoryOptions={categoryOptions}
          onSave={handleAddItem}
          onCancel={() => setShowAddItem(false)}
        />
      )}

      {editingItem && (
        <ResourceItemFormDialog
          initial={editingItem}
          categoryOptions={categoryOptions}
          onSave={handleEditItem}
          onCancel={() => setEditingItem(null)}
        />
      )}

      {showCategoryManager && (
        <ResourceCategoryManagerDialog
          onClose={() => setShowCategoryManager(false)}
          onChanged={() => {
            loadItems()
            loadCategories()
          }}
        />
      )}

      {removingItemId != null && (
        <ConfirmDialog
          title="Remove resource"
          message="Remove this resource from the library? This cannot be undone."
          onConfirm={handleRemoveItem}
          onCancel={() => setRemovingItemId(null)}
        />
      )}
    </div>
  )
}
