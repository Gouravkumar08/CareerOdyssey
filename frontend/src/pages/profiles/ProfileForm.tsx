import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { usersApi } from '../../api/users'
import type { ApiErrorBody, UserRequest } from '../../api/types'
import { Spinner } from '../../components/Spinner'

const emptyForm: UserRequest = {
  name: '',
  email: '',
  phone: '',
  location: '',
  linkedinUrl: '',
  githubUrl: '',
  portfolioUrl: '',
  skills: '',
  education: '',
  noticePeriod: '',
  expectedSalary: '',
  currentCompany: '',
  currentDesignation: '',
  currentStartDate: '',
  currentEndDate: '',
  currentSummary: '',
  previousCompany: '',
  previousDesignation: '',
  previousStartDate: '',
  previousEndDate: '',
  previousSummary: '',
  resumeDriveLink: '',
  gmailUsername: '',
  gmailAppPassword: '',
}

export function ProfileForm() {
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id
  const navigate = useNavigate()

  const [form, setForm] = useState<UserRequest>(emptyForm)
  const [currentlyWorking, setCurrentlyWorking] = useState(true)
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string> | null>(null)
  const [parseNotice, setParseNotice] = useState<string | null>(null)
  const [parseError, setParseError] = useState<string | null>(null)
  const [parsing, setParsing] = useState(false)

  useEffect(() => {
    if (!isEdit || !id) return
    setLoading(true)
    usersApi
      .get(Number(id))
      .then((data) => {
        setForm({
          name: data.name ?? '',
          email: data.email ?? '',
          phone: data.phone ?? '',
          location: data.location ?? '',
          linkedinUrl: data.linkedinUrl ?? '',
          githubUrl: data.githubUrl ?? '',
          portfolioUrl: data.portfolioUrl ?? '',
          skills: data.skills ?? '',
          education: data.education ?? '',
          noticePeriod: data.noticePeriod ?? '',
          expectedSalary: data.expectedSalary ?? '',
          currentCompany: data.currentCompany ?? '',
          currentDesignation: data.currentDesignation ?? '',
          currentStartDate: data.currentStartDate ?? '',
          currentEndDate: data.currentEndDate ?? '',
          currentSummary: data.currentSummary ?? '',
          previousCompany: data.previousCompany ?? '',
          previousDesignation: data.previousDesignation ?? '',
          previousStartDate: data.previousStartDate ?? '',
          previousEndDate: data.previousEndDate ?? '',
          previousSummary: data.previousSummary ?? '',
          resumeDriveLink: data.resumeDriveLink ?? '',
          gmailUsername: data.gmailUsername ?? '',
          gmailAppPassword: '',
        })
        setCurrentlyWorking(!data.currentEndDate)
        setError(null)
      })
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setError(body?.message ?? 'Failed to load profile.')
      })
      .finally(() => setLoading(false))
  }, [id, isEdit])

  const updateField = <K extends keyof UserRequest>(key: K, value: UserRequest[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const handleResumeSelected = (file: File | null) => {
    if (!file) return
    setParsing(true)
    setParseError(null)
    setParseNotice(null)
    usersApi
      .parseResume(file)
      .then((parsed) => {
        setForm((prev) => ({
          ...prev,
          ...Object.fromEntries(
            Object.entries(parsed).filter(([, v]) => v !== null && v !== undefined),
          ),
        }))
        setCurrentlyWorking(!parsed.currentEndDate)
        setParseNotice(
          'Resume parsed — fields have been pre-filled below. Review everything before saving.',
        )
      })
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setParseError(body?.message ?? 'Failed to parse resume.')
      })
      .finally(() => setParsing(false))
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setFieldErrors(null)

    const payload: UserRequest = {
      ...form,
      currentEndDate: currentlyWorking ? undefined : form.currentEndDate || undefined,
    }

    const request = isEdit ? usersApi.update(Number(id), payload) : usersApi.create(payload)

    request
      .then((data) => navigate(`/profiles/${data.id}`))
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setError(body?.message ?? 'Failed to save profile.')
        setFieldErrors(body?.fieldErrors ?? null)
      })
      .finally(() => setSaving(false))
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
        <h1>{isEdit ? 'Edit Profile' : 'Add Profile'}</h1>
      </div>

      {parseNotice && <div className="banner banner-success">{parseNotice}</div>}
      {parseError && <div className="banner banner-error">{parseError}</div>}
      {error && <div className="banner banner-error">{error}</div>}

      {!isEdit && (
        <div className="card mt-1">
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: '0.75rem', flexWrap: 'wrap' }}>
            <div className="field" style={{ flex: 1, minWidth: 220, marginBottom: 0 }}>
              <label>Upload resume to auto-fill (optional)</label>
              <input
                type="file"
                accept=".pdf"
                disabled={parsing}
                onChange={(e) => handleResumeSelected(e.target.files?.[0] ?? null)}
              />
            </div>
          </div>
          <p className="hint" style={{ marginBottom: 0 }}>
            PDF only. Pulls out contact info, skills, education, and a best guess at current/previous
            role — always review before saving, especially dates.
          </p>
        </div>
      )}

      <div className="card mt-1">
        <form onSubmit={handleSubmit}>
          <fieldset className="mt-1">
            <legend>Basic details</legend>
            <div className="field">
              <label>Name</label>
              <input
                type="text"
                required
                value={form.name}
                onChange={(e) => updateField('name', e.target.value)}
              />
              {fieldErrors?.name && <p className="hint">{fieldErrors.name}</p>}
            </div>
            <div className="field">
              <label>Email</label>
              <input
                type="email"
                required
                value={form.email}
                onChange={(e) => updateField('email', e.target.value)}
              />
              {fieldErrors?.email && <p className="hint">{fieldErrors.email}</p>}
            </div>
            <div className="field">
              <label>Phone</label>
              <input type="text" value={form.phone} onChange={(e) => updateField('phone', e.target.value)} />
            </div>
            <div className="field">
              <label>Location</label>
              <input
                type="text"
                value={form.location}
                onChange={(e) => updateField('location', e.target.value)}
              />
            </div>
          </fieldset>

          <fieldset className="mt-1">
            <legend>Current role</legend>
            <p className="hint" style={{ marginTop: 0 }}>
              Fill these once — every outreach email can reference them, current role first.
            </p>
            <div className="field">
              <label>Current Designation</label>
              <input
                type="text"
                placeholder="Software Engineer Trainee"
                value={form.currentDesignation}
                onChange={(e) => updateField('currentDesignation', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Current Company</label>
              <input
                type="text"
                placeholder="Xcelore"
                value={form.currentCompany}
                onChange={(e) => updateField('currentCompany', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Start Month</label>
              <input
                type="month"
                value={form.currentStartDate ?? ''}
                onChange={(e) => updateField('currentStartDate', e.target.value)}
              />
            </div>
            <div className="field">
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 'normal' }}>
                <input
                  type="checkbox"
                  checked={currentlyWorking}
                  onChange={(e) => {
                    setCurrentlyWorking(e.target.checked)
                    if (e.target.checked) updateField('currentEndDate', '')
                  }}
                  style={{ width: 'auto' }}
                />
                I currently work here
              </label>
            </div>
            {!currentlyWorking && (
              <div className="field">
                <label>End Month</label>
                <input
                  type="month"
                  value={form.currentEndDate ?? ''}
                  onChange={(e) => updateField('currentEndDate', e.target.value)}
                />
              </div>
            )}
            <div className="field">
              <label>Current Role Summary</label>
              <textarea
                rows={3}
                placeholder="What you do in this role..."
                value={form.currentSummary}
                onChange={(e) => updateField('currentSummary', e.target.value)}
              />
            </div>
          </fieldset>

          <fieldset className="mt-1">
            <legend>Previous role</legend>
            <div className="field">
              <label>Previous Designation</label>
              <input
                type="text"
                placeholder="Contract Java Developer"
                value={form.previousDesignation}
                onChange={(e) => updateField('previousDesignation', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Previous Company</label>
              <input
                type="text"
                placeholder="Naukri.com (Info Edge India Ltd.)"
                value={form.previousCompany}
                onChange={(e) => updateField('previousCompany', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Start Month</label>
              <input
                type="month"
                value={form.previousStartDate ?? ''}
                onChange={(e) => updateField('previousStartDate', e.target.value)}
              />
            </div>
            <div className="field">
              <label>End Month</label>
              <input
                type="month"
                value={form.previousEndDate ?? ''}
                onChange={(e) => updateField('previousEndDate', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Previous Role Summary</label>
              <textarea
                rows={3}
                placeholder="What you did in this role..."
                value={form.previousSummary}
                onChange={(e) => updateField('previousSummary', e.target.value)}
              />
            </div>
          </fieldset>

          <fieldset className="mt-1">
            <legend>Profile</legend>
            <div className="field">
              <label>Technical Skills</label>
              <textarea
                rows={2}
                placeholder="Java, Spring Boot, MySQL, Kafka..."
                value={form.skills}
                onChange={(e) => updateField('skills', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Education</label>
              <textarea
                rows={2}
                value={form.education}
                onChange={(e) => updateField('education', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Notice Period</label>
              <input
                type="text"
                value={form.noticePeriod}
                onChange={(e) => updateField('noticePeriod', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Expected Salary (optional)</label>
              <input
                type="text"
                value={form.expectedSalary}
                onChange={(e) => updateField('expectedSalary', e.target.value)}
              />
            </div>
          </fieldset>

          <fieldset className="mt-1">
            <legend>Gmail sending credentials</legend>
            <p className="hint" style={{ marginTop: 0 }}>
              Outreach for this profile is sent from this Gmail account. Use an{' '}
              <a href="https://myaccount.google.com/apppasswords" target="_blank" rel="noopener noreferrer">
                app password
              </a>
              , never the real account password.
            </p>
            <div className="field">
              <label>Gmail Address</label>
              <input
                type="email"
                placeholder="you@gmail.com"
                value={form.gmailUsername}
                onChange={(e) => updateField('gmailUsername', e.target.value)}
              />
            </div>
            <div className="field">
              <label>
                Gmail App Password
                <span className="info-tooltip" tabIndex={0}>
                  i
                  <span className="tooltip-text">
                    <strong>What it is:</strong> a 16-character password Google generates specifically
                    for apps like this one — not your real Gmail password.
                    <br />
                    <br />
                    <strong>Why it&apos;s required:</strong> Gmail blocks sign-in from apps like this
                    using your real password. An app password is the supported way to let this app send
                    email through your account.
                    <br />
                    <br />
                    <strong>How to create one:</strong>
                    <ol>
                      <li>Turn on 2-Step Verification on your Google Account (required first).</li>
                      <li>
                        Go to <em>myaccount.google.com/apppasswords</em>.
                      </li>
                      <li>Name it (e.g. &quot;CareerOdyssey&quot;) and click Create.</li>
                      <li>Copy the 16-character password shown and paste it here.</li>
                    </ol>
                  </span>
                </span>
              </label>
              <input
                type="password"
                autoComplete="new-password"
                placeholder="xxxx xxxx xxxx xxxx"
                value={form.gmailAppPassword}
                onChange={(e) => updateField('gmailAppPassword', e.target.value)}
              />
              {isEdit && <p className="hint">Leave blank to keep the currently saved app password.</p>}
            </div>
          </fieldset>

          <fieldset className="mt-1">
            <legend>Links & resume</legend>
            <div className="field">
              <label>LinkedIn URL</label>
              <input
                type="text"
                value={form.linkedinUrl}
                onChange={(e) => updateField('linkedinUrl', e.target.value)}
              />
            </div>
            <div className="field">
              <label>GitHub URL</label>
              <input
                type="text"
                value={form.githubUrl}
                onChange={(e) => updateField('githubUrl', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Portfolio URL</label>
              <input
                type="text"
                value={form.portfolioUrl}
                onChange={(e) => updateField('portfolioUrl', e.target.value)}
              />
            </div>
            <div className="field">
              <label>Resume Google Drive Link (optional)</label>
              <input
                type="text"
                placeholder="https://drive.google.com/..."
                value={form.resumeDriveLink}
                onChange={(e) => updateField('resumeDriveLink', e.target.value)}
              />
            </div>
          </fieldset>

          <div className="actions mt-1">
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? 'Saving...' : 'Save'}
            </button>
            <button className="btn btn-secondary" type="button" onClick={() => navigate('/profiles')}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
