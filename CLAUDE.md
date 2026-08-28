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
Run the full `verify` including tests — CI currently skips tests with `-DskipTests`; don't repeat that shortcut for changes you make.

Frontend:
```
cd frontend
npm ci
npm run lint
npx tsc --noEmit
npm run build
```

## Notes

- CI (`.github/workflows/ci.yml`) runs frontend typecheck+build and backend `mvnw clean verify -DskipTests` on push/PR to `main`.
- When working an issue via the `Claude Code` GitHub Action (`.github/workflows/claude.yml`), run the checks above before opening a PR, and request review from `rodolphe27` once the PR is open.
