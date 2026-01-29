# QSO Log - Ham Radio Logging Application

Production-quality ham radio contact (QSO) logging application built as a monorepo.

## Tech Stack

- **Backend**: Java 25 + Spring Boot 4 WebFlux + R2DBC (reactive) + PostgreSQL
- **Frontend**: Angular 21 SPA + Angular Material
- **Auth**: JWT bearer tokens with secure password hashing
- **Architecture**: Clean Architecture with SOLID principles, reactive throughout

## Repository Structure

```
/
  backend/          # Spring Boot WebFlux reactive backend
  frontend/         # Angular 21 SPA with Material UI
  docker/           # docker-compose.yml for local PostgreSQL
  .github/workflows/ # CI/CD pipelines
  iteration/        # Step-by-step implementation plan
  README.md         # This file
  CLAUDE.md         # Development guidelines for Claude Code
```

## Status

Currently under development. Implementation follows incremental steps in `/iteration/*.md` files.

## Key Features (Planned)

- Multi-user QSO logging with role-based access (ADMIN/OPERATOR)
- ADIF and CSV export with full ADIF standard compliance
- Real-time statistics and visualizations
- Callsign lookup integration (HamQTH API)
- AI-powered QSO descriptions and period reports
- QSL tracking (paper/LoTW/eQSL)
- Duplicate detection with user confirmation
- UTC timezone throughout

## Development

See `/iteration/*.md` for detailed implementation steps and `CLAUDE.md` for architecture and development guidelines.

### PostgreSQL Database

Start PostgreSQL using Docker Compose:

```bash
cd docker

# Start PostgreSQL only
docker-compose up -d

# Start with pgAdmin (optional database management UI)
docker-compose --profile tools up -d

# Stop services
docker-compose down

# Remove data volumes (clean slate)
docker-compose down -v
```

**Database connection details:**
- Host: localhost
- Port: 5432
- Database: qsolog
- Username: qsolog
- Password: qsolog

**pgAdmin (optional, with --profile tools):**
- URL: http://localhost:5050
- Email: admin@qsolog.local
- Password: admin

### Backend

Prerequisites:
- Java 21 or later
- Maven 3.9+
- PostgreSQL (via docker-compose or local installation)
- Docker (required for Testcontainers integration tests)

Build and run:
```bash
cd backend

# Run all tests (requires Docker for Testcontainers)
mvn test

# Run tests excluding integration tests (no Docker required)
mvn test -Dtest='!DatabaseMigrationTest'

# Run application (requires PostgreSQL running)
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/qsolog-backend-0.0.1-SNAPSHOT.jar
```

The backend will start on `http://localhost:8080`.

**Available endpoints:**

Public (no authentication required):
- **Health check**: `GET http://localhost:8080/actuator/health`
- **Ping**: `GET http://localhost:8080/api/v1/ping` (returns `{"status":"ok"}`)
- **OpenAPI UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs**: `http://localhost:8080/api-docs`

Authentication:
- **Register**: `POST /api/v1/auth/register` - Register new user (role: OPERATOR)
- **Login**: `POST /api/v1/auth/login` - Login and get JWT token
- **Current user**: `GET /api/v1/auth/me` - Get current user info (requires JWT)

Protected (JWT token required in Authorization header):
- **QSO endpoints**: `/api/v1/qso/**` (not yet implemented)
- **Stats endpoints**: `/api/v1/stats/**` (not yet implemented)
- **Export endpoints**: `/api/v1/export/**` (not yet implemented)
- **Lookup endpoints**: `/api/v1/lookup/**` (not yet implemented)
- **AI endpoints**: `/api/v1/ai/**` (not yet implemented)

**Environment variables:**

Database:
- `FLYWAY_ENABLED` - Enable Flyway migrations (default: false, set to "true" for production)
- `DB_URL` - R2DBC connection URL (default: r2dbc:postgresql://localhost:5432/qsolog)
- `DB_URL_JDBC` - JDBC connection for Flyway (default: jdbc:postgresql://localhost:5432/qsolog)
- `DB_USER` - Database username (default: qsolog)
- `DB_PASS` - Database password (default: qsolog)

Authentication:
- `JWT_SECRET` - JWT signing secret (default: CHANGE_THIS_SECRET_IN_PRODUCTION_MIN_256_BITS_LONG_KEY_FOR_HS512)
- `ADMIN_BOOTSTRAP` - Enable admin user creation on startup (default: false)
- `ADMIN_EMAIL` - Admin user email (required if ADMIN_BOOTSTRAP=true)
- `ADMIN_USERNAME` - Admin username (required if ADMIN_BOOTSTRAP=true)
- `ADMIN_PASSWORD` - Admin password (required if ADMIN_BOOTSTRAP=true)

Server:
- `SERVER_PORT` - Server port (default: 8080)
- `CORS_ORIGINS` - Allowed CORS origins (default: http://localhost:4200)

**Database schema:**
- `users` - User accounts with roles (ADMIN/OPERATOR)
- `qso` - QSO (contact) log entries with ADIF fields
- `ai_report_history` - AI-generated report history

Flyway migrations are in `src/main/resources/db/migration/`.

### Authentication Flow

1. **Register a new user:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","username":"myuser","password":"mypassword123"}'
```

2. **Login and get JWT token:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"myuser","password":"mypassword123"}'
```

Response:
```json
{
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresInSeconds": 86400
}
```

3. **Access protected endpoints with JWT:**
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer eyJhbGc..."
```

### Admin User Bootstrap

To create an admin user on startup (idempotent):

```bash
export ADMIN_BOOTSTRAP=true
export ADMIN_EMAIL=admin@qsolog.local
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=securepassword

mvn spring-boot:run
```

The admin user will be created on first startup if it doesn't already exist. Subsequent startups will skip creation.
