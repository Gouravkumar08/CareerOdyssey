import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { usersApi } from '../../api/users'
import type { ApiErrorBody, UserResponse } from '../../api/types'
import { Spinner } from '../../components/Spinner'
import { SearchIcon } from '../../components/icons/SearchIcon'
import { CardMenu } from '../../components/CardMenu'
import { PencilIcon, TrashIcon } from '../../components/icons/ActionIcons'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { avatarColorClass } from '../../utils/avatarColor'

export function ProfilesList() {
  const [users, setUsers] = useState<UserResponse[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const navigate = useNavigate()

  function load() {
    setLoading(true)
    usersApi
      .list(search || undefined)
      .then((data) => {
        setUsers(data)
        setError(null)
      })
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setError(body?.message ?? 'Failed to load profiles.')
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
      await usersApi.remove(id)
      load()
    } catch (err: any) {
      const body = err?.response?.data as ApiErrorBody | undefined
      setError(body?.message ?? 'Failed to delete profile.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Profiles</h1>
          <p>Manage candidate profiles used for outreach.</p>
        </div>
        <Link className="btn btn-primary" to="/profiles/new">
          + Add Profile
        </Link>
      </div>

      <form onSubmit={(e) => e.preventDefault()} className="search-input-wrap" style={{ marginBottom: '1.25rem' }}>
        <SearchIcon />
        <input
          type="search"
          placeholder="Search by name, email, or company..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </form>

      {error && <div className="banner banner-error">{error}</div>}

      {!loading && users.length > 0 && (
        <div className="page-stats">
          <div className="stat-card">
            <div className="stat-label">Total Profiles</div>
            <div className="stat-value">{users.length}</div>
          </div>
          <div className="stat-card accent-easy">
            <div className="stat-label">Ready to Send</div>
            <div className="stat-value easy-text">{users.filter((u) => u.gmailAppPasswordSet).length}</div>
          </div>
        </div>
      )}

      {loading && users.length === 0 && (
        <div className="card">
          <Spinner />
        </div>
      )}

      {!loading && !error && users.length === 0 && (
        <div className="card">
          {search ? (
            <p className="empty-state">No profiles match &quot;{search}&quot;.</p>
          ) : (
            <p className="empty-state">No profiles yet. Add one to start sending outreach.</p>
          )}
        </div>
      )}

      {users.length > 0 && (
        <div className={`item-list content-fade ${loading ? 'is-loading' : ''}`}>
          {users.map((user, i) => (
            <div key={user.id} className="item-card stagger-in" style={{ '--stagger-index': i } as React.CSSProperties}>
              <Link
                to={`/profiles/${user.id}`}
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
                <div className={`avatar-circle ${avatarColorClass(user.name || 'user')}`}>{user.name ? user.name.substring(0, 1) : '?'}</div>
                <div className="item-main">
                  <div className="item-title">{user.name}</div>
                  <div className="item-meta">
                    <span>{user.email}</span>
                    {user.location && <span className="dot">{user.location}</span>}
                    {user.noticePeriod && <span className="dot">{user.noticePeriod}</span>}
                  </div>
                </div>
              </Link>
              <div className="item-side">
                <span className="item-side-label">
                  {user.lastUsedDisplay ? `Last used ${user.lastUsedDisplay}` : 'Never used'}
                </span>
                <div className="item-actions">
                  <Link className="btn btn-secondary btn-sm" to={`/profiles/${user.id}`}>
                    View →
                  </Link>
                  <CardMenu
                    actions={[
                      { label: 'Edit', icon: <PencilIcon />, onClick: () => navigate(`/profiles/${user.id}/edit`) },
                      { label: 'Delete', icon: <TrashIcon />, danger: true, onClick: () => setDeleteId(user.id) },
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
          title="Delete profile"
          message="Delete this profile? This cannot be undone."
          onConfirm={handleDelete}
          onCancel={() => setDeleteId(null)}
        />
      )}
    </div>
  )
}
