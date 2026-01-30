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

## API Documentation

Once the backend is running, visit:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## License

MIT
