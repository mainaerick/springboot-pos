# Implement Story 8 — Branch Management

Implement Story 8 — Branch Management in the existing Spring Boot POS project.

Do not only analyze, explain, or propose code. Inspect the repository, implement the story completely, add the required tests, run the quality checks, and report the final results.

## 1. Mandatory First Actions

Before modifying any file:

1. Read the complete root `AGENTS.md`.
2. Read `docs/prompts/story-08-branch-management.md`.
3. Inspect the existing project architecture and coding conventions.
4. Inspect the current implementation of:
    - `BaseEntity`
    - JPA auditing
    - User Management
    - authentication and JWT handling
    - RBAC and method security
    - global exception handling
    - DTO validation
    - MapStruct configuration
    - repository patterns
    - pagination response patterns
    - OpenAPI documentation
    - Flyway migration numbering
    - unit and integration test conventions

5. Identify how tenant ownership is currently represented and resolved from the authenticated user.

Follow the existing architecture instead of introducing a parallel pattern.

`AGENTS.md` is authoritative. If this prompt conflicts with `AGENTS.md`, follow `AGENTS.md` and document the conflict in the final summary.

---

## 2. Architectural Precondition

Every branch must belong to exactly one tenant-level owner.

Use the tenant concept already present in the project, such as:

- Tenant
- Organization
- Company
- Business
- Account

Do not introduce a second tenant abstraction.

The tenant identifier must come from trusted server-side authentication or tenant context.

Never accept tenant ownership through:

- `CreateBranchRequest`
- `UpdateBranchRequest`
- query parameters
- path variables
- arbitrary request headers controlled directly by the client

### Blocking condition

If the project does not currently contain a secure tenant model or authenticated tenant-context mechanism:

1. Stop before implementing an insecure global Branch model.
2. Do not invent a temporary tenant ID supplied by clients.
3. Do not silently treat the application as single-tenant.
4. Report the missing architectural dependency clearly.
5. Describe the minimum tenant foundation required before Story 8 can be implemented safely.

Do not implement a full tenant-management module as part of Story 8.

---

## 3. Implementation Scope

Implement only Branch Management.

The implementation must include:

- Branch persistence entity
- Tenant ownership
- Flyway migration
- Create branch endpoint
- Paginated branch-list endpoint
- Get branch by UUID endpoint
- Update branch endpoint
- Activate/deactivate branch endpoint
- Request DTOs
- Response DTO
- Jakarta Bean Validation
- MapStruct mappings
- Repository
- Service interface and implementation
- Tenant-aware queries
- RBAC authorization
- Search
- Active-status filtering
- Pagination
- Safe sorting
- Global exception integration
- OpenAPI documentation
- Unit tests
- Repository tests
- Controller or API integration tests
- Security tests
- Tenant-isolation tests

Do not implement:

- Customers
- Suppliers
- Product categories
- Products
- Inventory
- Purchases
- Sales
- Reports
- Warehouses
- Storage bins
- Inter-branch transfers
- Branch-user assignments
- Cash registers
- POS terminals
- Receipt settings
- Tax settings
- Payment integrations
- Branch-specific pricing
- Primary/default branch behavior
- Hard deletion, unless already mandated by the project architecture

---

## 4. Package Structure

Use the existing package-by-feature convention.

Create Branch Management files in the project’s established feature structure.

Expected responsibilities include equivalents of:

```text
branch/
├── controller/
├── domain/
├── dto/
├── mapper/
├── repository/
└── service/
```

Do not force these exact directories if the existing project uses a slightly different feature structure.

Match the User Management module and `AGENTS.md`.

---

## 5. Branch Entity

Create a `Branch` entity extending the existing `BaseEntity`.

Use the existing UUID, auditing, Lombok, JPA, and equality conventions.

Required fields:

| Field           | Required | Rules                                       |
| --------------- | -------: | ------------------------------------------- |
| Tenant owner    |      Yes | Trusted server-side relationship            |
| `name`          |      Yes | Trimmed, maximum 120 characters             |
| `code`          |      Yes | Normalized uppercase, 2–30 characters       |
| `email`         |       No | Valid email, maximum 254 characters         |
| `phone`         |       No | Trimmed, maximum 30 characters              |
| `addressLine1`  |       No | Maximum 200 characters                      |
| `addressLine2`  |       No | Maximum 200 characters                      |
| `city`          |       No | Maximum 100 characters                      |
| `stateOrCounty` |       No | Maximum 100 characters                      |
| `postalCode`    |       No | Maximum 30 characters                       |
| `countryCode`   |       No | Exactly two uppercase letters when supplied |
| `active`        |      Yes | Defaults to `true`                          |

### Branch code rules

The branch code must:

- Be required
- Be trimmed
- Be converted to uppercase before persistence
- Have a minimum length of 2
- Have a maximum length of 30
- Start with a letter or digit
- Contain only uppercase letters, digits, hyphens, or underscores
- Be unique within a tenant
- Be allowed in another tenant

Use this validation pattern or an equivalent:

```regex
^[A-Z0-9][A-Z0-9_-]*$
```

Example:

```text
Input:  nrb-cbd
Stored: NRB-CBD
```

### Branch name rules

The branch name must:

- Be required
- Be trimmed
- Not contain only whitespace
- Have a maximum length of 120
- Be unique within a tenant using a case-insensitive comparison
- Be allowed in another tenant

### Country code rules

When supplied, the country code must:

- Be trimmed
- Be converted to uppercase
- Contain exactly two alphabetic characters

Example:

```text
Input:  ke
Stored: KE
```

### Active status

New branches must default to active.

The create request must not allow clients to set the initial active status.

General branch update must not change the active status.

Status changes must use the dedicated status endpoint.

---

## 6. Database Migration

Create the next correctly numbered Flyway migration.

Do not edit an already-applied migration.

Create the branch table according to existing naming conventions.

The migration must include:

- UUID primary key compatible with `BaseEntity`
- Tenant foreign key
- Required columns
- Audit columns compatible with the existing entity hierarchy
- Non-null constraints
- Appropriate column lengths
- Default active value
- Tenant-scoped unique branch code
- Case-insensitive tenant-scoped branch name uniqueness where practical
- Required indexes

At minimum, enforce:

```text
UNIQUE (tenant_id, code)
```

For case-insensitive name uniqueness, use the PostgreSQL approach that best matches the existing project.

Possible approaches include:

- Unique functional index on tenant ID and `LOWER(name)`
- Existing normalized-column pattern
- Existing `citext` strategy

Do not introduce a PostgreSQL extension unless it is already approved or clearly justified.

Add useful indexes for:

- Tenant-scoped listing
- Tenant and active filtering
- Tenant and code lookup
- Case-insensitive name or search queries

Do not rely on Hibernate schema generation.

---

## 7. DTOs

Create DTOs following existing naming and record/class conventions.

Expected DTOs:

```text
CreateBranchRequest
UpdateBranchRequest
UpdateBranchStatusRequest
BranchResponse
```

Do not expose the JPA entity from controllers.

### CreateBranchRequest

May contain:

- name
- code
- email
- phone
- addressLine1
- addressLine2
- city
- stateOrCounty
- postalCode
- countryCode

Must not contain:

- ID
- tenant ID
- active status
- audit fields
- deletion fields

### UpdateBranchRequest

May update approved branch profile and address fields.

It must not update:

- ID
- tenant ownership
- active status
- audit fields
- deletion fields

Determine whether branch code is mutable by inspecting existing business-code conventions.

Prefer an immutable branch code when business identifiers are treated as stable elsewhere in the project.

If code updates are allowed, normalize and validate uniqueness.

Document the chosen behavior.

### UpdateBranchStatusRequest

Contains only:

```json
{
    "active": false
}
```

The active value must be required and non-null.

### BranchResponse

Return approved public fields, including:

- UUID
- name
- code
- contact fields
- address fields
- country code
- active status
- permitted audit fields according to existing response conventions

Do not expose internal tenant details unnecessarily.

---

## 8. MapStruct Mapper

Create a mapper using the existing MapStruct configuration.

Support:

- `CreateBranchRequest` to `Branch`
- `Branch` to `BranchResponse`
- Applying `UpdateBranchRequest` to an existing managed `Branch`

Request mapping must ignore server-controlled fields:

- ID
- tenant owner
- active status
- audit fields
- soft-delete fields

Update mapping must not overwrite:

- ID
- tenant owner
- active status
- audit fields

Use explicit null-handling consistent with full `PUT` semantics and existing project conventions.

Do not use reflection-based property-copy utilities.

---

## 9. Repository

Create `BranchRepository` using the existing repository base type.

All operations involving a specific branch must be tenant-aware.

Implement equivalents of:

```java
Optional<Branch> findByIdAndTenantId(UUID branchId, UUID tenantId);
```

```java
boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
```

```java
boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
```

Use association-based equivalents when tenant ownership is represented as an entity.

Update uniqueness checks must exclude the current branch.

The list query must support:

- Authenticated tenant scope
- Case-insensitive name search
- Case-insensitive code search
- Optional active-status filter
- Pagination
- Safe sorting

Use the simplest established project approach:

- derived repository queries
- JPA Specifications
- Criteria API
- an existing query abstraction

Do not load all branches into memory and then filter them.

Do not use unrestricted `findById` where a tenant-scoped query can be used.

---

## 10. Service Layer

Create the service interface and implementation according to existing conventions.

Expected operations are equivalent to:

```java
BranchResponse createBranch(CreateBranchRequest request);
```

```java
PageResponse<BranchResponse> getBranches(
        String search,
        Boolean active,
        Pageable pageable);
```

```java
BranchResponse getBranch(UUID branchId);
```

```java
BranchResponse updateBranch(
        UUID branchId,
        UpdateBranchRequest request);
```

```java
BranchResponse updateBranchStatus(
        UUID branchId,
        UpdateBranchStatusRequest request);
```

Use the existing pagination wrapper instead of introducing a duplicate.

### Service responsibilities

The service must handle:

- Tenant-context resolution
- Tenant isolation
- Normalization
- Duplicate validation
- Branch lookup
- Business validation
- Mapping
- Transaction boundaries
- Exceptions
- Status changes

Use:

```java
@Transactional
```

for create, update, and status changes.

Use:

```java
@Transactional(readOnly = true)
```

for list and get operations.

Follow existing annotation-placement conventions.

### Duplicate protection

Perform friendly application-level duplicate checks.

Also handle database unique-constraint violations caused by concurrent requests and translate them into the project’s standard `409 Conflict` response.

---

## 11. REST Controller

Create `BranchController`.

Base path:

```http
/api/v1/branches
```

Required endpoints:

```http
POST   /api/v1/branches
GET    /api/v1/branches
GET    /api/v1/branches/{branchId}
PUT    /api/v1/branches/{branchId}
PATCH  /api/v1/branches/{branchId}/status
```

The controller must:

- Use constructor injection
- Validate request DTOs
- Delegate business logic to the service
- Return correct status codes
- Apply existing authorization conventions
- Use existing response wrappers
- Include OpenAPI annotations

The controller must not:

- Access repositories directly
- Resolve tenant ownership from request input
- Normalize branch data
- Perform duplicate checks
- Mutate entities
- Catch exceptions already handled globally

### Create

```http
POST /api/v1/branches
```

Return:

```http
201 Created
```

Include a `Location` header if existing project conventions require it.

### List

```http
GET /api/v1/branches
```

Support:

```text
page
size
sort
search
active
```

Return the existing standard paginated response.

When no active filter is provided, include active and inactive branches unless existing project conventions explicitly specify otherwise.

### Get by ID

```http
GET /api/v1/branches/{branchId}
```

Return `404 Not Found` when:

- the branch does not exist, or
- the branch belongs to another tenant

Do not reveal cross-tenant resource existence.

### Update

```http
PUT /api/v1/branches/{branchId}
```

Update only permitted fields.

Do not change status through this endpoint.

### Status update

```http
PATCH /api/v1/branches/{branchId}/status
```

Support activation and deactivation.

Treat setting the existing status again as an idempotent success unless project conventions require otherwise.

Do not implement a hard-delete endpoint.

---

## 12. Security

All branch endpoints must require authentication.

Reuse existing roles, authorities, and method-security patterns.

Do not invent conflicting role names.

Apply the existing RBAC model to the following conceptual access:

| Operation     | Conceptual access                                |
| ------------- | ------------------------------------------------ |
| Create        | Tenant administrator or branch-create permission |
| List          | Authorized tenant user                           |
| Get           | Authorized tenant user                           |
| Update        | Tenant administrator or branch-update permission |
| Change status | Tenant administrator or branch-status permission |

Tenant isolation is mandatory in addition to role checks.

A user with a valid administrative role in Tenant A must not access Tenant B branches.

Cross-tenant get, update, and status operations must return not found without exposing the other tenant’s data.

Do not disable security in tests.

---

## 13. Exception Handling

Reuse the existing global exception response.

Do not create a Branch-specific error format.

Expected statuses:

| Situation                                  | Status |
| ------------------------------------------ | -----: |
| Invalid request                            |    400 |
| Invalid UUID                               |    400 |
| Missing or invalid authentication          |    401 |
| Insufficient authorization                 |    403 |
| Branch unavailable in authenticated tenant |    404 |
| Duplicate branch name                      |    409 |
| Duplicate branch code                      |    409 |
| Successful creation                        |    201 |
| Successful retrieval/update                |    200 |

Use existing exception classes where appropriate.

Add narrowly scoped exceptions only when required and consistent with the project architecture.

---

## 14. OpenAPI Documentation

Document all Branch endpoints using existing OpenAPI conventions.

Include:

- Endpoint summaries
- Endpoint descriptions
- JWT bearer-security requirement
- Authorization expectations
- Path parameters
- Query parameters
- Request schemas
- Example requests
- Successful responses
- Validation responses
- Authentication responses
- Authorization responses
- Not-found responses
- Conflict responses

Confirm that the endpoints appear correctly in Swagger UI.

---

## 15. Automated Tests

Add complete automated coverage following `AGENTS.md`.

### Service unit tests

Cover at minimum:

1. Creates a branch successfully.
2. Resolves tenant ownership from trusted context.
3. Does not accept tenant ownership from request data.
4. Trims the branch name.
5. Normalizes the branch code to uppercase.
6. Normalizes country code to uppercase.
7. Defaults a new branch to active.
8. Rejects duplicate code within the same tenant.
9. Rejects duplicate name within the same tenant.
10. Allows the same code in another tenant.
11. Returns a tenant-owned branch.
12. Returns not found for an unknown branch.
13. Returns not found for another tenant’s branch.
14. Updates editable fields.
15. Preserves tenant ownership during update.
16. Preserves active status during general update.
17. Rejects an update causing a duplicate name.
18. Rejects an update causing a duplicate code when code changes are supported.
19. Deactivates a branch.
20. Reactivates a branch.
21. Handles an idempotent status update.
22. Returns paginated results.
23. Applies search.
24. Applies active-status filtering.

Use Mockito for unit collaborators only.

Do not mock the class under test.

### Repository tests

Use the existing repository-test strategy.

Prefer PostgreSQL Testcontainers when already configured.

Cover at minimum:

1. Persists a valid branch.
2. Enforces tenant ownership.
3. Finds a branch using ID and tenant.
4. Does not find another tenant’s branch.
5. Detects duplicate code in one tenant.
6. Allows the same code in different tenants.
7. Detects case-insensitive duplicate branch names.
8. Lists only branches for the requested tenant.
9. Filters active branches.
10. Filters inactive branches.
11. Searches by name case-insensitively.
12. Searches by code case-insensitively.
13. Applies pagination.
14. Applies sorting.
15. Enforces required database constraints.
16. Enforces unique tenant/code constraints.

Do not introduce H2 if PostgreSQL-specific behavior matters.

### Controller or API integration tests

Cover at minimum:

1. Authorized user creates a branch.
2. Creation returns `201 Created`.
3. Invalid request returns `400 Bad Request`.
4. Missing name is rejected.
5. Invalid code is rejected.
6. Invalid email is rejected.
7. Invalid country code is rejected.
8. Duplicate name returns `409 Conflict`.
9. Duplicate code returns `409 Conflict`.
10. Unauthenticated request returns `401 Unauthorized`.
11. Unauthorized role returns `403 Forbidden`.
12. Authorized user lists branches.
13. List response is paginated.
14. Search is applied.
15. Active filtering is applied.
16. Authorized user retrieves a branch.
17. Unknown branch returns `404 Not Found`.
18. Cross-tenant branch retrieval returns `404 Not Found`.
19. Authorized administrator updates a branch.
20. Authorized administrator deactivates a branch.
21. Authorized administrator reactivates a branch.
22. Unauthorized user cannot change branch status.
23. Success responses use the project response structure.
24. Error responses use the global error structure.

Use the project’s existing JWT and security-test helpers.

### Mapper tests

Add mapper tests when required by existing conventions.

Verify:

- Create fields map correctly
- Entity fields map correctly to response
- Tenant ownership is ignored from requests
- ID and audit fields are preserved
- Update does not overwrite active status
- Update does not overwrite tenant ownership

---

## 16. Acceptance Criteria

The implementation is accepted only when:

### Domain and persistence

- [ ] `Branch` exists in the correct feature package.
- [ ] `Branch` extends `BaseEntity`.
- [ ] Every branch has tenant ownership.
- [ ] IDs use UUIDs.
- [ ] Name is required and normalized.
- [ ] Code is required and normalized.
- [ ] Country code is normalized.
- [ ] New branches default to active.
- [ ] Flyway creates the schema.
- [ ] Required database constraints exist.
- [ ] Code uniqueness is tenant-scoped.
- [ ] The same code is allowed in separate tenants.
- [ ] Name uniqueness is tenant-scoped and case-insensitive.
- [ ] Appropriate indexes exist.

### API

- [ ] Authorized administrators can create branches.
- [ ] Authorized users can list branches.
- [ ] Listing is paginated.
- [ ] Listing supports search.
- [ ] Listing supports active filtering.
- [ ] Listing supports safe sorting.
- [ ] Authorized users can retrieve tenant-owned branches.
- [ ] Authorized administrators can update branches.
- [ ] Authorized administrators can deactivate branches.
- [ ] Authorized administrators can reactivate branches.
- [ ] No hard-delete endpoint is exposed.

### Security

- [ ] Authentication is required.
- [ ] Existing RBAC is reused.
- [ ] Tenant ownership is resolved from trusted server context.
- [ ] Request DTOs do not accept tenant IDs.
- [ ] Repository queries are tenant-scoped.
- [ ] Tenant A cannot retrieve Tenant B branches.
- [ ] Tenant A cannot update Tenant B branches.
- [ ] Tenant A cannot change Tenant B branch status.
- [ ] Cross-tenant branch IDs do not reveal resource existence.

### Architecture

- [ ] Controllers contain no business logic.
- [ ] Services contain branch business rules.
- [ ] Repositories handle persistence.
- [ ] DTOs isolate the REST contract from entities.
- [ ] MapStruct is used correctly.
- [ ] Global exception handling is reused.
- [ ] Transactions are correctly applied.
- [ ] Package-by-feature is followed.
- [ ] `AGENTS.md` is followed.

### Quality

- [ ] Unit tests pass.
- [ ] Repository tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] Tenant-isolation tests pass.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verification passes.
- [ ] Swagger documentation is complete.
- [ ] Sprint 1 functionality remains working.

---

## 17. Manual QA

After automated tests pass, provide beginner-friendly manual QA steps using Swagger UI.

The QA guide must include:

1. Start required Docker services.
2. Start the Spring Boot application.
3. Open Swagger UI.
4. Authenticate with an administrator JWT.
5. Create a branch using lowercase code and country code.
6. Confirm normalized response values.
7. Attempt creation without a name.
8. Attempt creation with invalid codes.
9. Attempt duplicate code creation.
10. Attempt duplicate case-insensitive name creation.
11. Create a second valid branch.
12. Test pagination and sorting.
13. Search by name.
14. Search by code.
15. Retrieve by UUID.
16. Update contact and address details.
17. Confirm general update does not change active status.
18. Deactivate the branch.
19. Filter inactive branches.
20. Reactivate the branch.
21. Test unauthorized write access.
22. Test cross-tenant get access.
23. Test cross-tenant update access.
24. Test cross-tenant status access.
25. Verify the database record and constraints through pgAdmin.
26. Run all Maven quality commands.

Include example request bodies and expected HTTP responses.

---

## 18. Quality Checks

Run all project-standard commands.

At minimum, run the applicable equivalents of:

```bash
./mvnw test
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw verify
```

Use `mvnw.cmd` when required by the environment.

Do not merely state that checks should pass.

Execute them and report:

- command run
- result
- failing tests or violations
- fixes made

Do not suppress quality violations without a documented architectural reason.

---

## 19. Definition of Done

Story 8 is done only when:

- [ ] Repository architecture was inspected.
- [ ] `AGENTS.md` was followed.
- [ ] Tenant ownership is secure.
- [ ] No client-controlled tenant assignment exists.
- [ ] Flyway migration is complete.
- [ ] Entity is complete.
- [ ] DTOs are complete.
- [ ] Mapper is complete.
- [ ] Repository is complete.
- [ ] Service is complete.
- [ ] Controller is complete.
- [ ] Security is complete.
- [ ] Tenant isolation is complete.
- [ ] Validation is complete.
- [ ] Exception handling is integrated.
- [ ] Pagination works.
- [ ] Search works.
- [ ] Filtering works.
- [ ] Safe sorting works.
- [ ] Activation works.
- [ ] Deactivation works.
- [ ] OpenAPI documentation is complete.
- [ ] Unit tests are complete.
- [ ] Repository tests are complete.
- [ ] API integration tests are complete.
- [ ] Security tests are complete.
- [ ] Tenant-isolation tests are complete.
- [ ] Manual QA instructions are provided.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verification passes.
- [ ] Existing tests remain green.
- [ ] No unrelated story was implemented.

---

## 20. Implementation Guardrails

Do not:

- Implement another Sprint 2 story.
- Add inventory fields to Branch.
- Add branch-user assignments.
- Add a default or primary branch.
- Add cash-register functionality.
- Add POS-terminal functionality.
- Add hard deletion.
- Accept tenant IDs from clients.
- Expose entities directly.
- Put business logic in controllers.
- Access repositories from controllers.
- Disable authentication or authorization.
- Use unrestricted cross-tenant repository access.
- Depend on Hibernate schema auto-generation.
- Rewrite unrelated Sprint 1 code.
- Change existing conventions without justification.
- Suppress Spotless or Checkstyle rules to make the build pass.

Keep all changes strictly necessary for Story 8.

---

## 21. Final Response Requirements

After implementation, provide a structured summary containing:

### Architecture discovered

- Existing package conventions
- Existing tenant model
- Existing tenant-resolution mechanism
- Existing RBAC model
- Existing exception and API response patterns
- Existing test strategy

### Implementation completed

- Entity
- Migration
- DTOs
- Mapper
- Repository
- Service
- Controller
- Security
- OpenAPI
- Tests

### Files changed

List every created or modified file and briefly explain why it changed.

### Key decisions

Explain:

- Tenant isolation strategy
- Name uniqueness strategy
- Code normalization strategy
- Whether branch code is mutable
- Search implementation
- Pagination implementation
- Status-change behavior

### Verification results

Report the actual results for:

```text
Tests
Spotless
Checkstyle
Maven verify
```

### Manual QA

Provide the complete beginner-friendly manual QA guide.

### Remaining concerns

Report any:

- architectural blockers
- assumptions
- failed checks
- unimplemented requirements
- follow-up work

Do not claim success for checks that were not run or did not pass.
