# Slot Ledger — Assignment Slot Booking Tool

A self-service booking tool for a professor to open **presentation/demo** and
**consultation/meeting** time slots. Student groups (or solo students) grab a slot,
first-come-first-served — like Calendly, but purpose-built for coursework.

- **Frontend:** React + Vite + TypeScript (hosted on **Vercel**)
- **Backend:** Spring Boot 3 + JPA (hosted on **Render**)
- **Database:** H2 file (local dev) / PostgreSQL (production, e.g. **Neon**)

## Key guarantee

**No booked slot can be overwritten.** Booking a slot is an atomic insert protected by a
`UNIQUE(slot_id)` database constraint. If two people click *Book* on the same slot at the
same instant, exactly one wins and the other gets a clear "just taken" message — enforced
at the database, not just in the UI. (Proven by an 8-way concurrency test in
`SlotBookingIntegrationTest`.)

## Rules

- One booking per slot.
- One booking per booker (name/group) per event.
- Self-cancel and rebook (must match the name used to book).
- The professor can clear/override any booking.
- No student login; the professor's management area is gated by a single admin passcode.

---

## How to use

> Replace `<your-site-url>` with your live site (e.g. your Vercel URL). The professor's
> passcode is set by the `APP_ADMIN_PASSCODE` environment variable on the backend — it is
> **not** stored in this repo. Keep it private and share it with no one but the instructor.

### For the professor (admin)

1. **Open the admin area:** go to `<your-site-url>/admin`.
2. **Sign in:** enter the admin passcode (`APP_ADMIN_PASSCODE`) → **Unlock**.
   `<ADMIN_PASSCODE_PLACEHOLDER>`
3. **Create a session:** in **New session**, enter a title (e.g. "Final Demos"), pick a
   type — **Presentation / demo** or **Consultation / meeting** — add an optional
   description, then **Create session**.
4. **Add time slots:** with the session selected, use **Add a slot** to set each slot's
   start date & time, its length in minutes, and an optional label (e.g. a room). Repeat
   for every slot you want to offer. (Slots are added one at a time.)
5. **Share the link:** give students `<your-site-url>` (the home page). They pick the
   session and book — no login needed.
6. **Track & manage bookings:** open a session to see who booked each slot. You can:
   - **Clear** any booking (frees the slot for someone else),
   - **Delete** a slot, or **Delete session** entirely,
   - **Export CSV** to download all bookings (name, members, meeting link, time).
7. **Join a meeting:** if a student attached a Google Meet link, a **↗ Join meeting**
   link appears on that slot — click it at the scheduled time.
8. **Sign out** when done (top-right of the admin page).

> Tip: if the site was idle, the first load after a while can take up to ~1 minute while
> the backend wakes — open it a couple of minutes before you announce booking.

### For students

1. **Open the site:** go to `<your-site-url>` (the link your instructor shares).
2. **Pick a session:** each card shows the type (presentation or consultation) and how
   many slots are still free. Click the one you need.
3. **Book a slot:** on an available time, click **Book**, enter your **name or group
   name** (and optional member names if it's a group), then **Confirm booking**.
   - You can hold **one booking per session**. If a slot was just taken by someone else,
     you'll get a message — simply pick another.
4. **Add your Google Meet link:** on your booked slot, click **Add Meet link**, paste your
   meeting URL (must start with `https://`), and **Save**. This is what the instructor
   clicks to join at your time. You can **Edit link** later.
5. **Change your mind?** click **Release** on your slot to free it, then book a different
   one. (You can only release a booking made under the same name you enter.)

---

## Run locally

### 1. Backend (port 8080)

```bash
cd backend
APP_ADMIN_PASSCODE=changeme mvn spring-boot:run
```

- Uses an H2 file database at `backend/data/bookings` (auto-created).
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/bookings`).
- Run tests: `mvn test`

### 2. Frontend (port 5173)

```bash
cd frontend
npm install        # first time only
npm run dev
```

Open http://localhost:5173. The frontend talks to `http://localhost:8080` by default; to
point elsewhere, create `frontend/.env` from `.env.example` and set `VITE_API_BASE_URL`.

The admin area is at http://localhost:5173/admin (passcode: whatever you set in
`APP_ADMIN_PASSCODE`, default `changeme`).

---

## Environment variables

### Backend
| Variable | Purpose | Local default |
|---|---|---|
| `APP_ADMIN_PASSCODE` | Admin passcode for `/api/admin/**` | `changeme` |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:5173` |
| `SPRING_DATASOURCE_URL` | JDBC URL (set to Postgres in prod) | H2 file |
| `SPRING_DATASOURCE_USERNAME` | DB user | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | (empty) |
| `PORT` | Server port (Render sets this) | `8080` |

### Frontend
| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Backend base URL (e.g. `https://your-backend.onrender.com`) |

---

## API reference

### Public
| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/events` | List events with slot/booked counts |
| `GET` | `/api/events/{id}` | Event with its slots and booking state |
| `POST` | `/api/slots/{slotId}/bookings` | Body `{bookerName, memberNames?}` → 201 / 409 |
| `DELETE` | `/api/bookings/{id}?bookerName=…` | Self-cancel (name must match) → 204 / 403 |

### Admin (header `X-Admin-Passcode: <passcode>`)
| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/admin/ping` | Validate passcode |
| `POST` | `/api/admin/events` | Create event |
| `PUT` | `/api/admin/events/{id}` | Edit event |
| `DELETE` | `/api/admin/events/{id}` | Delete event (cascades) |
| `POST` | `/api/admin/events/{id}/slots` | Add a slot |
| `DELETE` | `/api/admin/slots/{id}` | Delete a slot |
| `DELETE` | `/api/admin/bookings/{id}` | Force-clear a booking (override) |
| `GET` | `/api/admin/events/{id}/export.csv` | Download bookings as CSV |

---

## Deploy (all free tiers, no credit card)

> Do these in the browser / dashboards — nothing here runs automatically.

### A. Database — Neon (Postgres)
1. Create a project at neon.tech → copy the **connection string**.
2. Convert it to a JDBC URL, e.g.
   `jdbc:postgresql://<host>/<db>?sslmode=require` (username/password separate).
   - The Neon free plan is permanent (no expiry) and wakes in ~1s after idle.
   - Use Neon for the database — **not** Render's built-in free Postgres, which expires after 30 days.

### B. Backend — Render
1. New → **Web Service** → point at the `backend/` directory.
2. Build command: `./mvnw clean package -DskipTests` (or `mvn clean package`).
3. Start command: `java -jar target/slot-booking-0.1.0.jar`
4. Environment variables:
   - `SPRING_DATASOURCE_URL` = your Neon JDBC URL
   - `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` = Neon credentials
   - `APP_ADMIN_PASSCODE` = a strong secret
   - `APP_CORS_ALLOWED_ORIGINS` = your Vercel URL (e.g. `https://your-app.vercel.app`)
   - Note: the free web service sleeps after 15 min idle and takes ~1 min to cold-start.

### C. Frontend — Vercel
1. Import the repo → set **Root Directory** to `frontend/`.
2. Framework preset: **Vite**. Build: `npm run build`. Output: `dist`.
3. Environment variable: `VITE_API_BASE_URL` = your Render backend URL.
4. Deploy.

After all three are live, make sure `APP_CORS_ALLOWED_ORIGINS` (Render) includes the exact
Vercel URL, and `VITE_API_BASE_URL` (Vercel) points at the Render URL.

---

## Deferred (not built)

Real authentication/SSO, real-time (WebSocket) slot updates, auto-generated slot grids,
preference-based auto-assignment, and email notifications. See
`docs/superpowers/specs/2026-09-20-slot-booking-design.md` for the full design.
