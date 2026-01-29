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

### Backend

Prerequisites:
- Java 21 or later
- Maven 3.9+

Build and run:
```bash
cd backend

# Run tests
mvn test

# Run application (without database, uses in-memory for now)
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/qsolog-backend-0.0.1-SNAPSHOT.jar
```

The backend will start on `http://localhost:8080`.

Available endpoints:
- **Health check**: `GET http://localhost:8080/actuator/health`
- **Ping endpoint**: `GET http://localhost:8080/api/v1/ping` (returns `{"status":"ok"}`)
- **OpenAPI UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs (JSON)**: `http://localhost:8080/api-docs`

Environment variables (optional):
- `SERVER_PORT` - Server port (default: 8080)
- `DB_URL` - R2DBC PostgreSQL connection URL (not required yet)
- `DB_USER` - Database username (not required yet)
- `DB_PASS` - Database password (not required yet)
- `JWT_SECRET` - JWT signing secret (not implemented yet)
