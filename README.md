# Appointment Booking System

A production-minded full-stack Appointment Booking System assessment.

The current increment provides the Angular, Spring Boot, and PostgreSQL runtime layers plus secure local Admin authentication, Branch-bound Staff administration, one-time Staff account setup, a private Staff calendar with Availability Block management, the Staff Invitation lifecycle, privacy-safe Guest availability, and atomic idempotent Appointment confirmation.

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
│   ├── controller/      HTTP endpoints and allow-listed transport models
│   ├── exception/       feature-owned application failures
│   ├── persistence/     JPA entities and Spring Data repositories
│   ├── security/        feature-owned authentication and access policy
│   └── service/         use cases and business orchestration
├── platform/            security, web, observability and configuration
└── AppointmentBookingApiApplication.java
```

Feature packages are added only when their delivery ticket introduces real code; the repository does not keep empty architectural placeholder folders.

Frontend styling is mobile-first. `frontend/src/_theme.scss` owns platform design tokens, while `frontend/src/styles/_breakpoints.scss` owns shared responsive boundaries. Component styles consume those decisions but retain responsibility for their own layout. Repeated UI patterns, such as data tables, move into a Material-based component under `shared/ui/` once the product introduces a concrete reusable need.

## Run the complete foundation

Requirements: Docker with Compose support.

```bash
docker compose up --build
```

When the services are healthy:

- Application shell: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Generated OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Generated OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>
- Health: <http://localhost:8080/actuator/health>

The local profile generates the OpenAPI description from implemented Spring controllers, DTOs, validation rules, and focused Swagger annotations. API documentation is disabled by default outside the local profile.

When the `local` profile is active, the demo-data initializer creates one clearly synthetic Admin if it is absent:

- Email: `admin@example.test`
- Password: `admin`

The Admin can manage Staff across all configured Branches; Staff accounts remain attached to exactly one Branch. These are local demonstration values, not production defaults. Outside the `local` profile, the application never creates an Admin. Production account provisioning is an explicit operational responsibility outside this assessment POC.

### Exercise Staff account setup locally

Swagger UI can demonstrate the implemented Admin and Staff setup flow:

1. Call `POST /api/v1/sessions` with the local Admin credentials above.
2. Call `GET /api/v1/admin/branches` and select a Branch public code.
3. Call `POST /api/v1/admin/staff` with a unique Staff email:

   ```json
   {
     "branchCode": "JHB-DEMO",
     "displayName": "Demo Staff",
     "email": "staff@example.test"
   }
   ```

4. Copy the value after `#token=` from the one-time `setupUrl` response.
5. Call `POST /api/v1/account-setup`, supply that value in `X-Setup-Token`, and choose a password between 12 and 128 characters.
6. Log out the Admin, then sign in with the new Staff email and password.

The setup URL is returned once with `Cache-Control: no-store`; only its SHA-256 hash is persisted. Reusing a consumed token returns the same generic unavailable response as an invalid, expired, or revoked token.

### Exercise Staff Availability Blocks locally

After signing in as active Staff, Swagger UI can demonstrate the private calendar boundary:

1. Call `GET /api/v1/me/calendar` with required UTC `from` and `to` instants no more than 31 days apart.
2. Call `POST /api/v1/me/availability-blocks` with future UTC boundaries on the 15-minute grid:

   ```json
   {
     "startAt": "replace-with-a-future-15-minute-boundary",
     "endAt": "replace-with-a-later-15-minute-boundary"
   }
   ```

3. Read the calendar again and confirm the returned block uses the committed UTC interval.
4. Attempt an overlapping block and confirm the safe `409 CALENDAR_CONFLICT` response.
5. Call `DELETE /api/v1/me/availability-blocks/{blockId}` while the block remains in the future.

The account ID and Branch scope come from the authenticated session; they are never accepted from the request. Calendar reads expose the owning Staff User's Appointment summary and private block intervals only. PostgreSQL's shared reservation exclusion constraint remains authoritative for overlap races.

### Exercise the Staff Invitation lifecycle locally

After signing in as active Staff, Swagger UI can demonstrate the private Invitation lifecycle:

1. Call `POST /api/v1/me/invitations` to create a draft:

   ```json
   {
     "title": "Account discussion",
     "description": "Review the available account options.",
     "guest": {
       "displayName": "Alex Guest",
       "email": "alex.guest@example.test"
     },
     "defaultDurationMinutes": 60
   }
   ```

2. Optionally call `PATCH /api/v1/me/invitations/{invitationId}` while the Invitation is still a draft.
3. Call `POST /api/v1/me/invitations/{invitationId}/activate` with no body.
4. Copy the returned `shareUrl` immediately. It is returned once and cannot be retrieved later.
5. If the link is lost or should no longer be usable, call `POST /api/v1/me/invitations/{invitationId}/revoke` and create a replacement draft.

The backend derives immutable Staff and Branch ownership from the authenticated session. Activation uses a locked state transition, stores only a SHA-256 token hash, and returns the high-entropy capability in the URL fragment with `Cache-Control: no-store`. Drafts alone are editable, and only unused active Invitations are revocable.

### Exercise Guest Invitation and availability access locally

Use the raw value after `#token=` from the one-time Staff `shareUrl` as `X-Invitation-Token`:

1. Call `GET /api/v1/guest/invitation` to read the Guest-safe appointment purpose, fixed Staff host, supported durations, and Branch context.
2. Call `GET /api/v1/guest/availability` with a supported `durationMinutes`. Optional `from` values before backend time clamp forward, and `days` defaults to 14 with a maximum of 14.
3. Confirm that returned slots are on the 15-minute UTC grid and omit every interval whose complete duration overlaps an Appointment or Availability Block.
4. Repeat either request with an unknown, revoked, or consumed capability and confirm the same `404 INVITATION_UNAVAILABLE` response.

To commit one of the returned slots:

5. Generate a UUID and send it as `Idempotency-Key` with `POST /api/v1/guest/appointments`:

   ```json
   {
     "startAt": "replace-with-a-returned-UTC-slot-start",
     "durationMinutes": 60
   }
   ```

6. Confirm the new request returns `201 Created`, the committed Appointment and Branch details, and the visible simulated confirmation.
7. Repeat the identical request with the same UUID and capability. It returns the original result with `200 OK` and `Idempotency-Replayed: true` without creating another Appointment.
8. Reuse that UUID with different input and confirm the safe `409 IDEMPOTENCY_KEY_REUSED` response.

Both responses use `Cache-Control: no-store`. Availability contains selectable UTC start/end pairs only: it does not reveal reservation types, block reasons, Appointment details, Guest email, references, or lifecycle state. The Angular Guest screen will later render missing intervals as unavailable in the Branch timezone.

Confirmation revalidates the capability, complete duration, 15-minute grid, and rolling horizon. PostgreSQL's shared exclusion constraint remains authoritative if another request occupies the slot concurrently. A successful transaction inserts the Reservation and immutable Appointment, consumes the Invitation, and only then returns the simulated confirmation. Conflicts create no partial booking and leave the Invitation usable.

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
./mvnw verify
```

Backend verification compiles the application, runs the isolated test suite, packages the application, and checks the Java source against the repository's focused Checkstyle rules.

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

## Security posture

- Login and one-time Staff account setup are public capability endpoints; Admin and Staff operations require a server-managed authenticated session and the matching active workflow type.
- Session cookies are HttpOnly and SameSite Lax. Authenticated mutations retain SPA CSRF protection.
- Staff passwords use Spring Security's adaptive encoder, and setup capabilities are stored only as SHA-256 hashes.
- Staff records are soft-deactivated rather than deleted. Active Invitations or future Appointments block deactivation without exposing Guest content.
- Staff calendar commands derive ownership from the authenticated session. Availability Blocks and Appointments share authoritative PostgreSQL overlap protection.
- Guest capability reads resolve only the stored token hash, return one generic unavailable response for unusable tokens, apply bounded in-memory rate limits, and expose selectable intervals without private occupancy details.
- Guest confirmation requires a client-generated UUID Idempotency Key. Identical retries replay the original committed result; key reuse with changed input is rejected, and concurrent calendar conflicts cannot consume the Invitation or expose existing booking details.
- Raw credentials, personal data, session identifiers, and setup or Invitation tokens must never be committed or logged. Invitation capabilities are returned only inside one-time fragment-based share URLs.
- The global API boundary logs each handled failure once using its HTTP status, stable problem code, method, and path. An outer request filter logs unexpected failures with a server-side stack trace while preserving Spring's safe HTTP error handling.

## Current limitation

Public registration is intentionally unavailable. The Angular Staff and Guest experiences are not implemented yet, so the complete booking journey currently uses the generated Swagger UI. The real PostgreSQL transaction and conflict behaviour still require the later assembled Compose walkthrough; automated tests intentionally remain isolated from PostgreSQL and other services.
