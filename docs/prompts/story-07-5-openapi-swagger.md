# Story 7.5 — OpenAPI and Swagger Documentation

## Instructions for Codex

Read the repository root `AGENTS.md` first.

Then inspect the existing project before making changes, especially:

- `pom.xml`
- `SecurityConfig`
- Authentication controllers and DTOs
- User controllers and DTOs
- `GlobalExceptionHandler`
- `ErrorResponse`
- `application.yml`
- `application-dev.yml`
- Existing tests

Implement this story as a focused change. Do not modify unrelated business logic.

---

# 1. Context

This repository is an enterprise Inventory and Point of Sale SaaS backend built with:

- Java 17
- Spring Boot 4.x
- Spring MVC
- Spring Security
- JWT authentication
- PostgreSQL
- Flyway
- Maven
- MapStruct
- JUnit 5
- Mockito

The application already includes:

- User Management APIs
- JWT login
- Access tokens
- Refresh tokens
- Logout
- Role-based authorization
- Consistent API error responses

This story adds interactive API documentation using OpenAPI 3 and Swagger UI.

---

# 2. Objective

Implement production-quality OpenAPI documentation for the existing REST API.

The completed implementation must:

- Generate an OpenAPI 3 specification automatically.
- Provide Swagger UI.
- Document User Management endpoints.
- Document Authentication endpoints.
- Describe request and response DTOs.
- Document success and error status codes.
- Support JWT Bearer authentication inside Swagger UI.
- Keep login and refresh endpoints publicly accessible.
- Allow Swagger resources through Spring Security.
- Avoid exposing passwords, JWT secrets, or internal implementation details.
- Include automated tests and beginner-friendly QA instructions.

---

# 3. Dependency

Add the Spring Boot 4-compatible Springdoc dependency to `pom.xml`.

Use:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.3</version>
</dependency>
```

Before adding it, inspect the existing dependency tree and confirm the dependency does not already exist.

Do not add Springfox.

Do not add multiple OpenAPI libraries.

---

# 4. Files to Create

Create the following file unless an equivalent configuration already exists:

```text
src/main/java/com/devrick/pos/config/openapi/OpenApiConfig.java
```

Create tests in an appropriate package, for example:

```text
src/test/java/com/devrick/pos/config/openapi/OpenApiIntegrationTest.java
```

Update existing controller tests only where required.

---

# 5. Files to Modify

Modify only the files needed for documentation:

```text
pom.xml
src/main/java/com/devrick/pos/security/config/SecurityConfig.java
src/main/java/com/devrick/pos/security/controller/AuthenticationController.java
src/main/java/com/devrick/pos/user/controller/UserController.java
src/main/java/com/devrick/pos/security/dto/*
src/main/java/com/devrick/pos/user/dto/*
src/main/java/com/devrick/pos/exception/ErrorResponse.java
src/main/resources/application.yml
src/main/resources/application-dev.yml
```

The exact paths may differ. Reuse the repository’s current package structure.

Do not move classes merely to match this prompt.

---

# 6. OpenAPI Configuration

Create an `OpenApiConfig` class using Spring configuration.

The generated API documentation should define:

- Title: `Inventory & POS SaaS API`
- Version: `v1`
- Description explaining that this is a secured Inventory and POS REST API.
- Contact or license information only if real project information is available.
- JWT Bearer security scheme.
- Bearer format: `JWT`
- Authentication header: `Authorization`

Use an OpenAPI security scheme named:

```text
bearerAuth
```

The configuration should be equivalent in intent to:

```java
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI posOpenApi() {
        // Configure API metadata and bearerAuth.
    }
}
```

Do not hardcode example JWT values.

Do not expose the JWT signing secret.

---

# 7. Swagger Paths

Configure or preserve these conventional paths:

```text
/swagger-ui.html
/v3/api-docs
/v3/api-docs.yaml
```

A redirect to `/swagger-ui/index.html` is acceptable.

Swagger should be enabled for local development and testing.

For production, support disabling it using configuration such as:

```yaml
springdoc:
    api-docs:
        enabled: ${SPRINGDOC_API_DOCS_ENABLED:true}
    swagger-ui:
        enabled: ${SPRINGDOC_SWAGGER_UI_ENABLED:true}
```

In `application-prod.yml`, default these values to `false` unless the current deployment requirements explicitly require public production documentation.

Do not hardcode environment-specific behaviour in Java.

---

# 8. Security Configuration

Update `SecurityConfig` so these paths are publicly accessible:

```text
/swagger-ui.html
/swagger-ui/**
/v3/api-docs
/v3/api-docs/**
```

Keep existing public authentication endpoints accessible, such as:

```text
/api/v1/auth/login
/api/v1/auth/refresh
```

Everything else must retain its current security and authorization rules.

Do not use:

```java
.anyRequest().permitAll()
```

Do not weaken User Management authorization.

Do not disable JWT authentication.

A typical intent is:

```java
.requestMatchers(
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**")
.permitAll()
```

Adapt this to the existing Spring Boot 4 security configuration.

---

# 9. Controller Documentation

Document existing controllers using OpenAPI annotations from:

```text
io.swagger.v3.oas.annotations
```

Use annotations only when they add useful information.

Recommended annotations include:

- `@Tag`
- `@Operation`
- `@ApiResponse`
- `@ApiResponses`
- `@Parameter`
- `@SecurityRequirement`

Do not add excessive annotation noise.

---

# 10. Authentication Controller Documentation

Document the authentication controller with a tag such as:

```text
Authentication
```

Document these endpoints if they exist:

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET /api/v1/auth/me
```

## Login

Document:

- Purpose
- Request body
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- Example login request
- Example successful response

The login operation must not require `bearerAuth`.

## Refresh

Document:

- Purpose
- Refresh-token request body
- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`

The refresh operation must not require an access token unless the current implementation intentionally requires one.

## Logout

Document the exact current contract:

- Whether it uses the access token.
- Whether it accepts a refresh token.
- Whether it returns `200` or `204`.

Do not document behaviour that the code does not implement.

## Current user

Document:

- JWT requirement
- `200 OK`
- `401 Unauthorized`
- Safe response fields

Add:

```java
@SecurityRequirement(name = "bearerAuth")
```

where appropriate.

---

# 11. User Controller Documentation

Document the User controller with a tag such as:

```text
User Management
```

Document the current endpoints, such as:

```text
POST /api/v1/users
GET /api/v1/users
GET /api/v1/users/{id}
PUT /api/v1/users/{id}
PATCH /api/v1/users/{id}/disable
```

For each endpoint, document:

- Summary
- Description
- Required role or authorization expectation
- Request body where applicable
- Path parameters
- Pagination and sorting parameters
- Success response
- Validation failures
- Authentication failures
- Authorization failures
- Not-found errors
- Duplicate-email conflicts where applicable

All protected operations should include:

```java
@SecurityRequirement(name = "bearerAuth")
```

Do not claim a role is required unless the current security implementation actually enforces it.

---

# 12. DTO Schema Documentation

Use schema annotations where they provide meaningful examples or descriptions.

Examples:

```java
@Schema(description = "Employee email address", example = "cashier@example.com")
String email
```

Document role values using the existing enum.

Do not include a password example that resembles a real secret.

A safe password example is acceptable:

```text
Password123!
```

Mark password fields appropriately, for example:

```java
@Schema(format = "password", accessMode = WRITE_ONLY)
```

Ensure password fields never appear in response DTO schemas.

Document:

- Login request
- Login response
- Refresh request
- Refresh response
- Create user request
- Update user request
- User response
- Error response
- Validation error response if separate

Do not duplicate fields merely for documentation.

---

# 13. Error Documentation

Document the existing error response shape.

Example intent:

```json
{
    "timestamp": "2026-08-02T12:00:00Z",
    "status": 404,
    "error": "Not Found",
    "message": "User not found",
    "path": "/api/v1/users/{id}"
}
```

If validation errors include a field-error map, document it.

Common response codes:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
500 Internal Server Error
```

Do not expose stack traces or exception class names unnecessarily.

---

# 14. Pagination Documentation

For the users collection endpoint, document:

```text
page
size
sort
```

Include beginner-friendly descriptions:

- `page`: zero-based page number
- `size`: number of records per page
- `sort`: property and direction, for example `firstName,asc`

Preserve the current response type.

Do not replace a working paginated API solely for Swagger formatting.

---

# 15. Testing Requirements

## 15.1 Application context

Add or preserve a test confirming the application context loads with Springdoc enabled.

## 15.2 OpenAPI JSON endpoint

Add an integration or MVC test that verifies:

```text
GET /v3/api-docs
```

returns:

```text
200 OK
```

and JSON content.

Verify the generated document includes paths for:

```text
/api/v1/auth/login
/api/v1/users
```

Adapt this list to the actual endpoint mappings.

## 15.3 Swagger UI

Verify:

```text
GET /swagger-ui.html
```

returns a redirect or successful Swagger response.

A redirect to `/swagger-ui/index.html` is acceptable.

Verify `/swagger-ui/**` is not blocked by JWT security.

## 15.4 Security scheme

Verify the generated OpenAPI JSON includes:

```text
components.securitySchemes.bearerAuth
```

and that its type is HTTP Bearer authentication.

## 15.5 Existing security regression

Ensure tests still confirm:

- Protected API without token returns `401`.
- Protected API with valid token works.
- Cashier remains forbidden from Admin-only user-management operations.
- Login remains public.

Do not weaken tests to make Swagger pass.

---

# 16. Beginner-Friendly Manual QA Guide

After implementation, provide these steps in the Codex completion summary.

## Step 1 — Build

Run:

```bash
./mvnw clean verify
```

Expected:

```text
BUILD SUCCESS
```

## Step 2 — Start the application

Run:

```bash
./mvnw spring-boot:run
```

Expected:

- Application starts.
- No Springdoc bean errors.
- No security configuration errors.

## Step 3 — Open Swagger

Open:

```text
http://localhost:8080/swagger-ui.html
```

A redirect to:

```text
http://localhost:8080/swagger-ui/index.html
```

is acceptable.

Expected:

- Swagger UI loads.
- No login page appears.
- Authentication and User Management sections are visible.

## Step 4 — Check OpenAPI JSON

Open:

```text
http://localhost:8080/v3/api-docs
```

Expected:

- JSON is displayed.
- The API title is correct.
- Paths include authentication and user endpoints.
- `bearerAuth` exists.

## Step 5 — Test login through Swagger

1. Expand `POST /api/v1/auth/login`.
2. Click **Try it out**.
3. Enter valid credentials.
4. Click **Execute**.

Expected:

```text
200 OK
```

Copy the `accessToken`.

## Step 6 — Authorize Swagger

1. Click the **Authorize** button.
2. Paste the access token.

If Swagger automatically adds `Bearer`, paste only the token.

If the UI requests the full header value, use:

```text
Bearer <token>
```

Follow the generated Swagger UI behaviour.

## Step 7 — Test a protected endpoint

Call:

```text
GET /api/v1/users
```

Expected for an authorized Admin:

```text
200 OK
```

## Step 8 — Test without authorization

Click **Logout** in the Swagger authorization dialog, then call the protected endpoint again.

Expected:

```text
401 Unauthorized
```

## Step 9 — Confirm schemas

Inspect Schemas and verify:

- Password exists only in write/request schemas.
- Password does not appear in UserResponse.
- Role values are visible.
- ErrorResponse is documented.

---

# 17. Troubleshooting

## Swagger returns 401

Check that Swagger and API docs paths are permitted in `SecurityConfig`.

## Swagger UI is blank

Check browser console and `/v3/api-docs`.

## `/v3/api-docs` returns 500

Review annotation conflicts, unsupported schema types, and startup logs.

## Authorize button is missing

Confirm `bearerAuth` is defined in OpenAPI components.

## Token is sent but endpoint returns 401

Check whether Swagger expects only the raw token or the full `Bearer` value.

## Dependency conflict

Run:

```bash
./mvnw dependency:tree
```

Confirm only one Springdoc version is present.

---

# 18. Acceptance Criteria

- [ ] Springdoc dependency added once.
- [ ] Swagger UI loads.
- [ ] `/v3/api-docs` returns valid OpenAPI JSON.
- [ ] `/v3/api-docs.yaml` works.
- [ ] JWT Bearer security scheme appears.
- [ ] Login can be tested without a JWT.
- [ ] Protected endpoints support Swagger authorization.
- [ ] Existing security rules remain enforced.
- [ ] Authentication endpoints are documented.
- [ ] User endpoints are documented.
- [ ] DTO schemas are understandable.
- [ ] Passwords are write-only and never in responses.
- [ ] Standard errors are documented.
- [ ] Pagination parameters are documented.
- [ ] Automated tests pass.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] `./mvnw clean verify` succeeds.

---

# 19. Definition of Done

Before finishing:

1. Run:

```bash
./mvnw spotless:apply
```

2. Run:

```bash
./mvnw clean verify
```

3. Confirm Swagger manually.

4. Provide a completion report containing:

- Files created
- Files modified
- Dependency added
- Documentation paths
- Security changes
- Tests added
- Commands executed
- Test results
- Beginner QA steps
- Known limitations

Do not continue into Docker work.

Do not refactor unrelated application code.
