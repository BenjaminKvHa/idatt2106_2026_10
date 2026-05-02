# Nettdetektivene

A gamified web application that teaches children (grades 3–7) to recognize and handle disinformation, digital crime, and critical online situations. Built as part of IDATT2106 – Systemutvikling med smidig prosjekt at NTNU.

---

## Table of Contents

- [About the Project](#about-the-project)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation and Setup](#installation-and-setup)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [Demo Accounts](#demo-accounts)
- [API Documentation](#api-documentation)
- [Environment Variables](#environment-variables)
- [CI/CD](#cicd)
- [Contributors](#contributors)
- [License](#license)

---

## About the Project

Nettdetektivene is an educational web application developed as part of IDATT2106 - Systemutvikling med smidig prosjekt at NTNU, spring 2026.

Children are increasingly exposed to disinformation, phishing, and other digital threats, yet often lack the skills to recognize and handle them. Nettdetektivene addresses this by teaching pupils in grades 3-7 (ages 8-12) to build digital literacy through a gamified learning experience.

Players take on the role of a detective, navigating a map of "stops" such as Nyhetskvartalet, Fotografen and datasenteret, where they solve progressively harder tasks about source criticism and AI-generated content. Teachers can create classrooms, manage pupils, create custom tasks, and track progression.

**Key features:**

- **Pupils**: Avatar customization, map-based progression, notebook, medals, leaderboard, "Ukens mysterium" submissions
- **Teachers**: Classroom management, pupil progression overview, task creation, notification center

---

## Tech Stack

| Layer          | Technology                                                                 |
|----------------|----------------------------------------------------------------------------|
| Backend        | Java 21, Spring Boot 3.5.13 (Web, Data JPA, Security, Validation, Actuator) |
| Frontend       | Vue 3.5, TypeScript, Vite 8                                               |
| Database       | PostgreSQL 16                                                              |
| Auth           | JJWT 0.12.6 (JWT access tokens + HTTP-only refresh cookies)               |
| File Storage   | MinIO (S3-compatible), AWS SDK v2                                          |
| API Docs       | springdoc-openapi 2.8 (Swagger UI)                                        |
| Build          | Maven (backend), npm (frontend)                                           |
| Infrastructure | Docker Compose (db, backend, frontend, minio, db-seed)                    |
| CI/CD          | GitHub Actions                                                            |
| Testing        | JUnit, Mockito, Embedded Postgres (backend); Vitest, Cypress 15 (frontend) |
| Security       | OWASP dependency-check, JaCoCo (>=50% coverage gate)                      |


---

## Prerequisites

**To run the full stack (recommended):**
 
- **Docker** 24+ (includes Docker Compose)

**For local development outside containers:** 
- **Java** JDK 21 (Maven wrapper `./mvnw` is included)
- **Node.js** ^20.19 or >=22.12, with **npm**
- **PostgreSQL** 16 (only if not using the Docker `db` service)
- **Git** for cloning the project

---

## Installation and Setup

The full stack runs in Docker. Schema ([schema.sql](schema.sql)) and seed data ([seed.sql](seed.sql)) are applied automatically on first boot — no manual database setup needed.

### 1. Clone the repository

```bash
git clone https://github.com/ntnu-runners/idatt2106_2026_10.git
cd idatt2106_2026_10
```

### 2. Configure environment variables

Copy the template and fill in your credentials:

```bash
cp .env.template .env
```

Then edit `.env`, at minimum, replace the placeholders marked `change_me*` and generate your own `JWT_SECRET_KEY`:

```bash
openssl rand -base64 32
```


### 3. Start the stack

```bash
docker compose up -d --build
```

First boot takes ~30–60 seconds while the backend healthcheck passes and `db-seed` runs. Tail logs with `docker compose logs -f backend`.

Once healthy, services are available at:

| Service        | URL                                       |
|----------------|-------------------------------------------|
| Frontend       | http://localhost:3000                     |
| Backend API    | http://localhost:8081                     |
| Swagger UI     | http://localhost:8081/swagger-ui.html     |
| MinIO console  | http://localhost:9101                     |

(Ports are configurable via `.env`.)

### Common commands

```bash
docker compose down                          # stop the stack
docker compose down -v                       # stop AND wipe db + minio volumes (re-runs schema + seed on next up)
docker compose up -d --force-recreate frontend   # rebuild a single service
docker compose logs -f backend               # tail one service's logs
```

---

## Local development (outside Docker)

For faster iteration on backend or frontend code, run them directly against the Dockerized `db` and `minio` services. Make sure required env vars (`POSTGRES_*`, `JWT_SECRET_KEY`, `S3_*`) are exported in your shell first.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs at `http://localhost:5173`.

---

## Running Tests

### Backend

```bash
cd backend
./mvnw test            # runs unit tests + JaCoCo report
./mvnw verify          # additionally enforces the >=50% line-coverage gate
```

Coverage report is generated at `backend/target/site/jacoco/index.html`.

> Requirement: ≥ 50% server-side line coverage (enforced by `./mvnw verify`).

### Frontend

**Unit tests (Vitest):**

```bash
cd frontend
npm run test:unit -- --run         # one-shot
npm run test:coverage              # with coverage report
```

**End-to-end tests (Cypress):**

```bash
cd frontend
npm run build && npm run test:e2e  # headless run against `vite preview` (needs prod build first)
npm run test:e2e:dev               # interactive Cypress UI against the dev server
```

> Cypress specs live in [frontend/cypress/e2e/](frontend/cypress/e2e/). The e2e suite assumes the backend is running (start the stack with `docker compose up -d` first).

> Requirement: ≥ 30% client-side test coverage.

---

## Project Structure

```
idatt2106_2026_XX/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/nettdet/backend/
│   │   │   │   ├── controller/    # REST endpoints
│   │   │   │   ├── service/       # Business logic
│   │   │   │   ├── repository/    # Spring Data JPA repositories
│   │   │   │   ├── domain/        # JPA entities (+ enums/, ids/ for composite keys)
│   │   │   │   ├── dto/           # Request/response records
│   │   │   │   ├── security/      # SecurityConfig, JwtUserPrincipal
│   │   │   │   ├── util/          # JwtAuthFilter, JWT helpers, code generation
│   │   │   │   ├── config/        # S3/MinIO wiring, task seeders
│   │   │   │   ├── validation/    # Custom bean-validation annotations
│   │   │   │   └── exception/     # GlobalExceptionHandler
│   │   │   └── resources/         # application.properties, chapter-tips.json
│   │   └── test/                  # JUnit + Mockito tests
│   ├── Dockerfile
│   ├── mvnw / mvnw.cmd            # Maven wrapper
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/            # Reusable Vue components (pupil/, teacher/, shared/, ui/, misc/)
│   │   ├── views/                 # Page-level views (pupil/, teacher/, shared/)
│   │   ├── router/                # Vue Router config
│   │   ├── composables/           # Reusable composition functions (useAudio, useClassrooms, ...)
│   │   ├── utils/                 # apiUtils (fetch wrapper + 401/refresh), jwtUtils, authRedirect
│   │   ├── types/                 # Shared TypeScript types
│   │   ├── assets/                # Images, styles, sounds
│   │   ├── App.vue
│   │   └── main.ts
│   ├── cypress/                   # E2E tests (e2e/, fixtures/, support/)
│   ├── tests/                     # Vitest unit tests (components/, views/, router/, utils/)
│   ├── nginx/                     # nginx config (proxies /api/* to backend:8080)
│   ├── Dockerfile
│   ├── vite.config.ts
│   └── package.json
├── docs/                      # Additional documentation
└── README.md
```

## Demo Accounts

The `db-seed` service loads [`seed.sql`](seed.sql) on stack startup, populating Postgres with classrooms, accounts, file objects, progressions and weekly mysteries. This lets you log in and explore every flow without clicking through signup or grading dozens of tasks first.

> **Login URL:** http://localhost:5173 (or whatever `FRONTEND_PORT` you set)
> **Password for every demo account below:** `Test1234`

### Teachers

| Email                          | Owns                  | What it demonstrates                                                            |
|--------------------------------|-----------------------|---------------------------------------------------------------------------------|
| `teacher.anna@example.com`     | `DEMOAA`, `DEMOBB`, `MAPAAA` | Rich classroom data: pupil progressions, pending join requests, notifications, weekly mystery submissions to approve. **Best starting point.** |
| `teacher.marius@example.com`   | shares `DEMOAA`       | Co-teacher view — verify shared-classroom flows.                                |
| `teacher.empty@example.com`    | `EMPTYA`              | Empty-state UI: no pupils, no progress, no submissions.                         |

### Pupils

| Email                              | Classroom            | State                                                              |
|------------------------------------|----------------------|--------------------------------------------------------------------|
| `pupil.start@example.com`          | `DEMOAA` (confirmed) | **Nothing unlocked** — fresh account, only chapter 1 available.    |
| `pupil.pending@example.com`        | `DEMOAA` (pending)   | Waiting-room flow — sees "venter på godkjenning"-skjerm.           |
| `pupil.map.partial@example.com`    | `MAPAAA` (confirmed) | **Partial unlock** — chapter 1+2 complete, chapter 3 available.    |
| `pupil.complete@example.com`       | `DEMOAA` (confirmed) | **Everything unlocked** — all chapters complete, full medal set.   |
| `pupil.stuck@example.com`          | `DEMOAA` (confirmed) | Stuck/struggling scenario — triggers teacher progress notification. |
| `pupil.weekly@example.com`         | `DEMOAA` (confirmed) | Has an approved weekly mystery + medal — for "Ukens mysterium"-flyt. |

### Recommended walkthrough

1. **Teacher view:** log in as `teacher.anna@example.com`, open `DEMOAA` → see pupil list, progressions, the pending join request from `pupil.pending@example.com`, and a pending weekly-mystery submission you can approve.
2. **Empty pupil:** log in as `pupil.start@example.com` to see the map with only chapter 1 unlocked.
3. **Mid-progress pupil:** log in as `pupil.map.partial@example.com` to see partially unlocked map progression.
4. **Completed pupil:** log in as `pupil.complete@example.com` to see the leaderboard, full medal collection, and end-game state.
5. **Submitting a mystery:** log in as `pupil.start@example.com`, submit an image under "Ukens mysterium", then switch back to `teacher.anna@example.com` to approve it.

> **Resetting state:** `docker compose down -v && docker compose up -d --build` wipes Postgres + MinIO and re-runs `seed.sql` from scratch.

The full account inventory (including secondary pupils used for leaderboards, multi-teacher classrooms, and pending-request fixtures) is documented at the top of [`seed.sql`](seed.sql).

## API Documentation

REST endpoints are documented via **springdoc-openapi** (Swagger UI). With the stack running, the interactive docs and raw OpenAPI spec are available at:

| Resource           | URL                                                    |
|--------------------|--------------------------------------------------------|
| Swagger UI         | http://localhost:8081/swagger-ui.html                  |
| OpenAPI JSON spec  | http://localhost:8081/v3/api-docs                      |

> When running the backend natively (`./mvnw spring-boot:run`), substitute port `8080` for `8081`.

### Endpoint groups

| Path prefix              | Purpose                                              |
|--------------------------|------------------------------------------------------|
| `/api/auth`              | Login, signup, refresh (public)                      |
| `/api/me`                | Current authenticated user                           |
| `/api/classrooms`        | Classroom CRUD, teacher/pupil membership             |
| `/api/pupils`            | Pupil-side actions and profile                       |
| `/api/chapters`          | Chapters and per-pupil progression                   |
| `/api/tasks`             | Task content (per chapter)                           |
| `/api/admin/tasks`       | Teacher task creation and management                 |
| `/api/progress`          | Run submissions, scoring                             |
| `/api/leaderboard`       | Classroom and global leaderboards                    |
| `/api/notebook`          | Pupil notebook entries                               |
| `/api/notifications`     | Teacher notification center                          |
| `/api/medals`            | Medal awards and lookups                             |
| `/api/avatar-assets`     | Avatar customization items                           |
| `/api/avatar-image`      | Pupil avatar image rendering                         |
| `/api/files`             | File uploads/downloads (mediated by access control)  |
| `/api/weekly-mystery`    | "Ukens mysterium" submissions                        |
| `/api/tips`              | Chapter tips ([chapter-tips.json](backend/src/main/resources/chapter-tips.json)) |

All routes except `/api/auth/login`, `/api/auth/signup`, `/api/auth/refresh`, and the Swagger paths require a Bearer JWT. Role enforcement (`TEACHER` / `PUPIL`) happens via `@PreAuthorize` on individual endpoints.

---

## Environment Variables

All configuration is read from a single `.env` file at the repo root (see [.env.template](.env.template)). Docker Compose loads it automatically and injects the values into the relevant containers.

### Host ports

| Variable              | Description                          | Default |
|-----------------------|--------------------------------------|---------|
| `FRONTEND_PORT`       | Host port for the nginx frontend     | `3000`  |
| `BACKEND_PORT`        | Host port for the Spring Boot API    | `8081`  |
| `DB_PORT`             | Host port for Postgres               | `5432`  |
| `MINIO_API_PORT`      | Host port for the MinIO S3 API       | `9100`  |
| `MINIO_CONSOLE_PORT`  | Host port for the MinIO web console  | `9101`  |

### Postgres

| Variable              | Description                          | Default     |
|-----------------------|--------------------------------------|-------------|
| `POSTGRES_DB`         | Database name                        | `app`       |
| `POSTGRES_USER`       | Database user                        | `app`       |
| `POSTGRES_PASSWORD`   | Database password (**change in prod**) | `change_me` |

### Object storage (MinIO)

| Variable               | Description                          | Default                |
|------------------------|--------------------------------------|------------------------|
| `MINIO_ROOT_USER`      | MinIO root username                  | `minioadmin`           |
| `MINIO_ROOT_PASSWORD`  | MinIO root password (**change**)     | `change_me_minio`      |
| `S3_ACCESS_KEY`        | Access key the backend uses          | `minioadmin`           |
| `S3_SECRET_KEY`        | Secret key the backend uses          | `change_me_minio`      |

### Auth & uploads

| Variable                                   | Description                                      | Default |
|--------------------------------------------|--------------------------------------------------|---------|
| `JWT_SECRET_KEY`                           | Base64 key for signing JWTs (`openssl rand -base64 32`) | — |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE`   | Max single upload size                           | `20MB`  |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE`| Max total request size                           | `50MB`  |

### Frontend

The frontend has no custom env vars — nginx ([frontend/nginx/default.conf](frontend/nginx/default.conf)) proxies `/api/*` to the backend service inside the Docker network, so no API base URL needs to be configured at build time.

---

## CI

This project uses **GitHub Actions** for Continuous Integration only — there is no automated deployment. All workflows execute on **NTNU Enterprise self-hosted runners** (`runs-on: ntnu-runners`) and trigger on `workflow_dispatch` and on pushes / PRs to `main` and `dev`.

| Workflow                                                                  | Scope    | What it does                                                                                                                                                                                                                                |
|---------------------------------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [backend-ci.yml](.github/workflows/backend-ci.yml)                        | Backend  | Sets up JDK 21 (Temurin), caches `~/.m2`, runs `./mvnw clean verify` — builds, runs JUnit/Mockito tests, and enforces the **≥ 50% JaCoCo line-coverage gate**. Uploads the JaCoCo report as an artifact (`jacoco-coverage-report`, 30-day retention). |
| [frontend-ci.yml](.github/workflows/frontend-ci.yml)                      | Frontend | Sets up Node 20 with npm cache, runs `npm ci`, `npm run build`, and Vitest with coverage — enforces **≥ 30%** on lines / functions / branches / statements.                                                                                  |
| [owasp-ci.yml](.github/workflows/owasp-ci.yml)                            | Both     | **Backend:** OWASP Dependency-Check via Maven (`-DfailBuildOnCVSS=7`, requires `NVD_API_KEY` secret); uploads the HTML report as `dependency-check-report`. **Frontend:** Retire.js scan (`--severity high`) over installed npm dependencies. |

Path filters keep build minutes lean: `backend-ci.yml` only runs on `backend/**` changes, `frontend-ci.yml` only on `frontend/**`. The OWASP workflow runs on every push/PR.

> **Coverage gates are enforced in CI** — a PR that drops backend coverage below 50% or frontend below 30% will fail.

---

## Contributors

**Scrum Team 10** — IDATT2106 Spring 2026, NTNU

| Name             | Role       |
|------------------|------------|
| Ingrid Neverås Opheim | Scrum Master, Developer |
| Benjamin Kvello-Hansen | Developer  |
| Theodor Strøm Thrane | Developer |
| Bjørn Adam Vangen | Developer |
| Thea Marie Alver Lønvik | Developer |
| Siri Volan Halting | Developer |
| Mateja Vasic | Developer |

---
