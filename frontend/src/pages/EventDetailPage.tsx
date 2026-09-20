import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import { ApiError, type EventDetail, type SlotResponse } from '../types'
import { fmtClock, fmtDate, fmtEndClock } from '../lib/format'
import { getRememberedName, rememberName } from '../lib/identity'

export default function EventDetailPage() {
  const { id } = useParams<{ id: string }>()
  const eventId = Number(id)

  const [event, setEvent] = useState<EventDetail | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [myName, setMyName] = useState<string>(getRememberedName())
  const [modalSlot, setModalSlot] = useState<SlotResponse | null>(null)
  const [linkSlot, setLinkSlot] = useState<SlotResponse | null>(null)
  const [toast, setToast] = useState<string | null>(null)

  const reload = useCallback(() => {
    return api
      .getEvent(eventId)
      .then(setEvent)
      .catch((e: ApiError) => setLoadError(e.message))
  }, [eventId])

  useEffect(() => {
    reload()
  }, [reload])

  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(null), 2600)
    return () => clearTimeout(t)
  }, [toast])

  const isMine = (slot: SlotResponse) =>
    slot.booked && !!myName && slot.bookerName?.toLowerCase() === myName.trim().toLowerCase()

  const myBooking = event?.slots.find(isMine) ?? null

  async function handleCancel(slot: SlotResponse) {
    if (!slot.bookingId) return
    if (!window.confirm('Release this slot? Someone else may take it.')) return
    try {
      await api.cancel(slot.bookingId, myName)
      setToast('Slot released.')
      await reload()
    } catch (e) {
      const err = e as ApiError
      setToast(err.message)
      await reload()
    }
  }

  if (loadError) {
    return (
      <>
        <Link to="/" className="backlink">
          ← All sessions
        </Link>
        <p className="notice notice--error">{loadError}</p>
      </>
    )
  }

  if (!event) {
    return <p className="placeholder">Loading session…</p>
  }

  return (
    <div className={`type-${event.type.toLowerCase()}`}>
      <Link to="/" className="backlink">
        ← All sessions
      </Link>

      <div className="detail-head">
        <span className="badge">{event.type}</span>
        <h1>{event.name}</h1>
        {event.description && <p>{event.description}</p>}
      </div>

      {myBooking && (
        <p className="notice notice--info">
          You’re booked as <strong>{myBooking.bookerName}</strong> at{' '}
          {fmtClock(myBooking.startTime)} on {fmtDate(myBooking.startTime)}.{' '}
          {myBooking.meetingLink
            ? 'Your meeting link is attached below.'
            : 'Add your Google Meet link below so the instructor can join.'}{' '}
          One booking per session — release it below to switch.
        </p>
      )}

      {event.slots.length === 0 ? (
        <div className="placeholder">
          <div className="placeholder__mark">—</div>
          <p>No slots have been added to this session yet.</p>
        </div>
      ) : (
        <div className="ledger">
          {event.slots.map((slot, i) => {
            const mine = isMine(slot)
            return (
              <div
                key={slot.id}
                className={`slot-row ${mine ? 'is-mine' : slot.booked ? 'is-booked' : ''}`}
                style={{ animationDelay: `${i * 40}ms` }}
              >
                <div className="slot-time">
                  {fmtClock(slot.startTime)}–{fmtEndClock(slot.startTime, slot.durationMinutes)}
                  <small>
                    {fmtDate(slot.startTime)} · {slot.durationMinutes} min
                  </small>
                </div>

                <div className="slot-main">
                  {slot.label && <span className="slot-label">{slot.label}</span>}
                  {slot.booked ? (
                    <span className="slot-booker">
                      {slot.bookerName}
                      <span className="stamp">{mine ? 'Yours' : 'Booked'}</span>
                    </span>
                  ) : (
                    <span className="slot-state">Available</span>
                  )}
                  {mine && slot.meetingLink && (
                    <a className="meet-link" href={slot.meetingLink} target="_blank" rel="noreferrer">
                      ↗ {slot.meetingLink}
                    </a>
                  )}
                </div>

                <div className="slot-action">
                  {mine ? (
                    <div className="row-actions">
                      <button className="btn btn--ghost btn--sm" onClick={() => setLinkSlot(slot)}>
                        {slot.meetingLink ? 'Edit link' : 'Add Meet link'}
                      </button>
                      <button className="btn btn--danger btn--sm" onClick={() => handleCancel(slot)}>
                        Release
                      </button>
                    </div>
                  ) : slot.booked ? (
                    <span className="slot-state">—</span>
                  ) : (
                    <button
                      className="btn btn--primary btn--sm"
                      disabled={!!myBooking}
                      title={myBooking ? 'You already hold a slot in this session' : undefined}
                      onClick={() => setModalSlot(slot)}
                    >
                      Book
                    </button>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {modalSlot && (
        <BookingModal
          slot={modalSlot}
          defaultName={myName}
          onClose={() => setModalSlot(null)}
          onBooked={async (name) => {
            rememberName(name)
            setMyName(name)
            setModalSlot(null)
            setToast('Slot booked — see you there.')
            await reload()
          }}
          onConflict={async (message) => {
            setModalSlot(null)
            setToast(message)
            await reload()
          }}
        />
      )}

      {linkSlot && linkSlot.bookingId && (
        <MeetingLinkModal
          slot={linkSlot}
          bookerName={myName}
          onClose={() => setLinkSlot(null)}
          onSaved={async (message) => {
            setLinkSlot(null)
            setToast(message)
            await reload()
          }}
        />
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  )
}

interface ModalProps {
  slot: SlotResponse
  defaultName: string
  onClose: () => void
  onBooked: (name: string) => void | Promise<void>
  onConflict: (message: string) => void | Promise<void>
}

function BookingModal({ slot, defaultName, onClose, onBooked, onConflict }: ModalProps) {
  const [name, setName] = useState(defaultName)
  const [members, setMembers] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!name.trim()) {
      setError('Please enter your name or group name.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.book(slot.id, name.trim(), members.trim() || null)
      await onBooked(name.trim())
    } catch (e) {
      const err = e as ApiError
      if (err.status === 409) {
        await onConflict(err.message)
      } else {
        setError(err.message)
        setSubmitting(false)
      }
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Book this slot</h3>
        <p className="modal__sub">
          {fmtClock(slot.startTime)}–{fmtEndClock(slot.startTime, slot.durationMinutes)} · {fmtDate(slot.startTime)}
          {slot.label ? ` · ${slot.label}` : ''}
        </p>

        {error && <p className="notice notice--error">{error}</p>}

        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="bk-name">Your name / group name</label>
            <input
              id="bk-name"
              value={name}
              autoFocus
              placeholder="e.g. Team Nimbus or Priya Rao"
              onChange={(e) => setName(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="bk-members">Members (optional)</label>
            <textarea
              id="bk-members"
              value={members}
              placeholder="Comma-separated names, if a group"
              onChange={(e) => setMembers(e.target.value)}
            />
          </div>
          <div className="modal__actions">
            <button type="button" className="btn btn--ghost" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? 'Booking…' : 'Confirm booking'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

interface LinkModalProps {
  slot: SlotResponse
  bookerName: string
  onClose: () => void
  onSaved: (message: string) => void | Promise<void>
}

function MeetingLinkModal({ slot, bookerName, onClose, onSaved }: LinkModalProps) {
  const [link, setLink] = useState(slot.meetingLink ?? '')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function save(e: FormEvent) {
    e.preventDefault()
    const trimmed = link.trim()
    if (trimmed && !/^https?:\/\//i.test(trimmed)) {
      setError('Enter a full URL starting with https:// (e.g. a Google Meet link).')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.setMeetingLink(slot.bookingId!, bookerName, trimmed || null)
      await onSaved(trimmed ? 'Meeting link saved.' : 'Meeting link removed.')
    } catch (e) {
      const err = e as ApiError
      setError(err.message)
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Meeting link</h3>
        <p className="modal__sub">
          {fmtClock(slot.startTime)}–{fmtEndClock(slot.startTime, slot.durationMinutes)} · {fmtDate(slot.startTime)}
        </p>

        {error && <p className="notice notice--error">{error}</p>}

        <form onSubmit={save}>
          <div className="field">
            <label htmlFor="meet-link">Google Meet (or any) link</label>
            <input
              id="meet-link"
              value={link}
              autoFocus
              placeholder="https://meet.google.com/abc-defg-hij"
              onChange={(e) => setLink(e.target.value)}
            />
          </div>
          <div className="modal__actions">
            <button type="button" className="btn btn--ghost" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? 'Saving…' : 'Save link'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
