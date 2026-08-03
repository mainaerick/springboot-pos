# Implement Story 7A — Tenant Foundation

Implement Story 7A — Tenant Foundation in the existing Spring Boot POS project.

Do not only analyze, explain, or propose code. Inspect the repository, implement the tenant foundation completely, add the required automated tests, run all project quality checks, and report the actual results.

This story is a prerequisite for Story 8 — Branch Management and all later tenant-owned business modules.

---

## 1. Mandatory First Actions

Before modifying any file:

1. Read the complete root `AGENTS.md`.
2. Inspect the entire existing project structure.
3. Inspect the current implementations of:
    - `BaseEntity`
    - JPA auditing
    - `User`
    - `UserRepository`
    - user creation and update flows
    - authentication
    - login
    - refresh-token handling
    - logout
    - JWT generation
    - JWT validation
    - `JwtAuthenticationFilter`
    - `CustomUserDetailsService`
    - authenticated principal or `UserDetails` implementation
    - role and authority handling
    - bootstrap or seeded administrator creation
    - Flyway migration numbering and SQL conventions
    - global exception handling
    - OpenAPI configuration
    - test infrastructure
    - Testcontainers configuration, if present

4. Search for any existing tenant-like concepts, including:
    - tenant
    - organization
    - company
    - business
    - account
    - workspace

5. Confirm that no existing ownership abstraction should be reused.
6. Inspect all existing tests that may be affected by adding mandatory tenant ownership to users.

Follow the established project architecture and coding standards.

`AGENTS.md` is authoritative. If this prompt conflicts with `AGENTS.md`, follow `AGENTS.md` and explain the conflict in the final report.

---

## 2. Story Objective

Introduce the minimum secure multi-tenant foundation required for later business modules.

After this story:

- Every normal application user belongs to exactly one tenant.
- Tenant ownership is resolved from trusted server-side authentication state.
- Services can reliably obtain the authenticated tenant ID.
- JWT authentication carries or resolves trusted tenant information.
- Existing login, refresh-token, logout, user management, and authorization behavior remains functional.
- Future modules can scope entities and repository queries by tenant.
- Clients cannot choose or override their tenant through ordinary business API requests.

This story must not implement Branch Management or any later business module.

---

## 3. User Story

As the SaaS platform, I need every application user and future business record to be associated with a trusted tenant so that data belonging to different businesses remains isolated and secure.

---

## 4. Architectural Principles

The implementation must enforce these principles:

1. A tenant represents one subscribed business or customer account.
2. A tenant is not a branch.
3. A normal application user belongs to exactly one tenant.
4. Tenant ownership must be established by the server.
5. Tenant ownership must not be accepted from arbitrary business request bodies, query parameters, or client-controlled headers.
6. Authentication and tenant isolation are separate concerns:
    - authentication determines who the user is;
    - authorization determines what the user may do;
    - tenant isolation determines which business data the user may access.

7. Roles are tenant-local unless the existing project explicitly defines platform-level roles.
8. Future tenant-owned entities must be queryable using both resource ID and tenant ID.
9. Cross-tenant access must not be possible merely because two users share the same role.

---

## 5. Scope

### 5.1 Included

Implement:

- Tenant entity
- Tenant status model
- Tenant repository
- Flyway migration for the tenant table
- User-to-tenant association
- Flyway migration for `users.tenant_id`
- Safe migration of existing users
- Trusted current-tenant resolution
- Tenant-aware authenticated principal or equivalent security abstraction
- JWT tenant support where appropriate
- Login compatibility
- Refresh-token compatibility
- Logout compatibility
- User creation compatibility
- User retrieval compatibility
- Bootstrap or seed compatibility
- Tenant-aware test fixtures
- Automated unit tests
- Repository tests
- Authentication and security integration tests
- Migration verification
- Beginner-friendly manual QA instructions
- Documentation of the tenant access pattern for future modules

### 5.2 Excluded

Do not implement:

- Branch Management
- Customer Management
- Supplier Management
- Product Categories
- Products
- Inventory
- Purchases
- Sales
- Reports
- Tenant registration API
- Public tenant signup
- Tenant onboarding workflows
- Subscription plans
- Billing
- Tenant invitations
- Tenant switching
- Users belonging to multiple tenants
- Platform administrator impersonation
- Cross-tenant support access
- Tenant domains
- Tenant settings
- Tenant branding
- Per-tenant database schemas
- Separate databases per tenant
- Row-level security unless already established
- Frontend changes

Keep this story limited to the minimum secure tenant foundation.

---

## 6. Tenant Model

Create a tenant domain entity using the project’s package-by-feature conventions.

Use the name:

```text
Tenant
```

unless an equivalent existing domain term is discovered during repository inspection.

The entity must extend the existing `BaseEntity`.

### 6.1 Required fields

| Field        | Required | Rules                                            |
| ------------ | -------: | ------------------------------------------------ |
| `id`         |      Yes | UUID inherited from `BaseEntity`                 |
| `name`       |      Yes | Trimmed, maximum 150 characters                  |
| `code`       |      Yes | Stable, normalized uppercase business identifier |
| `status`     |      Yes | Tenant lifecycle status                          |
| audit fields |      Yes | Inherited from `BaseEntity`                      |

### 6.2 Tenant name

The tenant name represents the business name.

Examples:

```text
Acme Pharmacy Ltd
Sunrise Hardware
Maina Restaurant Group
```

Rules:

- Required
- Trimmed
- Must not contain only whitespace
- Maximum 150 characters
- Prefer case-insensitive uniqueness unless the existing architecture indicates that duplicate display names are acceptable

If duplicate tenant names are allowed, document that tenant code is the unique business identifier.

### 6.3 Tenant code

The tenant code must:

- Be required
- Be trimmed
- Be normalized to uppercase
- Be globally unique
- Have a minimum length of 2
- Have a maximum length of 50
- Start with a letter or digit
- Contain only uppercase letters, digits, hyphens, or underscores
- Be treated as a stable identifier

Recommended validation pattern:

```regex
^[A-Z0-9][A-Z0-9_-]*$
```

Examples:

```text
ACME
SUNRISE-HARDWARE
DEFAULT
TENANT_001
```

Tenant code should be immutable after creation unless the existing project has a clear business-code update convention.

### 6.4 Tenant status

Create a tenant status model consistent with project enum conventions.

Recommended values:

```text
ACTIVE
SUSPENDED
INACTIVE
```

Behavior:

- `ACTIVE`: users may authenticate and use the system.
- `SUSPENDED`: authentication or business access should be denied.
- `INACTIVE`: tenant is no longer operational and access should be denied.

For this story, do not implement full tenant lifecycle administration APIs.

The initial migrated tenant should be `ACTIVE`.

Do not add statuses without a defined behavior.

---

## 7. User-to-Tenant Association

Modify the existing `User` entity so every normal user belongs to exactly one tenant.

Use an association equivalent to:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

The exact mapping must follow existing JPA, Lombok, equality, and serialization conventions.

### Requirements

- A user must have one tenant.
- A tenant may have many users.
- Tenant loading should be lazy unless existing conventions require otherwise.
- Tenant ownership must not be exposed through entity serialization.
- User equality or hash-code behavior must not accidentally traverse the tenant relationship.
- Existing password, role, audit, active, and security behavior must remain intact.
- Deleting a tenant must not cascade-delete users automatically.
- Deleting a user must not delete the tenant.
- The foreign key must be mandatory after migration.

Do not implement many-to-many user-to-tenant membership.

---

## 8. Database Migrations

Create the next correctly numbered Flyway migration or migrations.

Do not edit migrations that have already been applied.

Use the project’s existing SQL style, UUID strategy, timestamp types, constraint naming, and audit-column conventions.

### 8.1 Tenant table migration

Create a `tenants` table or the equivalent table name required by existing conventions.

Include:

- UUID primary key
- name
- code
- status
- inherited audit-compatible columns
- non-null constraints
- globally unique tenant code
- appropriate indexes

Recommended constraints:

```text
UNIQUE (code)
```

Where suitable, enforce case-insensitive uniqueness using normalized uppercase persisted values or an approved PostgreSQL index strategy.

### 8.2 Existing-user migration

The current users table contains users with no tenant association.

The migration must safely preserve existing users.

Use a strategy equivalent to:

1. Create the tenant table.
2. Insert one initial tenant for the existing installation.
3. Add `tenant_id` to `users` as nullable.
4. Assign every existing user to the initial tenant.
5. Add the tenant foreign key.
6. Make `users.tenant_id` non-null.
7. Add an index on `users.tenant_id`.

The final schema must not leave normal users with nullable tenant ownership.

### 8.3 Initial tenant

Create one initial tenant for the current installation.

Recommended development values:

```text
Name: Default Business
Code: DEFAULT
Status: ACTIVE
```

However:

- Follow any established bootstrap configuration if one already exists.
- Avoid environment-specific production assumptions inside reusable SQL.
- Ensure migration SQL is deterministic.
- Do not generate a different tenant UUID every time if later migration statements need to reference it.
- Use the project’s approved UUID generation strategy.

Document why the initial tenant exists: it safely migrates pre-tenant users and preserves authentication after the schema change.

### 8.4 Migration safety

The migrations must:

- Work on a database containing existing users.
- Work on a clean database.
- Preserve existing user IDs.
- Preserve existing passwords and roles.
- Preserve existing audit data.
- Avoid creating duplicate default tenants.
- Be compatible with Flyway validation.
- Not rely on Hibernate schema generation.

If the project is still strictly development-only and database reset is officially permitted by `AGENTS.md`, still prefer a migration that demonstrates safe enterprise evolution unless there is a strong architectural reason not to.

---

## 9. Tenant Repository

Create:

```text
TenantRepository
```

using the project’s existing repository conventions.

Expected capabilities include equivalents of:

```java
Optional<Tenant> findByCodeIgnoreCase(String code);
```

```java
boolean existsByCodeIgnoreCase(String code);
```

Add only repository methods required by this story.

Do not create tenant CRUD APIs.

---

## 10. Tenant Access Abstraction

Create a stable abstraction that future services can use to obtain the current tenant.

Preferred conceptual interface:

```java
public interface CurrentTenantProvider {

    UUID getCurrentTenantId();

    Tenant getCurrentTenant();
}
```

The exact methods may vary to match project conventions.

Alternative names are acceptable, such as:

```text
CurrentTenantService
TenantContext
AuthenticatedTenantProvider
```

Choose one clear abstraction and use it consistently.

### Requirements

The provider must:

- Read from trusted Spring Security authentication state.
- Never read tenant ownership from arbitrary request bodies.
- Return the tenant associated with the authenticated user.
- Fail clearly when no authenticated user exists.
- Fail clearly when the authenticated principal has no tenant.
- Reject suspended or inactive tenants according to the selected security design.
- Be usable by later business services.
- Avoid exposing mutable global static state.
- Avoid unsafe `ThreadLocal` use unless it is populated and cleared by a carefully designed filter.

Prefer obtaining tenant data from the authenticated principal.

Do not query the database on every service call when the trusted authenticated principal already contains the tenant ID.

---

## 11. Authenticated Principal Design

Inspect the current `UserDetails` implementation.

Introduce or update the authenticated principal so it contains trusted identity information needed by application services.

Recommended fields:

```text
userId
tenantId
email or username
authorities
user active status
tenant status
```

A conceptual implementation may resemble:

```java
public record AuthenticatedUser(
        UUID userId,
        UUID tenantId,
        String email,
        String password,
        boolean enabled,
        Collection<? extends GrantedAuthority> authorities)
        implements UserDetails {
}
```

Do not copy this blindly. Follow the project’s Java style and Spring Security implementation.

### Requirements

- The principal must be constructed from persisted server-side user data.
- The tenant ID must come from the user-to-tenant association.
- The password must not be exposed outside the authentication process.
- Authorities must continue to work exactly as before.
- Existing `Authentication#getName()` behavior should remain compatible unless all usages are safely updated.
- The principal must not depend on a live Hibernate proxy after the transaction closes.
- Map required tenant and user values into immutable principal fields.

---

## 12. CustomUserDetailsService

Update `CustomUserDetailsService` so authentication loads the user and tenant information required to construct the authenticated principal.

Requirements:

- Continue resolving users using the existing login identifier, currently email unless repository inspection shows otherwise.
- Load or fetch the tenant association efficiently.
- Avoid lazy-loading failures after the repository transaction ends.
- Reject users whose tenant association is missing.
- Reject login when the tenant is suspended or inactive.
- Preserve existing user-enabled, locked, expired, and credential checks.
- Preserve roles and authorities.

Use an entity graph, fetch join, projection, or another project-consistent strategy if needed.

Do not solve lazy loading by globally changing unrelated relationships to eager loading.

---

## 13. JWT Changes

Inspect the existing JWT service and token structure.

Add trusted tenant support without breaking login, refresh, logout, expiration, signing, or validation.

### Recommended claims

Include trusted claims equivalent to:

```json
{
    "sub": "admin@example.com",
    "userId": "user-uuid",
    "tenantId": "tenant-uuid",
    "roles": ["ADMIN"]
}
```

Only include claims that match the existing JWT architecture.

### Requirements

- `tenantId` must be generated by the server from the authenticated principal.
- The client must never submit or override it.
- Access tokens must contain trusted tenant context or be resolvable securely from the authenticated principal.
- Refresh tokens must remain associated with the correct user and tenant.
- Token validation must continue to verify signature, expiration, subject, and existing security properties.
- Do not trust an unsigned or unvalidated tenant claim.
- Do not accept tenant ID from a request header as a replacement for trusted authentication.
- Existing tokens created before this change may become invalid in development; document this clearly if unavoidable.
- Do not weaken token security to preserve legacy tokens.

If the project intentionally reloads the user from the database on every request, tenant information may be derived from that trusted reload. Still include tenant ID in the principal made available to services.

---

## 14. JWT Authentication Filter

Update `JwtAuthenticationFilter` as required.

It must continue to:

1. Extract the bearer token.
2. Validate the token.
3. Resolve the authenticated user.
4. Construct trusted authentication.
5. Populate the `SecurityContext`.

Tenant-related requirements:

- The resulting principal must contain the trusted tenant ID.
- The JWT tenant claim, if present, must not be accepted independently of the authenticated user.
- If both persisted tenant ID and JWT tenant ID are available, verify they match.
- A mismatch must invalidate authentication.
- Suspended or inactive tenants must not receive authenticated access.
- No tenant information should be taken from a client-controlled ordinary header.

Avoid placing mutable tenant state in static fields.

---

## 15. Login Flow

Ensure login continues to work.

Expected flow:

```text
Email and password
    ↓
AuthenticationManager
    ↓
CustomUserDetailsService
    ↓
User loaded with Tenant
    ↓
Authenticated principal created
    ↓
JWT created with trusted user and tenant information
    ↓
Access and refresh tokens returned
```

Requirements:

- Active user in active tenant can log in.
- Invalid credentials remain rejected.
- Disabled users remain rejected.
- Users in suspended tenants are rejected.
- Users in inactive tenants are rejected.
- Tokens contain or resolve trusted tenant context.
- Login response format remains backward compatible unless a documented change is required.
- Do not expose sensitive tenant internals.

A public tenant-selection field must not be added to login.

---

## 16. Refresh Token Flow

Inspect the existing refresh-token design.

Ensure refresh remains tenant-safe.

Requirements:

- A refresh token must remain associated with its original user.
- The refreshed access token must use the user’s current persisted tenant.
- A user must not obtain a token for another tenant.
- A suspended or inactive tenant must not receive refreshed access.
- Revoked or expired refresh tokens remain rejected.
- Existing logout or revocation behavior remains functional.
- If the JWT refresh token contains `tenantId`, verify it against persisted trusted data.
- Do not accept tenant choice in the refresh request.

---

## 17. Logout Flow

Ensure logout remains functional.

Requirements:

- Logout continues invalidating or revoking tokens according to the existing implementation.
- Tenant changes must not bypass revocation.
- No tenant-specific logout endpoint is required.
- Existing logout tests must remain green.

---

## 18. User Management Changes

Update existing user creation and update flows carefully.

### 18.1 Existing user APIs

Inspect who can currently create users and how users are created.

Normal user-management requests must not allow arbitrary tenant assignment.

For tenant-local administrators:

- New users must automatically inherit the authenticated administrator’s tenant.
- Tenant ID must not be accepted from the request DTO.
- A Tenant A administrator must not create a user in Tenant B.
- User lists and lookups should become tenant-scoped if they expose tenant-owned users.

This is essential: adding `tenant_id` to the entity without tenant-scoping existing User Management APIs would leave a cross-tenant security vulnerability.

### 18.2 User repository

Add tenant-aware methods where required, such as equivalents of:

```java
Optional<User> findByIdAndTenantId(UUID userId, UUID tenantId);
```

```java
Page<User> findAllByTenantId(UUID tenantId, Pageable pageable);
```

Existing authentication lookup by globally unique email may remain global if email is globally unique.

If email uniqueness is currently global, do not silently change it to tenant-scoped uniqueness in this story without evaluating login ambiguity.

### 18.3 User service

Update user service operations so:

- Tenant-local users only see users in their tenant.
- Tenant-local administrators create users in their own tenant.
- Tenant-local administrators cannot update or delete users from another tenant.
- Cross-tenant user IDs return the project’s secure not-found behavior.
- Existing role checks remain enforced.

Do not create platform-wide tenant administration.

### 18.4 Request and response DTOs

Do not add freely client-controlled `tenantId` to normal User Management request DTOs.

Whether tenant information appears in user responses should follow existing API exposure rules.

Prefer not to expose it unless needed.

---

## 19. Bootstrap and Seed Behavior

Inspect whether the project creates an initial administrator through:

- Flyway data
- `CommandLineRunner`
- `ApplicationRunner`
- test fixture
- environment configuration
- manual SQL
- startup initializer

Update the bootstrap process so tenant creation precedes user creation.

Expected order:

```text
Create or locate initial tenant
    ↓
Create initial administrator
    ↓
Associate administrator with initial tenant
```

Requirements:

- Bootstrap must be idempotent.
- Repeated application starts must not duplicate the tenant.
- Repeated application starts must not duplicate the administrator.
- The administrator must never be created without a tenant.
- Bootstrap secrets must not be committed.
- Environment-specific values must use the existing configuration pattern.
- The initial tenant must be active.

If bootstrap is handled completely by Flyway, adapt the migration consistently rather than introducing an unnecessary second bootstrap system.

---

## 20. Tenant Status Enforcement

Choose one clear enforcement point consistent with the existing security architecture.

Preferred behavior:

- `CustomUserDetailsService` rejects authentication when tenant status is not `ACTIVE`.
- Refresh-token issuance also checks current tenant status.
- Current-tenant resolution fails if an authenticated principal somehow refers to an inactive tenant.

Use a suitable Spring Security exception or project exception.

Do not return detailed authentication messages that reveal unnecessary account state to unauthenticated users.

Tests may verify internal cause while public responses remain generic.

---

## 21. Future Tenant-Owned Entity Pattern

Add concise developer documentation in an appropriate existing documentation location, or update `AGENTS.md` only if project policy allows it.

Document the required future pattern:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

Tenant-owned repositories should use methods equivalent to:

```java
Optional<Entity> findByIdAndTenantId(UUID entityId, UUID tenantId);
```

Services should resolve tenant identity through the trusted tenant provider.

Controllers must not accept tenant ownership for normal tenant-local operations.

Do not create generic tenant-owned base entities unless there is a demonstrated need and `AGENTS.md` permits it. Prefer composition through an explicit tenant relationship.

---

## 22. API Behavior and Error Handling

Reuse the existing global exception format.

Do not create tenant-specific error response structures.

Expected behavior:

| Situation                                   | Expected result                                                            |
| ------------------------------------------- | -------------------------------------------------------------------------- |
| Missing token                               | `401 Unauthorized`                                                         |
| Invalid token                               | `401 Unauthorized`                                                         |
| User has no tenant                          | authentication or server security failure according to project conventions |
| Tenant suspended                            | login and refresh denied                                                   |
| Tenant inactive                             | login and refresh denied                                                   |
| Cross-tenant user ID                        | `404 Not Found`                                                            |
| Tenant-local admin creates user             | user inherits current tenant                                               |
| Tenant-local admin attempts tenant override | field absent, ignored, or rejected                                         |
| Valid active tenant user                    | authentication succeeds                                                    |

Do not reveal whether another tenant or user exists through cross-tenant errors.

---

## 23. OpenAPI

Update OpenAPI documentation only where existing public contracts change.

Potentially affected endpoints include:

- login
- refresh token
- User Management create
- User Management list
- User Management get
- User Management update
- User Management delete or status operations

Requirements:

- Do not expose tenant ID as a client-selectable login field.
- Do not expose tenant ID as a normal user-create field.
- Document tenant-local behavior where relevant.
- Preserve JWT bearer documentation.
- Keep examples compatible with the final DTOs.

No Tenant CRUD API documentation is needed because this story does not expose Tenant Management endpoints.

---

## 24. Automated Test Requirements

All tests must follow `AGENTS.md` and existing testing conventions.

Do not disable security or tenant checks to simplify tests.

### 24.1 Tenant entity and repository tests

Cover at minimum:

1. Persists a valid tenant.
2. Tenant code is required.
3. Tenant code is globally unique.
4. Tenant code normalization is enforced by the application.
5. Tenant status is required.
6. Finds tenant by code case-insensitively.
7. Audit fields are populated.
8. Duplicate tenant code fails at the database level.
9. Tenant name constraints are enforced.
10. Default tenant migration data exists.

### 24.2 User repository tests

Cover at minimum:

1. Persists a user with a tenant.
2. Rejects a user without a tenant.
3. Finds a user by ID and tenant ID.
4. Does not find a user using another tenant ID.
5. Lists users only for the requested tenant.
6. Existing authentication lookup by email still works.
7. Tenant association is loaded correctly for authentication.
8. Deleting a user does not delete the tenant.
9. Multiple users may belong to one tenant.
10. Users in different tenants remain isolated.

### 24.3 Current tenant provider unit tests

Cover at minimum:

1. Returns tenant ID from authenticated principal.
2. Returns tenant information from authenticated principal or trusted lookup.
3. Rejects unauthenticated access.
4. Rejects anonymous authentication.
5. Rejects a principal with no tenant.
6. Does not read tenant ID from request input.
7. Handles an inactive tenant according to the selected design.

### 24.4 CustomUserDetailsService tests

Cover at minimum:

1. Loads an active user in an active tenant.
2. Builds a principal containing user ID.
3. Builds a principal containing tenant ID.
4. Preserves roles and authorities.
5. Rejects unknown email.
6. Rejects a user without tenant ownership.
7. Rejects a suspended tenant.
8. Rejects an inactive tenant.
9. Avoids lazy-loading failure when principal data is accessed.

### 24.5 JWT service tests

Cover at minimum:

1. Access token contains trusted subject.
2. Access token contains trusted user ID when implemented.
3. Access token contains trusted tenant ID.
4. Token tenant ID matches authenticated principal.
5. Token validation succeeds for valid data.
6. Token validation rejects tampering.
7. Token validation rejects expiration.
8. Tenant claim cannot be overridden by request input.
9. Refreshed access token retains the correct tenant.
10. Existing roles remain represented correctly.

### 24.6 JWT filter tests

Cover at minimum:

1. Valid token creates authenticated security context.
2. Principal contains tenant ID.
3. Invalid token does not authenticate.
4. Expired token does not authenticate.
5. JWT tenant mismatch with persisted user tenant is rejected.
6. Suspended tenant is rejected.
7. Inactive tenant is rejected.
8. Existing authorities remain available.
9. Missing bearer token preserves existing unauthenticated behavior.

### 24.7 Authentication integration tests

Cover at minimum:

1. Active user in active tenant can log in.
2. Returned access token contains or resolves tenant context.
3. Returned refresh token works.
4. Invalid credentials return the existing authentication error.
5. Suspended tenant cannot log in.
6. Inactive tenant cannot log in.
7. Disabled user remains unable to log in.
8. Refresh succeeds for active tenant.
9. Refresh fails for suspended tenant.
10. Refresh fails for inactive tenant.
11. Logout remains functional.
12. Revoked refresh token remains unusable.

### 24.8 User Management tenant-isolation tests

Cover every existing user-management endpoint applicable to the repository.

At minimum:

1. Tenant A administrator creates a user.
2. Created user automatically belongs to Tenant A.
3. Create request contains no arbitrary tenant ID.
4. Tenant A administrator lists only Tenant A users.
5. Tenant A administrator retrieves a Tenant A user.
6. Tenant A administrator cannot retrieve a Tenant B user.
7. Tenant A administrator cannot update a Tenant B user.
8. Tenant A administrator cannot delete or deactivate a Tenant B user.
9. Cross-tenant IDs return `404 Not Found`.
10. A Tenant B administrator may use the same role without gaining Tenant A access.
11. Existing role authorization remains enforced.
12. Unauthenticated requests remain rejected.

### 24.9 Migration tests

Verify the Flyway migrations against PostgreSQL.

Cover:

1. Migration succeeds on a clean database.
2. Tenant table is created.
3. Default tenant is inserted.
4. Users table receives `tenant_id`.
5. Existing users are assigned to the default tenant.
6. `tenant_id` becomes non-null.
7. Foreign key exists.
8. Tenant code unique constraint exists.
9. User tenant index exists.
10. Flyway validation succeeds.
11. Existing user IDs, emails, passwords, and roles are preserved.

Use PostgreSQL Testcontainers when available.

Do not use H2 for PostgreSQL-specific migration verification.

---

## 25. Acceptance Criteria

Story 7A is accepted only when all criteria below are satisfied.

### Tenant persistence

- [ ] Tenant entity exists.
- [ ] Tenant extends `BaseEntity`.
- [ ] Tenant uses UUID identifiers.
- [ ] Tenant name is required.
- [ ] Tenant code is required.
- [ ] Tenant code is normalized to uppercase.
- [ ] Tenant code is globally unique.
- [ ] Tenant status is persisted.
- [ ] Tenant audit fields work.
- [ ] Flyway creates the tenant table.

### User ownership

- [ ] Every normal user belongs to exactly one tenant.
- [ ] The user tenant foreign key is non-null.
- [ ] Existing users are safely migrated.
- [ ] Existing users retain IDs, credentials, roles, and audit data.
- [ ] User-to-tenant association follows JPA best practices.
- [ ] Tenant deletion does not cascade-delete users.
- [ ] User deletion does not delete tenants.

### Authentication

- [ ] Authenticated principal contains trusted tenant identity.
- [ ] `CustomUserDetailsService` loads tenant information.
- [ ] Active tenant users can log in.
- [ ] Suspended tenant users cannot log in.
- [ ] Inactive tenant users cannot log in.
- [ ] JWT access tokens contain or securely resolve tenant identity.
- [ ] Refresh tokens remain tenant-safe.
- [ ] Logout remains functional.
- [ ] Existing role authorization still works.

### Tenant context

- [ ] A reusable current-tenant abstraction exists.
- [ ] Services can obtain the current tenant ID.
- [ ] Tenant identity comes from trusted authentication state.
- [ ] Tenant identity does not come from request bodies.
- [ ] Tenant identity does not come from arbitrary client headers.
- [ ] Missing tenant state fails securely.

### User Management isolation

- [ ] New users inherit the authenticated administrator’s tenant.
- [ ] Normal user requests cannot select another tenant.
- [ ] User listing is tenant-scoped.
- [ ] User lookup is tenant-scoped.
- [ ] User updates are tenant-scoped.
- [ ] User deletion or deactivation is tenant-scoped.
- [ ] Tenant A cannot access Tenant B users.
- [ ] Cross-tenant user IDs do not reveal resource existence.

### Migrations

- [ ] Flyway migration succeeds on a clean database.
- [ ] Migration safely handles existing users.
- [ ] Default tenant is deterministic and active.
- [ ] `users.tenant_id` is non-null after migration.
- [ ] Required foreign keys exist.
- [ ] Required indexes exist.
- [ ] Flyway validation passes.
- [ ] Hibernate schema auto-generation is not used.

### Architecture

- [ ] Package-by-feature is followed.
- [ ] `AGENTS.md` is followed.
- [ ] No Tenant CRUD API is introduced.
- [ ] No client-controlled tenant switching is introduced.
- [ ] No unrelated business story is implemented.
- [ ] Existing global exception handling is reused.
- [ ] Existing response conventions are preserved.
- [ ] Existing security architecture is extended rather than replaced unnecessarily.

### Quality

- [ ] Tenant repository tests pass.
- [ ] User repository tests pass.
- [ ] Current-tenant provider tests pass.
- [ ] Authentication tests pass.
- [ ] JWT tests pass.
- [ ] Refresh-token tests pass.
- [ ] User tenant-isolation tests pass.
- [ ] Migration tests pass.
- [ ] Existing Sprint 1 tests remain green.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verification passes.

---

## 26. Beginner-Friendly Manual QA

After automated tests pass, provide and execute where practical the following manual QA workflow.

### Test 1 — Start from the migrated database

1. Start PostgreSQL and required Docker services.
2. Run Flyway migrations through application startup.
3. Open pgAdmin.
4. Inspect the `tenants` table.
5. Inspect the `users` table.

Expected:

- A default active tenant exists.
- Every existing user has a non-null `tenant_id`.
- Tenant foreign key and indexes exist.
- Existing user emails, password hashes, roles, and IDs remain unchanged.

### Test 2 — Existing administrator login

1. Start the Spring Boot application.
2. Open Swagger UI.
3. Log in using an existing administrator account.

Expected:

- Login succeeds.
- Access and refresh tokens are returned.
- Existing response shape remains compatible.

### Test 3 — Inspect JWT tenant claim

Decode the access token using a safe local JWT decoder or debugger.

Do not share production tokens publicly.

Expected:

- Subject is correct.
- User ID is correct if included.
- Tenant ID is present if included by the implementation.
- Roles remain correct.
- No password or sensitive tenant data is present.

### Test 4 — Access an authenticated endpoint

Use the access token in Swagger UI.

Call an existing protected endpoint.

Expected:

- Authentication succeeds.
- Existing authorization still works.

### Test 5 — Refresh the access token

Use the existing refresh endpoint.

Expected:

- Refresh succeeds.
- New access token belongs to the same user and tenant.
- Roles remain correct.

### Test 6 — Logout

Call the existing logout endpoint.

Expected:

- Logout succeeds.
- Revoked token behavior remains consistent with Sprint 1.

### Test 7 — Create a tenant-local user

Authenticate as an administrator.

Create a user through the existing User Management endpoint.

Expected:

- Request does not require a tenant ID.
- Created user automatically belongs to the administrator’s tenant.
- Password is hashed.
- Roles are saved correctly.

### Test 8 — Verify user tenant in PostgreSQL

Inspect the newly created user in pgAdmin.

Expected:

- `tenant_id` matches the authenticated administrator’s tenant.
- The client did not choose this value.

### Test 9 — Prepare a second tenant for isolation testing

Use SQL test fixtures, integration-test seed data, or an approved development-only setup mechanism to create:

```text
Tenant A
Tenant B
```

Create one administrator in each tenant.

Do not add a production Tenant CRUD API merely for this test.

### Test 10 — Tenant A user listing

Authenticate as Tenant A administrator.

Call the existing user-list endpoint.

Expected:

- Only Tenant A users appear.
- Tenant B users do not appear.

### Test 11 — Cross-tenant user retrieval

While authenticated as Tenant A administrator, request a Tenant B user UUID.

Expected:

- `404 Not Found`
- No Tenant B data is exposed.

### Test 12 — Cross-tenant update

Attempt to update a Tenant B user while authenticated as Tenant A.

Expected:

- `404 Not Found`
- Tenant B user remains unchanged.

### Test 13 — Cross-tenant delete or status change

Attempt any existing delete, deactivate, activate, or role-management operation against a Tenant B user.

Expected:

- `404 Not Found`
- Tenant B user remains unchanged.

### Test 14 — Same role, separate tenant

Confirm Tenant A admin and Tenant B admin both have the same administrator role.

Expected:

- Each administrator can manage their own tenant users.
- Neither can manage the other tenant’s users.
- Role equality does not bypass tenant isolation.

### Test 15 — Suspended tenant login

Set Tenant B status to:

```text
SUSPENDED
```

Attempt login as a Tenant B user.

Expected:

- Login is denied.
- Public error response does not expose unnecessary internal detail.

### Test 16 — Suspended tenant refresh

Using a previously issued Tenant B refresh token, attempt token refresh.

Expected:

- Refresh is denied.

### Test 17 — Reactivate tenant

Set Tenant B back to:

```text
ACTIVE
```

Attempt login again.

Expected:

- Login succeeds.

### Test 18 — Missing tenant protection

In an isolated test database, attempt to create or persist a user without a tenant.

Expected:

- Validation or database constraint rejects the user.
- No normal user can exist without tenant ownership.

### Test 19 — Run all quality checks

Run the project-standard commands, including equivalents of:

```bash
./mvnw test
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw verify
```

Use `mvnw.cmd` when required.

Expected:

- All tests pass.
- Spotless passes.
- Checkstyle passes.
- Maven verification passes.

---

## 27. Quality Checks

Run all project-standard quality commands.

At minimum, run the applicable equivalents of:

```bash
./mvnw test
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw verify
```

Do not merely state that these commands should pass.

Execute them and report:

- Each command
- Exit result
- Test counts where available
- Any failures discovered
- Fixes applied
- Final status

Do not suppress Spotless, Checkstyle, test, Flyway, or compiler failures without a documented and valid reason.

---

## 28. Definition of Done

Story 7A is complete only when:

- [ ] `AGENTS.md` was read and followed.
- [ ] Existing architecture was inspected before changes.
- [ ] Tenant entity is implemented.
- [ ] Tenant repository is implemented.
- [ ] Tenant status behavior is implemented.
- [ ] Tenant Flyway migration is implemented.
- [ ] Existing users are migrated safely.
- [ ] `User` has mandatory tenant ownership.
- [ ] User repository queries are tenant-aware where required.
- [ ] User services are tenant-scoped.
- [ ] User controllers remain free of tenant business logic.
- [ ] New users inherit the authenticated tenant.
- [ ] Client-controlled tenant assignment is impossible in normal user flows.
- [ ] Authenticated principal includes trusted tenant identity.
- [ ] Current-tenant provider exists.
- [ ] Login is tenant-aware.
- [ ] JWT generation is tenant-aware.
- [ ] JWT validation is tenant-safe.
- [ ] Refresh tokens are tenant-safe.
- [ ] Logout remains functional.
- [ ] Suspended tenants cannot authenticate.
- [ ] Inactive tenants cannot authenticate.
- [ ] Tenant-isolation tests pass.
- [ ] Migration tests pass.
- [ ] Existing Sprint 1 tests remain green.
- [ ] OpenAPI remains correct.
- [ ] Manual QA instructions are complete.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verification passes.
- [ ] No Branch Management code was implemented.
- [ ] No unrelated business module was implemented.
- [ ] Future tenant-owned entity conventions are documented.

---

## 29. Implementation Guardrails

Do not:

- Implement Story 8.
- Add Branch entities or endpoints.
- Implement Tenant CRUD APIs.
- Add public tenant registration.
- Add tenant selection to login.
- Accept tenant IDs from normal client request DTOs.
- Accept arbitrary tenant headers.
- Trust JWT tenant claims without validation.
- Make users optionally tenant-owned.
- Add multi-tenant membership.
- Add tenant switching.
- Add platform impersonation.
- Add billing or subscription plans.
- Add schema-per-tenant or database-per-tenant architecture.
- Put tenant logic directly in controllers.
- Use unrestricted user queries in tenant-local APIs.
- Disable security in tests.
- Change global email uniqueness without evaluating authentication impact.
- Replace the existing authentication architecture unnecessarily.
- Rewrite unrelated Sprint 1 modules.
- Edit previously applied Flyway migrations.
- Depend on Hibernate schema generation.
- Suppress test or formatting failures.
- Claim commands passed if they were not run.

Keep all modifications strictly necessary for the secure tenant foundation.

---

## 30. Expected Files

Create or update only files required by the discovered architecture.

Expected equivalents may include:

```text
src/main/java/com/devrick/pos/tenant/entity/Tenant.java
src/main/java/com/devrick/pos/tenant/entity/TenantStatus.java
src/main/java/com/devrick/pos/tenant/repository/TenantRepository.java
src/main/java/com/devrick/pos/tenant/security/CurrentTenantProvider.java
src/main/java/com/devrick/pos/tenant/security/SecurityCurrentTenantProvider.java
src/main/java/com/devrick/pos/security/model/AuthenticatedUser.java
src/main/java/com/devrick/pos/user/entity/User.java
src/main/java/com/devrick/pos/user/repository/UserRepository.java
src/main/java/com/devrick/pos/user/service/...
src/main/java/com/devrick/pos/security/service/CustomUserDetailsService.java
src/main/java/com/devrick/pos/security/service/JwtService.java
src/main/java/com/devrick/pos/security/filter/JwtAuthenticationFilter.java
src/main/java/com/devrick/pos/auth/...
src/main/resources/db/migration/V<next>__create_tenant_foundation.sql
src/test/java/com/devrick/pos/tenant/...
src/test/java/com/devrick/pos/security/...
src/test/java/com/devrick/pos/user/...
```

Do not force these exact paths if `AGENTS.md` or the existing package-by-feature structure uses different naming.

---

## 31. Final Response Requirements

After implementation, provide a structured final report.

### Architecture discovered

Report:

- Existing package-by-feature structure
- Existing `BaseEntity` and audit model
- Existing user creation flow
- Existing security principal
- Existing JWT claims
- Existing refresh-token model
- Existing bootstrap behavior
- Existing test strategy
- Existing migration conventions

### Tenant design implemented

Explain:

- Tenant entity fields
- Tenant status behavior
- Tenant code normalization
- User-to-tenant relationship
- Default tenant migration strategy
- Existing-user migration strategy
- Tenant loading strategy
- Current-tenant provider design
- Authenticated principal design

### Security changes

Explain:

- How tenant ID enters the authenticated principal
- Whether JWT includes tenant ID
- How JWT tenant mismatch is handled
- How suspended and inactive tenants are blocked
- How refresh tokens remain tenant-safe
- How user-management APIs became tenant-scoped

### Files changed

List every created or modified file.

For each file, briefly explain:

- Why it changed
- What responsibility it now has

### Database changes

Report:

- Flyway migration name
- Tables created
- Columns added
- Backfill behavior
- Foreign keys
- Unique constraints
- Indexes
- Default tenant values

### Automated tests

Summarize:

- Unit tests added
- Repository tests added
- Integration tests added
- Tenant-isolation tests added
- Migration tests added
- Existing tests affected

### Verification results

Report actual results for:

```text
Maven test
Spotless
Checkstyle
Maven verify
Flyway validation
```

Do not claim success for checks that were skipped or failed.

### Manual QA

Provide the complete beginner-friendly manual QA guide with example requests and expected results.

### Remaining concerns

Clearly report:

- Failed checks
- Architectural assumptions
- Compatibility changes
- Tokens that must be regenerated
- Migration limitations
- Requirements not completed
- Follow-up recommendations before Story 8

### Story 8 readiness

End by stating whether the project is now ready for Story 8 — Branch Management.

The project is ready only if:

- authenticated tenant resolution works;
- users are tenant-owned;
- user APIs are tenant-isolated;
- JWT and refresh flows are tenant-safe;
- all required tests and quality checks pass.
