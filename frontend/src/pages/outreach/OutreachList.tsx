import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { outreachApi } from '../../api/outreach'
import type { OutreachResponse, ReplyStatus } from '../../api/types'
import { REPLY_STATUS_LABELS } from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Spinner } from '../../components/Spinner'
import { CardMenu } from '../../components/CardMenu'
import { TrashIcon } from '../../components/icons/ActionIcons'
import { avatarColorClass } from '../../utils/avatarColor'

const REPLY_STATUS_OPTIONS = Object.keys(REPLY_STATUS_LABELS).filter((s) => s !== 'SENT') as ReplyStatus[]
const NUDGE_THRESHOLD_DAYS = 15

function daysSince(iso: string) {
  return Math.floor((Date.now() - new Date(iso).getTime()) / (1000 * 60 * 60 * 24))
}

export function OutreachList() {
  const [records, setRecords] = useState<OutreachResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [confirmingDeleteAll, setConfirmingDeleteAll] = useState(false)
  const [activeTab, setActiveTab] = useState<'sent' | 'replied'>('sent')
  const [dismissedNudges, setDismissedNudges] = useState<number[]>([])
  const location = useLocation()
  const navigate = useNavigate()

  const sendResults = (location.state as { results?: OutreachResponse[] } | null)?.results ?? null

  function load() {
    setLoading(true)
    outreachApi
      .list()
      .then(setRecords)
      .catch((err) => setError(err?.response?.data?.message ?? 'Failed to load outreach history'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function dismissBanner() {
    navigate('.', { replace: true, state: null })
  }

  async function handleDelete(id: number) {
    setDeleteId(null)
    try {
      await outreachApi.remove(id)
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to delete record')
    }
  }

  async function handleDeleteAll() {
    setConfirmingDeleteAll(false)
    try {
      await outreachApi.removeAll()
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to delete all records')
    }
  }

  async function handleReplyStatusChange(id: number, replyStatus: ReplyStatus) {
    try {
      await outreachApi.updateReplyStatus(id, replyStatus)
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to update reply status')
    }
  }

  const pendingNudges = records.filter(
    (r) =>
      r.status === 'SENT' &&
      r.replyStatus === 'SENT' &&
      r.sentAt &&
      daysSince(r.sentAt) >= NUDGE_THRESHOLD_DAYS &&
      !dismissedNudges.includes(r.id),
  )

  const repliedRecords = records.filter((r) => r.replyStatus === 'GOT_REPLY')
  const visibleRecords = activeTab === 'sent' ? records.filter((r) => r.replyStatus !== 'GOT_REPLY') : repliedRecords

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Outreach History</h1>
          <p>Review and manage every cold email you've sent.</p>
        </div>
        <div className="actions">
          <Link className="btn btn-primary" to="/outreach/new">
            + Send Cold Email
          </Link>
          {records.length > 0 && (
            <button className="btn btn-danger" type="button" onClick={() => setConfirmingDeleteAll(true)}>
              Delete All
            </button>
          )}
        </div>
      </div>

      {confirmingDeleteAll && (
        <ConfirmDialog
          title="Delete all outreach history"
          message="Delete ALL outreach history? This cannot be undone."
          onConfirm={handleDeleteAll}
          onCancel={() => setConfirmingDeleteAll(false)}
        />
      )}

      {deleteId !== null && (
        <ConfirmDialog
          title="Delete outreach record"
          message="Delete this outreach record?"
          onConfirm={() => handleDelete(deleteId)}
          onCancel={() => setDeleteId(null)}
        />
      )}

      {sendResults && sendResults.length > 0 && (
        <div className="banner banner-success">
          <strong>{sendResults.length} outreach attempt(s) recorded</strong>
          <ul>
            {sendResults.map((r) => (
              <li key={r.id}>
                <span className={`badge badge-${r.status}`}>{r.status}</span> {r.companyName} —{' '}
                {r.recipientEmail ? r.recipientEmail : 'no recipient email'}
                {r.notes ? ` (${r.notes})` : ''}
              </li>
            ))}
          </ul>
          <button className="btn btn-secondary btn-sm" type="button" onClick={dismissBanner} style={{ marginTop: '0.75rem' }}>
            Dismiss
          </button>
        </div>
      )}

      {error && <div className="banner banner-error">{error}</div>}

      {pendingNudges.length > 0 && (
        <div className="banner banner-error">
          <strong>
            {pendingNudges.length} outreach{pendingNudges.length > 1 ? 's' : ''} sent {NUDGE_THRESHOLD_DAYS}+ days ago still
            need a reply status
          </strong>
          <ul style={{ listStyle: 'none', padding: 0, marginTop: '0.6rem' }}>
            {pendingNudges.map((r) => (
              <li
                key={r.id}
                style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.4rem', flexWrap: 'wrap' }}
              >
                <span>
                  Did you get a reply from <strong>{r.companyName}</strong>? ({r.sentAtDisplay})
                </span>
                <button className="btn btn-secondary btn-sm" type="button" onClick={() => handleReplyStatusChange(r.id, 'GOT_REPLY')}>
                  Yes, replied
                </button>
                <button className="btn btn-secondary btn-sm" type="button" onClick={() => handleReplyStatusChange(r.id, 'NO_REPLY')}>
                  No reply yet
                </button>
                <button className="btn btn-secondary btn-sm" type="button" onClick={() => handleReplyStatusChange(r.id, 'DEAD')}>
                  Mark dead
                </button>
                <button
                  className="btn btn-secondary btn-sm"
                  type="button"
                  onClick={() => setDismissedNudges((prev) => [...prev, r.id])}
                >
                  Ask me later
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {!loading && records.length > 0 && (
        <div className="page-stats">
          <div className="stat-card">
            <div className="stat-label">Total</div>
            <div className="stat-value">{records.length}</div>
          </div>
          <div className="stat-card accent-easy">
            <div className="stat-label">Sent</div>
            <div className="stat-value easy-text">{records.filter((r) => r.status === 'SENT').length}</div>
          </div>
          <div className="stat-card accent-easy">
            <div className="stat-label">Replied</div>
            <div className="stat-value easy-text">{repliedRecords.length}</div>
          </div>
          <div className="stat-card accent-hard">
            <div className="stat-label">Failed</div>
            <div className="stat-value hard-text">{records.filter((r) => r.status === 'FAILED').length}</div>
          </div>
        </div>
      )}

      {!loading && records.length > 0 && (
        <div className="tabs" style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
          <button
            className={`btn btn-sm ${activeTab === 'sent' ? 'btn-primary' : 'btn-secondary'}`}
            type="button"
            onClick={() => setActiveTab('sent')}
          >
            Sent ({records.filter((r) => r.replyStatus !== 'GOT_REPLY').length})
          </button>
          <button
            className={`btn btn-sm ${activeTab === 'replied' ? 'btn-primary' : 'btn-secondary'}`}
            type="button"
            onClick={() => setActiveTab('replied')}
          >
            Replied ({repliedRecords.length})
          </button>
        </div>
      )}

      {loading ? (
        <div className="card">
          <Spinner />
        </div>
      ) : records.length === 0 ? (
        <div className="card">
          <p className="empty-state">Nothing sent yet — send your first cold email to get started.</p>
        </div>
      ) : visibleRecords.length === 0 ? (
        <div className="card">
          <p className="empty-state">No outreach in this view yet.</p>
        </div>
      ) : (
        <div className="item-list">
          {visibleRecords.map((o, i) => (
            <div className="item-card stagger-in" style={{ '--stagger-index': i } as React.CSSProperties} key={o.id}>
              <div className={`avatar-circle ${avatarColorClass(o.companyName || 'company')}`}>
                {o.companyName ? o.companyName.substring(0, 1) : '?'}
              </div>
              <div className="item-main">
                <div className="item-title">{o.companyName}</div>
                <div className="item-meta">
                  <span>{o.recipientEmail}</span>
                  {o.sentAtDisplay && <span className="dot">{o.sentAtDisplay}</span>}
                  <span className="dot text-muted">Sent by {o.userName}</span>
                </div>
              </div>
              <div className="item-side">
                <div className="badge-row">
                  {o.templateName && <span className="badge badge-type">{o.templateName}</span>}
                  {o.status === 'SENT' ? (
                    <span className={`badge badge-reply-${o.replyStatus}`}>{REPLY_STATUS_LABELS[o.replyStatus]}</span>
                  ) : (
                    <span className={`badge badge-${o.status}`}>{o.status}</span>
                  )}
                </div>
                <div className="item-actions">
                  <Link className="btn btn-secondary btn-sm" to={`/outreach/${o.id}/prepare`}>
                    Resend
                  </Link>
                  <Link className="btn btn-secondary btn-sm" to={`/outreach/${o.id}/prepare?followUp=true`}>
                    Follow up
                  </Link>
                  <CardMenu
                    trigger="Reply status"
                    triggerClassName="btn btn-secondary btn-sm"
                    actions={REPLY_STATUS_OPTIONS.map((rs) => ({
                      label: REPLY_STATUS_LABELS[rs],
                      onClick: () => handleReplyStatusChange(o.id, rs),
                    }))}
                  />
                  <CardMenu actions={[{ label: 'Delete', icon: <TrashIcon />, danger: true, onClick: () => setDeleteId(o.id) }]} />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
