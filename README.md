# Appointment Booking System

A production-minded full-stack foundation for an Appointment Booking System assessment.

The current increment establishes the Angular, Spring Boot, and PostgreSQL runtime layers. It intentionally contains no booking features yet; product behaviour will be delivered in small, reviewable increments after the database contract.

## Technology baseline

- Angular 22 and Angular Material
- Node.js 24 LTS and npm
- Java 25 and Spring Boot 4.1
- Maven Wrapper
- PostgreSQL 18
- Flyway Community Edition, owned by the backend
- Docker Compose

## Repository layout

```text
frontend/   Angular application and Nginx image
backend/    Spring Boot API, Flyway migrations, OpenAPI contract and image
compose.yaml
```

Flyway SQL is stored under `backend/src/main/resources/db/migration/`. PostgreSQL is a separate runtime service, not a separate schema repository.

## Source structure

The frontend uses standalone Angular components and lazy-loaded feature routes. Application-wide concerns are kept separate from feature code, and reusable UI moves to `shared/` only after more than one feature needs it:

```text
frontend/src/app/
├── core/          application-wide configuration, guards, interceptors and singleton services
├── layout/        application shells and navigation frames
├── features/      feature-owned pages, components, models, routes and API access
├── shared/        reusable presentation/utilities with no feature ownership (created when needed)
├── app.config.ts
└── app.routes.ts
```

The backend is organized by business feature rather than one global controllers/services/repositories tree. Each feature owns its adapters and logic:

```text
com.github.ppj789.booking/
├── <feature>/
│   ├── api/             HTTP controllers and transport models
│   ├── application/     use cases, orchestration and ports
│   ├── domain/          business rules and domain types
│   └── infrastructure/  persistence and external adapters
├── platform/            security, web, observability and configuration
└── AppointmentBookingApiApplication.java
```

Feature packages are added only when their delivery ticket introduces real code; the repository does not keep empty architectural placeholder folders.

## Run the complete foundation

Requirements: Docker with Compose support.

```bash
docker compose up --build
```

When the services are healthy:

- Application shell: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

Stop the stack while preserving the database volume:

```bash
docker compose down
```

Delete the disposable local database explicitly:

```bash
docker compose down --volumes
```

This destructive reset removes local assessment data. It is not a backup or production recovery procedure.

## Run checks independently

Frontend:

```bash
cd frontend
npm ci
npm run test:ci
npm run build
```

Backend, with Java 25 configured:

```bash
cd backend
./mvnw test
./mvnw package
```

On Windows PowerShell, use `./mvnw.cmd` instead.

Neither automated suite requires the other application service or PostgreSQL to be running. Real Flyway/PostgreSQL and assembled-system behaviour are validated through the Compose path.

## Local development

For Angular development, start a local backend on port `8080`, then run:

```bash
cd frontend
npm start
```

The Angular development proxy forwards API, health, OpenAPI, and Swagger UI requests to the backend.

For IntelliJ backend development, select a Java 25 SDK and run `AppointmentBookingApiApplication`. Start only PostgreSQL when needed:

```bash
docker compose up postgres
```

The default local credentials are intentionally fake and may be overridden through a private `.env` copied from `.env.example`.

## Foundation security posture

- No product endpoint is exposed by this increment.
- Health, the versioned OpenAPI document, and local Swagger UI are the only public backend surfaces.
- All other requests are denied until authentication and feature issues introduce them deliberately.
- Raw credentials, personal data, and invitation tokens must never be committed or logged.

## Current limitation

This is foundation code only. User registration, calendars, Invitations, availability, and Appointments are not implemented yet.
