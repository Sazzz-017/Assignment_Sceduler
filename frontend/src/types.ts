export type EventType = 'PRESENTATION' | 'CONSULTATION'

export interface EventSummary {
  id: number
  name: string
  type: EventType
  description: string | null
  slotCount: number
  bookedCount: number
}

export interface SlotResponse {
  id: number
  startTime: string
  durationMinutes: number
  label: string | null
  booked: boolean
  bookerName: string | null
  bookingId: number | null
  meetingLink: string | null
}

export interface EventDetail {
  id: number
  name: string
  type: EventType
  description: string | null
  slots: SlotResponse[]
}

export interface BookingResponse {
  id: number
  slotId: number
  eventId: number
  bookerName: string
  memberNames: string | null
  meetingLink: string | null
  createdAt: string
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export interface CreateEventInput {
  name: string
  type: EventType
  description: string | null
}

export interface CreateSlotInput {
  startTime: string
  durationMinutes: number
  label: string | null
}
