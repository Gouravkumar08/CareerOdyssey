type SpinnerProps = {
  label?: string
  size?: 'sm' | 'md'
}

export function Spinner({ label, size = 'md' }: SpinnerProps) {
  return (
    <div className="loading-state">
      <span className={`spinner ${size === 'sm' ? 'spinner-sm' : ''}`} />
      {label && <span>{label}</span>}
    </div>
  )
}
