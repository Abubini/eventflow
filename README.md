# EventFlow — Web-Based Event Management and Booking System

A fully-featured REST API built with **Spring Boot 3.3**, **PostgreSQL**, **Flyway**, and **JWT authentication**.

---

## Tech Stack

| Layer         | Technology                            |
|---------------|---------------------------------------|
| Framework     | Spring Boot 3.3 / Java 21             |
| Security      | Spring Security + JWT (jjwt 0.12.6)   |
| Persistence   | Spring Data JPA + Hibernate           |
| Database      | PostgreSQL 15                         |
| Migrations    | Flyway                                |
| Docs          | SpringDoc OpenAPI (Swagger UI)        |
| Build         | Maven                                 |
| Container     | Docker + Docker Compose               |

---

## Quick Start (Docker)

```bash
# Clone / unzip the project
cd eventflow

# Start everything (PostgreSQL + app)
docker compose up --build

# API available at:
#   http://localhost:8080
#   http://localhost:8080/swagger-ui.html
```

---

## Local Development (without Docker)

### Prerequisites
- Java 21+
- PostgreSQL 15 running locally
- Maven 3.9+

### 1. Create the database
```sql
CREATE DATABASE eventflow;
CREATE USER eventflow WITH PASSWORD 'eventflow';
GRANT ALL PRIVILEGES ON DATABASE eventflow TO eventflow;
```

### 2. Configure environment variables (or edit application.properties)
```bash
export DB_URL=jdbc:postgresql://localhost:5432/eventflow
export DB_USER=eventflow
export DB_PASS=eventflow
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

### 3. Run
```bash
./mvnw spring-boot:run
```

---

## API Overview

### Authentication
| Method | Endpoint               | Description           |
|--------|------------------------|-----------------------|
| POST   | /api/auth/register     | Register new user     |
| POST   | /api/auth/login        | Login → JWT token     |
| POST   | /api/auth/logout       | Logout (blacklist JWT)|

### Events (public read, protected write)
| Method | Endpoint                    | Role Required         |
|--------|-----------------------------|-----------------------|
| GET    | /api/events                 | Public                |
| GET    | /api/events/{id}            | Public                |
| GET    | /api/events/search          | Public                |
| POST   | /api/events                 | ORGANIZER / STAFF     |
| PUT    | /api/events/{id}            | ORGANIZER (own) / STAFF|
| DELETE | /api/events/{id}            | ORGANIZER (own) / STAFF|

### Registrations
| Method | Endpoint                        | Role Required         |
|--------|---------------------------------|-----------------------|
| POST   | /api/events/{id}/register       | ATTENDEE              |
| DELETE | /api/events/{id}/register       | Authenticated         |
| GET    | /api/events/{id}/attendees      | ORGANIZER / STAFF     |

### Schedules
| Method | Endpoint                        | Role Required         |
|--------|---------------------------------|-----------------------|
| GET    | /api/events/{id}/schedules      | Public                |
| POST   | /api/events/{id}/schedules      | ORGANIZER / STAFF     |

### Users
| Method | Endpoint                    | Role Required         |
|--------|-----------------------------|-----------------------|
| GET    | /api/users/me               | Authenticated         |
| PUT    | /api/users/me               | Authenticated         |
| GET    | /api/admin/users            | STAFF                 |
| PATCH  | /api/admin/users/{id}/role  | STAFF                 |

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

### 3. Create an Event (as ORGANIZER — update role first via admin)
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Tech Meetup","location":"Addis Ababa","dateTime":"2027-06-15T10:00:00","status":"PUBLISHED","capacity":200}'
```

### 4. Register for an Event (as ATTENDEE)
```bash
curl -X POST http://localhost:8080/api/events/1/register \
  -H "Authorization: Bearer <token>"
```

---

## Running Tests

```bash
./mvnw test
# With coverage report:
./mvnw test jacoco:report
# Report at: target/site/jacoco/index.html
```

---

## Roles

| Role      | Permissions                                           |
|-----------|-------------------------------------------------------|
| ATTENDEE  | Browse events, register/cancel registration           |
| ORGANIZER | All ATTENDEE perms + create/update/delete own events, add schedules |
| STAFF     | All ORGANIZER perms on any event + admin user management |

> **Note:** New users register as ATTENDEE by default.  
> A STAFF user can promote any user's role via `PATCH /api/admin/users/{id}/role`.

---

## Database Schema (Flyway Migrations)

| Version | File                          | Description                    |
|---------|-------------------------------|--------------------------------|
| V1      | V1__create_users.sql          | users table + user_role enum   |
| V2      | V2__create_events.sql         | events table + event_status enum|
| V3      | V3__create_registrations.sql  | registrations table            |
| V4      | V4__create_schedules.sql      | schedules table                |
| V5      | V5__create_token_blacklist.sql| JWT logout blacklist           |
| V6      | V6__create_indexes.sql        | Performance indexes            |

---

## Security Notes
- Passwords hashed with BCrypt (cost 12)
- JWT signed with HS256, 256-bit secret, 24h expiry
- JWT blacklist table used for logout invalidation
- Expired tokens purged nightly at 02:00 via scheduled task
- All secrets loaded from environment variables — never hardcoded
