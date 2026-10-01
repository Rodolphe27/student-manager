# Architecture

The current shape of the system. The design-phase diagrams and PDFs in
`backend/student-manager/docs/` and `docs/student-manager-diagrams.drawio` predate several
changes (see [Superseded documents](#superseded-documents)); this file is kept in step with the code.

## Runtime

```mermaid
flowchart LR
    Browser["Browser<br/>React SPA (PWA)"] -- "/api/** same-origin<br/>SESSION cookie + X-XSRF-TOKEN" --> Edge["Vite (dev) / nginx (compose) / Vercel rewrite (prod)"]
    Edge --> API["Spring Boot API :5030<br/>Spring Security + OwnershipGuard"]
    API -- "JPA (validate) + Flyway" --> DB[("PostgreSQL 16")]
    API -- "Spring Session JDBC" --> DB
```

- Authentication is a server-side session: login sets an HttpOnly `SESSION` cookie whose
  data lives in the `spring_session` tables. There is no token in the browser.
- CSRF: the API sets an `XSRF-TOKEN` cookie; axios echoes it in `X-XSRF-TOKEN` on writes.
- The schema is owned by Flyway (`db/migration`); Hibernate only validates it.

## Data model

```mermaid
erDiagram
    USERS ||--o| STUDENTS : "account (0..1)"
    USERS ||--o| TEACHERS : "account (0..1)"
    TEACHERS |o--o{ COURSES : "runs"
    TERMS |o--o{ COURSES : "contains"
    STUDENTS ||--o{ ENROLLMENTS : "has"
    COURSES ||--o{ ENROLLMENTS : "has"

    USERS { string username UK
            string email UK
            string role "STUDENT | TEACHER | ADMIN"
            string first_name "only for accounts without a profile" }
    STUDENTS { string matriculation_number UK
               string email UK }
    TEACHERS { string email UK
               string department }
    TERMS { string name UK
            date start_date
            date end_date }
    COURSES { string code UK
              int credit_hours
              string status "ACTIVE | INACTIVE | ARCHIVED" }
    ENROLLMENTS { string status "PENDING | CONFIRMED | CANCELLED"
                  string grade "A-F | NOT_GRADED"
                  boolean grade_seen "false = new grade the student has not acknowledged" }
```

Every table also carries `id`, audit columns (`created_at/by`, `updated_at/by`) and an
optimistic-lock `version`. `(student_id, course_id)` is unique on enrollments.

## Who may do what

The authoritative rules are `SecurityConfig` (role per route) and `OwnershipGuard` (ownership
per record). The README has the full endpoint table; in short:

| Concern | Rule |
|---|---|
| Course create | TEACHER (becomes its teacher) or ADMIN (picks teacher and term) |
| Course edit / delete | ADMIN, or the TEACHER who runs it |
| Enrollment list | ADMIN: all. TEACHER: only enrollments in their courses |
| Enroll | STUDENT: themselves. TEACHER: into their courses. ADMIN: anyone |
| Confirm / grade | ADMIN, or the course's TEACHER |
| Cancel | ADMIN, the course's TEACHER, or the owning STUDENT while `PENDING` |
| Students / teachers | TEACHER and ADMIN read, only ADMIN writes |
| Accounts | created by ADMIN only |

## Enrollment lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING: student / staff enrolls
    PENDING --> CONFIRMED: teacher or admin confirms
    PENDING --> CANCELLED: student withdraws / staff cancels
    CONFIRMED --> CANCELLED: staff cancels (grade cleared)
    CANCELLED --> PENDING: enrolling again reopens it
    CONFIRMED --> CONFIRMED: teacher saves a grade (student is notified until they mark it read)
```

## Superseded documents

These were produced during the design phase and are **not** regenerated with the code:

- `backend/student-manager/docs/student-manager-class-diagram.drawio` and the two UML PDFs
  still show the removed `RegistrationInvite` flow and JWT.
- `backend/student-manager/docs/student-manager-system-overview.*` and
  `docs/student-manager-diagrams.drawio` still describe JWT bearer authentication
  (now session cookies) and `ddl-auto=update` (now Flyway).
- `student-manager-db-design.pdf` and `student-manager-functional-requirements.pdf` predate
  Flyway, teacher-owned courses and the student withdraw flow.

Use this file and the README as the source of truth.
