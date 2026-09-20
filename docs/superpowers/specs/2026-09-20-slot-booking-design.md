# Assignment Slot Booking Tool — Design Spec

**Date:** 2026-09-20
**Status:** Approved (proceeding to implementation)

## Purpose

A tool for a professor to open bookable time slots for **presentation/demo** sessions
and **consultation/meeting** sessions. Student groups (or solo students) self-book a
slot, first-come-first-served, like Calendly. No student authentication; a single admin
passcode protects the professor's management area.

## Core requirements (decided during brainstorming)

1. **Slot purpose:** presentation/demo slots AND consultation/meeting slots (same mechanism).
2. **Booking model:** self-service, first-come-first-served.
3. **Identity:** no auth for students — they enter a name/group name when booking.
4. **Groups:** created on the fly by the booker; solo (individual) bookings allowed too.
5. **Slot setup:** professor adds slots manually, one by one.
6. **Booking rules:** one booking per slot; one booking per booker per event.
7. **Changes:** self-cancel and rebook; professor can override/clear any booking.
8. **Admin protection:** single shared admin passcode (env var).
9. **No-override guarantee (hard):** once a slot is booked, no one can overwrite it —
   enforced atomically at the database level, not just in the UI.

## Architecture

- **Frontend:** React + Vite + TypeScript, react-router. Hosted on **Vercel**.
- **Backend:** Spring Boot 3 (Java 17), Spring Web + Data JPA + Validation. Hosted on **Render**.
- **Database:** H2 file mode for local dev; **Neon** Postgres in production (Spring profile / env swap, no code change).
- **Cost:** all three free tiers, no credit card (Neon permanent free; Render free web service with ~1 min cold start after 15 min idle; use Neon for DB, NOT Render's 30-day free Postgres).

## Data model

### Event
| field | type | notes |
|---|---|---|
| id | Long | PK, generated |
| name | String | required |
| type | enum EventType {PRESENTATION, CONSULTATION} | required |
| description | String (text) | optional |
| createdAt | Instant | set on create |

### Slot
| field | type | notes |
|---|---|---|
| id | Long | PK |
| event | ManyToOne Event | `event_id`, required |
| startTime | Instant | required |
| durationMinutes | int | required, > 0 |
| label | String | optional (e.g. room) |

### Booking
| field | type | notes |
|---|---|---|
| id | Long | PK |
| slot | OneToOne Slot | `slot_id` **UNIQUE** → one booking per slot |
| eventId | Long | denormalized for uniqueness constraint |
| bookerName | String | required |
| memberNames | String (text) | optional |
| createdAt | Instant | set on create |

Constraints:
- `UNIQUE(slot_id)` on booking → **no-override guarantee**.
- `UNIQUE(event_id, booker_name)` → one booking per booker per event (exact-string match; trust model).

## Concurrency guarantee

Booking = `INSERT` into `booking` with `UNIQUE(slot_id)`. Two simultaneous requests →
DB accepts one, rejects the other with a constraint violation → service catches
`DataIntegrityViolationException` → returns **409 Conflict**. Works across multiple
backend instances (not in-process locking). This is the atomic no-override guarantee.

## API

### Public
- `GET /api/events` → `[{id, name, type, description, slotCount, bookedCount}]`
- `GET /api/events/{id}` → `{id, name, type, description, slots: [{id, startTime, durationMinutes, label, booked, bookerName?}]}`
- `POST /api/slots/{slotId}/bookings` body `{bookerName, memberNames?}`
  - `201` booking created
  - `409` slot already taken
  - `409` booker already has a booking in this event
  - `404` slot not found
- `DELETE /api/bookings/{bookingId}` body/query `{bookerName}` — must match booking's bookerName, else `403` (self-cancel guard). Rebook = cancel + book.

### Admin (header `X-Admin-Passcode` must equal configured passcode, else `401`)
- `POST /api/admin/events` — create event
- `PUT /api/admin/events/{id}` — edit event
- `DELETE /api/admin/events/{id}` — delete event (cascades slots/bookings)
- `POST /api/admin/events/{id}/slots` body `{startTime, durationMinutes, label?}` — add slot
- `DELETE /api/admin/slots/{id}` — delete slot
- `DELETE /api/admin/bookings/{id}` — force clear (professor override)
- `GET /api/admin/events/{id}/export.csv` — CSV of bookings

## Configuration

- `APP_ADMIN_PASSCODE` (default `changeme` in dev) — admin passcode.
- `APP_CORS_ALLOWED_ORIGINS` — comma-separated (dev: `http://localhost:5173`; prod: Vercel URL).
- Local: H2 file at `./data/bookings`, `ddl-auto=update`, H2 console on.
- Prod: `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (Neon), Postgres driver, `ddl-auto=update`.
- Frontend: `VITE_API_BASE_URL` — backend base URL.

## Frontend screens

- `/` — student event list (cards showing type + booked/total).
- `/events/:id` — slot list/grid: open slots have "Book" (modal: name + optional members);
  booked slots show "Booked by X" and are disabled; the booker's own slot shows "Cancel".
- `/admin` — passcode entry (stored in memory/session); on success:
  - create/edit/delete events
  - add/delete slots per event
  - view all bookings, clear any booking (override)
  - export CSV

## Testing

Backend (JUnit + Spring Boot Test / MockMvc):
- Concurrency: two racing booking requests on one slot → exactly one `201`, one `409`.
- One-per-event: same booker books a second slot in same event → `409`.
- Cancel frees slot → rebook succeeds.
- Admin endpoints without/with wrong passcode → `401`.

Frontend (React Testing Library, mocked fetch):
- Booked slots render disabled; successful book updates the UI; `409` shows a clear message.

## Deployment (documented, not executed here)

README will include: local run steps, env vars, and deploy notes for Vercel (frontend),
Render (backend with Neon `DATABASE_URL`), and Neon (create Postgres, copy connection string).

## Out of scope (deferred)

- Real authentication / SSO.
- Real-time WebSocket slot updates (Approach C).
- Auto-generated slot grids, preference-based auto-assignment.
- Email notifications.
