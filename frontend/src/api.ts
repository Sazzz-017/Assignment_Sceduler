import {
  ApiError,
  type BookingResponse,
  type CreateEventInput,
  type CreateSlotInput,
  type EventDetail,
  type EventSummary,
} from './types'

const BASE = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')

const ADMIN_HEADER = 'X-Admin-Passcode'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const { headers: initHeaders, ...rest } = init ?? {}
  let resp: Response
  try {
    resp = await fetch(`${BASE}${path}`, {
      ...rest,
      headers: { 'Content-Type': 'application/json', ...(initHeaders ?? {}) },
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?')
  }

  if (resp.status === 204) {
    return undefined as T
  }

  const text = await resp.text()
  const body = text ? safeJson(text) : null

  if (!resp.ok) {
    const message = (body && typeof body === 'object' && 'message' in body && (body as { message?: string }).message)
      ? (body as { message: string }).message
      : `Request failed (${resp.status})`
    throw new ApiError(resp.status, message)
  }
  return body as T
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function adminHeaders(passcode: string): Record<string, string> {
  return { [ADMIN_HEADER]: passcode }
}

/* ---------- Public API ---------- */
export const api = {
  listEvents: () => request<EventSummary[]>('/api/events'),

  getEvent: (id: number) => request<EventDetail>(`/api/events/${id}`),

  book: (slotId: number, bookerName: string, memberNames: string | null) =>
    request<BookingResponse>(`/api/slots/${slotId}/bookings`, {
      method: 'POST',
      body: JSON.stringify({ bookerName, memberNames }),
    }),

  cancel: (bookingId: number, bookerName: string) =>
    request<void>(`/api/bookings/${bookingId}?bookerName=${encodeURIComponent(bookerName)}`, {
      method: 'DELETE',
    }),

  setMeetingLink: (bookingId: number, bookerName: string, meetingLink: string | null) =>
    request<BookingResponse>(`/api/bookings/${bookingId}/meeting-link`, {
      method: 'PUT',
      body: JSON.stringify({ bookerName, meetingLink }),
    }),
}

/* ---------- Admin API ---------- */
export const adminApi = {
  ping: (passcode: string) => request<void>('/api/admin/ping', { headers: adminHeaders(passcode) }),

  createEvent: (passcode: string, input: CreateEventInput) =>
    request<EventDetail>('/api/admin/events', {
      method: 'POST',
      headers: adminHeaders(passcode),
      body: JSON.stringify(input),
    }),

  updateEvent: (passcode: string, id: number, input: CreateEventInput) =>
    request<EventDetail>(`/api/admin/events/${id}`, {
      method: 'PUT',
      headers: adminHeaders(passcode),
      body: JSON.stringify(input),
    }),

  deleteEvent: (passcode: string, id: number) =>
    request<void>(`/api/admin/events/${id}`, {
      method: 'DELETE',
      headers: adminHeaders(passcode),
    }),

  addSlot: (passcode: string, eventId: number, input: CreateSlotInput) =>
    request<void>(`/api/admin/events/${eventId}/slots`, {
      method: 'POST',
      headers: adminHeaders(passcode),
      body: JSON.stringify(input),
    }),

  deleteSlot: (passcode: string, slotId: number) =>
    request<void>(`/api/admin/slots/${slotId}`, {
      method: 'DELETE',
      headers: adminHeaders(passcode),
    }),

  clearBooking: (passcode: string, bookingId: number) =>
    request<void>(`/api/admin/bookings/${bookingId}`, {
      method: 'DELETE',
      headers: adminHeaders(passcode),
    }),

  async downloadCsv(passcode: string, eventId: number, filename: string): Promise<void> {
    const resp = await fetch(`${BASE}/api/admin/events/${eventId}/export.csv`, {
      headers: adminHeaders(passcode),
    })
    if (!resp.ok) {
      throw new ApiError(resp.status, `Export failed (${resp.status})`)
    }
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  },
}
