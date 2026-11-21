import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { LandingPage } from './pages/LandingPage'
import { ManagerPage } from './pages/ManagerPage'
import { WorkerPage } from './pages/WorkerPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/manager" element={<ManagerPage />} />
        <Route path="/worker" element={<WorkerPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
