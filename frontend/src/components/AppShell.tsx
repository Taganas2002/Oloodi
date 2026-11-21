import { NavLink } from 'react-router-dom'
import type { ReactNode } from 'react'

export function AppShell({
  title,
  subtitle,
  children,
  layout = 'saas',
}: {
  title: string
  subtitle?: string
  children: ReactNode
  layout?: 'saas' | 'portal'
}) {
  return (
    <div className={`app-shell app-shell--${layout}`}>
      <header className="topbar">
        <div className="topbar__brand">
          <span className="topbar__mark">Oloodi</span>
          <span className="topbar__product">Live Staffing</span>
        </div>
        <nav className="topbar__nav" aria-label="Primary">
          <NavLink
            to="/manager"
            className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
          >
            Manager
          </NavLink>
          <NavLink
            to="/worker"
            className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
          >
            Worker
          </NavLink>
        </nav>
      </header>
      <main className={`page page--${layout}`}>
        <div className="page__intro">
          <h1>{title}</h1>
          {subtitle ? <p className="page__subtitle">{subtitle}</p> : null}
        </div>
        {children}
      </main>
    </div>
  )
}
