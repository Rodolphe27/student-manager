# Student Manager

A student manager app to learn TypeScript and Spring Boot. Manage students, teachers, courses and enrollments through a React + TypeScript web interface backed by a Spring Boot REST API, with session-based login and role-based access.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4 (PWA) |
| Backend | Spring Boot 3.5, Java 21, Spring Security, Spring Session JDBC, Flyway |
| Database | PostgreSQL 16 (Docker Compose) |
| Containerization | Docker, Docker Compose |
| CI | GitHub Actions |

---

## What each role can do

| | STUDENT | TEACHER | ADMIN |
|---|---|---|---|
| Dashboard | own enrollments and open courses | stats plus recent enrollments **in their courses** | stats plus all recent enrollments |
| Course catalogue | browse (read-only) | browse; create courses (they become the course's teacher); edit / delete **their own** courses | create, edit, delete any course; assign teacher and term |
| Enrollments | enroll themselves in active courses, see their grades, **withdraw** a pending enrollment (and enroll again later) | see and enroll students in **their own courses**; confirm, cancel and **grade** enrollments of their courses | everything, plus delete enrollments |
| Students / Teachers | – | read-only lists | create, edit, delete (optionally with a login account) |
| Terms | see the term of a course | see terms | add terms (from the course form) |
| My Profile | edit own name / e-mail / username, change password | same | same |

A new enrollment starts as `PENDING`; a teacher (or admin) confirms it, and only confirmed enrollments can be graded. A teacher picks a grade and then clicks **Save grade** — nothing changes until it is saved. The student then sees a **New** badge, a banner on My Courses and a count in the sidebar until they click **Mark as read**. A student may withdraw while an enrollment is still pending.

Accounts are **only created by an ADMIN** (there is no self-registration): together with a new student/teacher ("Also create a login account") or via `POST /api/auth/register`.

---

## Features

- Session authentication (login / logout) — HttpOnly session cookie stored server-side in Postgres, with CSRF protection; no token in `localStorage`
- Role-based access (STUDENT, TEACHER, ADMIN) enforced in the API (`SecurityConfig` + ownership checks) and mirrored in the UI
- Students, teachers, courses (with teacher and term), terms and enrollments (enroll, confirm, cancel, grade)
- Server-side search, filtering and paging on every list
- Optimistic locking: editing a record somebody else changed answers `409` instead of overwriting it
- Deleting a record that is still referenced (e.g. a student with enrollments) answers `409` with a clear message
- Flyway-managed schema, optional demo data, Swagger UI behind the login
- Fully containerized with Docker Compose

---

## Project Structure

```
student-manager-app/
├── frontend/                  # React + Vite app
│   ├── src/
│   │   ├── components/        # Layout, Sidebar, CourseForm, StatusBadge, ErrorAlert, Pagination, ...
│   │   ├── context/           # AuthProvider, useAuth, usePermissions
│   │   ├── pages/             # Dashboard, Students, Teachers, Courses, Enrollments, MyCourses, Profile, Login
│   │   ├── services/          # Axios API services (one per resource)
│   │   └── types/             # TypeScript interfaces
│   ├── e2e/                   # Playwright tests
│   ├── Dockerfile
│   └── nginx.conf
├── backend/student-manager/   # Spring Boot app
│   ├── src/main/java/com/student_manager/
│   │   ├── feature/           # auth, profile, student, teacher, course (+terms), enrollment
│   │   └── shared/            # config, security (OwnershipGuard), exception, validation, demo (seeder)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/      # Flyway migrations (V1 baseline, V2 drop invites, V3 user names, V4 grade notification)
│   ├── docs/                  # design documents and diagrams
│   └── Dockerfile
├── docker-compose.yml
└── .github/workflows/         # ci.yml, deploy-backend.yml, keep-alive.yml
```

---

## API Endpoints

Every `/api/**` route except login requires a logged-in session (the HttpOnly `SESSION` cookie); without one the API answers `401`, with the wrong role `403`. State-changing requests (`POST`/`PUT`/`PATCH`/`DELETE`) must also send the `X-XSRF-TOKEN` header matching the `XSRF-TOKEN` cookie — axios does this automatically. The **Access** column is the rule enforced by `SecurityConfig`, plus the method-level ownership check where noted (`OwnershipGuard`).

List endpoints are paged and sortable: `?page=0&size=10&sort=lastName,asc` (size ≤ 100). The response is `{"content": [...], "page": {"size", "number", "totalElements", "totalPages"}}`. `PUT` bodies may carry the record's `version`; a stale one answers `409`.

### Auth
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/login` | public | Log in and start a session. Unknown user, wrong password and disabled account all return the same `401`. |
| GET | `/api/auth/me` | any authenticated | The account behind the current session (used on startup). |
| POST | `/api/auth/logout` | any | End the session (`204`). |
| POST | `/api/auth/register` | ADMIN | Create an account with the given `role`. |

### Profile (self-service)
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/profile` | any authenticated | The caller's account plus their student/teacher details. |
| PUT | `/api/profile` | any authenticated | Update own username, e-mail and personal details. |
| PUT | `/api/profile/password` | any authenticated | Change own password (needs the current one). |

### Students
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/students/me` | any authenticated | The caller's own student record (account link first, e-mail as fallback). |
| GET | `/api/students?q=` | TEACHER, ADMIN | Page of students; `q` searches name, matriculation number, e-mail. |
| GET | `/api/students/options` | TEACHER, ADMIN | All students as id + name + matriculation number (dropdowns). |
| GET | `/api/students/{id}` | TEACHER, ADMIN | One student. |
| POST | `/api/students` | ADMIN | Create a student; `createAccount: true` also creates a STUDENT login. |
| PUT | `/api/students/{id}` | ADMIN | Update a student (the linked account's e-mail follows). |
| DELETE | `/api/students/{id}` | ADMIN | Delete a student and their account; `409` while they have enrollments. |

### Teachers
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/teachers/me` | TEACHER, ADMIN | The caller's own teacher record (`404` if none is linked). |
| GET | `/api/teachers?q=` | TEACHER, ADMIN | Page of teachers; `q` searches name, e-mail, department. |
| GET | `/api/teachers/options` | TEACHER, ADMIN | All teachers as id + name + department. |
| GET | `/api/teachers/{id}` | TEACHER, ADMIN | One teacher. |
| POST | `/api/teachers` | ADMIN | Create a teacher; `createAccount: true` also creates a TEACHER login. |
| PUT | `/api/teachers/{id}` | ADMIN | Update a teacher. |
| DELETE | `/api/teachers/{id}` | ADMIN | Delete a teacher and their account; `409` while they run courses. |

### Terms
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/terms` | any authenticated | All terms, newest first. |
| POST | `/api/terms` | ADMIN | Add a term (`name`, optional `startDate` / `endDate`). |

### Courses
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/courses?q=&status=` | any authenticated | Page of courses (with teacher and term). |
| GET | `/api/courses/options` | TEACHER, ADMIN | Courses as id + code + title; a TEACHER only gets the courses they run. |
| GET | `/api/courses/{id}` | any authenticated | One course. |
| GET | `/api/courses/status/{status}` | any authenticated | All courses with that status. |
| POST | `/api/courses` | TEACHER, ADMIN | Create a course. A TEACHER always becomes its teacher; an ADMIN picks `teacherId` / `termId`. |
| PUT | `/api/courses/{id}` | ADMIN, or the TEACHER who runs it | Update a course. |
| DELETE | `/api/courses/{id}` | ADMIN, or the TEACHER who runs it | Delete a course; `409` while it has enrollments. |

### Enrollments
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/enrollments?status=&studentId=&courseId=` | TEACHER, ADMIN | Page of enrollments. A TEACHER only sees those in their own courses. |
| GET | `/api/enrollments/{id}` | TEACHER, ADMIN | One enrollment. |
| GET | `/api/enrollments/student/{studentId}` | the owning STUDENT, TEACHER, ADMIN | A student's enrollments; a STUDENT may only read their own. |
| GET | `/api/enrollments/course/{courseId}` | ADMIN, or the TEACHER who runs the course | A course's roster. |
| POST | `/api/enrollments` | STUDENT (self), TEACHER (own courses), ADMIN | Enroll a student; starts `PENDING`. |
| PATCH | `/api/enrollments/{id}/confirm` | ADMIN, or the course's TEACHER | Confirm an enrollment. |
| PATCH | `/api/enrollments/{id}/cancel` | ADMIN, the course's TEACHER, or the owning STUDENT while `PENDING` | Cancel / withdraw (clears any grade). |
| PATCH | `/api/enrollments/{id}/grade` | ADMIN, or the course's TEACHER | Set the grade (confirmed enrollments only); flags it as unseen for the student. |
| PATCH | `/api/enrollments/{id}/grade-seen` | the owning STUDENT | Acknowledge the grade (clears the "new grade" notice). |
| DELETE | `/api/enrollments/{id}` | ADMIN | Delete an enrollment record. |

### AI assistant (chat)
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/chat/status` | any authenticated | `{"enabled": true}` when the server has an API key; the chat button is hidden otherwise. |
| POST | `/api/chat` | any authenticated | Send the conversation so far; get the assistant's answer plus any changes it proposes. Limited to 10 messages per minute per user. |
| POST | `/api/chat/confirm` | the user the proposal was made for | Run a proposed change (enroll / cancel) after the user pressed *Confirm*. Rights are checked again here. |

The assistant (Claude, called from the backend) can look up courses and the caller's enrollments and answer questions about the app. It acts with exactly the caller's own rights, and it never changes data by itself: a change is only a *proposal* until the user confirms it in the chat. See `feature/chat`.

### Operations
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/actuator/health` (+ `/liveness`, `/readiness`) | public | Health checks. |
| GET | `/swagger-ui.html`, `/v3/api-docs` | any authenticated | Interactive API docs (log in to the app first; Swagger reuses the cookie). |

---

## Getting Started

### Prerequisites
- Java 21
- Node.js 22
- PostgreSQL 16 (or Docker)

### Local Development

**1. Start the database**
```bash
docker-compose up postgres
```

**2. Start the backend**
```bash
cd backend/student-manager
./mvnw spring-boot:run
```
Backend runs on `http://localhost:5030`. The schema is created by Flyway on first start. To get sample data too, start it with `SEED_DEMO_DATA=true` (see below).

**3. Start the frontend**
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:5173`; Vite proxies `/api` to the backend, so the session cookie stays same-origin.

### Demo data

With `SEED_DEMO_DATA=true` (switched on in `docker-compose.yml`, off everywhere else) the backend fills an **empty** database with an admin account plus **10 terms, 10 teachers, 10 students, 10 courses and 10 enrollments**. It only runs when the database is completely empty and does nothing otherwise, so it is safe to leave on.

| Account | Login | Role |
|---|---|---|
| `admin` | `admin@student-manager.local` | ADMIN |
| teachers, e.g. `helena.fischer` | `helena.fischer@student-manager.local` | TEACHER |
| students, e.g. `anna.schmidt` | `anna.schmidt@student-manager.local` | STUDENT |

Every seeded account's password is `DEFAULT_ACCOUNT_PASSWORD` (default `testuser12`). The username is the part of the e-mail before the `@`. Never enable this against a production database.

### Running the tests

**Backend**
```bash
cd backend/student-manager
./mvnw clean verify
```
Runs the full build including tests — don't add `-DskipTests`. Most are plain Mockito unit tests. The rest boot the full Spring context with MockMvc (e.g. `AuthControllerTest`, `AuthorizationRulesTest`) and need a Postgres at `localhost:5432` with a `studentmanager` DB and `postgres`/`postgres` credentials — `docker-compose up postgres` covers it. CI provisions the same via a `postgres:16` service container.

**Frontend**
```bash
cd frontend
npm ci
npm run lint        # ESLint — clean, enforced in CI
npx tsc --noEmit
npm test            # Vitest unit tests
npm run build
```
On Windows/WSL, run `npm ci` and the tests from a native Linux path (or natively on Windows); a `node_modules` installed from the other OS makes Vitest time out.

**Frontend E2E** (Playwright, in `frontend/e2e/`)
```bash
cd frontend
npx playwright install --with-deps chromium   # first run only
npm run build && npx playwright test
```
Set `PW_CHROMIUM_PATH` to use an already installed Chromium.

---

## Docker Compose (all services)

Build and run all services locally with one command from the project root:

```bash
docker-compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost |
| Backend API | http://localhost/api (proxied by nginx) or http://localhost:5030/api |
| Swagger UI | http://localhost:5030/swagger-ui.html (after logging in) |
| PostgreSQL | localhost:5432 |

Compose enables the demo data, so you can log in as `admin` right away (see above).

To stop:
```bash
docker-compose down
```

To stop and remove the database volume:
```bash
docker-compose down -v
```

---

## CI Pipeline

GitHub Actions ([.github/workflows/ci.yml](.github/workflows/ci.yml)) runs on every push and pull request to `main`:

- **`frontend`** — `npm ci`, ESLint, `tsc --noEmit`, Vitest unit tests, build
- **`frontend-e2e`** — Playwright end-to-end tests against a production build
- **`backend`** — `./mvnw clean verify` (Java 21) against a `postgres:16` service container

All three are required status checks for merging to `main`.

---

## Deploying the backend on Render

The backend runs as a Docker web service built from `backend/student-manager/Dockerfile`;
the frontend reaches it through the `/api` rewrite in `frontend/vercel.json`.
Deploys are triggered by the [Deploy backend](.github/workflows/deploy-backend.yml)
workflow: after CI passes on `main` and the commit touched `backend/`, it calls the
service's Render Deploy Hook (repository secret `RENDER_DEPLOY_HOOK_URL`). Render's own
auto-deploy is off, so a commit that fails CI never goes live.

| Render setting | Value |
|---|---|
| Service | `student-manager-backend` — free plan, Frankfurt (same region as the Supabase DB) |
| URL | https://student-manager-backend-ijo8.onrender.com |
| Runtime | Docker, root directory `backend/student-manager` |
| Health check path | `/actuator/health` |
| `PORT` | `5030` — tells Render which port the app listens on |
| `SESSION_COOKIE_SECURE` | `true` |
| `CORS_ALLOWED_ORIGINS` | `https://student-manager-fh.vercel.app` — the frontend's exact public origin. **Required:** browsers send an `Origin` header with every POST, Vercel forwards it, and Spring answers `403 Invalid CORS request` to any origin not listed — even though the `/api` proxy makes the calls look same-origin. Symptom if missing: login/registration fail with 403 while page loads and GET requests work. |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Supabase connection (JDBC URL) — set as secrets in the Render dashboard |
| `DEFAULT_ACCOUNT_PASSWORD` | set a private value — it is the initial password of every account an admin creates without typing one |

The database schema is applied by Flyway on startup (an existing database that predates Flyway is baselined at V1 and gets V2+). Do **not** set `SEED_DEMO_DATA` in production.

On the free instance type the service sleeps after 15 minutes without traffic, and the
first request afterwards waits about a minute while Spring Boot starts. The
[keep-alive workflow](.github/workflows/keep-alive.yml) pings `/actuator/health` every
10 minutes to prevent that. The JVM heap is sized from the container's memory limit
(`MaxRAMPercentage=75` in the Dockerfile).

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/studentmanager` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password (local-dev default — always override in a deployment) |
| `SESSION_TIMEOUT` | `1h` | Idle timeout of the server-side session |
| `SESSION_COOKIE_SECURE` | `false` | Mark the `SESSION` cookie `Secure` — set `true` in any HTTPS deployment |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost` | Comma-separated allowed browser origins |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | *(empty)* | Comma-separated origin patterns (e.g. `https://*.example.com`) |
| `SPRING_JPA_DDL_AUTO` | `validate` | Hibernate schema mode; the schema itself is managed by Flyway (`db/migration`) |
| `ANTHROPIC_API_KEY` | *(empty)* | API key for the in-app assistant. Without it the chat button is hidden and `/api/chat` answers 503. Set it only on the backend (never in the frontend). |
| `CHAT_MODEL` | `claude-opus-5-5` | Claude model the assistant uses |
| `DEFAULT_ACCOUNT_PASSWORD` | `testuser12` | Initial password for accounts an admin creates without typing one (e.g. "Also create a login account" on a new student/teacher) and for the demo accounts. Change it for anything beyond a demo |
| `SEED_DEMO_DATA` | `false` | Fill an empty database with an admin account and 10 terms/teachers/students/courses/enrollments (local demos only) |
| `SPRING_JPA_SHOW_SQL` | `false` | Log every SQL statement (dev only) |
| `LOG_LEVEL_APP` / `LOG_LEVEL_SECURITY` / `LOG_LEVEL_SQL` | `INFO` / `WARN` / `WARN` | Per-area log levels |
| `VITE_API_URL` | `/api` | Backend API base URL (frontend). Keep it same-origin — Vite, nginx and the Vercel rewrite proxy `/api` to the backend. |
