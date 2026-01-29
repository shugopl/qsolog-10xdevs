# Ham Radio Logbook Assistant – Product Requirements Document (PRD)

**Projekt**: Aplikacja do logowania kontaktów krótkofalarskich (QSO) z asystentem AI  
**Stack techniczny**: Angular 21 (frontend) + Spring Boot 4 (Java 25) + WebFlux (backend)  
**Wersja**: 1.0 MVP  
**Data**: styczeń 2026  

---

## 1. Wstęp i cel projektu

### 1.1 Cel biznesowy

Stworzenie webowej aplikacji do logowania i zarządzania kontaktami krótkofalarskimi (QSO – radio frequency contacts) dla radioamatorów. System wspiera:

- rejestrowanie datę, czas, znak stacji, pasmo, tryb i statusy QSL,
- przeglądanie historii łączności z filtrowaniem i statystykami,
- import/eksport logów w standardzie ADIF (Amateur Data Interchange Format),
- asystent AI generujący opisy QSO i raporty tygodniowe/miesięczne,
- lookup danych operatorów z callbooka HamQTH.

### 1.2 Cel edukacyjny (zaliczeniowy)

Projekt demonstruje:

- nowoczesny stack frontendowy (Angular 21 z Signal Forms, reactive forms, zone‑less change detection),
- reaktywny backend (Spring Boot 4 + WebFlux + R2DBC) w języku Java 25,
- współpracę AI jako „pair programmer" – dokumentacja promptów, refaktoryzacja, generacja kodu,
- best practices: REST API, testowanie, model danych ADIF‑compatible, bezpieczeństwo (JWT auth).

---

## 2. Wymagania funkcjonalne

### 2.1 Zarządzanie kontami operatorów

**F1. Rejestracja**

- Formularz: email, hasło, callsign (opcjonalnie imię/QTH).
- Walidacja: email unique, hasło min. 8 znaków.
- Możliwość zalogowania się potem wyłącznie z email + hasło.

**F2. Logowanie**

- Email + hasło.
- JWT token zwracany po pomyślnym logowaniu, przechowywany w local storage (SPA).
- Refresh token (opcjonalnie, na MVP może być bez refresh).

**F3. Profil operatora**

- Edycja danych: imię, QTH, grid square (opcjonalnie), preferowany język (PL/EN), callsign.
- Wylogowanie.

### 2.2 CRUD QSO (Create, Read, Update, Delete)

**F4. Dodawanie nowego QSO**

Formularz zawiera:

**Wymagane pola** (MVP minimum):
- `theirCallsign` – znak korespondenta (tekst, alphanumeric + cyferki),
- `qsoDate` – data w UTC (datepicker),
- `timeOn` – czas UTC start (time picker, HH:MM),
- `band` – dropdown z enum: 160m, 80m, 40m, 30m, 20m, 17m, 15m, 12m, 10m, 6m, 2m, 70cm,
- `mode` – dropdown z enum: CW, SSB, AM, FM, RTTY, PSK31, FT8, FT4, JS8, itp.

**Opcjonalne pola** (nice‑to‑have w MVP):
- `frequency` – częstotliwość w MHz (jeśli brak w band),
- `rstSent`, `rstRcvd` – raporty sygnałowe (np. „5/9", „559"),
- `qth` – lokalizacja korespondenta,
- `gridSquare` – grid operatora,
- `qslStatus` – enum: NONE, SENT, CONFIRMED,
- `lotwStatus` – enum: UNKNOWN, SENT, CONFIRMED,
- `eqslStatus` – enum: UNKNOWN, SENT, CONFIRMED,
- `notes` – wolny tekst.

**Obsługa formularz**:
- Signal Forms (Angular 21) dla reaktywności,
- real‑time walidacja,
- przycisk „Szukaj w callbooku" (lookup HamQTH) – jeśli callsign znaleziony, auto‑uzupełnienie imienia/QTH/grid (sugestie, nie nadpis),
- przycisk „Wygeneruj opis AI" – asystent AI zasugeruje krótki tekst dla notes.

**Duplikat detection**:
- Przy zapisie: jeśli QSO z tym samym `(theirCallsign, qsoDate, band, mode)` już istnieje → ostrzeżenie „To wygląda jak duplikat QSO. Czy chcesz zapisać?".

**F5. Przeglądanie listy QSO**

- Tabela / lista przeskrolowalna z kolumnami: data, czas, callsign, pasmo, tryb, QSL status.
- Sortowanie po kolumnach (data, band, mode, callsign).
- Paginacja (np. 20 na stronę).

**F6. Filtrowanie QSO**

- Po callsign (szukanie tekstu, case‑insensitive).
- Po dacie (range).
- Po paśmie (multi‑select).
- Po trybie (multi‑select).
- Po QSL statusie (NONE, SENT, CONFIRMED).

**F7. Edycja QSO**

- Edycja dowolnych pól istniejącego QSO.
- Duplikat detection przy aktualizacji (ostrzeżenie, jeśli zmiana prowadzi do kolizji).
- Przycisk „Odśwież dane z callbooka" (refresh HamQTH lookup).
- Przycisk „Regeneruj opis AI".

**F8. Usuwanie QSO**

- Potwierdzenie „Czy usunąć?".
- Soft delete (opcjonalnie, na MVP hard delete wystarczy).

### 2.3 Statystyki i raporty

**F9. Dashboard / Overview**

- Liczba wszystkich QSO,
- liczba potwierdzonych QSO (QSL CONFIRMED),
- top 5 pasm (liczba QSO per band),
- top 5 krajów (jeśli dostępne dane),
- liczba różnych callsignów (DXCC contacts).

**F10. Statystyki szczegółowe**

- Widok per pasmo: liczba QSO, top kraje, top operatorzy,
- wykres (opcjonalnie ngx‑charts): QSO per band, trend czasowy (dzienna/tygodniowa/miesięczna liczba QSO).

**F11. Raport okresowy (asystent AI)**

- Wybór zakresu: ostatni tydzień / ostatni miesiąc.
- Zapytanie do AI: „Podsumuj moją aktywność za ten okres".
- AI zwraca tekst (PL/EN) typu: „W ostatnim tygodniu przeprowadziłeś 23 łączności, głównie na 20m i 15m w trybie SSB. Najczęstszym partnerem były stacje z DL (8 QSO) i SP (6 QSO). Postęp: 0 potwierdzeń QSL."
- UI: przycisk „Wstaw do notes" (kopiuj do schowka), przycisk „Regeneruj".

**F12. Historia raportów AI**

- Widok: wszystkie wygenerowane raporty periodyczne (data, okres, zawartość),
- możliwość podglądu / ponownego skopiowania.

### 2.4 Import/Export

**F13. Eksport ADIF**

- Eksport wybranych QSO lub wszystkich do pliku `.adi` (ADIF format).
- Zawartość: CALL, QSO_DATE, TIME_ON, BAND/FREQ, MODE, RST_SENT, RST_RCVD, QTH, GRIDSQUARE, QSL_STATUS, NOTES, itp.
- Kompatybilność z LoTW, QRZ, innymi logbookami.

**F14. Eksport CSV**

- Eksport do CSV (data, czas, callsign, band, mode, qsl, notatki).
- Przydatne do importu do Excela.

**F15. Import ADIF**

- Upload pliku `.adi`.
- Parsowanie ADIF (biblioteka Java do ADIF).
- Dla każdego importowanego QSO:
  - szukaj istniejącego po kluczu `(theirCallsign, qsoDate, band, mode, timeOnRounded)`,
  - jeśli nie znalazłeś → utwórz nowy,
  - jeśli znalazłeś → aktualizuj pola: QSL status, LoTW/eQSL status, notatki (jeśli są puste u nas),
- Raport z importu: X nowych, Y zaktualizowanych, Z pominiętych (duplikaty / błędy).

**F16. Import CSV**

- Upload CSV (format: data, czas, callsign, band, mode, notatki, itp.).
- Bulk create nowych QSO z dupe‑checkiem (ostrzeżenia, nie hard блок).
- Raport z importu.

### 2.5 Integracje

**F17. Lookup HamQTH**

- Input callsign → zapytanie do API HamQTH,
- odpowiedź: imię, QTH, grid, kraj, itp.,
- cache in‑memory (TTL 24h),
- rate limit (max 10 lookupów/minutę na użytkownika),
- wyniki wyświetlane jako sugestie (user może je zaakceptować, edytować albo odrzucić).

**F18. Asystent AI (QSO notes + raporty)**

- Endpoint `/api/ai/qso-note`:
  - input: QSO details (callsign, band, mode, czas, notatki),
  - output: wygenerowany opis (PL/EN, zależnie od preferencji),
  - tekst jako suggestion (nie auto‑zapis).
- Endpoint `/api/ai/summary`:
  - input: data_start, data_end, language,
  - output: raport tekstowy aktywności operatora za ten okres,
  - zapis do tabeli `ai_reports` (historia).
- Implementacja: stub LLM (można mockować odpowiedzi) lub integracja z Ollama/ChatGPT (opcjonalnie).
- Polityka: teksty są sugestiami, użytkownik decyduje o zapisie.

---

## 3. Model danych

### 3.1 Encje

#### Operator
```
id: UUID
email: String (unique)
passwordHash: String
callsign: String
firstName: String (nullable)
qth: String (nullable)
gridSquare: String (nullable)
preferredLanguage: enum { PL, EN }
createdAt: LocalDateTime
updatedAt: LocalDateTime
deletedAt: LocalDateTime (nullable, soft delete optional)
```

#### Qso
```
id: UUID
operatorId: UUID (FK -> Operator)
theirCallsign: String
qsoDate: LocalDate (UTC)
timeOn: LocalTime (UTC, start)
timeOff: LocalTime (nullable, optional)
band: enum { M160, M80, M40, M30, M20, M17, M15, M12, M10, M6, M2, CM70 }
frequency: Double (MHz, nullable)
mode: enum { CW, SSB, AM, FM, RTTY, PSK31, FT8, FT4, JS8, ... }
customMode: String (nullable, jeśli mode == OTHER)
rstSent: String (nullable, np. "5/9")
rstRcvd: String (nullable, np. "5/8")
qth: String (nullable)
gridSquare: String (nullable)
qslStatus: enum { NONE, SENT, CONFIRMED }
lotwStatus: enum { UNKNOWN, SENT, CONFIRMED }
eqslStatus: enum { UNKNOWN, SENT, CONFIRMED }
notes: String (nullable)
lookupSource: enum { HAMQTH, QRZ, MANUAL } (nullable)
lookupLastUpdatedAt: LocalDateTime (nullable)
createdAt: LocalDateTime
updatedAt: LocalDateTime
```

#### AiReport
```
id: UUID
operatorId: UUID (FK -> Operator)
periodStart: LocalDate
periodEnd: LocalDate
language: enum { PL, EN }
content: String (długi tekst)
generatedAt: LocalDateTime
```

### 3.2 Indeksy

- `Qso`: (operatorId, qsoDate DESC), (operatorId, band), (operatorId, theirCallsign).
- `AiReport`: (operatorId, generatedAt DESC).

### 3.3 Słowniki (enums)

**Band** (zgodnie z ADIF):
- 160m, 80m, 40m, 30m, 20m, 17m, 15m, 12m, 10m, 6m, 2m, 70cm.

**Mode** (zgodnie z ADIF):
- CW, SSB, AM, FM, RTTY, PSK31, FT8, FT4, JS8, MFSK, OPSB, OTHER, …

**QslStatus**:
- NONE, SENT, CONFIRMED.

**LotwStatus / EqslStatus**:
- UNKNOWN, SENT, CONFIRMED.

---

## 4. Wymagania non‑funkcjonalne

### 4.1 Wydajność

- REST API response time: < 500ms dla typowych zapytań (CRUD QSO, lista filtru).
- Frontend: ładowanie strony < 3s (gzip, lazy loading modułów Angular).
- WebSocket (opcjonalnie) dla realtime licznika QSO.

### 4.2 Skalowość

- Backend: WebFlux (non‑blocking) do obsługi wielu równoczesnych użytkowników.
- Database: R2DBC dla Postgresa (czy MongoDB Reactive).
- Cache: Caffeine / Redis dla HamQTH lookup i sesji.

### 4.3 Bezpieczeństwo

- HTTPS obowiązkowy.
- JWT auth, no session cookies (SPA best practice).
- SQL injection protection: prepared statements (R2DBC).
- CORS configurable.
- Rate limiting na AI endpoints (max 10 req/min per user).
- Soft delete dla Operator (nie usuwamy danych na stałe).

### 4.4 Dostępność (accessibility)

- ARIA labels, semantic HTML.
- Keyboard navigation (Tab, Enter, Escape).
- Color contrast >= 4.5:1 (AA).
- Angular 21 wbudowane ARIA utilities.

### 4.5 Testowanie

- Backend: unit testy (Jupiter/Mockito), integration testy (WebTestClient dla WebFlux).
- Frontend: unit testy (Jasmine/Karma, Vitest w Angular 21), snapshot testy.
- Coverage >= 70%.

### 4.6 Dokumentacja

- API OpenAPI 3.0 (Springdoc).
- README z instrukcją setup, deploy, development.
- Dokumentacja procesu AI pair‑programmingu (prompty, refaktoryzacja).

---

## 5. Architektura

### 5.1 Backend (Java 25 + Spring Boot 4 + WebFlux)

```
com.yourcompany.hrla
├── domain
│   ├── model
│   │   ├── Operator.java (record/class)
│   │   ├── Qso.java
│   │   ├── AiReport.java
│   │   └── enums: Band, Mode, QslStatus, ...
│   └── port
│       ├── OperatorRepository.java (R2DBC reactive)
│       ├── QsoRepository.java
│       ├── AiReportRepository.java
│       └── HamqthClient.java (external API)
├── application
│   ├── service
│   │   ├── OperatorService.java
│   │   ├── QsoService.java
│   │   ├── AiService.java (generacja tekstów, stub LLM)
│   │   ├── ImportService.java (ADIF/CSV)
│   │   └── HamqthService.java (lookup cache)
│   └── dto
│       ├── CreateQsoRequest.java
│       ├── QsoResponse.java
│       ├── AiSummaryRequest.java
│       └── ...
├── adapter
│   ├── web
│   │   ├── OperatorController.java
│   │   ├── QsoController.java
│   │   ├── ImportController.java
│   │   ├── AiController.java
│   │   └── StatisticsController.java
│   ├── persistence
│   │   ├── R2dbcOperatorRepository.java (impl)
│   │   ├── R2dbcQsoRepository.java
│   │   └── ...
│   └── external
│       └── HamqthWebClient.java
├── config
│   ├── SecurityConfig.java (JWT)
│   ├── WebFluxConfig.java (CORS, error handling)
│   ├── CacheConfig.java (Caffeine)
│   └── DatabaseConfig.java (R2DBC)
└── HrlaApplication.java (main)
```

### 5.2 Frontend (Angular 21)

```
src/app
├── auth
│   ├── components: LoginComponent, RegisterComponent
│   ├── services: AuthService, TokenService
│   └── guards: AuthGuard
├── qso-log
│   ├── components
│   │   ├── QsoListComponent (tabela z filtrowaniem)
│   │   ├── QsoFormComponent (Signal Forms, reactive)
│   │   ├── QsoDetailComponent (edit/delete)
│   │   └── DupeCheckComponent (ostrzeżenia)
│   ├── services
│   │   ├── QsoService (CRUD REST calls)
│   │   ├── FilterService (zarządzanie filterami)
│   │   └── ImportService (upload ADIF/CSV)
│   └── models: Qso, QslStatus, Band, Mode
├── stats
│   ├── components
│   │   ├── DashboardComponent
│   │   ├── StatsDetailComponent
│   │   └── ChartComponent (ngx-charts)
│   └── services: StatsService
├── ai-assistant
│   ├── components
│   │   ├── QsoNoteAssistantComponent
│   │   ├── ReportAssistantComponent
│   │   └── ReportHistoryComponent
│   └── services: AiService (REST calls, prompt mgmt)
├── shared
│   ├── components: Header, Footer, Loading
│   ├── directives: ...
│   ├── pipes: ...
│   └── interceptors: JwtInterceptor, ErrorInterceptor
└── app-routing.module.ts
```

### 5.3 Database

- **PostgreSQL** + R2DBC (reactive driver) **LUB** MongoDB + Spring Data MongoDB Reactive.
- Migrations (Flyway / Liquibase dla SQL).

### 5.4 Deployment

- Docker:
  - `Dockerfile` backend (Java 25 + Spring Boot),
  - nginx dla frontendu (static files),
  - docker‑compose dla dev (Postgres + app).
- (Opcjonalnie) Kubernetes manifests / Helm (plus na zaliczenie, nie MVP).

---

## 6. Przygotowanie AI pair‑programming

### 6.1 Dokumentacja promptów

Katalog `docs/ai/` zawiera:

- `ai-prompts-backend.md` – przykłady promptów do generowania:
  - encji JPA/R2DBC,
  - kontrolerów REST (CRUD, filtering),
  - serwisów (logika biznesowa),
  - testów (unit, integration),
  - konfiguracji Spring Boot, WebFlux.

- `ai-prompts-frontend.md` – prompty do generowania:
  - komponentów Angular (template + TS class),
  - serwisów HTTP,
  - formularzy (Signal Forms, reactive forms),
  - typów DTO.

- `ai-refactoring.md` – przykłady zmian w kodzie wygenerowanym przez AI:
  - error handling fixes,
  - performance optimization,
  - naming/structure improvements.

### 6.2 Proces dokumentacji

- **Dla każdego dużego modułu** (np. QsoService, QsoController, QsoListComponent):
  - zapis prompt → odpowiedź AI (screenshot / markdown),
  - zapis zmian ręcznych post‑AI,
  - ocena: co było dobre, co trzeba poprawić.

- **Commity git**:
  - `WIP: QsoController generated via AI, needs manual fixes for error handling`,
  - `Refactor: QsoController - improve null checks post AI generation`,
  - `Test: Add unit tests for QsoService (generated by AI, then enhanced)`.

### 6.3 Raport zaliczeniowy

Rozdział w README lub osobny dokument `DEVELOPMENT.md`:

```markdown
## Jak użyłem AI w tym projekcie

### Wygenerowane komponenty (>70% kodu)
- QsoController (REST endpoints CRUD)
- QsoService (logika biznesowa)
- QsoListComponent (tabela Angular + filtering)
- QsoFormComponent (formularz)

### Refaktoryzacja post-AI
- Ulepszona obsługa błędów (null checks, bad requests)
- Dodane testy (unit + integration)
- Optymalizacja queries (N+1 problem fix)

### Gdzie AI się myliło
- Zapomniał o ReactiveRepositories import
- Generował callback hell zamiast flatMap
- Inicjalnie brak CORS w WebFluxConfig

### Wnioski
- AI świetny do bootstrap'owania struktury i boilerplate
- Wymaga code review i poprawek
- Testy muszą być napisane/rozszerzone ręcznie
```

---

## 7. Plan realizacji (MVP)

### Sprint 1 (tydzień 1–2): Backend foundation
- [ ] Setup Spring Boot 4 + WebFlux + R2DBC.
- [ ] Model danych (Operator, Qso, AiReport).
- [ ] Encje + repositories reactive.
- [ ] Security config (JWT).

### Sprint 2 (tydzień 3–4): Core API
- [ ] OperatorController (register, login, profile).
- [ ] QsoController (CRUD).
- [ ] QsoService (logika CRUD + duplikat detection).
- [ ] FilterService (querys reaktywne).
- [ ] Unit + integration testy.

### Sprint 3 (tydzień 5–6): Frontend core
- [ ] Angular setup + modules.
- [ ] Auth (login, register, guard).
- [ ] QsoList + QsoForm (Signal Forms, reactive).
- [ ] Komunikacja z API (HttpClient).
- [ ] Filtrowanie / sorting (frontend).

### Sprint 4 (tydzień 7–8): Integracje
- [ ] HamqthService + cache (backend).
- [ ] HamQTH lookup w formularzu (frontend).
- [ ] AiService (stub lub real LLM).
- [ ] AI assistant UI (QSO notes + raport).

### Sprint 5 (tydzień 9–10): Import/Export + stats
- [ ] ADIF parser/export (backend).
- [ ] Import endpoint + logic (create/update).
- [ ] CSV export.
- [ ] Statistics API.
- [ ] Dashboard / charts (frontend).

### Sprint 6 (tydzień 11–12): Polish + docs
- [ ] Error handling, edge cases.
- [ ] E2E testy (opcjonalnie).
- [ ] Dokumentacja (README, OpenAPI, AI prompts).
- [ ] Docker setup.
- [ ] Code review, cleanup.

---

## 8. Kryteria akceptacji (Definition of Done)

- [ ] Kod przechodzi alle unit testy (coverage >= 70%).
- [ ] API endpoints dokumentowane w OpenAPI.
- [ ] Frontend components accessible (ARIA, keyboard nav).
- [ ] Nie ma hardcoded wartości, konfiguracja externalized.
- [ ] Błędy obsłużone (400, 401, 404, 500 + user messages).
- [ ] Database migrations (Flyway) czyste i testowalne.
- [ ] Docker builds without errors.
- [ ] README zawiera setup + develop + deploy instructions.
- [ ] AI pair-programming dokumentacja kompletna (prompts + decisions).
- [ ] Git history czysty (meaningful commits, nie WIP na main).

---

## 9. Out of scope (post-MVP)

- Realtime sync via WebSocket (nice-to-have).
- Contest mode (automatic time‑on‑off).
- Advanced analytics (machine learning na wzorce QSO).
- Mobile app native (PWA opcjonalnie).
- Full integration z LoTW (automatic QSL sync).
- Multi-user collaboration (shared logbooks).
- QSO voice memo (recording).

---

## 10. Załączniki

### A. ADIF field mapping

| ADIF Field | Model Qso | Typ | Wymagane |
|---|---|---|---|
| `CALL` | theirCallsign | String | Tak |
| `QSO_DATE` | qsoDate | LocalDate | Tak |
| `TIME_ON` | timeOn | LocalTime | Tak |
| `BAND` | band | Enum | Tak/Warunkowe |
| `FREQ` | frequency | Double | Tak/Warunkowe |
| `MODE` | mode | Enum | Tak |
| `RST_SENT` | rstSent | String | Nie |
| `RST_RCVD` | rstRcvd | String | Nie |
| `QTH` | qth | String | Nie |
| `GRIDSQUARE` | gridSquare | String | Nie |
| `QSL_STATUS` | qslStatus | Enum | Nie |
| `QSL_RCVD_DATE` | – (nie MVP) | – | – |
| `NOTES` | notes | String | Nie |

### B. API Endpoints (REST)

```
POST   /api/v1/auth/register         – rejestracja
POST   /api/v1/auth/login            – logowanie (JWT)
GET    /api/v1/operator/profile      – profil (auth required)
PUT    /api/v1/operator/profile      – edycja profilu

GET    /api/v1/qso                   – lista (filtry: band, mode, date range)
POST   /api/v1/qso                   – nowy QSO
GET    /api/v1/qso/{id}              – szczegóły
PUT    /api/v1/qso/{id}              – edycja
DELETE /api/v1/qso/{id}              – usuwanie

POST   /api/v1/import/adif           – import ADIF
POST   /api/v1/import/csv            – import CSV
GET    /api/v1/export/adif           – eksport ADIF
GET    /api/v1/export/csv            – eksport CSV

GET    /api/v1/stats/overview        – dashboard
GET    /api/v1/stats/per-band        – statystyki per pasmo

POST   /api/v1/lookup/hamqth         – lookup callbooka
GET    /api/v1/lookup/hamqth/{call}  – cache check

POST   /api/v1/ai/qso-note           – generowanie opisu QSO
POST   /api/v1/ai/summary            – generowanie raportu
GET    /api/v1/ai/reports            – historia raportów

GET    /actuator/health              – health check
```

### C. Environment variables (backend)

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hamradio_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=your-secret-key-min-32-chars
JWT_EXPIRATION_MS=86400000

HAMQTH_API_URL=https://www.hamqth.com/api/
HAMQTH_CACHE_TTL_HOURS=24
HAMQTH_RATE_LIMIT_PER_MINUTE=10

AI_SERVICE_TYPE=MOCK  # or OLLAMA, OPENAI
AI_SERVICE_URL=http://localhost:11434  # dla Ollama
AI_SERVICE_MODEL=llama2  # lub inna
```

### D. Technology stack summary

| Layer | Technology | Version |
|---|---|---|
| **Backend** | |
| Language | Java | 25 |
| Framework | Spring Boot | 4.0.x |
| Web | Spring WebFlux | 7.x |
| Database Driver | R2DBC | Latest |
| Database | PostgreSQL | 15+ |
| Cache | Caffeine / Redis | Latest |
| Build | Maven / Gradle | Latest |
| Testing | JUnit 5, Mockito, WebTestClient | Latest |
| **Frontend** | |
| Framework | Angular | 21 |
| Forms | Signal Forms / Reactive Forms | 21 |
| HTTP | HttpClient | 21 |
| Charts | ngx-charts | Latest |
| CSS | Tailwind / Bootstrap | Latest |
| Testing | Jasmine, Vitest | Latest |
| Build | ng build (Webpack) | 21 |
| **DevOps** | |
| Container | Docker | Latest |
| Orchestration | Docker Compose (dev) | Latest |
| CI/CD | GitHub Actions / GitLab CI | TBD |
| Documentation | OpenAPI 3.0 / Springdoc | Latest |

---

**Dokument zatwierdzony**: -  
**Ostatnia zmiana**: styczeń 2026  
**Autor**: -  

---

*Projekt: Ham Radio Logbook Assistant with AI pair-programming support*
