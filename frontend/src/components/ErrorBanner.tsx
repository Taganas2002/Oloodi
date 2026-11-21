export function ErrorBanner({
  message,
  onDismiss,
}: {
  message: string
  onDismiss?: () => void
}) {
  return (
    <div className="error-banner" role="alert" data-testid="error-banner">
      <p>{message}</p>
      {onDismiss ? (
        <button type="button" className="btn btn--ghost btn--sm" onClick={onDismiss}>
          Dismiss
        </button>
      ) : null}
    </div>
  )
}
