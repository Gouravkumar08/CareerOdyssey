import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { companiesApi } from '../../api/companies'
import type { ApiErrorBody, CompanyRequest } from '../../api/types'
import { Spinner } from '../../components/Spinner'

const emptyForm: CompanyRequest = {
  companyName: '',
  location: '',
  website: '',
  careerSite: '',
  careerEmail: '',
  hrEmail: '',
  hrName: '',
  companyDescription: '',
  linkedinUrl: '',
  notes: '',
}

export function CompanyForm() {
  const { id } = useParams<{ id: string }>()
  const isEdit = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState<CompanyRequest>(emptyForm)
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string> | null>(null)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    companiesApi
      .get(Number(id))
      .then((data) => {
        setForm({
          companyName: data.companyName ?? '',
          location: data.location ?? '',
          website: data.website ?? '',
          careerSite: data.careerSite ?? '',
          careerEmail: data.careerEmail ?? '',
          hrEmail: data.hrEmail ?? '',
          hrName: data.hrName ?? '',
          companyDescription: data.companyDescription ?? '',
          linkedinUrl: data.linkedinUrl ?? '',
          notes: data.notes ?? '',
        })
        setError(null)
      })
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setError(body?.message ?? 'Failed to load company.')
      })
      .finally(() => setLoading(false))
  }, [id])

  const handleChange = (field: keyof CompanyRequest) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setFieldErrors(null)

    const payload: CompanyRequest = {
      ...form,
      location: form.location || undefined,
      website: form.website || undefined,
      careerSite: form.careerSite || undefined,
      careerEmail: form.careerEmail || undefined,
      hrEmail: form.hrEmail || undefined,
      hrName: form.hrName || undefined,
      companyDescription: form.companyDescription || undefined,
      linkedinUrl: form.linkedinUrl || undefined,
      notes: form.notes || undefined,
    }

    const request = isEdit ? companiesApi.update(Number(id), payload) : companiesApi.create(payload)

    request
      .then((result) => navigate(`/companies/${result.id}`))
      .catch((err) => {
        const body = err?.response?.data as ApiErrorBody | undefined
        setError(body?.message ?? 'Failed to save company.')
        setFieldErrors(body?.fieldErrors ?? null)
      })
      .finally(() => setSaving(false))
  }

  if (loading) {
    return (
      <div className="page">
        <Spinner />
      </div>
    )
  }

  return (
    <div className="page" style={{ maxWidth: 720 }}>
      <div className="page-header">
        <h1>{isEdit ? 'Edit Company' : 'Add Company'}</h1>
      </div>

      <div className="card">
        {error && <div className="banner banner-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Company Name</label>
            <input type="text" value={form.companyName} onChange={handleChange('companyName')} required />
            {fieldErrors?.companyName && <p className="hint">{fieldErrors.companyName}</p>}
          </div>

          <div className="field">
            <label>Location</label>
            <input type="text" value={form.location} onChange={handleChange('location')} />
          </div>

          <div className="field">
            <label>Website</label>
            <input type="text" value={form.website} onChange={handleChange('website')} />
          </div>

          <div className="field">
            <label>Career Site</label>
            <input type="text" value={form.careerSite} onChange={handleChange('careerSite')} />
          </div>

          <div className="field">
            <label>Career Email</label>
            <input type="email" value={form.careerEmail} onChange={handleChange('careerEmail')} />
            {fieldErrors?.careerEmail && <p className="hint">{fieldErrors.careerEmail}</p>}
          </div>

          <div className="field">
            <label>HR Email</label>
            <input type="email" value={form.hrEmail} onChange={handleChange('hrEmail')} />
            <p className="hint">Outreach automatically uses HR Email when set, otherwise falls back to Career Email.</p>
            {fieldErrors?.hrEmail && <p className="hint">{fieldErrors.hrEmail}</p>}
          </div>

          <div className="field">
            <label>HR Name</label>
            <input type="text" value={form.hrName} onChange={handleChange('hrName')} />
          </div>

          <div className="field">
            <label>Company Description</label>
            <textarea rows={3} value={form.companyDescription} onChange={handleChange('companyDescription')} />
          </div>

          <div className="field">
            <label>LinkedIn URL</label>
            <input type="text" value={form.linkedinUrl} onChange={handleChange('linkedinUrl')} />
          </div>

          <div className="field">
            <label>Notes</label>
            <textarea rows={3} value={form.notes} onChange={handleChange('notes')} />
          </div>

          <div className="actions mt-1">
            <button className="btn btn-primary" type="submit" disabled={saving}>
              Save
            </button>
            <Link className="btn btn-secondary" to="/companies">
              Cancel
            </Link>
          </div>
        </form>
      </div>
    </div>
  )
}
