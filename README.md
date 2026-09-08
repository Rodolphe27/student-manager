# Student Manager

A full-stack student management application built for FH Dortmund. Manage students, courses, and enrollments through a modern web interface secured with JWT authentication.

---

## Live Demo

| | URL |
|---|---|
| Web app | https://student-manager-fh.vercel.app |
| API | https://backend-production-8ceca.up.railway.app/api |
| API health | https://backend-production-8ceca.up.railway.app/actuator/health |
| Swagger UI | https://backend-production-8ceca.up.railway.app/swagger-ui.html |

Register an account to explore the **student** view. Self-registration always
creates a `STUDENT`; `TEACHER` and `ADMIN` roles are assigned server-side (a
deliberate guard against privilege escalation). The demo runs on free-tier
hosting, so the first request after a period of inactivity can take a few
seconds to wake up.

The frontend is on Vercel, the containerized backend on Railway, and the
database on Supabase — each layer deployed independently.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4 |
| Backend | Spring Boot 3, Java 21, Spring Security, JWT |
| Database | PostgreSQL — 16 locally (Docker Compose), Supabase Postgres in the deployed environment |
| Containerization | Docker, Docker Compose |
| Hosting | Vercel (web), Railway (API), Supabase (database) |
| CI | GitHub Actions |

---

## Features

- JWT authentication (register / login / logout)
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

Every `/api/**` route except `/api/auth/**` requires a `Bearer` JWT. The
**Access** column is the role rule enforced by `SecurityConfig` (plus, where
noted, a method-level ownership check).

### Auth — public
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user. Always creates a `STUDENT`; any `role` in the body is ignored. |
| POST | `/api/auth/login` | Log in, returns a JWT. Unknown user, wrong password and disabled account all return the same `401 Invalid username or password`. |

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
| GET | `/actuator/health` | Liveness/readiness health check. Used as the Railway service `healthcheckPath`. |
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
Frontend runs on `http://localhost:5173`

### Running the tests

**Backend**
```bash
cd backend/student-manager
./mvnw clean verify
```
Runs the full build including tests — don't add `-DskipTests`. Most are plain
Mockito unit tests (e.g. `EnrollmentServiceImplTest`, `JwtUtilTest`) and need
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

## Docker Deployment

Build and run all services with one command from the project root:

```bash
docker-compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost |
| Backend API | http://localhost:5030/api |
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

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/studentmanager` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | *(baked-in dev key)* | HMAC signing key for JWTs — **must** be overridden in any deployed environment |
| `JWT_EXPIRATION` | `86400000` | Token lifetime in milliseconds |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost` | Comma-separated allowed browser origins |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | *(empty)* | Comma-separated origin patterns (e.g. `https://*.vercel.app`) |
| `SPRING_JPA_DDL_AUTO` | `update` | Hibernate schema mode; set `validate` once migrations exist |
| `SPRING_JPA_SHOW_SQL` | `false` | Log every SQL statement (dev only) |
| `LOG_LEVEL_APP` / `LOG_LEVEL_SECURITY` / `LOG_LEVEL_SQL` | `INFO` / `WARN` / `WARN` | Per-area log levels |
| `VITE_API_URL` | `http://localhost:5030/api` | Backend API URL (frontend) |
