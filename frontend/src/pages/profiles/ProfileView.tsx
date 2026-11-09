import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { usersApi } from '../../api/users'
import type { ApiErrorBody, UserResponse } from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Spinner } from '../../components/Spinner'
import { GithubIcon, LinkedinIcon, LinkIcon, FileIcon } from '../../components/icons/BrandIcons'
import { avatarColorClass } from '../../utils/avatarColor'

export function ProfileView() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [user, setUser] = useState<UserResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    usersApi
      .get(Number(id))
      .then((data) => {
        setUser(data)
        setError(null)
      })
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setError(body?.message ?? 'Failed to load profile.')
      })
      .finally(() => setLoading(false))
  }, [id])

  const handleDelete = () => {
    if (!user) return
    setConfirmingDelete(false)
    usersApi
      .remove(user.id)
      .then(() => navigate('/profiles'))
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setDeleteError(body?.message ?? 'Failed to delete profile.')
      })
  }

  if (loading) {
    return (
      <div className="page">
        <Spinner />
      </div>
    )
  }

  if (error || !user) {
    return (
      <div className="page">
        <div className="banner banner-error">{error ?? 'Profile not found.'}</div>
      </div>
    )
  }

  const hasLinks =
    !!user.linkedinUrl || !!user.githubUrl || !!user.portfolioUrl || !!user.resumeDriveLink

  return (
    <div className="page">
      {deleteError && <div className="banner banner-error">{deleteError}</div>}

      <div className="page-header">
        <div className="profile-header" style={{ marginBottom: 0 }}>
          <div className={`avatar-circle ${avatarColorClass(user.name || 'user')}`}>{user.name ? user.name.substring(0, 1) : '?'}</div>
          <div className="identity">
            <h1>{user.name}</h1>
            {user.currentDesignation && user.currentCompany && (
              <p>
                {user.currentDesignation} at {user.currentCompany}
              </p>
            )}
          </div>
        </div>
        <div className="actions">
          <Link className="btn btn-secondary" to={`/profiles/${user.id}/edit`}>
            Edit
          </Link>
          <button className="btn btn-danger" type="button" onClick={() => setConfirmingDelete(true)}>
            Delete
          </button>
        </div>
      </div>

      {confirmingDelete && (
        <ConfirmDialog
          title="Delete profile"
          message="Delete this profile? This cannot be undone."
          onConfirm={handleDelete}
          onCancel={() => setConfirmingDelete(false)}
        />
      )}

      <div className="info-grid">
        <div className="info-section">
          <h3>Contact</h3>
          <div className="info-row">
            <span className="info-label">Email</span>
            <span className="info-value">{user.email}</span>
          </div>
          {user.phone && (
            <div className="info-row">
              <span className="info-label">Phone</span>
              <span className="info-value">{user.phone}</span>
            </div>
          )}
          {user.location && (
            <div className="info-row">
              <span className="info-label">Location</span>
              <span className="info-value">{user.location}</span>
            </div>
          )}
        </div>

        {hasLinks && (
          <div className="info-section">
            <h3>Links</h3>
            <div className="link-chip-list">
              {user.linkedinUrl && (
                <a className="link-chip" href={user.linkedinUrl} target="_blank" rel="noopener noreferrer">
                  <LinkedinIcon className="icon-linkedin" /> LinkedIn
                </a>
              )}
              {user.githubUrl && (
                <a className="link-chip" href={user.githubUrl} target="_blank" rel="noopener noreferrer">
                  <GithubIcon className="icon-github" /> GitHub
                </a>
              )}
              {user.portfolioUrl && (
                <a className="link-chip" href={user.portfolioUrl} target="_blank" rel="noopener noreferrer">
                  <LinkIcon /> Portfolio
                </a>
              )}
              {user.resumeDriveLink && (
                <a className="link-chip" href={user.resumeDriveLink} target="_blank" rel="noopener noreferrer">
                  <FileIcon /> Resume
                </a>
              )}
            </div>
          </div>
        )}

        <div className="info-section">
          <h3>Sends Outreach From</h3>
          {user.gmailUsername ? (
            <div className="info-row">
              <span className="info-value">{user.gmailUsername}</span>{' '}
              {user.gmailAppPasswordSet ? (
                <span className="status-pill ok">app password saved</span>
              ) : (
                <span className="status-pill warn">app password missing</span>
              )}
            </div>
          ) : (
            <div className="info-row text-muted">
              Not configured — set a Gmail address and app password to send outreach as this profile.
            </div>
          )}
          {user.lastUsedDisplay && (
            <div className="info-row">
              <span className="info-label">Last Used</span>
              <span className="info-value">{user.lastUsedDisplay}</span>
            </div>
          )}
        </div>
      </div>

      <div className="info-stack">
        {user.currentCompany && (
          <div className="info-section">
            <h3>Current Role</h3>
            <div className="info-row">
              <span className="info-value">
                {(user.currentDesignation || 'Employee') + ' at ' + user.currentCompany}
              </span>
            </div>
            {user.currentDurationDisplay && (
              <div className="info-row">
                <span className="info-label">Duration</span>
                <span className="info-value">{user.currentDurationDisplay}</span>
              </div>
            )}
            {user.currentSummary && (
              <div className="info-row">
                <span className="info-label">Summary</span>
                <span className="info-value">{user.currentSummary}</span>
              </div>
            )}
          </div>
        )}

        {user.previousCompany && (
          <div className="info-section">
            <h3>Previous Role</h3>
            <div className="info-row">
              <span className="info-value">
                {(user.previousDesignation || 'Employee') + ' at ' + user.previousCompany}
              </span>
            </div>
            {user.previousDurationDisplay && (
              <div className="info-row">
                <span className="info-label">Duration</span>
                <span className="info-value">{user.previousDurationDisplay}</span>
              </div>
            )}
            {user.previousSummary && (
              <div className="info-row">
                <span className="info-label">Summary</span>
                <span className="info-value">{user.previousSummary}</span>
              </div>
            )}
          </div>
        )}

        <div className="info-section">
          <h3>Profile</h3>
          {user.skills && (
            <div className="info-row">
              <span className="info-label">Technical Skills</span>
              <span className="info-value">{user.skills}</span>
            </div>
          )}
          {user.education && (
            <div className="info-row">
              <span className="info-label">Education</span>
              <span className="info-value">{user.education}</span>
            </div>
          )}
          {user.noticePeriod && (
            <div className="info-row">
              <span className="info-label">Notice Period</span>
              <span className="info-value">{user.noticePeriod}</span>
            </div>
          )}
          {user.expectedSalary && (
            <div className="info-row">
              <span className="info-label">Expected Salary</span>
              <span className="info-value">{user.expectedSalary}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
