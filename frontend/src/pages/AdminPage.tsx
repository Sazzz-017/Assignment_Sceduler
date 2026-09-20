import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { adminApi, api } from '../api'
import { ApiError, type EventDetail, type EventSummary, type EventType } from '../types'
import { fmtDateTimeFull, localInputToIso } from '../lib/format'

const PASS_KEY = 'slot-ledger.admin-pass'

export default function AdminPage() {
  const [passcode, setPasscode] = useState<string>(() => sessionStorage.getItem(PASS_KEY) ?? '')
  const [authed, setAuthed] = useState(false)
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    if (!passcode) {
      setChecking(false)
      return
    }
    adminApi
      .ping(passcode)
      .then(() => setAuthed(true))
      .catch(() => {
        sessionStorage.removeItem(PASS_KEY)
        setPasscode('')
      })
      .finally(() => setChecking(false))
  }, [passcode])

  if (checking) {
    return <p className="placeholder">Checking access…</p>
  }

  if (!authed) {
    return (
      <Gate
        onUnlock={(p) => {
          sessionStorage.setItem(PASS_KEY, p)
          setPasscode(p)
          setAuthed(true)
        }}
      />
    )
  }

  return (
    <Dashboard
      passcode={passcode}
      onSignOut={() => {
        sessionStorage.removeItem(PASS_KEY)
        setPasscode('')
        setAuthed(false)
      }}
    />
  )
}

/* ---------- Passcode gate ---------- */
function Gate({ onUnlock }: { onUnlock: (passcode: string) => void }) {
  const [value, setValue] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await adminApi.ping(value)
      onUnlock(value)
    } catch (e) {
      const err = e as ApiError
      setError(err.status === 401 ? 'Incorrect passcode.' : err.message)
      setBusy(false)
    }
  }

  return (
    <div className="gate">
      <div className="section-head">
        <h2>Instructor access</h2>
      </div>
      <div className="panel">
        <p className="modal__sub">Enter the admin passcode to manage sessions and slots.</p>
        {error && <p className="notice notice--error">{error}</p>}
        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="pass">Passcode</label>
            <input
              id="pass"
              type="password"
              value={value}
              autoFocus
              onChange={(e) => setValue(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn--primary" disabled={busy}>
            {busy ? 'Unlocking…' : 'Unlock'}
          </button>
        </form>
      </div>
    </div>
  )
}

/* ---------- Dashboard ---------- */
function Dashboard({ passcode, onSignOut }: { passcode: string; onSignOut: () => void }) {
  const [events, setEvents] = useState<EventSummary[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [detail, setDetail] = useState<EventDetail | null>(null)
  const [toast, setToast] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const refreshEvents = useCallback(() => {
    return api.listEvents().then(setEvents).catch((e: ApiError) => setError(e.message))
  }, [])

  const selectEvent = useCallback((id: number) => {
    setSelectedId(id)
    return api.getEvent(id).then(setDetail).catch((e: ApiError) => setError(e.message))
  }, [])

  useEffect(() => {
    refreshEvents()
  }, [refreshEvents])

  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(null), 2600)
    return () => clearTimeout(t)
  }, [toast])

  function flash(message: string) {
    setToast(message)
  }

  async function afterMutation(keepId: number | null) {
    await refreshEvents()
    if (keepId) await selectEvent(keepId)
  }

  return (
    <section>
      <div className="section-head">
        <h2>Manage sessions</h2>
        <span className="count">
          <button className="btn btn--ghost btn--sm" onClick={onSignOut}>
            Sign out
          </button>
        </span>
      </div>

      {error && <p className="notice notice--error">{error}</p>}

      <div className="admin-grid">
        <div>
          <div className="panel">
            <h3>New session</h3>
            <CreateEventForm
              passcode={passcode}
              onCreated={async (ev) => {
                flash('Session created.')
                await refreshEvents()
                await selectEvent(ev.id)
              }}
              onError={setError}
            />
          </div>

          <div style={{ height: 18 }} />

          <div className="panel">
            <h3>Sessions</h3>
            {events.length === 0 ? (
              <p className="modal__sub">None yet.</p>
            ) : (
              <div className="event-pick">
                {events.map((ev) => (
                  <button
                    key={ev.id}
                    className={ev.id === selectedId ? 'is-active' : ''}
                    onClick={() => selectEvent(ev.id)}
                  >
                    <span>{ev.name}</span>
                    <span className="count">
                      {ev.bookedCount}/{ev.slotCount}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="panel">
          {!detail ? (
            <p className="modal__sub">Select a session to manage its slots.</p>
          ) : (
            <EventEditor
              passcode={passcode}
              detail={detail}
              onFlash={flash}
              onError={setError}
              onChanged={() => afterMutation(detail.id)}
              onDeleted={async () => {
                flash('Session deleted.')
                setDetail(null)
                setSelectedId(null)
                await refreshEvents()
              }}
            />
          )}
        </div>
      </div>

      {toast && <div className="toast">{toast}</div>}
    </section>
  )
}

/* ---------- Create event ---------- */
function CreateEventForm({
  passcode,
  onCreated,
  onError,
}: {
  passcode: string
  onCreated: (ev: EventDetail) => void | Promise<void>
  onError: (m: string) => void
}) {
  const [name, setName] = useState('')
  const [type, setType] = useState<EventType>('PRESENTATION')
  const [description, setDescription] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    setBusy(true)
    try {
      const ev = await adminApi.createEvent(passcode, {
        name: name.trim(),
        type,
        description: description.trim() || null,
      })
      setName('')
      setDescription('')
      setType('PRESENTATION')
      await onCreated(ev)
    } catch (e) {
      onError((e as ApiError).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit}>
      <div className="field">
        <label htmlFor="ev-name">Title</label>
        <input id="ev-name" value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Final Demos" />
      </div>
      <div className="field">
        <label htmlFor="ev-type">Type</label>
        <select id="ev-type" value={type} onChange={(e) => setType(e.target.value as EventType)}>
          <option value="PRESENTATION">Presentation / demo</option>
          <option value="CONSULTATION">Consultation / meeting</option>
        </select>
      </div>
      <div className="field">
        <label htmlFor="ev-desc">Description (optional)</label>
        <textarea id="ev-desc" value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
      <button type="submit" className="btn btn--primary" disabled={busy}>
        {busy ? 'Creating…' : 'Create session'}
      </button>
    </form>
  )
}

/* ---------- Event editor ---------- */
function EventEditor({
  passcode,
  detail,
  onFlash,
  onError,
  onChanged,
  onDeleted,
}: {
  passcode: string
  detail: EventDetail
  onFlash: (m: string) => void
  onError: (m: string) => void
  onChanged: () => void | Promise<void>
  onDeleted: () => void | Promise<void>
}) {
  const [start, setStart] = useState('')
  const [duration, setDuration] = useState('15')
  const [label, setLabel] = useState('')
  const [busy, setBusy] = useState(false)

  async function addSlot(e: FormEvent) {
    e.preventDefault()
    if (!start) {
      onError('Pick a start date & time for the slot.')
      return
    }
    const mins = Number(duration)
    if (!Number.isFinite(mins) || mins <= 0) {
      onError('Duration must be a positive number of minutes.')
      return
    }
    setBusy(true)
    try {
      await adminApi.addSlot(passcode, detail.id, {
        startTime: localInputToIso(start),
        durationMinutes: mins,
        label: label.trim() || null,
      })
      setStart('')
      setLabel('')
      onFlash('Slot added.')
      await onChanged()
    } catch (e) {
      onError((e as ApiError).message)
    } finally {
      setBusy(false)
    }
  }

  async function deleteSlot(slotId: number) {
    if (!window.confirm('Delete this slot? Any booking on it is removed too.')) return
    try {
      await adminApi.deleteSlot(passcode, slotId)
      onFlash('Slot deleted.')
      await onChanged()
    } catch (e) {
      onError((e as ApiError).message)
    }
  }

  async function clearBooking(bookingId: number) {
    if (!window.confirm('Clear this booking? The slot becomes free for others.')) return
    try {
      await adminApi.clearBooking(passcode, bookingId)
      onFlash('Booking cleared.')
      await onChanged()
    } catch (e) {
      onError((e as ApiError).message)
    }
  }

  async function deleteEvent() {
    if (!window.confirm(`Delete “${detail.name}” and all its slots and bookings?`)) return
    try {
      await adminApi.deleteEvent(passcode, detail.id)
      await onDeleted()
    } catch (e) {
      onError((e as ApiError).message)
    }
  }

  return (
    <div className={`type-${detail.type.toLowerCase()}`}>
      <div className="row-actions" style={{ justifyContent: 'space-between' }}>
        <div>
          <span className="badge">{detail.type}</span>
          <h3 style={{ marginTop: 10 }}>{detail.name}</h3>
        </div>
        <div className="row-actions">
          <button
            className="btn btn--ghost btn--sm"
            onClick={() => adminApi.downloadCsv(passcode, detail.id, `${detail.name}-bookings.csv`).catch((e) => onError((e as ApiError).message))}
          >
            Export CSV
          </button>
          <button className="btn btn--danger btn--sm" onClick={deleteEvent}>
            Delete session
          </button>
        </div>
      </div>

      <div className="divider" />

      <h3 style={{ fontSize: 16 }}>Add a slot</h3>
      <form onSubmit={addSlot}>
        <div className="field">
          <label htmlFor="sl-start">Start (date &amp; time)</label>
          <input id="sl-start" type="datetime-local" value={start} onChange={(e) => setStart(e.target.value)} />
        </div>
        <div className="row-actions">
          <div className="field" style={{ flex: 1 }}>
            <label htmlFor="sl-dur">Minutes</label>
            <input id="sl-dur" type="number" min={1} value={duration} onChange={(e) => setDuration(e.target.value)} />
          </div>
          <div className="field" style={{ flex: 2 }}>
            <label htmlFor="sl-label">Label (optional)</label>
            <input id="sl-label" value={label} onChange={(e) => setLabel(e.target.value)} placeholder="Room / notes" />
          </div>
        </div>
        <button type="submit" className="btn btn--primary" disabled={busy}>
          {busy ? 'Adding…' : 'Add slot'}
        </button>
      </form>

      <div className="divider" />

      <h3 style={{ fontSize: 16 }}>
        Slots <span className="count">({detail.slots.length})</span>
      </h3>
      {detail.slots.length === 0 ? (
        <p className="modal__sub">No slots yet — add one above.</p>
      ) : (
        detail.slots.map((slot) => (
          <div className="admin-slot" key={slot.id}>
            <span className="t">{fmtDateTimeFull(slot.startTime)}</span>
            <span>
              {slot.label && <span className="slot-label">{slot.label} · </span>}
              {slot.booked ? (
                <strong>{slot.bookerName}</strong>
              ) : (
                <span className="slot-state">free</span>
              )}
              {slot.meetingLink && (
                <a className="meet-link" href={slot.meetingLink} target="_blank" rel="noreferrer">
                  ↗ Join meeting
                </a>
              )}
            </span>
            <div className="row-actions">
              {slot.booked && slot.bookingId && (
                <button className="btn btn--ghost btn--sm" onClick={() => clearBooking(slot.bookingId!)}>
                  Clear
                </button>
              )}
              <button className="btn btn--danger btn--sm" onClick={() => deleteSlot(slot.id)}>
                Delete
              </button>
            </div>
          </div>
        ))
      )}
    </div>
  )
}
