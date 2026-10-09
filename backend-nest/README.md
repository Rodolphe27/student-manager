# Student Manager — NestJS backend

A second implementation of the Student Manager API, written with NestJS. It is a drop-in twin of the Spring Boot backend in
`../backend/student-manager`: **same `/api` routes, same JSON, same database schema**. The frontend cannot tell them apart,
so you can switch between them.

| | Spring Boot | NestJS |
|---|---|---|
| Folder | `backend/student-manager` | `backend-nest` |
| Language | Java 21 | TypeScript (Node 22) |
| Database access | Spring Data JPA / Hibernate | `pg` with plain SQL |
| Schema migrations | Flyway (owner of the schema) | none — uses Spring's schema |
| Sessions | Spring Session (`spring_session` table), cookie `SESSION` | `express-session` (`nest_session` table), cookie `SESSION` |
| CSRF | `XSRF-TOKEN` cookie → `X-XSRF-TOKEN` header | the same |
| Port | 5030 | 5030 |

## Run it

```bash
cd backend-nest
npm install
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/studentmanager npm run start:dev
```

The database must already contain the schema, i.e. start the Spring backend once against it (it runs the Flyway migrations).
Both backends can share one database; only the login sessions are separate (a switch means signing in again).

| Variable | Default | Meaning |
|---|---|---|
| `DATABASE_URL` | `postgresql://postgres:postgres@localhost:5432/studentmanager` | Postgres connection string (`sslmode=require` is honoured) |
| `PORT` | `5030` | listen port |
| `SESSION_SECRET` | dev value | signs the session cookie — **required in production** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost` | allowed browser origins |
| `DEFAULT_ACCOUNT_PASSWORD` | `testuser12` | password for accounts an admin creates without typing one |
| `ANTHROPIC_API_KEY` | — | enables the AI assistant (chat); without it `/api/chat/status` says `enabled:false` |
| `CHAT_MODEL` | `claude-opus-5-5` | model used by the assistant |

## Switch between the backends

* **Locally:** stop one, start the other — both listen on 5030 and the Vite dev server proxies `/api` there
  (or set `API_TARGET=http://localhost:3000` for another port).
* **Deployed (Vercel):** `node scripts/use-backend.mjs nest` (or `spring`, or `status`) at the repo root rewrites the `/api` proxy in
  `frontend/vercel.json`; commit and push, and Vercel redeploys against the other backend. Addresses are in `backends.json`.

## Tests

```bash
npm test                     # starts its own throw-away Postgres (slow the first time)
node scripts/test-db.mjs &   # …or keep one running and reuse it:
TEST_DATABASE_URL=postgresql://postgres:postgres@localhost:54329/postgres npm test
```

The tests apply the Flyway migrations from the Spring project to a fresh database and exercise the API over HTTP, covering
login/CSRF, the role rules, ownership checks, validation messages, paging, optimistic locking, the enrollment flow, the profile and
the AI assistant (with a fake Claude).

## Layout

```
src/
  main.ts, app.factory.ts, app.module.ts    start-up, session/CSRF/CORS, wiring
  common/                                   access rules (roles), errors, validation, paging, ownership checks
  auth/ students/ teachers/ courses/ terms/ enrollments/ profile/ chat/    one folder per feature, like the Spring packages
test/                                       API tests (Jest + supertest)
```
