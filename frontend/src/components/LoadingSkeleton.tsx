export function LoadingSkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className="skeleton-stack" aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => (
        <div className="skeleton-row" key={i} />
      ))}
    </div>
  )
}
