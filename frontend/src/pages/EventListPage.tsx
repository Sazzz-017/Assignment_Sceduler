import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { ApiError, type EventSummary } from '../types'

export default function EventListPage() {
  const [events, setEvents] = useState<EventSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .listEvents()
      .then(setEvents)
      .catch((e: ApiError) => setError(e.message))
  }, [])

  if (error) {
    return <p className="notice notice--error">{error}</p>
  }

  if (!events) {
    return <p className="placeholder">Loading sessions…</p>
  }

  return (
    <section>
      <div className="section-head">
        <h2>Open sessions</h2>
        <span className="count">
          {events.length} {events.length === 1 ? 'entry' : 'entries'}
        </span>
      </div>

      {events.length === 0 ? (
        <div className="placeholder">
          <div className="placeholder__mark">∅</div>
          <p>No sessions have been opened yet. Check back soon.</p>
        </div>
      ) : (
        <div className="card-grid">
          {events.map((ev, i) => (
            <Link
              key={ev.id}
              to={`/events/${ev.id}`}
              className={`event-card type-${ev.type.toLowerCase()}`}
              style={{ animationDelay: `${i * 60}ms` }}
            >
              <span className="badge">{ev.type}</span>
              <h3 className="event-card__title">{ev.name}</h3>
              <p className="event-card__desc">{ev.description || 'Pick an available time to book.'}</p>
              <div className="event-card__foot">
                <div className="meter-label">
                  <span>
                    {ev.bookedCount} / {ev.slotCount} booked
                  </span>
                  <span>{ev.slotCount - ev.bookedCount} free</span>
                </div>
                <div className="meter">
                  <div
                    className="meter__fill"
                    style={{ width: ev.slotCount ? `${(ev.bookedCount / ev.slotCount) * 100}%` : '0%' }}
                  />
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}
