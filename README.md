# POS SaaS

Inventory and Point of Sale SaaS backend built with Spring Boot, PostgreSQL, Flyway, Spring Security, and OpenAPI.

## Tech Stack

- Java 17
- Spring Boot 4.x
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Spring Boot Actuator
- OpenAPI / Swagger UI
- Maven Wrapper

## Running Locally

The non-Docker development profile uses the local PostgreSQL instance documented in `src/main/resources/application-dev.yml`.

Current local defaults are:

- API port: `8080`
- Database name: `pos`
- Database username: `postgres`
- Database password: `password`

## Environment Variables

The Docker stack reads its values from `.env`.

Create it from the example file:

```bash
cp .env.example .env
```

Then replace the placeholder secrets before starting the stack.
Keep the `POSTGRES_*` and `DB_*` values aligned in `.env` so the API and the PostgreSQL container use the same database settings.

## First-Run Admin Bootstrap

The application can create exactly one initial `SUPER_ADMIN` account when the database is empty and bootstrap is explicitly enabled.

Use these environment variables:

- `BOOTSTRAP_ADMIN_ENABLED`
- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_PASSWORD`

Example for a first deployment:

```env
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=TemporaryStrongPassword123!
```

Flyway seeds the role catalog, but it does not create user credentials.

After the first successful deployment:

1. Verify the account exists by logging in with `/api/v1/auth/login`.
2. Confirm the response includes `mustChangePassword=true` if you want the user to change the temporary password.
3. Set `BOOTSTRAP_ADMIN_ENABLED=false` for the next startup.

If the email or password is invalid, fix the environment variables and restart the application. Never commit real passwords to Git.

## Database

Flyway manages the schema.

The initial local database connection for the non-Docker dev profile is:

- Host: `localhost`
- Port: `5432`
- Database: `pos`
- Username: `postgres`
- Password: `password`

## Docker

See [docs/docker/local-development.md](docs/docker/local-development.md) for the full setup and QA guide.

Quick start:

```bash
docker compose up --build
```

Useful URLs:

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- pgAdmin: `http://localhost:5050`
- PostgreSQL: `localhost:5432`

## Folder Structure

The project follows package-by-feature under `src/main/java/com/devrick/pos`.
