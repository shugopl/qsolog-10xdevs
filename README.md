# QSO Log - Ham Radio Logging Application

Production-quality ham radio contact (QSO) logging application built as a monorepo.

## Tech Stack

- **Backend**: Java 25 + Spring Boot 3.4.1 WebFlux + R2DBC (reactive) + PostgreSQL 16
- **Frontend**: Angular 21 SPA + Angular Material
- **Auth**: JWT bearer tokens with BCrypt password hashing
- **Architecture**: Clean Architecture with SOLID principles, fully reactive stack
- **CI/CD**: GitHub Actions with automated testing and Java version enforcement

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

✅ **Backend Core Features**: Complete (authentication, QSO CRUD, statistics, export, AI helper)
🚧 **Frontend**: Angular scaffolding complete, features in progress
✅ **CI/CD**: GitHub Actions with Java 25 enforcement and automated testing

Implementation follows incremental steps in `/iteration/*.md` files.

> **Recent Update**: Fixed CI test failures by adding explicit `@DateTimeFormat` annotations to LocalDate parameters. See [CI Fix Summary](iteration/CI_FIX_SUMMARY.md) for details.

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

**Prerequisites:**
- **Java 25** (enforced by Maven enforcer plugin - builds will fail on lower versions)
- Maven 3.8.0 or later
- PostgreSQL 16 (via docker-compose or local installation)
- Docker (required for Testcontainers integration tests)

**Quick Setup:**
```bash
# Verify Java 25 is installed
java -version
# Should show: openjdk version "25.0.1" or similar

# If Java 25 not installed, use SDKMAN:
curl -s "https://get.sdkman.io" | bash
sdk install java 25.0.1-amzn
sdk use java 25.0.1-amzn
```

**Build and run:**
```bash
cd backend

# Verify Java 25 is being used (enforcer plugin will check)
mvn -version

# Run all tests (requires Docker for Testcontainers)
mvn test

# Run tests excluding integration tests (no Docker required)
mvn test -Dtest='!DatabaseMigrationTest,!*ControllerTest'

# Run application (requires PostgreSQL running)
mvn spring-boot:run

# With Flyway migrations enabled
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.flyway.enabled=true"

# Or build and run JAR
mvn clean package
java -jar target/qsolog-backend-0.0.1-SNAPSHOT.jar
```

The backend will start on `http://localhost:8081` (configurable via `SERVER_PORT` env var).

> **Note**: Build will fail if Java version is not 25 due to Maven enforcer plugin. This ensures consistent behavior across development and CI environments.

**Available endpoints:**

Public (no authentication required):
- **Health check**: `GET http://localhost:8081/actuator/health`
- **Ping**: `GET http://localhost:8081/api/v1/ping` (returns `{"status":"ok"}`)
- **OpenAPI UI**: `http://localhost:8081/swagger-ui.html`
- **API Docs**: `http://localhost:8081/api-docs`

Authentication:
- **Register**: `POST /api/v1/auth/register` - Register new user (role: OPERATOR)
- **Login**: `POST /api/v1/auth/login` - Login and get JWT token
- **Current user**: `GET /api/v1/auth/me` - Get current user info (requires JWT)

Protected (JWT token required in Authorization header):
- **QSO endpoints**: `/api/v1/qso/**` - Create, read, update, delete QSO entries
- **Stats endpoints**: `/api/v1/stats/**` - Get statistics and summaries
- **Export endpoints**: `/api/v1/export/**` - Export QSO data as ADIF or CSV
- **Lookup endpoints**: `/api/v1/lookup/**` - Callsign lookup via HamQTH
- **Suggestions endpoints**: `/api/v1/suggestions/**` - Get suggestions from QSO history
- **AI endpoints**: `/api/v1/ai/**` - Generate QSO descriptions and period reports

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

External APIs (optional):
- `HAMQTH_USERNAME` - HamQTH.com username for callsign lookup (optional, falls back to history-only suggestions)
- `HAMQTH_PASSWORD` - HamQTH.com password (optional)
- `OPENAI_API_KEY` - OpenAI API key for AI-powered descriptions and reports (optional, falls back to mock adapter)

**Database schema:**
- `users` - User accounts with roles (ADMIN/OPERATOR)
- `qso` - QSO (contact) log entries with ADIF fields
- `ai_report_history` - AI-generated report history

Flyway migrations are in `src/main/resources/db/migration/`.

### Authentication Flow

1. **Register a new user:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","username":"myuser","password":"mypassword123"}'
```

2. **Login and get JWT token:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
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
curl -X GET http://localhost:8081/api/v1/auth/me \
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

### Frontend

Prerequisites:
- Node.js 22 or later
- npm (included with Node.js)
- Running backend API (see Backend section above)

Build and run:
```bash
cd frontend

# Install dependencies
npm ci

# Run development server
npm start
# Or: ng serve
```

The frontend will start on `http://localhost:4200`.

**Available routes:**
- `/login` - Login page
- `/register` - User registration
- `/qso` - QSO list with filters and pagination (protected)
- `/qso/new` - Create new QSO entry (protected)
- `/qso/:id/edit` - Edit existing QSO (protected)
- `/stats` - Statistics dashboard with charts (protected)
- `/ai-reports` - AI-generated reports (protected)

**Build for production:**
```bash
npm run build
# Output in dist/frontend/browser/
```

**Run tests:**
```bash
# Unit tests (requires Chrome/Chromium)
npm test

# Linting
npm run lint
```

**Environment configuration:**

Development: `src/environments/environment.ts`
- `apiBaseUrl: 'http://localhost:8080/api/v1'`

Production: `src/environments/environment.prod.ts` (not yet created)
- Configure production API URL before building for production

## Docker Deployment

### Local Development with Docker Compose

Run the complete stack (PostgreSQL + Backend + Frontend):

```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Remove all data (clean slate)
docker-compose down -v
```

**Service URLs:**
- Frontend: http://localhost (port 80)
- Backend API: http://localhost:8081
- PostgreSQL: localhost:5432
- pgAdmin (optional): http://localhost:5050

### Build Docker Images Manually

**Backend:**
```bash
cd backend
docker build -t qsolog-backend:latest .
docker run -p 8080:8080 \
  -e DB_URL=r2dbc:postgresql://host.docker.internal:5432/qsolog \
  -e DB_URL_JDBC=jdbc:postgresql://host.docker.internal:5432/qsolog \
  -e DB_USER=qsolog \
  -e DB_PASS=qsolog \
  -e JWT_SECRET=your-secret-key-min-256-bits \
  qsolog-backend:latest
```

**Frontend:**
```bash
cd frontend
docker build -t qsolog-frontend:latest .
docker run -p 80:80 qsolog-frontend:latest
```

**Note:** Frontend is built with hardcoded API URL from `environment.prod.ts`. For dynamic configuration, consider environment variable substitution in nginx or runtime configuration file.

## Production Deployment Checklist

Before deploying to production:

1. **Security:**
   - [ ] Change `JWT_SECRET` to a strong random value (minimum 256 bits)
   - [ ] Use strong passwords for database and admin user
   - [ ] Review and restrict `CORS_ORIGINS`
   - [ ] Enable HTTPS/TLS
   - [ ] Review nginx security headers in `frontend/nginx.conf`

2. **Database:**
   - [ ] Use managed PostgreSQL service or properly configured instance
   - [ ] Enable automated backups
   - [ ] Set `FLYWAY_ENABLED=true` for automatic schema migrations
   - [ ] Change default database credentials

3. **Environment Variables:**
   - [ ] Set all required environment variables (see Backend section)
   - [ ] Configure `ADMIN_BOOTSTRAP` with secure admin credentials
   - [ ] Add `HAMQTH_USERNAME` and `HAMQTH_PASSWORD` for callsign lookup (optional)
   - [ ] Add `OPENAI_API_KEY` for AI features (optional)

4. **Monitoring:**
   - [ ] Set up health check monitoring on `/actuator/health`
   - [ ] Configure logging and log aggregation
   - [ ] Set up alerting for service failures

5. **Frontend:**
   - [ ] Update `environment.prod.ts` with production API URL
   - [ ] Build with `npm run build -- --configuration production`
   - [ ] Verify CSP and security headers in nginx configuration

## Testing

### Backend Tests

**Run all tests** (requires Docker for Testcontainers):
```bash
cd backend
mvn test
```

**Run without Testcontainers** (skips integration tests):
```bash
mvn test -Dtest='!DatabaseMigrationTest,!AuthControllerTest,!AdminControllerTest,!QsoControllerTest,!StatsControllerTest,!ExportControllerTest,!LookupControllerTest,!AiControllerTest,!SuggestionsControllerTest'
```

**Run specific test class:**
```bash
mvn test -Dtest=SecurityTest
```

**Mimic CI environment** (for troubleshooting):
```bash
TZ=UTC LANG=C mvn -B -ntp test
```

**Test coverage:**
- Unit tests: Service layer and security utilities
- Integration tests: Full controller tests with real PostgreSQL (Testcontainers)
- Total: 71 tests across authentication, QSO management, statistics, export, AI, and suggestions

### Frontend Tests

```bash
cd frontend

# Run unit tests (requires Chrome/Chromium)
npm test

# Run linting
npm run lint
```

### CI/CD

GitHub Actions automatically runs tests on:
- Push to `main` or `develop` branches
- Pull requests

**Workflows:**
- `.github/workflows/backend-ci.yml` - Backend build and test (Java 25)
- `.github/workflows/ci.yaml` - Monorepo-wide CI with path filtering
- `.github/workflows/frontend-ci.yml` - Frontend build and lint

All workflows enforce Java 25 and include diagnostic output for troubleshooting.

## Troubleshooting

### Common Issues

**1. Tests fail with "Docker environment not found" or Testcontainers errors**

Known compatibility issue between Testcontainers 1.20.4 and Docker Desktop 29.x.

**Solutions:**
- Skip Testcontainers tests (see Testing section above)
- Downgrade to Docker Desktop 28.x
- Set `export TESTCONTAINERS_RYUK_DISABLED=true`

**2. Application fails to start with Flyway connection error**

PostgreSQL is not running or Flyway is enabled without database.

**Solutions:**
```bash
# Start PostgreSQL
cd docker && docker-compose up -d

# Or disable Flyway
export FLYWAY_ENABLED=false
mvn spring-boot:run
```

**3. 401 Unauthorized on protected endpoints**

Missing or invalid JWT token.

**Solutions:**
- Obtain token via `POST /api/v1/auth/login`
- Include header: `Authorization: Bearer <token>`
- Check token expiration (24 hours from issuance)

**4. 403 Forbidden on admin endpoints**

User role is OPERATOR but endpoint requires ADMIN.

**Solutions:**
- Create admin user via `ADMIN_BOOTSTRAP` environment variables
- Regular registration always creates OPERATOR users

**5. Build fails with "Java 25 is required"**

Maven enforcer plugin detected Java version < 25.

**Solutions:**
```bash
# Check current version
java -version

# Install Java 25 via SDKMAN
sdk install java 25.0.1-amzn
sdk use java 25.0.1-amzn

# Verify
mvn -version
```

**6. CI tests fail with 400 BAD_REQUEST (RESOLVED)**

This issue was caused by missing `@DateTimeFormat` annotations on LocalDate parameters.

**Fix**: Added explicit `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` to all controllers. See [CI_FIX_SUMMARY.md](iteration/CI_FIX_SUMMARY.md) for full details.

## API Documentation

Once the backend is running, visit:
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/api-docs

## Contributing

1. Follow the implementation plan in `/iteration/*.md`
2. Adhere to guidelines in `CLAUDE.md`
3. Ensure all tests pass before committing
4. Use conventional commits for clear history
5. Java 25 is required - no exceptions

## Project Structure

```
/
├── backend/                    # Spring Boot WebFlux backend
│   ├── src/main/java/
│   │   └── com/pl/shugo/gsolog/
│   │       ├── api/           # Controllers and DTOs
│   │       ├── application/   # Services (use cases)
│   │       ├── domain/        # Entities and repositories
│   │       └── infrastructure/# Security, config, adapters
│   ├── src/main/resources/
│   │   ├── db/migration/      # Flyway SQL migrations
│   │   └── application.yaml   # Configuration
│   └── pom.xml                # Maven dependencies
│
├── frontend/                   # Angular 21 SPA
│   ├── src/app/
│   │   ├── core/              # Services, guards, interceptors
│   │   ├── shared/            # Shared components, pipes
│   │   └── features/          # Feature modules
│   └── package.json
│
├── docker/                     # Local development
│   └── docker-compose.yml     # PostgreSQL + pgAdmin
│
├── .github/workflows/          # CI/CD pipelines
│   ├── backend-ci.yml         # Backend tests (Java 25)
│   ├── ci.yaml                # Monorepo CI
│   └── frontend-ci.yml        # Frontend build
│
├── iteration/                  # Implementation plan
│   ├── step-*.md              # Step-by-step guides
│   └── CI_FIX_SUMMARY.md      # Recent CI fix documentation
│
├── CLAUDE.md                   # Development guidelines
├── README.md                   # This file
└── docker-compose.yml          # Full stack deployment
```

## License

MIT
