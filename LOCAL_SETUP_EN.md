# Quick start with Docker Compose (DB + backend + frontend)

Goal: bring up the entire local stack — PostgreSQL, backend and frontend — with a single command in Docker.

## Requirements
- Docker Desktop/Engine with Docker Compose v2 support

## Start
1) From the repository root, run:
- `docker compose up --build`

2) Open in your browser:
- Frontend: http://localhost:3000
- Backend health check: `curl http://localhost:4040/ping`
- Postgres is exposed on local port 5432

## Common commands
- Run in background: `docker compose up -d`
- View logs: `docker compose logs -f` or for a specific service `docker compose logs -f backend`
- Restart backend only: `docker compose restart backend`
- Stop and remove containers: `docker compose down`

## Notes
- Postgres credentials: user=secret, password=secret, db=secrethitler, port=5432.
- Frontend and backend code are mounted into containers as volumes. Frontend picks up changes automatically; after changing Java code, restart the `backend` service.
- If ports 3000/4040/5432 are occupied — free them or change them in `docker-compose.yml`.
