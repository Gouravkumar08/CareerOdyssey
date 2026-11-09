import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { outreachApi } from '../../api/outreach'
import { templatesApi } from '../../api/templates'
import { usersApi } from '../../api/users'
import { companiesApi } from '../../api/companies'
import type { CompanyResponse, EmailTemplateResponse, OutreachResponse, OutreachSendRequest, UserResponse } from '../../api/types'
import { Spinner } from '../../components/Spinner'
import { EmailStatusModal } from '../../components/EmailStatusModal'

const emptyRequest: OutreachSendRequest = {
  userId: 0,
  companyIds: [],
  templateId: 0,
  jobTitle: '',
  jobUrl: '',
}

export function OutreachForm() {
  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const followUp = searchParams.get('followUp') === 'true'
  const companyIdParam = searchParams.get('companyId')
  const companyIdsParam = searchParams.get('companyIds')

  const [users, setUsers] = useState<UserResponse[]>([])
  const [companies, setCompanies] = useState<CompanyResponse[]>([])
  const [templates, setTemplates] = useState<EmailTemplateResponse[]>([])

  const [request, setRequest] = useState<OutreachSendRequest>(emptyRequest)
  const [resumeFile, setResumeFile] = useState<File | null>(null)

  const [loading, setLoading] = useState(true)
  const [sendStatus, setSendStatus] = useState<'idle' | 'sending' | 'sent'>('idle')
  const [error, setError] = useState<string | null>(null)
  const abortControllerRef = useRef<AbortController | null>(null)

  const submitting = sendStatus !== 'idle'
  const actionLabel = id ? (followUp ? 'Send Follow-up' : 'Resend') : 'Send Email'

  useEffect(() => {
    setLoading(true)
    setError(null)

    const optionsPromise = Promise.all([usersApi.list(), companiesApi.list(), templatesApi.list()])

    if (id) {
      Promise.all([optionsPromise, outreachApi.resendRequest(Number(id), followUp)])
        .then(([[u, c, t], prefill]) => {
          setUsers(u)
          setCompanies(c)
          setTemplates(t)
          setRequest(prefill)
        })
        .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load form data'))
        .finally(() => setLoading(false))
    } else {
      optionsPromise
        .then(([u, c, t]) => {
          setUsers(u)
          setCompanies(c)
          setTemplates(t)
          const preselected = companyIdsParam
            ? companyIdsParam.split(',').map(Number).filter((n) => !Number.isNaN(n))
            : companyIdParam
              ? [Number(companyIdParam)]
              : []
          setRequest({
            ...emptyRequest,
            companyIds: preselected,
          })
        })
        .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load form data'))
        .finally(() => setLoading(false))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, followUp, companyIdParam, companyIdsParam])

  function toggleCompany(companyId: number, checked: boolean) {
    setRequest((prev) => ({
      ...prev,
      companyIds: checked ? [...prev.companyIds, companyId] : prev.companyIds.filter((c) => c !== companyId),
    }))
  }

  function setAllCompanies(checked: boolean) {
    setRequest((prev) => ({
      ...prev,
      companyIds: checked ? companies.map((c) => c.id) : [],
    }))
  }

  // Sends one company per HTTP request, sequentially — not all companyIds in a single backend
  // call. The backend's /outreach/send loop has no way to notice a client abort mid-batch (it's
  // one synchronous request, and Spring doesn't check for disconnection between iterations), so
  // canceling used to have zero effect on a multi-company batch: every remaining company still
  // got emailed. Looping per-company here means Cancel actually stops the *next* companies from
  // being emailed — the one send already in flight at the moment of cancel may already be
  // delivered (an email can't be recalled once handed to the mail server), which is called out
  // explicitly below rather than silently claiming nothing was sent.
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSendStatus('sending')
    const results: OutreachResponse[] = []
    let canceled = false

    for (const companyId of request.companyIds) {
      const controller = new AbortController()
      abortControllerRef.current = controller
      try {
        const response = await outreachApi.send({ ...request, companyIds: [companyId] }, resumeFile, controller.signal)
        results.push(...response)
      } catch (err: any) {
        if (err?.code === 'ERR_CANCELED') {
          canceled = true
          break
        }
        setError(err?.response?.data?.message ?? 'Failed to send outreach')
        setSendStatus('idle')
        abortControllerRef.current = null
        return
      }
    }
    abortControllerRef.current = null

    if (canceled) {
      setSendStatus('idle')
      if (results.length > 0) {
        setError(
          `Canceled — ${results.length} of ${request.companyIds.length} email(s) had already been sent before you canceled and can't be undone. The rest were not sent.`,
        )
      }
      return
    }

    setSendStatus('sent')
    setTimeout(() => {
      navigate('/outreach', { state: { results } })
    }, 900)
  }

  function handleCancelSend() {
    abortControllerRef.current?.abort()
  }

  if (loading) {
    return (
      <div className="page" style={{ maxWidth: 720 }}>
        <Spinner />
      </div>
    )
  }

  return (
    <div className="page" style={{ maxWidth: 720 }}>
      <div className="page-header">
        <h1>{actionLabel}</h1>
        <a className="btn btn-secondary" href="/outreach">
          View history
        </a>
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Profile</label>
            <select
              required
              value={request.userId || ''}
              onChange={(e) => setRequest((prev) => ({ ...prev, userId: Number(e.target.value) }))}
            >
              <option value="">-- select --</option>
              {users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.name} ({user.email})
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label>Email Template</label>
            <select
              required
              value={request.templateId || ''}
              onChange={(e) => setRequest((prev) => ({ ...prev, templateId: Number(e.target.value) }))}
            >
              <option value="">-- select --</option>
              {templates.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.name}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label>Companies</label>
            <p className="hint" style={{ marginTop: 0 }}>
              Pick one to send a single email, or several to send the same email to all of them.
            </p>
            <fieldset>
              <legend>
                <a
                  href="#"
                  onClick={(e) => {
                    e.preventDefault()
                    setAllCompanies(true)
                  }}
                >
                  select all
                </a>{' '}
                ·{' '}
                <a
                  href="#"
                  onClick={(e) => {
                    e.preventDefault()
                    setAllCompanies(false)
                  }}
                >
                  clear
                </a>
              </legend>
              {companies.length > 0 ? (
                <div className="checkbox-list">
                  {companies.map((company) => (
                    <label className="checkbox-row" key={company.id}>
                      <input
                        type="checkbox"
                        checked={request.companyIds.includes(company.id)}
                        onChange={(e) => toggleCompany(company.id, e.target.checked)}
                      />
                      <span>{company.companyName}</span>
                      {company.location && <span className="text-muted"> · {company.location}</span>}
                      <span className="text-muted"> · {company.hrEmail ? 'HR email' : company.careerEmail ? 'career email' : 'no email'}</span>
                    </label>
                  ))}
                </div>
              ) : (
                <p className="empty-state">No companies yet — add one first.</p>
              )}
            </fieldset>
          </div>

          <div className="field">
            <label>Job Title (optional)</label>
            <input
              type="text"
              value={request.jobTitle ?? ''}
              onChange={(e) => setRequest((prev) => ({ ...prev, jobTitle: e.target.value }))}
            />
          </div>

          <div className="field">
            <label>Job URL (optional)</label>
            <input
              type="text"
              value={request.jobUrl ?? ''}
              onChange={(e) => setRequest((prev) => ({ ...prev, jobUrl: e.target.value }))}
            />
          </div>

          <div className="field">
            <label>Resume (optional)</label>
            <input
              type="file"
              accept=".pdf,.doc,.docx"
              onChange={(e) => setResumeFile(e.target.files?.[0] ?? null)}
            />
            <p className="hint">No resume is attached unless you upload one here.</p>
          </div>

          {request.companyIds.length > 0 && (
            <div className="field">
              <label>Sending to</label>
              <ul style={{ margin: 0, paddingLeft: '1.2rem' }}>
                {request.companyIds.map((companyId) => {
                  const company = companies.find((c) => c.id === companyId)
                  if (!company) return null
                  const email = company.hrEmail || company.careerEmail
                  return (
                    <li key={companyId}>
                      <strong>{company.companyName}</strong>
                      {' — '}
                      {email ? (
                        <>
                          {email} <span className="text-muted">({company.hrEmail ? 'HR' : 'career'})</span>
                        </>
                      ) : (
                        <span className="text-muted">no email on file</span>
                      )}
                    </li>
                  )
                })}
              </ul>
            </div>
          )}

          <div className="actions mt-1">
            <button className="btn btn-primary" type="submit" disabled={submitting}>
              {submitting ? 'Sending…' : actionLabel}
            </button>
          </div>
        </form>
      </div>

      {sendStatus !== 'idle' && (
        <EmailStatusModal
          status={sendStatus}
          sendingLabel={`${actionLabel === 'Send Follow-up' ? 'Sending follow-up' : actionLabel === 'Resend' ? 'Resending' : 'Sending'}…`}
          sentLabel={actionLabel === 'Send Follow-up' ? 'Follow-up sent!' : actionLabel === 'Resend' ? 'Resent!' : 'Sent!'}
          recipients={request.companyIds
            .map((companyId) => companies.find((c) => c.id === companyId))
            .filter((c): c is CompanyResponse => !!c)
            .map((c) => {
              const templateName = templates.find((t) => t.id === request.templateId)?.name
              return templateName ? `${templateName} to ${c.companyName}` : c.companyName
            })}
          onCancel={sendStatus === 'sending' ? handleCancelSend : undefined}
        />
      )}
    </div>
  )
}
