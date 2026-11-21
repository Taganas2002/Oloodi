import { useEffect } from 'react'

export type ToastTone = 'success' | 'info' | 'danger'

export interface ToastMessage {
  id: number
  text: string
  tone: ToastTone
}

export function ToastStack({
  toasts,
  onDismiss,
}: {
  toasts: ToastMessage[]
  onDismiss: (id: number) => void
}) {
  return (
    <div className="toast-stack" aria-live="polite" aria-relevant="additions">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onDismiss={onDismiss} />
      ))}
    </div>
  )
}

function ToastItem({
  toast,
  onDismiss,
}: {
  toast: ToastMessage
  onDismiss: (id: number) => void
}) {
  useEffect(() => {
    const timer = window.setTimeout(() => onDismiss(toast.id), 4200)
    return () => window.clearTimeout(timer)
  }, [toast.id, onDismiss])

  return (
    <div className={`toast toast--${toast.tone}`} role="status" data-testid="toast">
      <p className="toast__text">{toast.text}</p>
      <button
        type="button"
        className="toast__dismiss"
        aria-label="Dismiss notification"
        onClick={() => onDismiss(toast.id)}
      >
        ×
      </button>
    </div>
  )
}
