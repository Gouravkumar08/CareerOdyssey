import { useEffect, useState } from 'react'
import { quickLinksApi } from '../../api/quickLinks'
import type { QuickLinkResponse } from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'

function hostOf(url: string) {
  try {
    return new URL(url).hostname
  } catch {
    return ''
  }
}

function QuickLinkIcon({ link }: { link: QuickLinkResponse }) {
  const [failed, setFailed] = useState(false)
  const host = hostOf(link.url)
  return (
    <span className="quick-link-icon">
      {host && !failed ? (
        <img
          src={`https://www.google.com/s2/favicons?domain=${host}&sz=64`}
          alt=""
          onError={() => setFailed(true)}
        />
      ) : (
        <span className="quick-link-letter">{link.title.charAt(0).toUpperCase()}</span>
      )}
    </span>
  )
}

type LinkDialogProps = {
  link: QuickLinkResponse | null
  onSaved: () => Promise<void>
  onClose: () => void
}

function LinkDialog({ link, onSaved, onClose }: LinkDialogProps) {
  const [title, setTitle] = useState(link?.title ?? '')
  const [url, setUrl] = useState(link?.url ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!url.trim()) {
      setError('Enter a link')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const payload = { title: title.trim() || undefined, url: url.trim() }
      await (link ? quickLinksApi.update(link.id, payload) : quickLinksApi.create(payload))
      await onSaved()
      onClose()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save link')
      setBusy(false)
    }
  }

  return (
    <div className="modal-overlay" onMouseDown={onClose}>
      <form className="modal-dialog quick-link-dialog" onMouseDown={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3>{link ? 'Edit link' : 'Add link'}</h3>
        {error && <div className="banner banner-error">{error}</div>}
        <label className="quick-link-field">
          <span>Link</span>
          <input type="text" value={url} onChange={(e) => setUrl(e.target.value)} placeholder="example.com" autoFocus />
        </label>
        <label className="quick-link-field">
          <span>Name (optional)</span>
          <input type="text" value={title} maxLength={100} onChange={(e) => setTitle(e.target.value)} placeholder="Defaults to the site name" />
        </label>
        <div className="modal-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  )
}

/** Bookmark tiles on the dashboard: click to open, hover to edit or remove. */
export function QuickLinks() {
  const [links, setLinks] = useState<QuickLinkResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [dialog, setDialog] = useState<{ link: QuickLinkResponse | null } | null>(null)
  const [deleting, setDeleting] = useState<QuickLinkResponse | null>(null)

  function load() {
    return quickLinksApi
      .list()
      .then((l) => {
        setLinks(l)
        setError(null)
      })
      .catch(() => setError("Couldn't load your links."))
  }

  useEffect(() => {
    load()
  }, [])

  async function handleDelete(link: QuickLinkResponse) {
    try {
      await quickLinksApi.remove(link.id)
      await load()
    } catch {
      setError("Couldn't remove that link.")
    }
  }

  return (
    <section className="quick-links-section" aria-label="Quick links">
      {error && <div className="banner banner-error">{error}</div>}

      <div className="quick-links">
        {links.map((link) => (
          <div className="quick-link" key={link.id}>
            <a className="quick-link-anchor" href={link.url} target="_blank" rel="noopener noreferrer">
              <QuickLinkIcon link={link} />
              <span className="quick-link-title">{link.title}</span>
            </a>
            <div className="quick-link-actions">
              <button type="button" aria-label={`Edit ${link.title}`} title="Edit" onClick={() => setDialog({ link })}>
                ✎
              </button>
              <button type="button" aria-label={`Remove ${link.title}`} title="Remove" onClick={() => setDeleting(link)}>
                ×
              </button>
            </div>
          </div>
        ))}

        <button type="button" className="quick-link quick-link-add" onClick={() => setDialog({ link: null })}>
          <span className="quick-link-anchor">
            <span className="quick-link-icon">+</span>
            <span className="quick-link-title">Add link</span>
          </span>
        </button>
      </div>

      {dialog && <LinkDialog link={dialog.link} onSaved={load} onClose={() => setDialog(null)} />}

      {deleting && (
        <ConfirmDialog
          title="Remove link?"
          message={`"${deleting.title}" will be removed from your quick links.`}
          confirmLabel="Remove"
          onCancel={() => setDeleting(null)}
          onConfirm={() => {
            const link = deleting
            setDeleting(null)
            handleDelete(link)
          }}
        />
      )}
    </section>
  )
}
