# EventFlow — Web-Based Event Management and Booking System

A fully-featured REST API built with **Spring Boot 3.3**, **PostgreSQL**, **Flyway**, **JWT authentication**, and **Resend** transactional email.

---

## Tech Stack

| Layer         | Technology                            |
|---------------|---------------------------------------|
| Framework     | Spring Boot 3.3 / Java 21             |
| Security      | Spring Security + JWT (jjwt 0.12.6)   |
| Persistence   | Spring Data JPA + Hibernate           |
| Database      | PostgreSQL 16                         |
| Migrations    | Flyway                                |
| Email         | Resend (transactional email API)      |
| Docs          | SpringDoc OpenAPI (Swagger UI)        |
| Build         | Maven                                 |
| Container     | Docker + Docker Compose               |

---

## Quick Start (Docker)

### 1. Copy the environment file and fill in your values

```bash
cp .env.example .env
```

Open `.env` and set:

```bash
POSTGRES_PASSWORD=your_strong_password
JWT_SECRET=                # run: openssl rand -hex 32
RESEND_API_KEY=re_...      # from resend.com
MAIL_FROM=you@yourdomain.com
BASE_URL=http://localhost:8080
```

### 2. Start everything

```bash
docker compose up --build
```

The app is available at:
- API → `http://localhost:8080`
- Swagger UI → `http://localhost:8080/swagger-ui.html`

---

## Local Development (without Docker)

### Prerequisites
- Java 21+
- PostgreSQL 16 running locally
- Maven 3.9+

### 1. Create the database

```sql
CREATE USER eventflow WITH PASSWORD 'eventflow';
CREATE DATABASE eventflow OWNER eventflow;
```

### 2. Set environment variables

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/eventflow
export SPRING_DATASOURCE_USERNAME=eventflow
export SPRING_DATASOURCE_PASSWORD=eventflow
export APP_JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
export RESEND_API_KEY=re_your_api_key
export MAIL_FROM=you@yourdomain.com
export BASE_URL=http://localhost:8080
```

### 3. Run

```bash
./mvnw spring-boot:run
```

---

## Environment Variables Reference

| Variable | Required | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Yes | JDBC URL for PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password |
| `APP_JWT_SECRET` | Yes | 64-char hex string for JWT signing |
| `APP_JWT_EXPIRATION_MS` | No | JWT expiry in ms (default: 86400000 = 24h) |
| `RESEND_API_KEY` | Yes | API key from resend.com |
| `MAIL_FROM` | Yes | Verified sender email address |
| `BASE_URL` | No | Public URL used in email links (default: http://localhost:8080) |

Generate a secure JWT secret:
```bash
openssl rand -hex 32
```

---

## Roles

| Role | Permissions |
|---|---|
| `ATTENDEE` | Browse events, register/cancel bookings, join waitlist, view own tickets |
| `ORGANIZER` | All ATTENDEE permissions + create/update/delete own events, add schedules, view attendees, register for other organizers' events |
| `STAFF` | All ORGANIZER permissions on **any** event + admin user management, manually register users, create events for organizers |

> New users register as `ATTENDEE` by default.
> Staff can promote roles manually via `PATCH /api/admin/users/{id}/role`
> or by approving an organizer request via `POST /api/admin/organizer-requests/{id}/review`.

---

## API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new user |
| `POST` | `/api/auth/login` | Public | Login → returns JWT token |
| `POST` | `/api/auth/logout` | Authenticated | Invalidate current JWT |

### Events

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/events` | Public | List all published events (paginated) |
| `GET` | `/api/events/{id}` | Public | Get event details |
| `GET` | `/api/events/search` | Public | Search by keyword, location, status, date range |
| `GET` | `/api/events/mine` | ORGANIZER / STAFF | List events created by the current user |
| `POST` | `/api/events` | ORGANIZER / STAFF | Create a new event (owned by caller) |
| `PUT` | `/api/events/{id}` | ORGANIZER (own) / STAFF | Update an event |
| `DELETE` | `/api/events/{id}` | ORGANIZER (own) / STAFF | Delete an event |
| `POST` | `/api/admin/events` | STAFF | Create an event on behalf of an organizer |

**Search parameters:**

| Param | Type | Description |
|---|---|---|
| `keyword` | string | Matches event title or description |
| `location` | string | Matches event location |
| `status` | enum | `DRAFT`, `PUBLISHED`, `CANCELLED` |
| `from` | ISO datetime | Events on or after this date |
| `to` | ISO datetime | Events on or before this date |

### Registrations & Bookings

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/events/{id}/register` | ATTENDEE / ORGANIZER | Book an event (see below for body) |
| `DELETE` | `/api/events/{id}/register` | Authenticated | Cancel booking (before event starts) |
| `GET` | `/api/users/me/registrations` | Authenticated | List my bookings (paginated) |
| `GET` | `/api/events/{id}/attendees` | ORGANIZER / STAFF | List attendees for an event |
| `POST` | `/api/admin/events/{id}/register/{userId}` | STAFF | Manually register a user for an event |

**Booking request body** (optional — defaults to 1 attendee):

```json
{
  "attendeeCount": 3
}
```

Booking rules:
- A user can book **1–20 seats** per booking.
- Organizers can register for events **created by other organizers** but not their own.
- Cancellation is only allowed **before the event start time**.
- When a booking is cancelled, the freed seats automatically notify waitlisted users.

### Waitlist

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/events/{id}/waitlist` | Authenticated | Join the waitlist for a full event |
| `DELETE` | `/api/events/{id}/waitlist` | Authenticated | Leave the waitlist |
| `GET` | `/api/users/me/waitlist` | Authenticated | List my waitlist entries |

Waitlist rules:
- Only available when an event is at full capacity.
- When a slot opens (cancellation or capacity increase), the first person(s) on the waitlist receive an email notification.
- Each slot that opens notifies exactly one waiting person — no double notifications.

### Schedules

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/events/{id}/schedules` | Public | List sessions for an event |
| `POST` | `/api/events/{id}/schedules` | ORGANIZER (own) / STAFF | Add a session to an event |

**Session request body:**

```json
{
  "sessionTitle": "Keynote Address",
  "description": "Opening keynote",
  "startTime": "2027-06-15T10:00:00",
  "endTime": "2027-06-15T11:30:00"
}
```

End time must be after start time.

### Users

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/me` | Authenticated | Get current user's profile |
| `PUT` | `/api/users/me` | Authenticated | Update current user's profile |
| `GET` | `/api/admin/users` | STAFF | List all users (paginated) |
| `PATCH` | `/api/admin/users/{id}/role` | STAFF | Change a user's role |

### Organizer Requests

Attendees can apply to become an organizer by submitting a request form. Staff review and approve or decline. Both actions trigger email notifications.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/organizer-requests` | ATTENDEE | Submit an organizer request |
| `GET` | `/api/organizer-requests/mine` | Authenticated | View my submitted requests |
| `GET` | `/api/admin/organizer-requests` | STAFF | List all requests (filterable by status) |
| `GET` | `/api/admin/organizer-requests/{id}` | STAFF | View a single request |
| `POST` | `/api/admin/organizer-requests/{id}/review` | STAFF | Approve or decline a request |

**Request form body:**

```json
{
  "name": "Alice Tadesse",
  "email": "alice@example.com",
  "phone": "+251911000000",
  "message": "I have organized 10+ tech meetups in Addis Ababa..."
}
```

**Review body:**

```json
{
  "decision": "APPROVED",
  "note": "Welcome aboard!"
}
```

`decision` must be `APPROVED` or `DECLINED`. When approved, the user's role is automatically changed to `ORGANIZER`.

---

## Email Notifications

EventFlow sends transactional emails via **Resend** for the following events:

| Trigger | Recipient | Subject |
|---|---|---|
| Booking confirmed | Attendee | Booking Confirmed – {event} |
| Booking cancelled | Attendee | Booking Cancelled – {event} |
| Event date changed | All registered attendees | 📅 Date Changed – {event} |
| Slot opened on waitlist | Waitlisted user(s) | 🎟️ A slot just opened – {event} |
| Waitlist joined | Attendee | You're on the waitlist – {event} |
| Organizer request submitted | All staff members | 📋 New Organizer Request – {name} |
| Organizer request approved | Applicant | 🎉 Your organizer request has been approved! |
| Organizer request declined | Applicant | Your organizer request – update |

All emails are sent **asynchronously** — email failures are logged but never break a business transaction.

### Setting up Resend

1. Create a free account at [resend.com](https://resend.com)
2. Go to **API Keys** → **Create API Key** → copy the key (starts with `re_`)
3. Go to **Domains** → add and verify your domain (add 3 DNS records)
4. Set `RESEND_API_KEY` and `MAIL_FROM` in your environment

> **Free tier:** 3,000 emails/month. No credit card required.

---

## Example Usage


### 1. Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"password123"}'
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"password123"}'
# → copy the "token" from the response
```
#### for admin/staff use this exact cridential
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@eventflow.com","password":"admin123"}'
# → copy the "token" from the response
```

### 3. Create an event (as ORGANIZER)

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tech Meetup",
    "location": "Addis Ababa",
    "dateTime": "2027-06-15T10:00:00",
    "status": "PUBLISHED",
    "capacity": 200
  }'
```

### 4. Book an event (2 seats)

```bash
curl -X POST http://localhost:8080/api/events/1/register \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"attendeeCount": 2}'
```

### 5. Join the waitlist (when event is full)

```bash
curl -X POST http://localhost:8080/api/events/1/waitlist \
  -H "Authorization: Bearer <token>"
```

### 6. Submit an organizer request (as ATTENDEE)

```bash
curl -X POST http://localhost:8080/api/organizer-requests \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Tadesse",
    "email": "alice@example.com",
    "phone": "+251911000000",
    "message": "I have 3 years of experience organizing tech events in Addis Ababa."
  }'
```

### 7. Approve an organizer request (as STAFF)

```bash
curl -X POST http://localhost:8080/api/admin/organizer-requests/1/review \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"decision": "APPROVED", "note": "Welcome aboard!"}'
```

### 8. Staff creates an event for an organizer

```bash
curl -X POST http://localhost:8080/api/admin/events \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "organizerId": 5,
    "title": "Annual Developers Conference",
    "location": "Addis Ababa",
    "dateTime": "2027-09-01T09:00:00",
    "capacity": 500,
    "status": "DRAFT"
  }'
```

---

## Database Schema

Flyway manages all schema migrations automatically on startup.

| Version | File | Description |
|---|---|---|
| V1 | `V1__create_users.sql` | users table + `user_role` enum |
| V2 | `V2__create_events.sql` | events table + `event_status` enum |
| V3 | `V3__create_registrations.sql` | registrations table + `reg_status` enum |
| V4 | `V4__create_schedules.sql` | schedules table |
| V5 | `V5__create_token_blacklist.sql` | JWT logout blacklist |
| V6 | `V6__create_indexes.sql` | Performance indexes |
| V7 | `V7__add_attendee_count_and_waitlist.sql` | `attendee_count` on registrations + waitlist table |
| V8 | `V8__create_organizer_requests.sql` | Organizer request applications table |

---

## Running Tests

```bash
# Run all tests
./mvnw test

# Run tests with JaCoCo coverage report
./mvnw verify

# View the coverage report
open target/site/jacoco/index.html
```

Test coverage is enforced at **70% minimum** by JaCoCo. The build fails if coverage drops below this threshold.

The test suite includes:
- **Unit tests** — all service, mapper, repository, security, and config classes
- **Controller (slice) tests** — `@WebMvcTest` for every controller with role enforcement
- **Repository (slice) tests** — `@DataJpaTest` with H2 in-memory database
- **Integration test** — full Spring context load verification

---

## Security

- Passwords hashed with **BCrypt** (cost factor 12)
- JWTs signed with **HMAC-SHA256**, 256-bit secret, 24-hour expiry
- Blacklisted tokens stored in database and checked on every request
- Expired blacklist entries purged nightly at **02:00** by a scheduled task
- All secrets loaded from **environment variables** — never hardcoded
- App runs as a **non-root user** inside Docker
- Production profile disables Swagger UI

---

## Project Structure