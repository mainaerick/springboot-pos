Implement a secure, production-ready first-run admin bootstrap mechanism for this Spring Boot POS application.

## Goal

When the application starts with an empty database, it should create the first `SUPER_ADMIN` user only when explicitly enabled through configuration.

On subsequent startups, it must not create duplicate administrators.

## Requirements

### 1. Inspect the existing project first

Before changing code:

- Review the current user, role, authentication, security, Flyway, auditing, exception-handling, and configuration structure.
- Reuse existing entities, repositories, services, DTOs, enums, password encoders, naming conventions, and package structure.
- Do not create duplicate abstractions if equivalent functionality already exists.
- Preserve the project's feature-first and clean-architecture conventions.

### 2. Seed roles using Flyway

Create a new Flyway migration that safely inserts the required roles, including:

- `SUPER_ADMIN`
- `ADMIN`
- `MANAGER`
- `CASHIER`

Use the role naming format already used by the project. For example, use either `SUPER_ADMIN` or `ROLE_SUPER_ADMIN`, but do not mix conventions.

The migration must:

- be idempotent at the database-constraint level;
- use the project's existing UUID generation approach;
- not insert an admin user;
- not contain passwords or password hashes;
- preserve all existing migrations without modifying previously applied migration files.

Ensure the role name column has a unique constraint if one does not already exist.

### 3. Add typed bootstrap configuration

Create typed configuration properties using the prefix:

```text
app.bootstrap-admin
```

Support these properties:

```yaml
app:
    bootstrap-admin:
        enabled: ${BOOTSTRAP_ADMIN_ENABLED:false}
        email: ${BOOTSTRAP_ADMIN_EMAIL:}
        password: ${BOOTSTRAP_ADMIN_PASSWORD:}
```

Use a `@ConfigurationProperties` class or record consistent with the project's Spring Boot version.

Do not use scattered `@Value` fields.

### 4. Create the bootstrap initializer

Implement a Spring startup component using `ApplicationRunner` or `CommandLineRunner`.

The initializer must:

1. Exit immediately when bootstrap is disabled.
2. Check whether a user with the super-admin role already exists.
3. Exit without changes when a super admin already exists.
4. Validate that the configured email is present and valid.
5. Normalize the email by trimming it and converting it to lowercase.
6. Validate that the password is present and satisfies the project's password policy.
7. Require at least 12 characters if no central password policy currently exists.
8. Fail application startup with a clear exception when bootstrap is enabled but configuration is invalid.
9. Detect whether the configured email is already assigned to another non-super-admin user.
10. Fail safely instead of silently promoting that existing user.
11. Load the super-admin role from the database.
12. Create the user through the existing user/application service when possible.
13. Encode the password using the configured Spring Security `PasswordEncoder`.
14. Mark the user as active or enabled.
15. Set `mustChangePassword` to `true` if the project supports this field.
16. Assign only the required super-admin role unless the current role model requires otherwise.
17. Execute the operation within a transaction.
18. Never log the plaintext password.
19. Log only a safe success message containing the normalized email.
20. Remain safe under repeated application restarts.

Prefer calling an existing user creation service instead of constructing and saving the entity directly. If the existing service cannot support system-created users, extend it cleanly without weakening normal registration rules.

### 5. Handle concurrent startup safely

The solution must tolerate two application instances starting at the same time.

Use database-backed protection such as:

- unique constraints on email;
- unique role constraints;
- transactional creation;
- appropriate duplicate-key handling.

Do not rely only on an in-memory `exists` check.

If concurrent instances attempt creation, one may create the account and the other should detect the resulting constraint conflict and exit safely after confirming that a super admin now exists.

### 6. Password-change requirement

If the user entity does not currently support forced password changes, add a field such as:

```java
private boolean mustChangePassword;
```

Add the corresponding Flyway migration and update the authentication response or login flow so that the frontend can determine whether the user must change the temporary password.

Do not fully redesign authentication unless required. Keep the implementation focused.

### 7. Audit logging

If the project already has audit logging, record an event such as:

```text
BOOTSTRAP_ADMIN_CREATED
```

Include:

- user ID;
- normalized email;
- timestamp;
- source as `SYSTEM_BOOTSTRAP`.

Do not include the password or password hash.

If no audit framework exists yet, add a structured application log entry and leave a clear TODO rather than building an unrelated audit subsystem.

### 8. Repository queries

Add only the repository methods required by the existing entity model, such as equivalents of:

```java
boolean existsByEmailIgnoreCase(String email);

boolean existsByRolesName(String roleName);
```

Adapt query names to the actual relationships and naming conventions in the project.

Avoid inefficient loading of all users or roles into memory.

### 9. Environment configuration

Update local Docker Compose configuration so the application can receive:

```env
BOOTSTRAP_ADMIN_ENABLED
BOOTSTRAP_ADMIN_EMAIL
BOOTSTRAP_ADMIN_PASSWORD
```

Do not hardcode real credentials.

Update `.env.example` with safe placeholders:

```env
BOOTSTRAP_ADMIN_ENABLED=false
BOOTSTRAP_ADMIN_EMAIL=
BOOTSTRAP_ADMIN_PASSWORD=
```

Ensure real `.env` files remain ignored by Git.

Do not overwrite an existing `.env`.

### 10. Documentation

Add a concise section to the appropriate project documentation explaining:

- how first-admin bootstrap works;
- which environment variables are required;
- how to enable it for the first deployment;
- how to verify the admin was created;
- why it must be disabled after first setup;
- how to recover when credentials are invalid;
- that passwords must never be committed to Git;
- that Flyway seeds roles but not user credentials.

Include a local example but use fake credentials only.

### 11. Tests

Add automated tests covering at least:

1. Bootstrap disabled: no user is created.
2. Bootstrap enabled with an empty database: super admin is created.
3. Existing super admin: no duplicate user is created.
4. Missing email: startup bootstrap fails.
5. Missing password: startup bootstrap fails.
6. Weak password: startup bootstrap fails.
7. Existing user with the configured email but without the super-admin role: bootstrap fails safely.
8. Email is normalized before storage.
9. Password is encoded and not stored as plaintext.
10. Required role is assigned.
11. `mustChangePassword` is set when supported.
12. Repeated execution remains idempotent.
13. Concurrent or duplicate creation is handled safely where practical.

Use the project's existing testing conventions.

Prefer:

- JUnit 5;
- Mockito for focused unit tests;
- Spring Boot integration tests for configuration and persistence behavior;
- Testcontainers PostgreSQL if the project already uses it.

Do not replace PostgreSQL behavior with H2 when testing PostgreSQL-specific constraints or SQL.

### 12. Security constraints

Do not:

- hardcode credentials;
- put passwords in Flyway migrations;
- print credentials in logs;
- expose a public unauthenticated admin-creation endpoint;
- automatically recreate an admin when one is deleted unless bootstrap is explicitly enabled;
- silently promote an existing ordinary user;
- weaken the current password encoder;
- modify old Flyway migration files;
- store plaintext passwords;
- use command-line password arguments in documentation examples.

### 13. Expected behavior

With:

```env
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=TemporaryStrongPassword123!
```

and no existing super admin, startup should create exactly one enabled super-admin account with an encoded password.

On the next startup, the initializer should detect the existing super admin and perform no changes.

With:

```env
BOOTSTRAP_ADMIN_ENABLED=false
```

the initializer must perform no database writes.

### 14. Final verification

After implementation:

- run formatting;
- run static analysis if configured;
- run unit and integration tests;
- run Flyway migration validation;
- build the application;
- inspect the final diff for leaked credentials;
- confirm no existing behavior was unintentionally changed.

Provide a final summary containing:

1. Files created.
2. Files modified.
3. Database migrations added.
4. Configuration variables introduced.
5. Bootstrap lifecycle.
6. Tests added and their results.
7. Any assumptions made.
8. Any remaining security or architectural concerns.

Do not stop after generating a plan. Inspect the repository and implement the complete solution.
