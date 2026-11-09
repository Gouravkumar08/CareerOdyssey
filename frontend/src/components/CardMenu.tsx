import { useEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

export type CardMenuAction = {
  label: string
  onClick: () => void
  danger?: boolean
  icon?: ReactNode
}

type Position = { top?: number; bottom?: number; right: number; maxHeight: number }

const VIEWPORT_MARGIN = 8
// Rough per-item height (padding + line height) used to decide whether the dropdown
// should open upward before it has actually been laid out.
const ESTIMATED_ITEM_HEIGHT = 40

export function CardMenu({
  actions,
  trigger,
  triggerClassName,
}: {
  actions: CardMenuAction[]
  trigger?: ReactNode
  triggerClassName?: string
}) {
  const [open, setOpen] = useState(false)
  const [position, setPosition] = useState<Position | null>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const dropdownRef = useRef<HTMLDivElement>(null)

  function openMenu() {
    const rect = triggerRef.current?.getBoundingClientRect()
    if (rect) {
      const estimatedHeight = actions.length * ESTIMATED_ITEM_HEIGHT + 16
      const spaceBelow = window.innerHeight - rect.bottom - VIEWPORT_MARGIN
      const spaceAbove = rect.top - VIEWPORT_MARGIN
      const openUpward = estimatedHeight > spaceBelow && spaceAbove > spaceBelow

      setPosition(
        openUpward
          ? { bottom: window.innerHeight - rect.top + 6, right: window.innerWidth - rect.right, maxHeight: spaceAbove }
          : { top: rect.bottom + 6, right: window.innerWidth - rect.right, maxHeight: spaceBelow },
      )
    }
    setOpen(true)
  }

  useEffect(() => {
    if (!open) return

    function handleClick(event: MouseEvent) {
      const target = event.target as Node
      if (
        !triggerRef.current?.contains(target) &&
        !dropdownRef.current?.contains(target)
      ) {
        setOpen(false)
      }
    }
    function handleDismiss() {
      setOpen(false)
    }

    document.addEventListener('mousedown', handleClick)
    // Any scroll (including inside a nested scroll container) or resize can move the
    // trigger out from under a fixed-position portal dropdown, so just close it rather
    // than track/reposition — this mirrors how native OS context menus behave.
    window.addEventListener('scroll', handleDismiss, true)
    window.addEventListener('resize', handleDismiss)
    return () => {
      document.removeEventListener('mousedown', handleClick)
      window.removeEventListener('scroll', handleDismiss, true)
      window.removeEventListener('resize', handleDismiss)
    }
  }, [open])

  return (
    <div className="card-menu">
      <button
        ref={triggerRef}
        type="button"
        className={triggerClassName ?? 'card-menu-trigger'}
        onClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
          if (open) {
            setOpen(false)
          } else {
            openMenu()
          }
        }}
        aria-label={trigger ? undefined : 'More options'}
        aria-expanded={open}
      >
        {trigger ?? (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <circle cx="5" cy="12" r="2" />
            <circle cx="12" cy="12" r="2" />
            <circle cx="19" cy="12" r="2" />
          </svg>
        )}
      </button>
      {open &&
        position &&
        createPortal(
          <div
            ref={dropdownRef}
            className="card-menu-dropdown"
            style={{
              top: position.top,
              bottom: position.bottom,
              right: position.right,
              maxHeight: position.maxHeight,
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {actions.map((action, i) => (
              <button
                key={i}
                type="button"
                className={`card-menu-item ${action.danger ? 'danger' : ''}`}
                onClick={() => {
                  setOpen(false)
                  action.onClick()
                }}
              >
                {action.icon}
                {action.label}
              </button>
            ))}
          </div>,
          document.body,
        )}
    </div>
  )
}
