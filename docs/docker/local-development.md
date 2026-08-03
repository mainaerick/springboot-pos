# Docker Local Development

This repository now includes a reproducible Docker development stack for the API, PostgreSQL, and pgAdmin.

## What runs in Docker

- `api`: the Spring Boot application
- `postgres`: PostgreSQL for application data and Flyway migrations
- `pgadmin`: browser-based PostgreSQL administration

## Prerequisites

- Docker Desktop or Docker Engine
- Docker Compose v2

## Setup

Create a local environment file from the example:

```bash
cp .env.example .env
```

Open `.env` and replace the placeholder secrets before using the stack.
Keep the `POSTGRES_*` and `DB_*` values aligned, because the database container reads the `POSTGRES_*` variables while the API reads the `DB_*` variables.

If you want the application to create the first `SUPER_ADMIN` user on an empty database, set:

```env
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=TemporaryStrongPassword123!
```

Use fake credentials in local docs only. Turn bootstrap off again after the first deployment so the application does not try to seed another admin on restart.

## Start

```bash
docker compose up --build
```

## Start in the background

```bash
docker compose up --build -d
```

## View status

```bash
docker compose ps
```

## View logs

```bash
docker compose logs -f api
```

## Stop containers

```bash
docker compose down
```

## Stop and remove volumes

```bash
docker compose down -v
```

Warning: `-v` deletes the named PostgreSQL and pgAdmin volumes, so local data will be lost.

## Beginner QA checklist

### 1. Confirm Docker works

```bash
docker --version
docker compose version
```

Expected: version output without errors.

### 2. Create the local environment file

```bash
cp .env.example .env
```

Open `.env` and replace the placeholder secrets before starting the stack.

### 3. Validate Compose

```bash
docker compose config
```

Expected: valid rendered YAML with no errors.

### 4. Build and start

```bash
docker compose up --build
```

Expected startup order:

1. PostgreSQL starts.
2. PostgreSQL becomes healthy.
3. API starts.
4. Flyway applies migrations.
5. Spring Boot starts.
6. pgAdmin starts.

### 5. Check container status

```bash
docker compose ps
```

Expected: `postgres` is healthy, `api` is running or healthy, and `pgadmin` is running.

### 6. Test the health endpoint

Open:

```text
http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

### 7. Test Swagger

Open:

```text
http://localhost:8080/swagger-ui.html
```

Expected: Swagger UI loads.

### 8. Test login

Use Bruno or Swagger:

```text
POST /api/v1/auth/login
```

Expected: `200 OK`.

### 9. Test a protected endpoint

Use the returned JWT:

```text
GET /api/v1/users
Authorization: Bearer <token>
```

Expected: `200 OK` or `403 Forbidden` depending on the role.

### 10. Open pgAdmin

Open:

```text
http://localhost:5050
```

Register a server using:

- Host: `postgres`
- Port: `5432`
- Database: value of `POSTGRES_DB`
- Username: value of `POSTGRES_USER`
- Password: value of `POSTGRES_PASSWORD`

### 11. Verify persistence

Create a user, then restart the API:

```bash
docker compose restart api
```

The user should still exist.

Then stop and start the stack:

```bash
docker compose down
docker compose up -d
```

The user should still exist because PostgreSQL uses a named volume.

### 12. Stop safely

```bash
docker compose down
```

Data remains on disk.

### 13. Remove data intentionally

Only when you want to reset local data:

```bash
docker compose down -v
```

This deletes the PostgreSQL and pgAdmin volumes.

## Rebuild the API image

```bash
docker compose build api
docker compose up -d api
```

## URLs

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- pgAdmin: `http://localhost:5050`
- PostgreSQL: `localhost:5432`

## pgAdmin connection

Use these values when registering the PostgreSQL server in pgAdmin:

- Host: `postgres`
- Port: `5432`
- Database: value of `POSTGRES_DB`
- Username: value of `POSTGRES_USER`
- Password: value of `POSTGRES_PASSWORD`

Inside Compose, `postgres` is the database container name on the default project network, so `localhost` would point to the pgAdmin container itself.
