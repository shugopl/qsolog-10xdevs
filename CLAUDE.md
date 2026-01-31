# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Production-quality ham radio QSO (contact) logging application built as a monorepo:
- **Backend**: Java 21 (targeting Java 25) + Spring Boot 3.4.1 WebFlux + R2DBC (reactive) + PostgreSQL 16
- **Frontend**: Angular 21 SPA + Angular Material (not yet implemented)
- **Auth**: JWT bearer tokens with BCrypt password hashing
- **Architecture**: Clean Architecture with SOLID principles, fully reactive stack

**Key Dependencies:**
- Spring Boot 3.4.1 (WebFlux, Security, Data R2DBC, Actuator)
- R2DBC PostgreSQL driver (reactive database access)
- Flyway 10.x (database migrations via JDBC)
- SpringDoc OpenAPI 2.7.0 (API documentation)
- Testcontainers 1.20.4 (integration tests with real PostgreSQL)
- JUnit 5 (testing framework)

## Implementation Status

**Completed:**
- ✓ Backend foundation with Spring Boot WebFlux
- ✓ PostgreSQL schema via Flyway migrations
- ✓ JWT authentication and authorization system
- ✓ User management (register, login, roles, admin endpoints)
- ✓ Domain entities and enums (Band, AdifMode, AdifSubmode, QSL statuses)
- ✓ QSO CRUD operations with multi-tenancy
- ✓ Statistics endpoints (band, mode, QSL stats)
- ✓ ADIF/CSV export functionality
- ✓ Callsign lookup integration (HamQTH API)
- ✓ AI-powered QSO descriptions and period reports
- ✓ Suggestions from user history

**Not Yet Implemented:**
- Frontend application (Angular 21 SPA)
- Full CI/CD pipeline

## Commands

### Backend Development

**Build and Test:**
```bash
cd backend

# Compile only
mvn clean compile

# Run all tests (requires Docker for Testcontainers)
mvn test

# Run tests excluding integration tests (no Docker required)
mvn test -Dtest='!DatabaseMigrationTest,!AuthControllerTest'

# Run specific test
mvn test -Dtest=SecurityTest

# Package JAR
mvn clean package
```

**Run Application:**
```bash
cd backend

# Run with default settings (Flyway disabled)
mvn spring-boot:run

# Run with Flyway migrations enabled
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.flyway.enabled=true"

# Run with admin bootstrap
ADMIN_BOOTSTRAP=true ADMIN_EMAIL=admin@qsolog.local ADMIN_USERNAME=admin ADMIN_PASSWORD=securepass mvn spring-boot:run
```

**Database:**
```bash
cd docker

# Start PostgreSQL
docker-compose up -d

# Start PostgreSQL + pgAdmin
docker-compose --profile tools up -d

# Stop services
docker-compose down

# Reset database (removes all data)
docker-compose down -v
```

## Architecture

### Clean Architecture Layers

**Package**: `com.pl.shugo.gsolog` (note: actual package differs from original `com.example.qsolog`)

```
com.pl.shugo.gsolog/
├── api/                        # Outer layer - HTTP interface
│   ├── controller/             # REST controllers
│   └── dto/                    # Request/Response DTOs
├── application/                # Use cases layer
│   └── service/                # Business logic orchestration
├── domain/                     # Core domain layer (no framework dependencies)
│   ├── entity/                 # Domain entities
│   ├── enums/                  # Domain value objects
│   └── repository/             # Repository interfaces (ports)
└── infrastructure/             # Outer layer - Technical details
    ├── config/                 # Spring configuration
    ├── error/                  # Global error handling
    └── security/               # JWT and security implementation
```

**Dependency Rule**: Dependencies point inward. Domain has no dependencies on outer layers.

### Database Schema

**Flyway Migrations**: `backend/src/main/resources/db/migration/V1__init.sql`

**Tables:**
- `users` - User accounts with UUID id, email (unique), username (unique), password_hash, role (ADMIN/OPERATOR)
- `qso` - QSO contacts with user_id FK, ADIF fields (their_callsign, qso_date, time_on, band, frequency_khz, mode, submode, custom_mode), QSL tracking
- `ai_report_history` - AI-generated report history

**Important Indexes:**
- `qso(user_id, qso_date DESC)` - For date-ordered queries
- `qso(user_id, their_callsign)` - For callsign lookups
- `qso(user_id, band)` - For band filtering

### Domain Model

**Enums** (in `domain/enums/`):
- `Role`: ADMIN, OPERATOR
- `Band`: All ADIF bands (160m, 80m, ..., 1mm) with `getAdifValue()` and `fromAdifValue()` methods
- `AdifMode`: CW, SSB, AM, FM, RTTY, PSK, MFSK, DATA
- `AdifSubmode`: FT8, FT4, JS8, PSK31, JT65, JT9, OLIVIA, CONTESTIA, PSK63, PSK125
- `QslStatus`: NONE, SENT, CONFIRMED
- `LotwStatus`, `EqslStatus`: UNKNOWN, SENT, CONFIRMED

**Mode/Submode Validation Rules** (enforced at service layer):
- If `submode` ∈ {FT8, FT4, JS8} → `mode` must be MFSK
- If `submode` ∈ {PSK31, PSK63, PSK125} → `mode` must be PSK
- If `custom_mode` not null → `mode` must be DATA and `submode` must be null
- Validation failures return 400 Bad Request with descriptive error messages

### Security Architecture

**JWT Authentication Flow:**
1. User registers via `POST /api/v1/auth/register` → BCrypt hashed password stored
2. User logs in via `POST /api/v1/auth/login` → JWT token returned
3. Client includes `Authorization: Bearer <token>` header in requests
4. `JwtAuthenticationConverter` extracts token from header
5. `JwtAuthenticationManager` validates token and creates Authentication object
6. Spring Security authorizes based on role

**JWT Token Structure:**
- `sub`: userId (UUID string)
- `username`: username
- `role`: ADMIN or OPERATOR
- `exp`: expiration (24 hours from issuance)

**Path Security** (configured in `SecurityConfig.java`):
- **Public**: `/api/v1/ping`, `/api/v1/auth/**`, `/actuator/**`, `/swagger-ui.html`, `/api-docs`
- **Protected**: `/api/v1/qso/**`, `/api/v1/stats/**`, `/api/v1/export/**`, `/api/v1/lookup/**`, `/api/v1/ai/**`

### R2DBC and Reactive Programming

All database operations use **R2DBC** (Reactive Relational Database Connectivity):
- Repository methods return `Mono<T>` (0 or 1 result) or `Flux<T>` (0 to N results)
- Controllers return `Mono<ResponseDTO>` or `Flux<ResponseDTO>`
- Use `.block()` ONLY in tests, never in production code
- Chain reactive operations with `.flatMap()`, `.map()`, `.switchIfEmpty()`, etc.

**UserRepository Example:**
```java
public interface UserRepository extends R2dbcRepository<User, UUID> {
    Mono<User> findByEmail(String email);
    Mono<Boolean> existsByUsername(String username);
}
```

### Multi-Tenancy Pattern

**All QSO operations must be scoped to the authenticated user:**
- Extract `userId` from JWT token (available in `Authentication.getPrincipal()`)
- Filter all queries by `user_id = :userId`
- Prevent users from accessing other users' data
- Enforced at service layer, not just controller layer

## API Endpoints

**Public Endpoints:**
- `GET /api/v1/ping` - Health check, returns `{"status":"ok"}`
- `GET /actuator/health` - Spring Boot health endpoint
- `GET /swagger-ui.html` - OpenAPI/Swagger UI
- `GET /api-docs` - OpenAPI JSON specification

**Authentication** (`/api/v1/auth` - public):
- `POST /register` - Register new user (default role: OPERATOR)
- `POST /login` - Login with username/email and password, returns JWT
- `GET /me` - Get current authenticated user info (requires JWT)

**Admin** (`/api/v1/admin` - ADMIN role required):
- `GET /users` - List all users

**QSO Management** (`/api/v1/qso` - authenticated):
- `POST /` - Create new QSO entry
- `GET /` - List user's QSO entries with pagination and filtering
- `GET /{id}` - Get specific QSO entry
- `PUT /{id}` - Update QSO entry
- `DELETE /{id}` - Delete QSO entry

**Statistics** (`/api/v1/stats` - authenticated):
- `GET /summary` - Overall statistics summary
- `GET /by-band` - QSO counts grouped by band
- `GET /by-mode` - QSO counts grouped by mode
- `GET /qsl-status` - QSL confirmation statistics

**Export** (`/api/v1/export` - authenticated):
- `GET /adif` - Export QSO log as ADIF format
- `GET /csv` - Export QSO log as CSV format

**Lookup** (`/api/v1/lookup` - authenticated):
- `GET /callsign/{callsign}` - Lookup callsign via HamQTH API

**Suggestions** (`/api/v1/suggestions` - authenticated):
- `GET /for-callsign/{callsign}` - Get suggestions from user's history

**AI Helper** (`/api/v1/ai` - authenticated):
- `POST /describe-qso` - Generate AI description for QSO entry
- `POST /generate-report` - Generate AI period report

## Testing Strategy

**Test Types:**
1. **Unit Tests**: Test services and utilities in isolation (no Spring context)
2. **Integration Tests**: Test with Spring Boot context and in-memory H2 database
3. **Testcontainers Tests**: Test with real PostgreSQL container (requires Docker)

**Test Naming Convention:**
- Integration tests with Testcontainers: `*Test.java` (e.g., `AuthControllerTest`, `DatabaseMigrationTest`)
- Other tests: `*Test.java` (all use JUnit 5)

**Running Tests:**
```bash
# All tests (requires Docker)
mvn test

# Skip Testcontainers tests (no Docker required)
mvn test -Dtest='!DatabaseMigrationTest,!AuthControllerTest,!AdminControllerTest,!QsoControllerTest,!StatsControllerTest,!ExportControllerTest,!LookupControllerTest,!AiControllerTest,!SuggestionsControllerTest'

# Run only unit tests (quick)
mvn test -Dtest=SecurityTest,PingControllerTest

# Specific test class
mvn test -Dtest=SecurityTest

# Specific test method
mvn test -Dtest=SecurityTest#ping_shouldBeAccessibleWithoutAuthentication
```

## Environment Variables

**Database:**
- `FLYWAY_ENABLED` - Enable Flyway migrations (default: false)
- `DB_URL` - R2DBC URL (default: `r2dbc:postgresql://localhost:5432/qsolog`)
- `DB_URL_JDBC` - Flyway JDBC URL (default: `jdbc:postgresql://localhost:5432/qsolog`)
- `DB_USER` - Database user (default: `qsolog`)
- `DB_PASS` - Database password (default: `qsolog`)

**Authentication:**
- `JWT_SECRET` - JWT signing secret (min 256 bits for HS512, default provided)
- `ADMIN_BOOTSTRAP` - Create admin user on startup (default: false)
- `ADMIN_EMAIL` - Admin email (required if ADMIN_BOOTSTRAP=true)
- `ADMIN_USERNAME` - Admin username (required if ADMIN_BOOTSTRAP=true)
- `ADMIN_PASSWORD` - Admin password (required if ADMIN_BOOTSTRAP=true)

**External APIs (optional):**
- `HAMQTH_USERNAME` - HamQTH.com username for callsign lookup
- `HAMQTH_PASSWORD` - HamQTH.com password
- `OPENAI_API_KEY` - OpenAI API key for AI features (falls back to mock adapter if not set)

**Server:**
- `SERVER_PORT` - HTTP port (default: 8080)
- `CORS_ORIGINS` - CORS allowed origins (default: `http://localhost:4200`)

## Business Rules

**UTC Everywhere**: All dates and times stored and processed in UTC timezone.

**Roles:**
- `ADMIN` - Full access (created via admin bootstrap or database seed)
- `OPERATOR` - Standard user (default for registration)
- Design allows future extension to "club log" with multi-operator shared logs

**ADIF Compliance:**
- Required fields: CALL (their_callsign), QSO_DATE, TIME_ON, MODE, BAND (or FREQ)
- MVP requires BAND; frequency_khz is optional
- Export must produce valid ADIF format with proper field encoding

**Duplicate Detection:**
- Detect potential duplicates: same (user_id, their_callsign, qso_date, band, mode)
- Do NOT block saves - warn user and let them confirm
- Implemented at service layer, not database constraint

**QSL Tracking:**
- `qsl_status`: Paper QSL card status
- `lotw_status`: Logbook of the World confirmation
- `eqsl_status`: Electronic QSL confirmation
- Stats should include "all QSOs" and "confirmed QSOs" counts

**Callsign Lookup:**
- Use HamQTH API (to be implemented in Step 5)
- Cache results to reduce API calls
- Suggestions only - never auto-overwrite user data
- Used for pre-filling name, QTH, grid square fields

**AI Helper:**
- Support PL (Polish) and EN (English) languages
- Neutral, loose tone
- Store generated report history in `ai_report_history` table
- Default: mock adapter (no API key needed)
- Optional: OpenAI adapter with `OPENAI_API_KEY` env var

## Development Guidelines

**When Adding New Features:**

1. **Follow Clean Architecture:**
   - Add DTOs in `api/dto/`
   - Add controllers in `api/controller/`
   - Add use cases in `application/service/`
   - Add entities in `domain/entity/`
   - Add repository interfaces in `domain/repository/`
   - Add infrastructure adapters in `infrastructure/`

2. **Follow Reactive Patterns:**
   - Return `Mono<T>` or `Flux<T>` from all async methods
   - Never call `.block()` in production code (tests only)
   - Use `.flatMap()` for chaining operations that return Mono/Flux
   - Use `.map()` for simple transformations
   - Use `.switchIfEmpty()` for fallback values

3. **Security:**
   - Always filter data by `user_id` from JWT token
   - Use BCrypt for password hashing (never plain text)
   - Validate input with Jakarta Validation annotations
   - Return appropriate HTTP status codes (401, 403, 404, 409, etc.)

4. **Database:**
   - Create Flyway migration for schema changes (`V{number}__{description}.sql`)
   - Use R2DBC repositories extending `R2dbcRepository<Entity, UUID>`
   - Add database indexes for frequently queried columns
   - Use `UUID` for all primary keys

5. **Testing:**
   - Write integration tests for all controllers (use `@SpringBootTest` + `WebTestClient`)
   - Write unit tests for complex business logic
   - Use Testcontainers for tests requiring real database
   - Verify security rules (401 without token, 403 for wrong user, etc.)

6. **Error Handling:**
   - Global error handler in `GlobalErrorHandler` provides RFC7807-like responses
   - Throw `ResponseStatusException` with appropriate HTTP status and message
   - Validation errors automatically formatted with field-level details

## ADIF Export Mapping

The ADIF export functionality uses this mapping:

| UI Label | mode | submode | custom_mode | ADIF Output |
|----------|------|---------|-------------|-------------|
| CW | CW | null | null | `MODE=CW` |
| SSB | SSB | null | null | `MODE=SSB` |
| PSK31 | PSK | PSK31 | null | `MODE=PSK, SUBMODE=PSK31` |
| FT8 | MFSK | FT8 | null | `MODE=MFSK, SUBMODE=FT8` |
| Other "VARAC" | DATA | null | "VARAC" | `MODE=DATA, APP_QSOLOG_CUSTOMMODE=VARAC` |

Vendor field format: `<APP_QSOLOG_CUSTOMMODE:{len}>{value}`

## Common Issues

**Tests fail with "Could not find valid Docker environment" or "BadRequestException (Status 400)":**
- Known compatibility issue between Testcontainers and Docker Desktop 29.x
- **Workaround 1**: Skip Testcontainers tests:
  ```bash
  mvn test -Dtest='!DatabaseMigrationTest,!AuthControllerTest,!AdminControllerTest,!QsoControllerTest,!StatsControllerTest,!ExportControllerTest,!LookupControllerTest,!AiControllerTest,!SuggestionsControllerTest'
  ```
- **Workaround 2**: Try setting environment variable:
  ```bash
  export TESTCONTAINERS_RYUK_DISABLED=true
  ```
- **Workaround 3**: Downgrade to Docker Desktop 28.x or earlier
- **Workaround 4**: Wait for Testcontainers or Docker Desktop update that fixes compatibility

**Application fails to start with Flyway connection error:**
- PostgreSQL not running or Flyway enabled without database
- Start database: `cd docker && docker-compose up -d`
- Or disable Flyway: set `FLYWAY_ENABLED=false` or omit from environment

**401 Unauthorized on protected endpoints:**
- Missing or invalid JWT token in Authorization header
- Token expired (24 hour expiration from issuance)
- Use `Authorization: Bearer <token>` header format
- Obtain token via `POST /api/v1/auth/login`

**403 Forbidden on admin endpoints:**
- User role is OPERATOR but endpoint requires ADMIN
- Admin users must be created via ADMIN_BOOTSTRAP or database seed
- Regular registration always creates OPERATOR users

**Package name mismatch:**
- Code uses `com.pl.shugo.gsolog` (actual implementation)
- Documentation may reference `com.example.qsolog` (original plan)
- Always use actual package structure when creating new files
