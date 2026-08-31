# Student Manager

A full-stack student management application built for FH Dortmund. Manage students, courses, and enrollments through a modern web interface secured with JWT authentication.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4 |
| Backend | Spring Boot 3, Java 21, Spring Security, JWT |
| Database | PostgreSQL 16 |
| Containerization | Docker, Docker Compose |
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
│   │   ├── context/           # AuthContext
│   │   ├── pages/             # Dashboard, Students, Courses, Enrollments, Login, Register
│   │   ├── services/          # Axios API services
│   │   └── types/             # TypeScript interfaces
│   ├── Dockerfile
│   └── nginx.conf
├── backend/student-manager/   # Spring Boot app
│   ├── src/main/java/
│   │   └── com/student_manager/
│   │       ├── feature/       # auth, student, course, enrollment
│   │       └── shared/        # config, exceptions
│   ├── src/main/resources/
│   │   └── application.yml
│   └── Dockerfile
├── docker-compose.yml
└── .github/workflows/ci.yml
```

---

## API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT token |

### Students
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/students` | List all students |
| GET | `/api/students/{id}` | Get student by ID |
| POST | `/api/students` | Create a student |
| PUT | `/api/students/{id}` | Update a student |
| DELETE | `/api/students/{id}` | Delete a student |

### Courses
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/courses` | List all courses |
| GET | `/api/courses/{id}` | Get course by ID |
| GET | `/api/courses/status/{status}` | Filter by status |
| POST | `/api/courses` | Create a course |
| PUT | `/api/courses/{id}` | Update a course |
| DELETE | `/api/courses/{id}` | Delete a course |

### Enrollments
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/enrollments` | List all enrollments |
| POST | `/api/enrollments` | Create enrollment |
| PATCH | `/api/enrollments/{id}/confirm` | Confirm enrollment |
| PATCH | `/api/enrollments/{id}/cancel` | Cancel enrollment |
| PATCH | `/api/enrollments/{id}/grade` | Update grade |
| DELETE | `/api/enrollments/{id}` | Delete enrollment |

### Operations
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Liveness/readiness health check (public, no auth). Use as the Railway service `healthcheckPath`. |

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

GitHub Actions runs on every push and pull request to `main`:

- **Frontend job** — install dependencies, type-check, build
- **Backend job** — Maven build with Java 21

See [.github/workflows/ci.yml](.github/workflows/ci.yml).

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/studentmanager` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password |
| `VITE_API_URL` | `http://localhost:5030/api` | Backend API URL (frontend) |
