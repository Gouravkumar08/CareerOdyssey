import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { companiesApi } from '../../api/companies'
import type { ApiErrorBody, CompanyResponse } from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Spinner } from '../../components/Spinner'
import { LinkedinIcon, LinkIcon } from '../../components/icons/BrandIcons'
import { avatarColorClass } from '../../utils/avatarColor'

export function CompanyView() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [company, setCompany] = useState<CompanyResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    companiesApi
      .get(Number(id))
      .then((data) => {
        setCompany(data)
        setError(null)
      })
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setError(body?.message ?? 'Failed to load company.')
      })
      .finally(() => setLoading(false))
  }, [id])

  const handleDelete = () => {
    if (!company) return
    setConfirmingDelete(false)
    companiesApi
      .remove(company.id)
      .then(() => navigate('/companies'))
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setDeleteError(body?.message ?? 'Failed to delete company.')
      })
  }

  if (loading) {
    return (
      <div className="page">
        <Spinner />
      </div>
    )
  }

  if (error || !company) {
    return (
      <div className="page">
        <div className="banner banner-error">{error ?? 'Company not found.'}</div>
      </div>
    )
  }

  const hasContactEmails = Boolean(company.hrEmail || company.careerEmail)
  const hasLinks = Boolean(company.website || company.careerSite || company.linkedinUrl)

  return (
    <div className="page">
      {deleteError && <div className="banner banner-error">{deleteError}</div>}

      <div className="page-header">
        <div className="profile-header" style={{ marginBottom: 0 }}>
          <div className={`avatar-circle ${avatarColorClass(company.companyName || 'company')}`}>
            {company.companyName ? company.companyName.substring(0, 1) : '?'}
          </div>
          <div className="identity">
            <h1>{company.companyName}</h1>
            {company.location && <p>{company.location}</p>}
          </div>
        </div>
        <div className="actions">
          <Link className="btn btn-primary" to={`/outreach/new?companyId=${company.id}`}>
            Send Email
          </Link>
          <Link className="btn btn-secondary" to={`/companies/${company.id}/edit`}>
            Edit
          </Link>
          <button className="btn btn-danger" type="button" onClick={() => setConfirmingDelete(true)}>
            Delete
          </button>
        </div>
      </div>

      {confirmingDelete && (
        <ConfirmDialog
          title="Delete company"
          message="Delete this company? This cannot be undone."
          onConfirm={handleDelete}
          onCancel={() => setConfirmingDelete(false)}
        />
      )}

      <div className="info-grid">
        {hasContactEmails && (
          <div className="info-section">
            <h3>Contact Emails</h3>
            {company.hrEmail && (
              <div className="info-row">
                <span className="info-label">HR Email</span>
                <span className="info-value">{company.hrEmail}</span>
              </div>
            )}
            {company.hrName && (
              <div className="info-row">
                <span className="info-label">HR Name</span>
                <span className="info-value">{company.hrName}</span>
              </div>
            )}
            {company.careerEmail && (
              <div className="info-row">
                <span className="info-label">Career Email</span>
                <span className="info-value">{company.careerEmail}</span>
              </div>
            )}
          </div>
        )}

        {hasLinks && (
          <div className="info-section">
            <h3>Links</h3>
            <div className="link-chip-list">
              {company.website && (
                <a className="link-chip" href={company.website} target="_blank" rel="noreferrer">
                  <LinkIcon /> Website
                </a>
              )}
              {company.careerSite && (
                <a className="link-chip" href={company.careerSite} target="_blank" rel="noreferrer">
                  <LinkIcon /> Career Site
                </a>
              )}
              {company.linkedinUrl && (
                <a className="link-chip" href={company.linkedinUrl} target="_blank" rel="noreferrer">
                  <LinkedinIcon className="icon-linkedin" /> LinkedIn
                </a>
              )}
            </div>
          </div>
        )}

        {company.companyDescription && (
          <div className="info-section">
            <h3>Description</h3>
            <div className="info-row">
              <span className="info-value">{company.companyDescription}</span>
            </div>
          </div>
        )}

        {company.notes && (
          <div className="info-section">
            <h3>Notes</h3>
            <div className="info-row">
              <span className="info-value">{company.notes}</span>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
