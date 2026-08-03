# Story 7.6 — Docker Development Environment

## Instructions for Codex

Read the repository root `AGENTS.md` first.

Then inspect:

- `pom.xml`
- Maven Wrapper files
- All application profile files
- Database and Flyway configuration
- Actuator configuration
- Existing `.gitignore`
- Existing Docker-related files
- README
- OpenAPI configuration
- The current application port
- PostgreSQL database name and credentials used locally

Implement this story as a focused infrastructure change.

Do not modify unrelated business logic.

---

# 1. Context

This repository is an enterprise Inventory and Point of Sale SaaS backend built with:

- Java 17
- Spring Boot 4.x
- Maven Wrapper
- PostgreSQL
- Flyway
- Spring Security
- JWT
- Spring Boot Actuator
- OpenAPI and Swagger UI

The application currently runs locally using a manually installed PostgreSQL instance.

This story introduces Docker-based builds and a reproducible local development stack.

---

# 2. Objective

Implement:

- A production-oriented multi-stage `Dockerfile`.
- A `.dockerignore`.
- A root-level `compose.yml`.
- Docker services for:
    - Spring Boot POS API
    - PostgreSQL
    - pgAdmin

- Environment-variable configuration.
- Persistent database and pgAdmin volumes.
- Container health checks.
- Correct startup ordering.
- A Docker-specific Spring profile if needed.
- Beginner-friendly Docker QA instructions.
- README documentation.
- Automated configuration checks where practical.

Do not add Redis yet unless application code already depends on Redis.

Do not add Kubernetes.

Do not add a cloud deployment pipeline.

---

# 3. Files to Create

Create at the project root:

```text
Dockerfile
.dockerignore
compose.yml
.env.example
```

Create if needed:

```text
src/main/resources/application-docker.yml
```

Create optional documentation:

```text
docs/docker/local-development.md
```

Update:

```text
README.md
.gitignore
```

Do not commit a real `.env` containing secrets.

---

# 4. Dockerfile Requirements

Create a multi-stage Dockerfile.

## Build stage

Use a Java 17 JDK image.

Use the Maven Wrapper included in the project.

Recommended structure:

1. Set a working directory.
2. Copy Maven Wrapper files and `pom.xml`.
3. Download dependencies where practical for layer caching.
4. Copy source code.
5. Run the Maven package or verify command.
6. Produce the executable Spring Boot JAR.

Do not depend on Maven being installed on the developer’s computer.

Do not skip tests silently.

A build argument may allow tests to be skipped explicitly for specialized builds, but the default build should be safe and documented.

## Runtime stage

Use a smaller Java 17 runtime image.

Requirements:

- Copy only the built JAR and required runtime files.
- Create and use a non-root user.
- Expose the application port.
- Use an explicit `ENTRYPOINT`.
- Support JVM options using an environment variable.
- Support Spring profiles using environment variables.
- Do not include source code, Maven caches, or build tools in the final runtime image.

Use a stable Java 17 image family such as Eclipse Temurin.

Do not use `latest` tags.

Select an explicit supported Java 17 tag and document it.

---

# 5. Container Security

The API container must not run as root.

Create a dedicated user such as:

```text
spring
```

The runtime image should execute the JAR as that user.

Do not bake secrets into the image.

Do not copy `.env`, `.git`, IDE files, local databases, or test reports into the image.

---

# 6. `.dockerignore`

Exclude at minimum:

```text
.git
.github
.idea
.vscode
target
*.log
.env
.env.*
!.env.example
README-local*
docs/drafts
```

Also exclude OS-specific temporary files.

Do not exclude files required by the Maven Wrapper build.

Confirm `.mvn`, `mvnw`, `mvnw.cmd`, `pom.xml`, and `src` remain available to the Docker build context.

---

# 7. Docker Compose Services

Create these services:

```text
api
postgres
pgadmin
```

Use descriptive container names only if the repository convention requires them. Avoid names that prevent scaling unnecessarily.

---

# 8. PostgreSQL Service

Use a pinned supported PostgreSQL image version.

Do not use `latest`.

Configure with environment variables:

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
```

Provide safe development defaults through Compose interpolation, while allowing overrides from `.env`.

Example intent:

```yaml
environment:
    POSTGRES_DB: ${POSTGRES_DB:-pos}
    POSTGRES_USER: ${POSTGRES_USER:-postgres}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
```

Use a named volume for data:

```text
postgres_data
```

Expose PostgreSQL to the host only if local database tools require it.

Suggested host mapping:

```text
5432:5432
```

Allow the host port to be overridden:

```text
${POSTGRES_PORT:-5432}:5432
```

---

# 9. PostgreSQL Health Check

Add a health check using `pg_isready`.

The API must wait until PostgreSQL is healthy.

Example intent:

```yaml
healthcheck:
    test:
        - CMD-SHELL
        - pg_isready -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-pos}
    interval: 5s
    timeout: 5s
    retries: 10
    start_period: 10s
```

Use valid Compose syntax.

Do not rely only on container startup order.

---

# 10. API Service

Build the API from the root Dockerfile.

Configure:

```text
SPRING_PROFILES_ACTIVE=docker
DB_HOST=postgres
DB_PORT=5432
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_ACCESS_TOKEN_EXPIRATION
JWT_REFRESH_TOKEN_EXPIRATION
SPRINGDOC_API_DOCS_ENABLED
SPRINGDOC_SWAGGER_UI_ENABLED
```

Use the property names expected by the current application.

Do not invent new names when existing configuration properties already exist.

Map the application port:

```text
8080:8080
```

Allow the host port to be overridden:

```text
${API_PORT:-8080}:8080
```

Use:

```yaml
depends_on:
    postgres:
        condition: service_healthy
```

Add a restart policy appropriate for local development, such as:

```text
unless-stopped
```

Avoid infinite restart loops that hide configuration errors.

---

# 11. API Health Check

Prefer Spring Boot Actuator:

```text
/actuator/health
```

Ensure the health endpoint is available to Docker without exposing all Actuator endpoints publicly.

Add or verify:

```yaml
management:
    endpoints:
        web:
            exposure:
                include: health,info
```

If Spring Security protects Actuator, permit only:

```text
/actuator/health
```

or use another safe container health-check strategy.

The Compose health check may use `wget`, `curl`, or Java-based checks only if the selected image contains the required tool.

Do not assume `curl` exists in the runtime image.

If the runtime image lacks an HTTP client, either:

- Install a minimal client intentionally, or
- Use a Java-compatible health-check approach, or
- Omit the API Compose health check and clearly document the reason.

PostgreSQL health checking is mandatory.

---

# 12. pgAdmin Service

Use a pinned pgAdmin image version.

Configure:

```text
PGADMIN_DEFAULT_EMAIL
PGADMIN_DEFAULT_PASSWORD
```

Use development defaults through environment interpolation.

Suggested host port:

```text
5050:80
```

Allow override:

```text
${PGADMIN_PORT:-5050}:80
```

Use a named volume:

```text
pgadmin_data
```

Make pgAdmin depend on PostgreSQL where appropriate.

Do not store real production credentials in Compose.

---

# 13. Docker Spring Profile

Create `application-docker.yml` if it improves clarity.

It should configure the datasource using environment variables.

Example intent:

```yaml
spring:
    datasource:
        url: jdbc:postgresql://${DB_HOST:postgres}:${DB_PORT:5432}/${DB_NAME:pos}
        username: ${DB_USERNAME:postgres}
        password: ${DB_PASSWORD:postgres}

    jpa:
        hibernate:
            ddl-auto: validate

    flyway:
        enabled: true
```

Preserve:

```text
ddl-auto: validate
```

Flyway remains the schema source of truth.

Do not use:

```text
create
create-drop
update
```

Do not duplicate common configuration unnecessarily.

---

# 14. Environment File

Create:

```text
.env.example
```

Include documented placeholders and safe local defaults:

```dotenv
API_PORT=8080

POSTGRES_DB=pos
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
POSTGRES_PORT=5432

PGADMIN_DEFAULT_EMAIL=admin@example.com
PGADMIN_DEFAULT_PASSWORD=change-me
PGADMIN_PORT=5050

JWT_SECRET=replace-with-a-long-random-secret
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

SPRINGDOC_API_DOCS_ENABLED=true
SPRINGDOC_SWAGGER_UI_ENABLED=true
```

Use the exact JWT duration units expected by the application.

Do not guess units without inspecting the JWT configuration.

Add `.env` to `.gitignore`.

Keep `.env.example` tracked.

---

# 15. Compose Networking

Allow Compose to use its default project network unless a named network adds real value.

Inside Docker, the API must connect to PostgreSQL using:

```text
postgres
```

not:

```text
localhost
```

Explain in documentation:

- `localhost` inside the API container refers to the API container itself.
- The Compose service name `postgres` resolves to the database container.

Do not use hardcoded container IP addresses.

---

# 16. Flyway Behaviour

When the API container starts:

1. It connects to PostgreSQL.
2. Flyway inspects schema history.
3. Pending migrations run.
4. Hibernate validates the resulting schema.
5. The application starts.

Do not create a separate Flyway container unless there is a clearly documented architectural reason.

Do not edit already-applied migrations.

---

# 17. README Documentation

Add a Docker section covering:

## Prerequisites

- Docker Desktop or Docker Engine
- Docker Compose v2

## Setup

```bash
cp .env.example .env
```

Tell the developer to replace development secrets.

## Start

```bash
docker compose up --build
```

## Start in background

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

Warn clearly that `-v` deletes local database data.

## Rebuild API

```bash
docker compose build api
docker compose up -d api
```

## URLs

```text
API: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html
Health: http://localhost:8080/actuator/health
pgAdmin: http://localhost:5050
PostgreSQL: localhost:5432
```

Use configurable ports in the explanation.

---

# 18. pgAdmin Connection Guide

Document the connection fields:

```text
Host: postgres
Port: 5432
Database: value of POSTGRES_DB
Username: value of POSTGRES_USER
Password: value of POSTGRES_PASSWORD
```

When pgAdmin runs in Compose, its database host is `postgres`, not `localhost`.

---

# 19. Build and Configuration Tests

Run:

```bash
./mvnw clean verify
```

Then:

```bash
docker compose config
```

Expected:

- Valid rendered Compose configuration.
- No unresolved required variables.
- No syntax errors.

Build:

```bash
docker compose build --no-cache api
```

Expected:

- Maven build succeeds.
- Runtime image is created.
- Tests are not silently disabled.
- Final image does not contain Maven or source code.

Start:

```bash
docker compose up -d
```

Then:

```bash
docker compose ps
```

Expected:

- PostgreSQL is healthy.
- API is running or healthy if an API health check exists.
- pgAdmin is running.

---

# 20. Beginner-Friendly Manual QA Guide

Include these instructions in the Codex completion report.

## Step 1 — Confirm Docker works

Run:

```bash
docker --version
docker compose version
```

Expected: version information without errors.

## Step 2 — Create local environment file

Run:

```bash
cp .env.example .env
```

Open `.env` and replace placeholder secrets.

## Step 3 — Validate Compose

Run:

```bash
docker compose config
```

Expected: fully rendered YAML with no errors.

Do not paste the rendered output publicly because it may contain local secrets.

## Step 4 — Build and start

Run:

```bash
docker compose up --build
```

Watch the logs.

Expected sequence:

1. PostgreSQL starts.
2. PostgreSQL becomes healthy.
3. API starts.
4. Flyway applies migrations.
5. Spring Boot starts.
6. pgAdmin starts.

## Step 5 — Check containers

In another terminal:

```bash
docker compose ps
```

Expected:

```text
api       running
postgres  healthy
pgadmin   running
```

Exact names may differ.

## Step 6 — Test health

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

## Step 7 — Test Swagger

Open:

```text
http://localhost:8080/swagger-ui.html
```

Expected: Swagger UI loads.

## Step 8 — Test login

Use Bruno or Swagger:

```text
POST /api/v1/auth/login
```

Expected:

```text
200 OK
```

## Step 9 — Test protected endpoint

Use the JWT:

```text
GET /api/v1/users
Authorization: Bearer <token>
```

Expected according to role:

```text
200 OK
```

or:

```text
403 Forbidden
```

## Step 10 — Open pgAdmin

Open:

```text
http://localhost:5050
```

Sign in using `.env` credentials.

Register a server using host:

```text
postgres
```

Verify the `pos` database and Flyway tables exist.

## Step 11 — Verify persistence

Create a user.

Run:

```bash
docker compose restart api
```

Verify the user still exists.

Then run:

```bash
docker compose down
docker compose up -d
```

Verify the user still exists because PostgreSQL uses a named volume.

## Step 12 — Inspect logs

Run:

```bash
docker compose logs -f api
```

Verify:

- No database connection errors.
- No Flyway errors.
- No JWT secrets or passwords are logged.

## Step 13 — Stop safely

Run:

```bash
docker compose down
```

Data should remain.

## Step 14 — Volume deletion test

Only when intentionally resetting local data:

```bash
docker compose down -v
```

Warn that this deletes PostgreSQL and pgAdmin local data.

---

# 21. Failure Scenarios to Test

## Database unavailable

Stop PostgreSQL:

```bash
docker compose stop postgres
```

Observe API behaviour.

Expected:

- API reports database connectivity failure.
- No data corruption.
- Logs explain the connection issue.

Restart:

```bash
docker compose start postgres
```

Restart API if necessary.

## Incorrect database password

Temporarily set the wrong database password and restart.

Expected:

- API fails clearly.
- It does not silently connect elsewhere.
- Password is not printed in logs.

Restore the correct value.

## Occupied port

If port 8080 or 5432 is already in use, Compose should report a port-binding error.

Document changing:

```dotenv
API_PORT=8081
POSTGRES_PORT=5433
```

## Missing JWT secret

Remove or invalidate the JWT secret.

Expected:

- Application fails safely during startup if the secret is mandatory.
- It must not fall back to a hardcoded production secret.

---

# 22. Image Inspection

After building, run:

```bash
docker images
```

Optionally inspect:

```bash
docker image history <api-image-name>
```

Verify:

- No `.env` was copied.
- No secret appears in image history.
- Runtime image layers do not contain source code unnecessarily.
- Runtime container runs as a non-root user.

Check the user:

```bash
docker compose exec api id
```

Expected: non-root UID.

---

# 23. Acceptance Criteria

- [ ] Multi-stage Dockerfile exists.
- [ ] Java 17 is used.
- [ ] Final container runs as non-root.
- [ ] `.dockerignore` exists.
- [ ] `.env.example` exists.
- [ ] `.env` is ignored.
- [ ] `compose.yml` is valid.
- [ ] PostgreSQL service uses a pinned version.
- [ ] PostgreSQL has a named volume.
- [ ] PostgreSQL has a health check.
- [ ] API waits for healthy PostgreSQL.
- [ ] pgAdmin uses a pinned version.
- [ ] pgAdmin has a named volume.
- [ ] API uses the Docker profile.
- [ ] Database configuration uses environment variables.
- [ ] Flyway remains enabled.
- [ ] Hibernate remains on `validate`.
- [ ] Swagger works inside Docker.
- [ ] Actuator health works.
- [ ] JWT authentication works inside Docker.
- [ ] Data survives normal container restarts.
- [ ] `docker compose config` succeeds.
- [ ] `docker compose build` succeeds.
- [ ] `docker compose up` succeeds.
- [ ] `./mvnw clean verify` succeeds.
- [ ] README is updated.
- [ ] Beginner QA instructions are included.

---

# 24. Definition of Done

Before completing:

1. Run:

```bash
./mvnw clean verify
```

2. Run:

```bash
docker compose config
```

3. Run:

```bash
docker compose build --no-cache api
```

4. Run:

```bash
docker compose up -d
```

5. Run:

```bash
docker compose ps
```

6. Verify health, Swagger, login, JWT, and PostgreSQL persistence.

7. Run:

```bash
docker compose down
```

Provide a completion report containing:

- Files created
- Files modified
- Docker images selected
- Environment variables introduced
- Ports used
- Volumes created
- Health-check design
- Commands executed
- Automated test results
- Manual QA results
- Known limitations

Do not add Redis unless the project already uses it.

Do not implement Branch Management.

Do not modify unrelated domain code.
