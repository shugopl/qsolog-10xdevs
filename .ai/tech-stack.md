# Ham Radio Logbook Assistant — Tech Stack

**Version:** 1.0 (MVP)  
**Date:** 2026-01  
**Goal:** SPA do logowania QSO + reaktywny backend + import/eksport ADIF + integracje (HamQTH) + asystent AI (notes + raporty)

---

## 1) High-level architecture

- **Frontend:** Angular 21 (SPA)
- **Backend:** Java 25 + Spring Boot 4 + WebFlux (Reactive)
- **Database:** PostgreSQL 15+ (R2DBC)
- **Migrations:** Flyway
- **Auth:** JWT (access token; refresh token opcjonalnie poza MVP)
- **External integrations:** HamQTH (callbook lookup)
- **AI:** MOCK / Ollama / OpenAI (w MVP dopuszczalny stub; tekst jako sugestia)
- **Docs:** OpenAPI 3.0 (Springdoc)
- **Dev runtime:** Docker Compose (dev), opcjonalnie K8s/Helm (post-MVP)

---

## 2) Frontend (Angular 21)

### 2.1 Core
- **Framework:** Angular 21
- **Approach:** SPA + modular routing
- **Forms:** Signal Forms + Reactive Forms
- **HTTP:** HttpClient + interceptory
- **State:** komponentowy + serwisy; ewentualnie sygnały
- **Auth storage:** Local Storage (JWT) + interceptor `Authorization: Bearer`
- **UX requirements:** loading/empty/error states, dostępność (ARIA, keyboard nav)

### 2.2 Suggested libraries (MVP-friendly)
- UI: Angular Material *lub* Tailwind (decyzja projektowa; oba OK)
- Charts (opcjonalnie): `ngx-charts`
- Validation: wbudowane Angular validators (+ custom)
- i18n: Angular i18n lub prosty mechanizm (PL/EN)

### 2.3 App modules (proposed)
- `auth` (login/register/profile)
- `qso-log` (list, filters, form, details)
- `stats` (overview, per-band)
- `ai-assistant` (qso-note, summary, history)
- `shared` (layout, components, interceptors)

---

## 3) Backend (Java 25 + Spring Boot 4 + WebFlux)

### 3.1 Core
- **Language:** Java 25
- **Framework:** Spring Boot 4.0.x
- **Web:** Spring WebFlux (Reactive)
- **DB access:** R2DBC + PostgreSQL
- **Migrations:** Flyway (SQL)
- **DTO:** Java records
- **Error handling:** global `@ControllerAdvice` + spójny format błędów (problem details / custom)

### 3.2 Persistence
- PostgreSQL 15+
- Indeksy (min.):
  - `qso (operator_id, qso_date desc)`
  - `qso (operator_id, band)`
  - `qso (operator_id, their_callsign)`
  - `ai_reports (operator_id, generated_at desc)`

### 3.3 Security
- HTTPS (prod)
- JWT auth (stateless)
- CORS configurable
- Rate limiting:
  - HamQTH: max 10 lookup/min/user
  - AI endpoints: max 10 req/min/user
- Password hashing: BCrypt/Argon2 (prefer Argon2 jeśli łatwo dostępne)

### 3.4 Integrations
- **HamQTH client:** WebClient
- **Cache:** Caffeine (TTL 24h) lub Redis (opcjonalnie)
- **AI service:**
  - `AI_SERVICE_TYPE=MOCK|OLLAMA|OPENAI`
  - w MVP dopuszczalne: deterministyczny mock + testy

---

## 4) API standards

- Base path: `/api/v1`
- Content-type: `application/json`
- Pagination: `page`, `size`, `sort`
- Filtering: query params (`callsign`, `dateFrom`, `dateTo`, `band`, `mode`, `qslStatus`, itp.)
- Idempotency (import): dupe-check + raport importu

---

## 5) Tooling, quality, testing

### 5.1 Backend tests
- JUnit 5 + Mockito
- WebTestClient (integration)
- Testcontainers (opcjonalnie, ale rekomendowane dla Postgresa)
- Coverage target: ≥ 70%

### 5.2 Frontend tests
- Unit tests: Jasmine/Karma lub Vitest (w zależności od setup)
- Coverage target: ≥ 70% (target edukacyjny)

### 5.3 Static analysis
- Backend: Spotless/Checkstyle + (opcjonalnie) Sonar
- Frontend: ESLint + Prettier

---

## 6) Observability

- Logs: JSON/logback pattern (prod-friendly)
- Actuator: `/actuator/health`
- Metrics (opcjonalnie): Micrometer + Prometheus (post-MVP)

---

## 7) Deployment

### 7.1 Dev
- `docker-compose.yml`: postgres + backend + frontend (nginx dla static)
- Seed data: opcjonalnie (developer convenience)

### 7.2 Prod (opcjonalnie post-MVP)
- Kubernetes manifests / Helm
- Ingress + TLS
- Secrets: JWT_SECRET, DB creds, HamQTH creds, AI creds

---

## 8) Environment variables (backend)

- `SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/hamradio_db`
- `SPRING_R2DBC_USERNAME=postgres`
- `SPRING_R2DBC_PASSWORD=password`
- `JWT_SECRET=<min-32-chars>`
- `JWT_EXPIRATION_MS=86400000`
- `HAMQTH_API_URL=https://www.hamqth.com/api/`
- `HAMQTH_CACHE_TTL_HOURS=24`
- `HAMQTH_RATE_LIMIT_PER_MINUTE=10`
- `AI_SERVICE_TYPE=MOCK`  *(or OLLAMA / OPENAI)*
- `AI_SERVICE_URL=http://localhost:11434` *(dla Ollama)*
- `AI_SERVICE_MODEL=llama2` *(przykład)*

---
