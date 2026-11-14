# Appointment Booking System

A production-minded full-stack system for scheduling an appointment with a specific Staff member at a physical Branch.

## Quick start

Requirements: Docker with Compose support.

```bash
docker compose up --build
```

When all three containers are healthy:

- Application: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

Local demonstration login:

- Email: `admin@example.test`
- Password: `admin`

The local profile creates this synthetic Admin and two synthetic Branches for repeatable developer testing. The short credential is a deliberate local-only convenience, not a production default or a value accepted by the Staff password-setup flow. Outside the `local` profile, startup never creates an account; production provisioning is an explicit operational responsibility.

### Five-step booking walkthrough

1. Sign in as the local Admin and select **Add Staff**.
2. Create a Staff account at either demo Branch and copy the one-time setup link.
3. Open the link in a private browser session, choose a 12–128 character password, and sign in as Staff.
4. Create and activate an Invitation, then copy its one-time Guest link. Availability Blocks can be added first when particular times must be unavailable.
5. Open the Guest link in another private session, choose a returned time and duration, and confirm the Appointment. The committed simulated confirmation includes its `APT-...` reference and Branch details.

The generated OpenAPI JSON and YAML are available at `/v3/api-docs` and `/v3/api-docs.yaml`. The [Local Demonstration Walkthrough](https://github.com/ppj789/appointment-booking-system/wiki/Local-Demonstration-Walkthrough) contains the detailed UI and API exercise stories; the rest of the [project wiki](https://github.com/ppj789/appointment-booking-system/wiki) documents implemented journeys and boundaries.

Stop the stack while retaining the local database:

```bash
docker compose down
```

Delete the disposable local database explicitly:

```bash
docker compose down --volumes
```

The second command permanently removes local assessment data.

## Product interpretation and trade-offs

This solution interprets a Branch Appointment as time with a specific Staff member—for example, a follow-up with an adviser who may not work at the Branch every day. It is not modelled as an anonymous Branch ticket or queue position.

Staff therefore creates a single-use Invitation for a known Guest and appointment purpose. The Guest still schedules the Appointment: they choose the available start time and duration and commit the booking themselves. This boundary supports Staff-specific availability, keeps private calendar occupancy hidden, and gives one clear owner to the authoritative overlap rule.

Within the assessment capacity, the implementation prioritises a complete, secure vertical journey and race-safe booking over a broader discovery catalogue. The trade-off is that a Customer cannot currently begin at a public Branch page, select a service, and discover suitable Staff. A future customer-initiated journey can add Branch and service/reason discovery before routing to eligible Staff availability, while retaining Invitations for known-adviser follow-ups.

## Technology baseline

- Angular 22 and Angular Material
- Node.js 24 LTS and npm
- Java 25 and Spring Boot 4.1
- Maven Wrapper
- PostgreSQL 18
- Flyway Community Edition, owned by the backend
- Unprivileged Nginx frontend runtime
- Docker Compose

## Repository layout

```text
frontend/   Angular application, Nginx configuration and frontend image
backend/    Spring Boot API, Flyway migrations, OpenAPI contract and backend image
compose.yaml
```

Flyway SQL is stored under `backend/src/main/resources/db/migration/`. The backend owns and ships the single migration chain; PostgreSQL is a runtime service rather than a separate schema project.

## Source structure

The frontend uses standalone Angular components and lazy-loaded feature routes. Application-wide concerns remain separate from feature code, and reusable UI moves to `shared/` only after more than one feature proves the need:

```text
frontend/src/app/
├── core/          application-wide configuration, guards, interceptors and singleton services
├── layout/        application shells and navigation frames
├── features/      feature-owned pages, components, models, routes and API access
├── shared/        reusable presentation and utilities with no feature ownership
├── app.config.ts
└── app.routes.ts
```

The backend is organised by business feature rather than one global controller/service/repository tree. Each feature owns its adapters and logic:

```text
com.github.ppj789.booking/
├── <feature>/
│   ├── controller/      HTTP endpoints and allow-listed transport models
│   ├── exception/       feature-owned application failures
│   ├── persistence/     JPA entities and Spring Data repositories
│   ├── security/        feature-owned authentication and access policy
│   └── service/         use cases and business orchestration
├── platform/            security, web, observability and configuration
└── AppointmentBookingApiApplication.java
```

Feature packages are added only when delivery introduces real code; the repository does not keep empty architectural placeholder folders. Frontend styling is mobile-first: `frontend/src/_theme.scss` owns platform design tokens, while `frontend/src/styles/_breakpoints.scss` owns shared responsive boundaries.

## Container assembly

There are two Dockerfiles because the frontend and backend have different build and runtime requirements. The frontend image builds Angular with Node and serves the static output from unprivileged Nginx. The backend image builds and verifies the Spring Boot application with Maven, then runs it as a non-root user on a Java runtime.

Compose assembles those two application images with PostgreSQL, private service networking, health-based startup ordering, persistent local database storage and disposable local configuration. The two Dockerfiles are build/runtime definitions for independent deployable components; Compose is the local orchestration definition for the complete system.

## Build and test independently

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
./mvnw verify
```

On Windows PowerShell, use `./mvnw.cmd verify`.

Backend verification compiles and packages the application, runs the focused unit and controller suite, and applies the repository Checkstyle rules. The backend Docker build also runs `mvn verify`; the frontend Docker build performs a production Angular build but does not run the frontend test suite.

Automated suites do not require the other application service or PostgreSQL. Real Flyway/PostgreSQL constraints and the assembled journey are validated through the Compose path. This is an explicit assessment trade-off: the strongest database and cross-service guarantees are not yet protected by automated Testcontainers or end-to-end tests.

## Local development

For Angular development, start the backend on port `8080`, then run:

```bash
cd frontend
npm start
```

The Angular development proxy forwards API, health, OpenAPI and Swagger UI requests to the backend.

For backend development, select a Java 25 SDK and run `AppointmentBookingApiApplication`. PostgreSQL can be started independently with:

```bash
docker compose up postgres
```

The `local` Spring profile provides disposable database defaults. Other profiles require explicit `BOOKING_DB_URL`, `BOOKING_DB_USERNAME` and `BOOKING_DB_PASSWORD` values and fail startup when they are missing.

## Correctness and data integrity

- PostgreSQL's exclusion constraint is authoritative for overlapping Staff calendar reservations, including concurrent requests.
- Appointment creation, Reservation creation and Invitation consumption commit in one transaction; a failed booking leaves the Invitation usable.
- Guest confirmation uses an Idempotency Key. Identical retries replay the committed result, while changed input with the same key is rejected.

## Operability

- Every backend request returns an `X-Correlation-ID`. A client-supplied canonical UUID is preserved; malformed or unsafe values are replaced with a server-generated UUID.
- API failures use RFC 9457 `application/problem+json` responses with a stable `code` and the same `correlationId` returned in the header. Internal exception details are not exposed.
- Backend stdout uses structured JSON. Request completion and handled-failure records contain correlation ID, method, safe path, status, outcome, duration and stable error code.
- Logs exclude request and response bodies, query strings, cookies, credentials, personal details, session identifiers and raw setup or Invitation capabilities. Unexpected server failures retain a server-side stack trace for diagnosis.
- Liveness reports only the application lifecycle and does not depend on PostgreSQL. Readiness includes application readiness and database connectivity. Health details are not exposed publicly.
- The backend uses graceful shutdown with a bounded drain period. Docker Compose allows 30 seconds before forcing container termination.
- Nginx and Spring Security apply restrictive content-type, framing, referrer, browser-permission and Content Security Policy headers. HSTS is intentionally absent from the local HTTP demonstration and belongs at the production TLS boundary.
- Nginx overwrites client-supplied forwarding chains with its observed source address, and the backend resolves forwarded addresses only in the supported edge-proxy topology.
- Backend and frontend containers run as non-root users.

## Security posture

- Login and one-time Staff account setup are public capability endpoints; Admin and Staff operations require a server-managed authenticated session and the matching active workflow type.
- Session cookies are HttpOnly and SameSite Lax. Authenticated mutations retain SPA CSRF protection.
- Staff passwords use Spring Security's adaptive encoder, and setup capabilities are stored only as SHA-256 hashes.
- Staff records are soft-deactivated rather than deleted. Active Invitations or future Appointments block deactivation without exposing Guest content.
- Staff calendar commands derive ownership from the authenticated session. Availability Blocks and Appointments share authoritative PostgreSQL overlap protection.
- Guest capability reads resolve only the stored token hash, return one generic unavailable response for unusable tokens, and expose selectable intervals without private occupancy details.
- Guest confirmation requires a client-generated UUID Idempotency Key. Identical retries replay the original committed result; changed input with the same key is rejected, and concurrent calendar conflicts cannot consume the Invitation or expose existing booking details.
- Raw credentials, personal data, session identifiers, and setup or Invitation tokens must never be committed or logged. Invitation capabilities are returned only inside one-time fragment-based share URLs.
- The global API boundary logs each handled failure once using its HTTP status, stable problem code, method, safe path and correlation ID. An outer request filter records every completion and logs unexpected failures with a server-side stack trace while preserving safe framework error handling.

The in-memory fixed-window limiter maintains an independent bounded store for each protected feature. Expired entries are reclaimed and the oldest active entry is evicted when a feature reaches its 10,000-window capacity, so flooding one feature cannot exhaust another feature's table. Counts remain local to one backend process and reset on restart; shared or gateway-level limiting is required before horizontal scaling.

## Current limitations

- Booking begins with a Staff-issued Invitation; there is no public Branch/service discovery journey.
- Availability uses a 24-hour calendar minus explicit Staff blocks and Appointments; Branch operating hours and holidays are not yet modelled.
- Email/SMS delivery, Guest cancellation/rescheduling and customer booking history are not implemented.
- Automated tests are isolated from PostgreSQL and there is no browser end-to-end suite.
- The local profile uses synthetic data and credentials and must never be enabled in production.

Public registration is intentionally unavailable. The Angular application provides login, session restoration, Admin Staff management, one-time Staff account setup, logout, protected workflow routes, a Branch-time Staff calendar, Availability Block management, the Staff Invitation lifecycle, and the public Guest booking/simulated-confirmation journey.

The assembled journey has been exercised against PostgreSQL, including contested-slot rejection, transaction rollback that preserves the Invitation, persistence across backend restart, and graceful shutdown. Automated suites intentionally remain isolated from PostgreSQL and other services, so those assembled guarantees are documented manual evidence rather than automated regression coverage.

These limitations and their possible evolution are documented in the [Future Roadmap](https://github.com/ppj789/appointment-booking-system/wiki/Future-Roadmap).
