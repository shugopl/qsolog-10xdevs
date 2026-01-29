You are Claude Code working inside a mono-repo. Implement a production-quality training project:
A web app for ham radio QSO logging: Angular 21 SPA + Java 25 Spring Boot 4 WebFlux reactive backend + PostgreSQL (R2DBC).
User story: operator registers/logs in, creates/updates QSO records, filters list, views basic stats, exports CSV/ADIF (ADIF can be staged), and uses an AI helper for text summaries.

CRITICAL RULES
- Produce working code and repository structure.
- Follow SOLID and Clean Architecture boundaries.
- Use reactive stack: Spring WebFlux + R2DBC + Reactor.
- Passwords MUST NOT be stored in plain text. Use BCrypt or Argon2.
- Authentication/authorization: JWT bearer tokens.
- API must be versioned (/api/v1).
- Provide CI/CD pipelines and local PostgreSQL Docker setup.
- Frontend uses Angular Material.
- Provide tests (unit + integration) for key flows.
- Provide README with run instructions.

NON-NEGOTIABLE SECURITY BASELINE
- Assume transport security (HTTPS) in real environments. For local dev use http://localhost allowed.
- Backend stores only password hashes (BCrypt/Argon2).
- JWT access tokens with expiry; refresh tokens optional (nice-to-have).
- Validate input, enforce per-user ownership of QSO records.
- Do not leak sensitive data in logs.

MONOREPO STRUCTURE (create this)
/
  backend/
  frontend/
  docker/
  .github/workflows/   (or gitlab-ci.yml if you choose GitLab; pick ONE and implement fully)
  README.md

========================
1) BACKEND (Java 25, Spring Boot 4, WebFlux, PostgreSQL, R2DBC)
========================

1.1 Architecture & Modules (inside /backend)
Use a clean layered structure:
- api (controllers, request/response DTOs)
- application (use cases, services orchestration)
- domain (entities, value objects, domain services, ports)
- infrastructure (persistence adapters, security, external clients, configuration)

Prefer packages like:
com.example.qsolog
  api
  application
  domain
  infrastructure

1.2 Data model (PostgreSQL)
Create tables:
- users:
  id (uuid pk), email (unique), username (unique), password_hash, created_at, updated_at
- qso:
  id (uuid pk), user_id (fk users.id), callsign,
  start_time (timestamp), end_time (timestamp nullable),
  band (varchar), frequency_khz (numeric), mode (varchar),
  rst_sent (varchar), rst_recv (varchar),
  qth (varchar), name (varchar), notes (text),
  qsl_status (varchar), created_at, updated_at

Indexes:
- qso(user_id, start_time desc)
- qso(user_id, callsign)
- qso(user_id, band)

Migrations:
- Use Flyway for schema migrations. Because R2DBC is used for runtime, still include JDBC driver ONLY for Flyway migrations at startup (common pattern).
- Provide V1__init.sql etc.

1.3 R2DBC persistence
- Reactive repositories: Spring Data R2DBC.
- Map entities properly.
- Ensure multi-tenant by user_id ownership filters.

1.4 Auth: Spring Security + JWT
Implement endpoints:
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me

- register: email/username/password -> create user with BCrypt hash.
- login: verify password -> issue JWT access token.
- JWT contains: sub=userId, username, roles=[USER], exp.
- Secure all /api/v1/qso/** and /api/v1/stats/** endpoints with Bearer JWT.
- Add CORS config for local SPA dev.

1.5 QSO CRUD API (MVP)
Endpoints:
POST   /api/v1/qso
GET    /api/v1/qso?callsign=&band=&from=&to=&page=&size=&sort=
GET    /api/v1/qso/{id}
PUT    /api/v1/qso/{id}
DELETE /api/v1/qso/{id}

Rules:
- All operations scoped to authenticated user.
- Validate fields (e.g., callsign not blank, start_time required, band/mode from enums if you define them).
- Filtering: by callsign (contains/startsWith), band, date range (from-to).
- Pagination: page/size; default size e.g. 20.

1.6 Suggestions (nice-to-have)
Endpoint:
GET /api/v1/suggestions/callsign/{callsign}
Behavior:
- If the user has previous QSOs with this callsign, return last-known name/qth/notes hints.
- Optionally return most common mode/band.

1.7 Stats (MVP)
Endpoint:
GET /api/v1/stats/summary?from=&to=
Return:
- countsByBand: [{band, count}]
- countsByMode: [{mode, count}]
- countsByDay: [{date, count}]  (for chart)
All computed by classic DB queries (reactive).

1.8 Export (nice-to-have)
Endpoints:
GET /api/v1/export/csv?from=&to=
GET /api/v1/export/adif?from=&to=   (can be TODO but include skeleton & tests for csv)
- CSV should stream as text/csv (reactive streaming if possible).
- ADIF can be minimal and correct enough for basic import.

1.9 External callsign lookup (nice-to-have)
- Create a port in domain: CallsignLookupPort
- Provide infrastructure adapter: mock client (returns sample) + optional real client placeholder
Endpoint:
GET /api/v1/lookup/{callsign}

1.10 AI Helper (nice-to-have but scaffold well)
Provide two endpoints:
POST /api/v1/ai/qso-description   (takes QSO payload, returns PL/EN text)
POST /api/v1/ai/period-report?from=&to=   (uses stats + AI to produce narrative)
Implementation:
- Create AiHelperPort in domain.
- Provide default adapter that returns deterministic mock text (no external key needed).
- Provide optional OpenAI adapter behind profile "openai" with env var OPENAI_API_KEY (do not hardcode).
- Ensure the app works without OpenAI key (mock mode default).

1.11 Observability & production readiness
- Actuator endpoints: health, info, prometheus (optional).
- Structured logging (logback) without sensitive payloads.
- Global error handling (ProblemDetails / RFC7807 style).
- OpenAPI/Swagger for WebFlux (springdoc-openapi-starter-webflux-ui) with /swagger-ui.html.

1.12 Tests
- Unit tests for UseCases.
- Integration tests with Testcontainers PostgreSQL (recommended) OR R2DBC + docker for CI.
- Security tests: unauthorized access blocked; ownership enforced.

1.13 Backend deliverables
Create:
- backend/pom.xml with required dependencies
- application.yaml (dev) + application-prod.yaml (production placeholders)
- Flyway migration SQL
- Complete controllers, services/usecases, repositories, security config, jwt util, DTOs, mappers
- README section for backend run

========================
2) FRONTEND (Angular 21 + Angular Material SPA)
========================

2.1 App requirements
- Angular 21, standalone components preferred.
- Angular Material for UI.
- Routing:
  /login
  /register
  /qso
  /qso/new
  /qso/:id/edit
  /stats
- Auth guard to protect /qso and /stats.
- Use HttpClient with interceptor adding Authorization: Bearer <token>.
- Store tokens securely:
  - Use memory + sessionStorage fallback (explain tradeoffs). Prefer short-lived access tokens.
  - Avoid storing password anywhere.

2.2 "Password not sent in plain text" interpretation
- Implement HTTPS assumption: for production, frontend must call https backend.
- For local dev: http://localhost ok.
- Do NOT implement fake encryption that provides no benefit.
- Optionally implement client-side hashing (SHA-256) BEFORE sending, but then backend must store hash-of-hash and you lose bcrypt benefits unless carefully designed; so keep it OFF by default and document why.
=> Implement correct baseline: TLS + backend bcrypt.

2.3 Screens
- Login/Register forms (Material form fields, validation)
- QSO list with:
  - table (MatTable) + paginator + sort
  - filters: callsign, band, date range
  - actions: edit/delete
- QSO form:
  - fields described in requirements
  - suggestions: on callsign blur call /suggestions and prefill name/qth/notes if found
- Stats page:
  - show charts (you can use a lightweight chart library or implement simple SVG/canvas; keep it stable)
  - counts by band/mode/day
- AI helper UI:
  - On QSO form: button "Generate description" -> calls /ai/qso-description and displays text; user can copy into notes.
  - On Stats page: button "Generate weekly report" -> calls /ai/period-report for selected range.

2.4 Frontend deliverables
- Angular project under /frontend
- environment.ts pointing to backend base URL
- Material theme setup
- interceptor, auth service, guards
- components for pages and shared UI
- tests: at least for AuthService + one component (basic)

========================
3) DOCKER & LOCAL DEV DATABASE
========================

3.1 Local Postgres
Under /docker provide:
- docker-compose.yml with postgres:16+ (or latest stable), exposed 5432
- volumes for persistence
- optional pgadmin service (nice-to-have)
- init scripts optional (but Flyway in backend will create schema)

Also provide a standalone Dockerfile that starts postgres for local testing if specifically requested,
but docker-compose is recommended.

3.2 Backend containerization (production-like)
Provide backend/Dockerfile:
- Multi-stage build with Maven
- Run as non-root
- Expose 8080
- ENV placeholders

3.3 Frontend containerization
Provide frontend/Dockerfile:
- Build Angular -> serve via nginx
- Provide nginx.conf with SPA fallback to index.html

========================
4) CI/CD
========================
Pick GitHub Actions and implement:
- backend: mvn test, build, (optionally) container build
- frontend: npm ci, npm test (or ng test headless), ng build
- lint/format checks
- Optionally run integration tests with Testcontainers in CI
- On main: build docker images (no push necessary, but pipeline must be ready)

========================
5) DOCUMENTATION
========================
Create top-level README.md:
- Architecture overview diagram (ASCII ok)
- How to run local: docker-compose up (postgres) + backend + frontend
- Env vars: JWT_SECRET, DB_URL, DB_USER, DB_PASS, OPENAI_API_KEY(optional)
- API endpoints list
- Security notes and why TLS is required

========================
IMPLEMENTATION PLAN
========================
Execute in incremental commits (or clear steps):
1) Scaffold backend project + DB + migrations + healthchecks
2) Implement auth + JWT + security
3) Implement QSO CRUD + validation + tests
4) Implement filtering + pagination + suggestions
5) Implement stats + charts DTO
6) Scaffold AI helper ports + mock adapter + endpoints
7) Scaffold frontend app + auth + guards + layout
8) Implement QSO list + form + suggestions
9) Implement stats page + AI report UI
10) Add Dockerfiles + docker-compose + CI workflows + README polish

OUTPUT REQUIREMENTS
- Actually create/modify files in the repo as needed.
- Show a final tree of created files.
- Ensure the app runs locally with clear instructions.
- Keep code clean, consistent naming, and production-quality defaults.

Start now by creating the repository structure, then implement backend first (steps 1-6), then frontend (steps 7-9), then DevOps (10).
