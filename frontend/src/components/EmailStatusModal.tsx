type EmailStatusModalProps = {
  status: 'sending' | 'sent'
  sendingLabel?: string
  sentLabel?: string
  recipients?: string[]
  onCancel?: () => void
}

export function EmailStatusModal({
  status,
  sendingLabel = 'Sending…',
  sentLabel = 'Sent!',
  recipients,
  onCancel,
}: EmailStatusModalProps) {
  return (
    <div className="modal-overlay">
      <div className="modal-dialog email-status-dialog" role="status" aria-live="polite">
        {status === 'sending' ? (
          <>
            <span className="spinner" />
            <p>{sendingLabel}</p>
            {recipients && recipients.length > 0 && (
              <p className="text-muted email-status-recipients">{recipients.join(', ')}</p>
            )}
            {onCancel && (
              <button type="button" className="btn btn-secondary btn-sm" onClick={onCancel}>
                Cancel
              </button>
            )}
          </>
        ) : (
          <>
            <span className="email-status-check">✓</span>
            <p className="email-status-sent-label">{sentLabel}</p>
            {recipients && recipients.length > 0 && (
              <p className="text-muted email-status-recipients">{recipients.join(', ')}</p>
            )}
          </>
        )}
      </div>
    </div>
  )
}
