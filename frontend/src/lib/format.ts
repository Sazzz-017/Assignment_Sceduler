export function fmtClock(iso: string): string {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
}

export function fmtEndClock(iso: string, minutes: number): string {
  const end = new Date(new Date(iso).getTime() + minutes * 60_000)
  return end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
}

export function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' })
}

export function fmtDateTimeFull(iso: string): string {
  return new Date(iso).toLocaleString([], {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

/** Converts a <input type="datetime-local"> value (local time, no zone) to an ISO instant. */
export function localInputToIso(value: string): string {
  return new Date(value).toISOString()
}
