import { NavLink, Route, Routes } from 'react-router-dom'
import EventListPage from './pages/EventListPage'
import EventDetailPage from './pages/EventDetailPage'
import AdminPage from './pages/AdminPage'

const today = new Date().toLocaleDateString([], {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

export default function App() {
  return (
    <div className="shell">
      <header className="masthead">
        <NavLink to="/" className="masthead__brand">
          <span className="masthead__kicker">The Assignment Register</span>
          <h1 className="masthead__title">
            Slot <em>Ledger</em>
          </h1>
        </NavLink>
        <nav className="masthead__nav">
          <NavLink to="/" end>
            Sessions
          </NavLink>
          <NavLink to="/admin">Admin</NavLink>
        </nav>
      </header>
      <div className="dateline">
        <span>Book a time · presentations &amp; consultations</span>
        <span>{today}</span>
      </div>

      <Routes>
        <Route path="/" element={<EventListPage />} />
        <Route path="/events/:id" element={<EventDetailPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Routes>

      <footer className="site-foot">
        <span className="site-foot__rule" />
        <span className="site-foot__text">
          Developed by <strong>Saptarshi + Claude</strong> 😜
        </span>
      </footer>
    </div>
  )
}
