import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { companiesApi } from '../../api/companies'
import type { ApiErrorBody, CompanyResponse } from '../../api/types'
import { Spinner } from '../../components/Spinner'
import { SearchIcon } from '../../components/icons/SearchIcon'
import { CardMenu } from '../../components/CardMenu'
import { PencilIcon, TrashIcon } from '../../components/icons/ActionIcons'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { avatarColorClass } from '../../utils/avatarColor'

export function CompaniesList() {
  const [companies, setCompanies] = useState<CompanyResponse[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const navigate = useNavigate()

  function load() {
    setLoading(true)
    companiesApi
      .list(search || undefined)
      .then((data) => {
        setCompanies(data)
        setError(null)
      })
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setError(body?.message ?? 'Failed to load companies.')
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    const handle = setTimeout(load, 250)
    return () => clearTimeout(handle)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search])

  async function handleDelete() {
    if (deleteId == null) return
    const id = deleteId
    setDeleteId(null)
    try {
      await companiesApi.remove(id)
      load()
    } catch (err: any) {
      const body = err?.response?.data as ApiErrorBody | undefined
      setError(body?.message ?? 'Failed to delete company.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Companies</h1>
          <p>Track target companies and their HR/career contacts.</p>
        </div>
        <Link className="btn btn-primary" to="/companies/new">
          + Add Company
        </Link>
      </div>

      <form onSubmit={(e) => e.preventDefault()} className="search-input-wrap" style={{ marginBottom: '1.25rem' }}>
        <SearchIcon />
        <input
          type="search"
          placeholder="Search by company, location, or HR/career email..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </form>

      {error && <div className="banner banner-error">{error}</div>}

      {!loading && companies.length > 0 && (
        <div className="page-stats">
          <div className="stat-card">
            <div className="stat-label">Total Companies</div>
            <div className="stat-value">{companies.length}</div>
          </div>
          <div className="stat-card accent-easy">
            <div className="stat-label">With HR Email</div>
            <div className="stat-value easy-text">{companies.filter((c) => c.hrEmail).length}</div>
          </div>
          <div className="stat-card accent-medium">
            <div className="stat-label">With Career Email</div>
            <div className="stat-value">{companies.filter((c) => c.careerEmail).length}</div>
          </div>
        </div>
      )}

      {loading && companies.length === 0 && (
        <div className="card">
          <Spinner />
        </div>
      )}

      {!loading && !error && companies.length === 0 && (
        <div className="card">
          {search ? (
            <p className="empty-state">No companies match &quot;{search}&quot;.</p>
          ) : (
            <p className="empty-state">No companies yet. Add one before sending outreach.</p>
          )}
        </div>
      )}

      {companies.length > 0 && (
        <div className={`item-list content-fade ${loading ? 'is-loading' : ''}`}>
          {companies.map((company, i) => (
            <div key={company.id} className="item-card stagger-in" style={{ '--stagger-index': i } as React.CSSProperties}>
              <Link
                to={`/companies/${company.id}`}
                style={{
                  textDecoration: 'none',
                  color: 'inherit',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  flex: 1,
                  minWidth: 0,
                }}
              >
                <div className={`avatar-circle ${avatarColorClass(company.companyName || 'company')}`}>
                  {company.companyName ? company.companyName.substring(0, 1) : '?'}
                </div>
                <div className="item-main">
                  <div className="item-title">{company.companyName}</div>
                  <div className="item-meta">
                    {company.location && <span>{company.location}</span>}
                    {company.hrEmail && <span className="dot">{company.hrEmail}</span>}
                    {company.careerEmail && <span className="dot">{company.careerEmail}</span>}
                  </div>
                </div>
              </Link>
              <div className="item-side">
                <div className="item-actions">
                  <Link className="btn btn-secondary btn-sm" to={`/companies/${company.id}`}>
                    View
                  </Link>
                  <Link className="btn btn-primary btn-sm" to={`/outreach/new?companyId=${company.id}`}>
                    Send
                  </Link>
                  <CardMenu
                    actions={[
                      { label: 'Edit', icon: <PencilIcon />, onClick: () => navigate(`/companies/${company.id}/edit`) },
                      { label: 'Delete', icon: <TrashIcon />, danger: true, onClick: () => setDeleteId(company.id) },
                    ]}
                  />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {deleteId !== null && (
        <ConfirmDialog
          title="Delete company"
          message="Delete this company? This cannot be undone."
          onConfirm={handleDelete}
          onCancel={() => setDeleteId(null)}
        />
      )}
    </div>
  )
}
