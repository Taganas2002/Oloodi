import { Link } from 'react-router-dom'

export function LandingPage() {
  return (
    <div className="landing">
      <div className="landing__panel">
        <p className="landing__brand">Oloodi Technologies</p>
        <h1>Live Staffing Dashboard</h1>
        <p className="landing__lede">
          Manage shift invitations and worker check-in for the demo event staffing shift.
        </p>
        <div className="landing__actions">
          <Link className="btn btn--primary" to="/manager">
            Manager view
          </Link>
          <Link className="btn btn--secondary" to="/worker">
            Worker view
          </Link>
        </div>
      </div>
    </div>
  )
}
