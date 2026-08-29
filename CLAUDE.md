# student-manager

A student management app with a Spring Boot backend and a React frontend.

## Structure

- `backend/student-manager/` — Spring Boot 3.5.x, Java 21, Maven (`./mvnw`)
- `frontend/` — React 19 + TypeScript, Vite, Tailwind CSS 4, npm
- `docker-compose.yml` (repo root) — Postgres + backend + frontend for local runs

## Build & test

Backend:
```
cd backend/student-manager
./mvnw clean verify
```
Run the full `verify` including tests — don't add `-DskipTests` back. Most tests are pure unit tests (Mockito, e.g. `EnrollmentServiceImplTest`, `JwtUtilTest`) and need nothing external. A few boot the full Spring context with MockMvc (e.g. `AuthControllerTest`) and need a real Postgres reachable at `localhost:5432` with a `studentmanager` DB / `postgres`/`postgres` credentials (matching `application.yml`'s defaults) — either `docker compose up postgres` from the repo root, or a local `postgresql` service (`service postgresql start`, then `createdb studentmanager` as the `postgres` role). CI provisions this via a `postgres:16` service container in `.github/workflows/ci.yml`.

Frontend:
```
cd frontend
npm ci
npm run lint
npx tsc --noEmit
npm test
npm run build
```

Frontend E2E (Playwright, in `frontend/e2e/`): `npm run build && npx playwright test`. Browsers aren't auto-installed — run `npx playwright install --with-deps chromium` first (CI does this; a preinstalled Chromium at `/opt/pw-browsers/chromium` is already wired into `playwright.config.ts` for this sandbox).

## Notes

- CI (`.github/workflows/ci.yml`) runs frontend typecheck+unit-tests+build, a separate frontend-e2e job (Playwright), and backend `mvnw clean verify` (with a `postgres:16` service container) on push/PR to `main`.
- When working an issue via the `Claude Code` GitHub Action (`.github/workflows/claude.yml`), run the checks above before opening a PR, and request review from `rodolphe27` once the PR is open.
