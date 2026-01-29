# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a production-quality ham radio QSO (contact) logging application built as a monorepo containing:
- **Backend**: Java 25 + Spring Boot 4 WebFlux + R2DBC (reactive) + PostgreSQL
- **Frontend**: Angular 21 SPA + Angular Material
- **Auth**: JWT bearer tokens with BCrypt/Argon2 password hashing
- **Architecture**: Clean Architecture with SOLID principles, reactive stack throughout

## Project Status

Currently a planning/specification repository. Implementation follows the incremental steps in `/iteration/*.md` files.

## Monorepo Structure (Target)

```
/
  backend/          # Spring Boot WebFlux reactive backend
  frontend/         # Angular 21 SPA with Material UI
  docker/           # docker-compose.yml for local PostgreSQL
  .github/workflows/ # CI/CD pipelines
  README.md         # Main documentation
  prompt_v1.md      # Original specification
  iteration/        # Step-by-step implementation plan
```

## Backend Architecture

### Layer Structure (Clean Architecture)

Package: `com.example.qsolog`
- **api**: REST controllers, DTOs (request/response)
- **application**: Use cases, service orchestration
- **domain**: Entities, value objects, domain services, ports
- **infrastructure**: Persistence adapters, security, external clients, config

### Database Schema

**users** table:
- `id` (UUID PK), `email` (unique), `username` (unique), `password_hash`
- `role` (ADMIN/OPERATOR)
- `created_at`, `updated_at`

**qso** table (QSO = ham radio contact):
- `id` (UUID PK), `user_id` (FK to users)
- `their_callsign` (CALL in ADIF)
- `qso_date` (date UTC), `time_on` (time UTC)
- `band` (varchar, REQUIRED), `frequency_khz` (numeric, optional)
- `mode` (AdifMode enum), `submode` (AdifSubmode enum, nullable), `custom_mode` (varchar, nullable)
- `rst_sent`, `rst_recv`, `qth`, `grid_square`, `notes`
- `qsl_status`, `lotw_status`, `eqsl_status`
- `created_at`, `updated_at`
- Indexes: (user_id, qso_date DESC), (user_id, their_callsign), (user_id, band)

**ai_report_history** table:
- Stores generated AI reports with language (PL/EN), date range, content

### Domain Enums

**Band** (ADIF standard): 160m, 80m, 60m, 40m, 30m, 20m, 17m, 15m, 12m, 10m, 6m, 4m, 2m, 1.25m, 70cm, 33cm, 23cm, 13cm, 9cm, 6cm, 3cm, 1.25cm, 6mm, 4mm, 2.5mm, 2mm, 1mm

**AdifMode** core: CW, SSB, AM, FM, RTTY, PSK, MFSK, DATA

**AdifSubmode** core: FT8, FT4, JS8, PSK31 (plus optional: JT65, JT9, OLIVIA, CONTESTIA, PSK63, PSK125)

**QslStatus**: NONE, SENT, CONFIRMED
**LotwStatus/EqslStatus**: UNKNOWN, SENT, CONFIRMED

### Mode/Submode Validation Rules

- If `submode` in {FT8, FT4, JS8} → `mode` must be MFSK
- If `submode` in {PSK31, PSK63, PSK125} → `mode` must be PSK
- If `custom_mode` is not null → `mode` must be DATA and `submode` must be null

### ADIF Export Mapping

| UI Label | mode | submode | custom_mode | ADIF Output |
|----------|------|---------|-------------|-------------|
| CW | CW | null | null | `MODE=CW` |
| SSB | SSB | null | null | `MODE=SSB` |
| PSK31 | PSK | PSK31 | null | `MODE=PSK, SUBMODE=PSK31` |
| FT8 | MFSK | FT8 | null | `MODE=MFSK, SUBMODE=FT8` |
| Other/Custom "VARAC" | DATA | null | "VARAC" | `MODE=DATA, APP_QSOLOG_CUSTOMMODE=VARAC` |

Vendor field format: `<APP_QSOLOG_CUSTOMMODE:{len}>{value}`

### API Endpoints (v1)

**Auth** (`/api/v1/auth`):
- `POST /register` - Create user with BCrypt hash
- `POST /login` - Return JWT access token
- `GET /me` - Current user info

**QSO** (`/api/v1/qso`):
- `POST /` - Create QSO (user-scoped)
- `GET /` - List with filters: `?callsign=&band=&from=&to=&page=&size=&sort=`
- `GET /{id}` - Get single QSO
- `PUT /{id}` - Update QSO
- `DELETE /{id}` - Delete QSO

**Suggestions** (`/api/v1/suggestions`):
- `GET /callsign/{callsign}` - Return last-known name/qth/notes for callsign

**Stats** (`/api/v1/stats`):
- `GET /summary?from=&to=` - Returns countsByBand, countsByMode, countsByDay

**Export** (`/api/v1/export`):
- `GET /adif?from=&to=` - Stream valid ADIF file
- `GET /csv?from=&to=` - Stream CSV

**Callsign Lookup** (`/api/v1/lookup`):
- `GET /{callsign}` - External lookup via HamQTH API (cached, suggestions only)

**AI Helper** (`/api/v1/ai`):
- `POST /qso-description` - Generate description for QSO
- `POST /period-report?from=&to=` - Generate narrative report
- Default: mock adapter (no API key needed)
- Optional: OpenAI adapter with `OPENAI_API_KEY` env var

### Security Requirements

- **NO plain text passwords**: Use BCrypt or Argon2
- **JWT**: Access tokens with expiry, sub=userId, roles=[USER/ADMIN]
- **Multi-tenancy**: All QSO operations filtered by `user_id`
- **CORS**: Configured for local SPA dev
- **Duplicate detection**: Warn on potential duplicates (same callsign + date + band + mode), but allow save

### Database Migrations

- **Flyway**: For schema migrations
- Include JDBC driver ONLY for Flyway (R2DBC for runtime)
- Migrations in `backend/src/main/resources/db/migration/`

### Testing Strategy

- Unit tests for use cases
- Integration tests with Testcontainers PostgreSQL
- Security tests: unauthorized access blocked, ownership enforced
- ADIF export regression tests for mode/submode/customMode mapping (see iteration/6.md for test cases)

### Dependencies & Configuration

- Spring Boot 4 WebFlux
- Spring Data R2DBC
- Spring Security with JWT
- Flyway for migrations
- Actuator (health, info, prometheus optional)
- springdoc-openapi-starter-webflux-ui for Swagger at `/swagger-ui.html`

## Frontend Architecture

### Technology Stack

- Angular 21 (standalone components preferred)
- Angular Material for UI
- HttpClient with JWT interceptor

### Routes

- `/login` - Login form
- `/register` - Registration form
- `/qso` - QSO list with filters
- `/qso/new` - Create new QSO
- `/qso/:id/edit` - Edit QSO
- `/stats` - Stats dashboard with charts

### Auth & Security

- Auth guard protects `/qso` and `/stats`
- JWT token storage: memory + sessionStorage fallback
- Interceptor adds `Authorization: Bearer <token>` header
- **TLS assumption**: Production uses HTTPS, local dev uses `http://localhost`

### Key Features

- **QSO List**: Material table with paginator, sort, filters (callsign, band, date range)
- **QSO Form**: On callsign blur, call `/suggestions` to prefill name/qth/notes
- **Stats Page**: Charts for counts by band/mode/day
- **AI Helper UI**:
  - Button "Generate description" on QSO form
  - Button "Generate weekly report" on Stats page

## Docker & DevOps

### Local Development

`docker/docker-compose.yml`:
- PostgreSQL 16+ on port 5432
- Optional pgAdmin service
- Volumes for persistence

### Containerization

**Backend** (`backend/Dockerfile`):
- Multi-stage build with Maven
- Run as non-root
- Expose 8080

**Frontend** (`frontend/Dockerfile`):
- Build Angular → serve via nginx
- nginx.conf with SPA fallback to `index.html`

### CI/CD (GitHub Actions)

- Backend: `mvn test`, build, optional container build
- Frontend: `npm ci`, `npm test` (headless), `ng build`
- Lint/format checks
- Integration tests with Testcontainers in CI

## Implementation Plan

Follow incremental steps in `/iteration/*.md`:

1. Scaffold backend + DB + migrations + healthchecks
2. PostgreSQL schema via Flyway, R2DBC config, domain enums
3. Implement auth + JWT + security
4. Implement QSO CRUD + validation + tests
5. Implement filtering + pagination + suggestions
6. Implement export (ADIF/CSV) with mode/submode/customMode mapping
7. Implement stats + AI helper ports + mock adapter
8. Scaffold frontend app + auth + guards + layout
9. Implement QSO list + form + suggestions
10. Implement stats page + AI report UI
11. Add Dockerfiles + docker-compose + CI workflows + README

## Environment Variables

- `JWT_SECRET` - Secret for JWT signing
- `DB_URL` - PostgreSQL R2DBC connection URL
- `DB_USER` - Database username
- `DB_PASS` - Database password
- `OPENAI_API_KEY` - Optional for AI helper (falls back to mock)

## Business Rules

- **UTC everywhere**: All dates/times in UTC
- **Roles**: ADMIN and OPERATOR (design allows future "club log" multi-operator extension)
- **ADIF/LoTW requirements**: CALL, QSO_DATE, TIME_ON, MODE, plus BAND or FREQ (MVP requires BAND)
- **QSL tracking**: qslStatus, lotwStatus, eqslStatus
- **No hard duplicate blocking**: Detect and warn, let user confirm
- **Callsign lookup**: HamQTH API, cached, suggestions only (never auto-overwrite)
- **AI helper**: PL/EN support, neutral tone, keep history

## Anti-Hallucination Rules

1. Never assume files exist - inspect repo tree first
2. Make only changes needed for current step
3. After implementing, run build/tests and fix failures
4. Mark TODOs clearly with "TODO(step-X)", keep app runnable
5. After finishing, print updated file tree and run instructions
