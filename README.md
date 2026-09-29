# Student Manager

A simple student manager app to learn TypeScript and Spring Boot. Manage students, courses, and enrollments through a React + TypeScript web interface backed by a Spring Boot REST API, with session-based login and role-based access.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4 |
| Backend | Spring Boot 3, Java 21, Spring Security, Spring Session JDBC |
| Database | PostgreSQL 16 (Docker Compose) |
| Containerization | Docker, Docker Compose |
| CI | GitHub Actions |

---

## Features

- Session authentication (register / login / logout) — HttpOnly session cookie stored server-side in Postgres, with CSRF protection; no token in `localStorage`
- Role-based access (STUDENT, TEACHER, ADMIN)
- Student management — create, view, delete
- Course management — create, view, delete, status tracking
- Enrollment management — enroll, confirm, cancel, grade assignment
- Dashboard with stats and recent activity
- Fully containerized with Docker Compose

---

## Project Structure

```
student-manager-app/
├── frontend/                  # React + Vite app
│   ├── src/
│   │   ├── components/        # Layout, Sidebar, ProtectedRoute, StatCard
│   │   ├── context/           # AuthProvider + auth-context + useAuth hook
│   │   ├── pages/             # Dashboard, Students, Courses, Enrollments, MyCourses, Login, Register
│   │   ├── services/          # Axios API services
│   │   └── types/             # TypeScript interfaces
│   ├── Dockerfile
│   └── nginx.conf
├── backend/student-manager/   # Spring Boot app
│   ├── src/main/java/
│   │   └── com/student_manager/
│   │       ├── feature/       # auth, student, course, enrollment
│   │       └── shared/        # config (security), security (OwnershipGuard), exceptions
│   ├── src/main/resources/
│   │   └── application.yml
│   └── Dockerfile
├── docker-compose.yml
└── .github/workflows/ci.yml
```

---

## API Endpoints

Every `/api/**` route except register/login requires a logged-in session (the
HttpOnly `SESSION` cookie); without one the API answers `401`, with the wrong
role `403`. State-changing requests (`POST`/`PUT`/`PATCH`/`DELETE`) must also
send the `X-XSRF-TOKEN` header matching the `XSRF-TOKEN` cookie — axios does
this automatically. The **Access** column is the role rule enforced by
`SecurityConfig` (plus, where noted, a method-level ownership check).

### Auth
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | public | Register a new user and log in. Always creates a `STUDENT`; any `role` in the body is ignored. |
| POST | `/api/auth/login` | public | Log in and start a session. Unknown user, wrong password and disabled account all return the same `401 Invalid username or password`. |
| GET | `/api/auth/me` | any authenticated | The account behind the current session (used by the frontend on startup). |
| POST | `/api/auth/logout` | any | End the session (`204`). |

### Students
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/students/me` | any authenticated | The caller's own student record (matched by account e-mail) |
| GET | `/api/students` | TEACHER, ADMIN | List all students |
| GET | `/api/students/{id}` | TEACHER, ADMIN | Get student by ID |
| POST | `/api/students` | ADMIN | Create a student |
| PUT | `/api/students/{id}` | ADMIN | Update a student |
| DELETE | `/api/students/{id}` | ADMIN | Delete a student |

### Courses
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/courses` | any authenticated | List all courses |
| GET | `/api/courses/{id}` | any authenticated | Get course by ID |
| GET | `/api/courses/status/{status}` | any authenticated | Filter by status |
| POST | `/api/courses` | TEACHER, ADMIN | Create a course |
| PUT | `/api/courses/{id}` | TEACHER, ADMIN | Update a course |
| DELETE | `/api/courses/{id}` | TEACHER, ADMIN | Delete a course |

### Enrollments
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/enrollments/student/{studentId}` | the owning STUDENT, or TEACHER / ADMIN | That student's enrollments. A STUDENT may only read their own — enforced by `@PreAuthorize` on top of the role rule. |
| GET | `/api/enrollments` | TEACHER, ADMIN | List all enrollments |
| GET | `/api/enrollments/{id}` | TEACHER, ADMIN | Get enrollment by ID |
| GET | `/api/enrollments/course/{courseId}` | TEACHER, ADMIN | Enrollments for a course |
| POST | `/api/enrollments` | TEACHER, ADMIN | Create enrollment |
| PATCH | `/api/enrollments/{id}/confirm` | TEACHER, ADMIN | Confirm enrollment |
| PATCH | `/api/enrollments/{id}/cancel` | TEACHER, ADMIN | Cancel enrollment (clears any grade) |
| PATCH | `/api/enrollments/{id}/grade` | TEACHER, ADMIN | Set grade (confirmed enrollments only) |
| DELETE | `/api/enrollments/{id}` | ADMIN | Delete enrollment |

### Operations — public
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Liveness/readiness health check. |
| GET | `/swagger-ui.html`, `/v3/api-docs` | Interactive API docs / OpenAPI spec |

---

## Getting Started

### Prerequisites
- Java 21
- Node.js 22
- PostgreSQL 16 (or Docker)
- Maven

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
Backend runs on `http://localhost:5030`

**3. Start the frontend**
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:5173`; Vite proxies `/api` to the backend, so
the session cookie stays same-origin.

### Running the tests

**Backend**
```bash
cd backend/student-manager
./mvnw clean verify
```
Runs the full build including tests — don't add `-DskipTests`. Most are plain
Mockito unit tests (e.g. `EnrollmentServiceImplTest`, `StudentServiceImplTest`) and need
nothing external. A few boot the full Spring context with MockMvc (e.g.
`AuthControllerTest`, `AuthorizationRulesTest`) and need a Postgres at
`localhost:5432` with a `studentmanager` DB and `postgres`/`postgres`
credentials — `docker-compose up postgres` from the repo root covers it. CI
provisions the same via a `postgres:16` service container.

**Frontend**
```bash
cd frontend
npm ci
npm run lint        # ESLint — clean, enforced in CI
npx tsc --noEmit
npm test            # Vitest unit tests
npm run build
```

**Frontend E2E** (Playwright, in `frontend/e2e/`)
```bash
cd frontend
npx playwright install --with-deps chromium   # first run only
npm run build && npx playwright test
```

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
| Swagger UI | http://localhost:5030/swagger-ui.html |
| PostgreSQL | localhost:5432 |

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
Every push to `main` deploys automatically (Render's GitHub app is connected to this repo).

| Render setting | Value |
|---|---|
| Service | `student-manager-backend` — free plan, Frankfurt (same region as the Supabase DB) |
| URL | https://student-manager-backend-ijo8.onrender.com |
| Runtime | Docker, root directory `backend/student-manager` |
| Health check path | `/actuator/health` |
| `PORT` | `5030` — tells Render which port the app listens on |
| `SESSION_COOKIE_SECURE` | `true` |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Supabase connection (JDBC URL) — set as secrets in the Render dashboard |

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
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password |
| `SESSION_TIMEOUT` | `1h` | Idle timeout of the server-side session |
| `SESSION_COOKIE_SECURE` | `false` | Mark the `SESSION` cookie `Secure` — set `true` in any HTTPS deployment |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost` | Comma-separated allowed browser origins |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | *(empty)* | Comma-separated origin patterns (e.g. `https://*.example.com`) |
| `SPRING_JPA_DDL_AUTO` | `update` | Hibernate schema mode; set `validate` once migrations exist |
| `SPRING_JPA_SHOW_SQL` | `false` | Log every SQL statement (dev only) |
| `LOG_LEVEL_APP` / `LOG_LEVEL_SECURITY` / `LOG_LEVEL_SQL` | `INFO` / `WARN` / `WARN` | Per-area log levels |
| `VITE_API_URL` | `/api` | Backend API base URL (frontend). Keep it same-origin — Vite, nginx and the Vercel rewrite proxy `/api` to the backend. |
