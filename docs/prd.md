# prd.md
# Ham Radio Logbook Assistant — PRD

**Projekt:** aplikacja do logowania QSO z asystentem AI  
**Wersja:** 1.0 MVP  
**Data:** 2026-01  
**Stack:** Angular 21 + Java 25 + Spring Boot 4 + WebFlux + PostgreSQL (R2DBC)  

---

## 1) Wstęp i cel

### 1.1 Cel biznesowy
Stworzenie webowej aplikacji do rejestrowania i zarządzania kontaktami krótkofalarskimi (QSO), wspierającej:
- logowanie QSO (UTC: data/czas, callsign, pasmo, tryb, statusy QSL),
- historię QSO (tabela, sortowanie, filtrowanie, paginacja),
- import/eksport logów (ADIF + CSV),
- integracje: lookup HamQTH,
- asystenta AI: generowanie notatek QSO + raportów okresowych (tydzień/miesiąc).

### 1.2 Cel edukacyjny (zaliczeniowy)
Projekt demonstruje:
- Angular 21 (Signal Forms, reactive forms, nowoczesny setup),
- reaktywny backend WebFlux + R2DBC (Java 25),
- REST API + testy + migracje bazy (Flyway),
- JWT auth i podstawy hardeningu (rate limit, CORS),
- użycie AI jako pair-programmer (prompty, refaktoryzacje, dokumentacja).

### 1.3 Zakres
**In-scope (MVP):**
- konta operatorów: register/login/profile
- CRUD QSO + dupe-check
- lista QSO: filtrowanie + sortowanie + paginacja
- statystyki overview (podstawowe)
- import/export: ADIF + CSV
- HamQTH lookup (z cache + rate limit)
- AI: generowanie sugestii (notes + summary) + historia raportów

**Out-of-scope (post-MVP):**
- WebSocket realtime
- contest mode
- pełna integracja LoTW sync
- multi-user shared logbooks
- mobile app native (PWA opcjonalnie)

---

## 2) Użytkownicy i scenariusze

### 2.1 Persony
- **Operator (radioamator):** prowadzi własny log QSO, chce szybko dodawać łączności, analizować statystyki, eksportować do ADIF.
- **Admin (opcjonalnie):** brak w MVP (może pojawić się później).

### 2.2 Kluczowe scenariusze (happy paths)
1. Rejestracja → logowanie → dodanie pierwszego QSO → przegląd listy.
2. Dodanie QSO + lookup HamQTH → AI generuje notkę → operator zapisuje.
3. Filtr po callsign i dacie → export ADIF → import do innego logbooka.
4. Import ADIF → raport: nowe / zaktualizowane / pominięte.
5. Wygenerowanie raportu tygodniowego AI → zapis do historii.

### 2.3 Edge cases (przykłady)
- duplikat QSO przy tym samym `(theirCallsign, qsoDate, band, mode)` → ostrzeżenie, możliwość zapisu mimo to
- błędy HamQTH (limit, timeout) → fallback do ręcznego wypełnienia
- AI niedostępne → UI pokazuje komunikat, funkcja nie blokuje CRUD

---

## 3) Wymagania funkcjonalne (FR)

### 3.1 Konta operatorów
**FR-01 Rejestracja**
- pola: email, hasło, callsign (opcjonalnie imię/QTH)
- walidacja: email unique, hasło min. 8 znaków

**FR-02 Logowanie**
- email + hasło → JWT access token
- przechowywanie po stronie SPA (Local Storage)
- refresh token: opcjonalnie (poza MVP)

**FR-03 Profil operatora**
- edycja: imię, QTH, grid square (opcjonalnie), język (PL/EN), callsign
- wylogowanie

### 3.2 CRUD QSO
**FR-04 Dodawanie QSO**
- wymagane: `theirCallsign`, `qsoDate(UTC)`, `timeOn(UTC)`, `band`, `mode`
- opcjonalne: `frequency`, `rstSent`, `rstRcvd`, `qth`, `gridSquare`, `qslStatus`, `lotwStatus`, `eqslStatus`, `notes`
- UI: Signal Forms + realtime walidacja
- akcje:
  - lookup HamQTH (sugestie; nie nadpisuj automatycznie)
  - „Wygeneruj opis AI” → sugestia notki do `notes`

**FR-05 Duplikat detection**
- przy zapisie/edycji: jeśli istnieje QSO z `(theirCallsign, qsoDate, band, mode)` → ostrzeżenie + decyzja użytkownika

**FR-06 Lista QSO**
- tabela z: data, czas, callsign, pasmo, tryb, QSL status
- sortowanie: data, band, mode, callsign
- paginacja: default 20/strona

**FR-07 Filtrowanie QSO**
- callsign (case-insensitive contains)
- zakres dat
- pasmo (multi)
- tryb (multi)
- QSL status (NONE, SENT, CONFIRMED)

**FR-08 Edycja QSO**
- edycja dowolnych pól
- refresh HamQTH
- regeneracja opisu AI

**FR-09 Usuwanie QSO**
- potwierdzenie usunięcia
- MVP: hard delete (soft delete opcjonalnie)

### 3.3 Statystyki i raporty
**FR-10 Dashboard / Overview**
- liczba wszystkich QSO
- liczba potwierdzonych QSO
- top 5 pasm
- top 5 krajów (jeśli dane dostępne)
- liczba unikalnych callsignów

**FR-11 Raport okresowy AI**
- okres: tydzień / miesiąc (lub zakres dat)
- AI zwraca tekst PL/EN (wg profilu)
- przyciski: kopiuj / wstaw do notes / regeneruj

**FR-12 Historia raportów AI**
- lista raportów (data, okres)
- podgląd + kopiowanie

### 3.4 Import/Export
**FR-13 Eksport ADIF**
- eksport wszystkich / wybranych QSO do `.adi`
- mapowanie pól ADIF (CALL, QSO_DATE, TIME_ON, BAND/FREQ, MODE, RST, QTH, GRIDSQUARE, QSL_STATUS, NOTES…)

**FR-14 Eksport CSV**
- kolumny: data, czas, callsign, band, mode, qsl, notatki

**FR-15 Import ADIF**
- upload `.adi` → parsowanie
- dla każdego rekordu:
  - match po `(theirCallsign, qsoDate, band, mode, timeOnRounded)`
  - jeśli brak → create
  - jeśli jest → update: statusy QSL/LoTW/eQSL, notatki (gdy puste)
- wynik: X nowych, Y zaktualizowanych, Z pominiętych/błędnych

**FR-16 Import CSV**
- upload CSV → bulk create + ostrzeżenia duplikatów
- wynik importu analogiczny

### 3.5 Integracje
**FR-17 HamQTH lookup**
- endpoint: input callsign → dane (imię/QTH/grid/kraj…)
- cache TTL 24h
- rate limit: max 10 lookup/min/user
- UI jako sugestie do akceptacji

**FR-18 Asystent AI**
- `/api/v1/ai/qso-note`: generuje opis notki QSO
- `/api/v1/ai/summary`: generuje raport okresowy + zapis do `ai_reports`
- polityka: sugestie, użytkownik decyduje o zapisie

---

## 4) Model danych (MVP)

### 4.1 Operator
- `id: UUID`
- `email: String (unique)`
- `passwordHash: String`
- `callsign: String`
- `firstName: String?`
- `qth: String?`
- `gridSquare: String?`
- `preferredLanguage: PL|EN`
- `createdAt, updatedAt`
- `deletedAt?` (soft delete opcjonalnie)

### 4.2 Qso
- `id: UUID`
- `operatorId: UUID`
- `theirCallsign: String`
- `qsoDate: LocalDate (UTC)`
- `timeOn: LocalTime (UTC)`
- `timeOff: LocalTime?` (opcjonalnie)
- `band: enum`
- `frequency: Double?`
- `mode: enum`
- `customMode: String?` (gdy mode=OTHER)
- `rstSent: String?`, `rstRcvd: String?`
- `qth: String?`, `gridSquare: String?`
- `qslStatus: NONE|SENT|CONFIRMED`
- `lotwStatus: UNKNOWN|SENT|CONFIRMED`
- `eqslStatus: UNKNOWN|SENT|CONFIRMED`
- `notes: String?`
- `lookupSource: HAMQTH|QRZ|MANUAL?`
- `lookupLastUpdatedAt: LocalDateTime?`
- `createdAt, updatedAt`

### 4.3 AiReport
- `id: UUID`
- `operatorId: UUID`
- `periodStart: LocalDate`
- `periodEnd: LocalDate`
- `language: PL|EN`
- `content: String`
- `generatedAt: LocalDateTime`

---

## 5) Wymagania niefunkcjonalne (NFR)

### 5.1 Wydajność
- API: typowe operacje CRUD/list < 500ms
- Frontend: initial load < 3s (lazy loading, gzip)

### 5.2 Skalowalność
- WebFlux (non-blocking)
- R2DBC (reactive)
- cache lookup HamQTH (Caffeine/Redis)

### 5.3 Bezpieczeństwo
- HTTPS w produkcji
- JWT auth, brak sesji cookie
- prepared statements (R2DBC)
- CORS konfigurowalny
- rate limiting (AI + HamQTH)
- soft delete operatora (opcjonalnie)

### 5.4 Dostępność (a11y)
- ARIA labels + semantic HTML
- keyboard navigation
- kontrast >= 4.5:1 (AA)

### 5.5 Testowanie i jakość
- Backend: JUnit5/Mockito + WebTestClient
- Frontend: unit tests (Jasmine/Karma lub Vitest)
- Coverage target: ≥ 70%
- OpenAPI 3.0 + README

---

## 6) API (MVP)

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET/PUT /api/v1/operator/profile`
- `GET/POST /api/v1/qso`
- `GET/PUT/DELETE /api/v1/qso/{id}`
- `POST /api/v1/import/adif`
- `POST /api/v1/import/csv`
- `GET /api/v1/export/adif`
- `GET /api/v1/export/csv`
- `GET /api/v1/stats/overview`
- `GET /api/v1/stats/per-band`
- `POST /api/v1/lookup/hamqth` *(lub GET /{call})*
- `POST /api/v1/ai/qso-note`
- `POST /api/v1/ai/summary`
- `GET /api/v1/ai/reports`
- `GET /actuator/health`

---

## 7) Plan realizacji (MVP)

### Sprint 1: Backend foundation
- WebFlux + R2DBC + Postgres + Flyway
- encje + repo
- JWT security

### Sprint 2: Core API
- auth + profile
- CRUD QSO + dupe-check + filtering
- testy unit + integration

### Sprint 3: Frontend core
- auth flow + guard + interceptory
- QsoList + QsoForm (Signal Forms)
- filtrowanie/sortowanie/paginacja

### Sprint 4: Integracje + AI
- HamQTH (cache + rate limit) + UI
- AI endpoints (MOCK) + UI + historia raportów

### Sprint 5: Import/Export + stats
- ADIF import/export, CSV import/export
- stats endpoints + dashboard

### Sprint 6: Hardening + docs
- error handling, edge cases
- Docker setup
- OpenAPI + README + AI prompts docs

---

## 8) Kryteria akceptacji (DoD)

- testy przechodzą, coverage ≥ 70%
- OpenAPI dostępne i spójne z implementacją
- obsługa błędów: 400/401/404/500 + komunikaty UI
- migracje Flyway: czyste i powtarzalne
- build Docker bez błędów
- README: setup + develop + deploy
- dokumentacja AI pair-programming kompletna (prompty + decyzje)

---
