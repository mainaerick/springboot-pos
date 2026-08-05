# Implement Story 8 — Branch Management

Implement Story 8 — Branch Management in the existing Spring Boot POS project.

Story 7A — Tenant Foundation has already been implemented. Build Branch Management on top of the completed tenant architecture.

Do not only analyze, explain, or propose code. Inspect the repository, implement the complete story, add the required automated tests, run all quality checks, and report the actual results.

---

## 1. Mandatory First Actions

Before modifying any file:

1. Read the complete root `AGENTS.md`.
2. Read the existing Story 8 specification under:

```text
docs/prompts/story-08-branch-management.md
```

3. Inspect the implementation completed for Story 7A, including:
    - `Tenant`
    - tenant status
    - tenant repository
    - User-to-Tenant association
    - authenticated principal
    - current-tenant provider or tenant-context abstraction
    - JWT tenant claim handling
    - login and refresh-token tenant validation
    - tenant-aware User Management repository and service patterns

4. Inspect the existing implementations of:
    - `BaseEntity`
    - JPA auditing
    - User Management
    - DTO conventions
    - MapStruct configuration
    - service interfaces and implementations
    - repository query conventions
    - pagination response types
    - global exception handling
    - RBAC and method security
    - OpenAPI documentation
    - Flyway migration numbering
    - Testcontainers and test infrastructure

5. Run the existing tests before implementation to confirm the repository starts from a passing state.
6. Reuse the exact tenant classes and terminology already introduced in Story 7A.

Do not introduce another tenant abstraction.

`AGENTS.md` is authoritative. If this prompt conflicts with `AGENTS.md`, follow `AGENTS.md` and document the conflict in the final report.

---

## 2. Story Objective

Implement secure tenant-owned Branch Management.

After this story:

- Each branch belongs to exactly one tenant.
- Tenant ownership is assigned from trusted authenticated context.
- Clients cannot submit or override tenant ownership.
- Authorized users can list and retrieve branches in their tenant.
- Authorized administrators can create, update, activate, and deactivate branches.
- One tenant cannot access or modify another tenant’s branches.
- Branches provide the foundation for Inventory, Purchases, Sales, Expenses, and Reports.

---

## 3. User Story

As an authorized tenant administrator, I want to create and manage business branches so that future operational records can be associated with the correct physical or operational location.

---

## 4. Business Context

A tenant represents the subscribed business account.

A branch represents one location belonging to that tenant.

Example:

```text
Tenant: Acme Pharmacy Ltd
├── Nairobi CBD Branch
├── Westlands Branch
└── Mombasa Branch
```

A branch must never exist globally without tenant ownership.

Later modules will use branches for:

- Inventory quantities
- Purchases
- Sales and POS transactions
- Expenses
- Reports
- Employee assignments
- Stock transfers
- Cash registers

Do not implement those modules in this story.

---

## 5. Scope

### Included

Implement:

- Branch entity
- Tenant-to-Branch relationship
- Flyway migration
- Create branch API
- Paginated branch-list API
- Get branch by UUID API
- Update branch API
- Activate/deactivate branch API
- Request and response DTOs
- Jakarta Bean Validation
- MapStruct mapper
- Branch repository
- Branch service
- Tenant-scoped repository queries
- Trusted current-tenant resolution
- RBAC authorization
- Search by branch name or code
- Active-status filtering
- Pagination
- Safe sorting
- Global exception integration
- OpenAPI documentation
- Unit tests
- Repository tests
- Controller or integration tests
- Security tests
- Tenant-isolation tests
- Beginner-friendly manual QA instructions

### Excluded

Do not implement:

- Customer Management
- Supplier Management
- Product Categories
- Product Management
- Inventory
- Purchases
- Sales
- Reports
- Warehouses or storage bins
- Inter-branch stock transfers
- User-to-branch assignments
- Branch access restrictions
- Cash registers
- POS terminals
- Receipt settings
- Tax settings
- Branch-specific pricing
- Opening hours
- Payment integrations
- M-Pesa configuration
- Primary/default branch rules
- Tenant CRUD
- Hard deletion

---

## 6. Package Structure

Use the existing package-by-feature structure.

Expected responsibilities may be organized as:

```text
branch/
├── controller/
├── dto/
├── entity/
├── mapper/
├── repository/
└── service/
```

Follow the exact naming conventions already used by User Management and Tenant Foundation.

Do not force these exact folders if the existing architecture differs.

---

## 7. Branch Entity

Create a `Branch` JPA entity extending the existing `BaseEntity`.

Use existing UUID, audit, equality, Lombok, and entity conventions.

### Required fields

| Field         | Required | Rules                               |
| ------------- | -------: | ----------------------------------- |
| tenant        |      Yes | Mandatory `ManyToOne` association   |
| name          |      Yes | Trimmed, maximum 120 characters     |
| code          |      Yes | Uppercase, 2–30 characters          |
| email         |       No | Valid email, maximum 254 characters |
| phone         |       No | Maximum 30 characters               |
| addressLine1  |       No | Maximum 200 characters              |
| addressLine2  |       No | Maximum 200 characters              |
| city          |       No | Maximum 100 characters              |
| stateOrCounty |       No | Maximum 100 characters              |
| postalCode    |       No | Maximum 30 characters               |
| countryCode   |       No | Two uppercase alphabetic characters |
| active        |      Yes | Defaults to `true`                  |

### Tenant relationship

Use the Tenant entity created in Story 7A.

Implement an association equivalent to:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

Follow existing package names and conventions.

Requirements:

- Every branch must belong to one tenant.
- One tenant may have multiple branches.
- Branch deletion must not delete the tenant.
- Tenant deletion must not automatically cascade-delete branches.
- Tenant ownership must not be mutable through request DTOs.
- Tenant must not be traversed in `equals`, `hashCode`, or `toString`.
- Do not expose Hibernate proxy internals through API serialization.

---

## 8. Branch Name Rules

The branch name must:

- Be required.
- Be trimmed before persistence.
- Not consist only of whitespace.
- Have a maximum length of 120.
- Be unique within the current tenant.
- Be compared case-insensitively for uniqueness.
- Be allowed in another tenant.

Valid:

```text
Tenant A → Main Branch
Tenant B → Main Branch
```

Invalid:

```text
Tenant A → Main Branch
Tenant A → main branch
```

Use application-level checks and a database constraint or functional unique index.

---

## 9. Branch Code Rules

The branch code is a stable tenant-local business identifier.

Examples:

```text
MAIN
NRB-CBD
MSA-01
WAREHOUSE_1
```

Requirements:

- Required
- Trimmed
- Normalized to uppercase
- Minimum 2 characters
- Maximum 30 characters
- Starts with a letter or digit
- Contains only uppercase letters, numbers, hyphens, or underscores
- Unique within the tenant
- Allowed in another tenant

Use this pattern or an equivalent:

```regex
^[A-Z0-9][A-Z0-9_-]*$
```

Example:

```text
Input:  nrb-cbd
Stored: NRB-CBD
```

Treat branch code as immutable after creation unless the existing repository has a clear established pattern allowing business-code changes.

Prefer immutable code.

If code is immutable:

- Do not include it in `UpdateBranchRequest`, or
- Ignore attempts to change it only if that matches existing API conventions.

Do not silently accept and discard invalid client data. Prefer excluding immutable fields from the update DTO.

Document the final decision.

---

## 10. Country Code Rules

When provided, `countryCode` must:

- Be trimmed.
- Be normalized to uppercase.
- Contain exactly two alphabetic characters.

Example:

```text
Input:  ke
Stored: KE
```

Use Bean Validation and service normalization.

---

## 11. Active Status

New branches must default to:

```text
active = true
```

The create request must not contain an active field.

The general update request must not change active status.

Use a dedicated status endpoint for activation and deactivation.

Inactive branches:

- Remain retrievable
- Remain available for historical relationships
- Can be reactivated
- Must not be physically deleted

Do not implement hard deletion.

---

## 12. Flyway Migration

Create the next correctly numbered Flyway migration.

Do not edit an already-applied migration.

Create a `branches` table according to project naming conventions.

The migration must include:

- UUID primary key
- `tenant_id`
- branch fields
- active status
- audit fields compatible with `BaseEntity`
- foreign key to `tenants`
- non-null constraints
- appropriate lengths
- tenant-scoped unique code
- tenant-scoped case-insensitive unique name
- useful indexes

### Required constraints

At minimum:

```sql
UNIQUE (tenant_id, code)
```

For case-insensitive name uniqueness, use a PostgreSQL functional unique index equivalent to:

```sql
CREATE UNIQUE INDEX ...
ON branches (tenant_id, LOWER(name));
```

Use existing constraint and index naming conventions.

### Required indexes

Add indexes supporting:

- Tenant-scoped branch listing
- Tenant and active filtering
- Tenant and code lookup
- Tenant and case-insensitive name lookup

Avoid redundant indexes already covered by unique indexes.

Do not use Hibernate schema generation.

---

## 13. DTOs

Create:

```text
CreateBranchRequest
UpdateBranchRequest
UpdateBranchStatusRequest
BranchResponse
```

Follow existing class, record, validation-message, and package conventions.

### CreateBranchRequest

Fields:

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

Must not include:

- ID
- tenant ID
- tenant object
- active status
- audit fields
- soft-delete fields

### UpdateBranchRequest

Include only editable branch profile fields.

Recommended fields:

- name
- email
- phone
- addressLine1
- addressLine2
- city
- stateOrCounty
- postalCode
- countryCode

Prefer excluding `code` so the business identifier remains immutable.

Must not include:

- ID
- tenant ID
- tenant object
- active status
- audit fields

### UpdateBranchStatusRequest

Contains only:

```json
{
    "active": false
}
```

The value must be required and non-null.

### BranchResponse

Return:

- UUID
- name
- code
- email
- phone
- address fields
- country code
- active status
- permitted audit fields according to existing conventions

Do not expose:

- Tenant entity
- Tenant internals
- Hibernate relationships
- Internal deletion metadata
- Authentication information

Whether to expose `tenantId` should follow existing API patterns. Prefer omitting it because every request is already tenant-scoped.

---

## 14. MapStruct Mapper

Create a Branch mapper using the existing central MapStruct configuration.

Support:

- `CreateBranchRequest` to `Branch`
- `Branch` to `BranchResponse`
- applying `UpdateBranchRequest` to an existing managed Branch

Ignore server-controlled fields during request mapping:

- ID
- tenant
- active
- audit fields
- soft-delete fields

Update mapping must preserve:

- ID
- tenant
- code, when immutable
- active status
- audit fields

Use explicit MapStruct ignores where necessary.

Do not use BeanUtils or reflection-based property copying.

---

## 15. Branch Repository

Create:

```text
BranchRepository
```

using the project’s existing repository base type.

All branch access must be tenant-scoped.

Implement equivalents appropriate to the actual Tenant mapping:

```java
Optional<Branch> findByIdAndTenantId(
        UUID branchId,
        UUID tenantId);
```

```java
boolean existsByTenantIdAndCodeIgnoreCase(
        UUID tenantId,
        String code);
```

```java
boolean existsByTenantIdAndNameIgnoreCase(
        UUID tenantId,
        String name);
```

For update name validation, support excluding the current branch.

For example, an equivalent of:

```java
boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(
        UUID tenantId,
        String name,
        UUID branchId);
```

Repository list operations must support:

- Tenant scope
- Optional search
- Optional active filter
- Pagination
- Sorting

Search must match branch:

- name, case-insensitively
- code, case-insensitively

Use the project’s established approach, such as:

- JPA Specifications
- derived queries
- Criteria API
- custom repository query

Do not retrieve every row and filter in Java.

Do not use unrestricted `findById`.

---

## 16. Current Tenant Resolution

Use the current-tenant provider or authenticated principal introduced in Story 7A.

Every service operation must obtain tenant ownership from trusted authentication state.

Conceptually:

```java
UUID tenantId = currentTenantProvider.getCurrentTenantId();
```

For branch creation, obtain the actual Tenant entity using the established Story 7A pattern.

Possible approved approaches include:

- `currentTenantProvider.getCurrentTenant()`
- tenant repository `getReferenceById(tenantId)`
- trusted Tenant principal data followed by repository lookup

Follow the existing implementation.

Do not:

- Accept tenant ID from the request
- Accept tenant ID from a query parameter
- Accept arbitrary tenant headers
- Derive tenant from branch input
- Use a default tenant silently

---

## 17. Service Layer

Create the Branch service interface and implementation according to existing conventions.

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

Use the project’s existing pagination response type.

### Create behavior

The service must:

1. Resolve the authenticated tenant.
2. Normalize the name, code, country code, and optional text fields.
3. Check tenant-scoped duplicate code.
4. Check tenant-scoped duplicate name.
5. Map the request.
6. Assign the trusted Tenant entity.
7. Set active to true.
8. Save.
9. Return `BranchResponse`.

### List behavior

The service must:

1. Resolve tenant ID.
2. Normalize optional search input.
3. Query only branches for that tenant.
4. Apply optional active filter.
5. Apply pagination.
6. Apply safe sorting.
7. Map results to the existing page response.

When `active` is absent, return active and inactive branches.

### Get behavior

Use both:

- branch ID
- current tenant ID

If no matching branch exists, throw the existing resource-not-found exception.

A branch belonging to another tenant must also return not found.

### Update behavior

The service must:

1. Resolve tenant ID.
2. Load the branch using tenant-scoped lookup.
3. Normalize fields.
4. Check duplicate name excluding the current branch.
5. Apply editable fields.
6. Preserve tenant ownership.
7. Preserve code if immutable.
8. Preserve active status.
9. Return the updated response.

### Status behavior

The service must:

1. Resolve tenant ID.
2. Load branch using tenant-scoped lookup.
3. Change only the active field.
4. Support activation and deactivation.
5. Treat setting the same status as idempotent success.
6. Return the updated response.

### Transactions

Use:

```java
@Transactional
```

for:

- create
- update
- status update

Use:

```java
@Transactional(readOnly = true)
```

for:

- list
- get

Follow existing annotation placement.

---

## 18. Duplicate and Race-Condition Handling

Perform service-level checks to provide friendly errors.

The database must remain the final authority.

Handle database unique-constraint violations caused by concurrent requests and translate them into the project’s existing `409 Conflict` response.

Do not expose raw PostgreSQL or Hibernate exception details.

Duplicate messages should distinguish, where project conventions allow:

- duplicate branch code
- duplicate branch name

Do not reveal another tenant’s conflicting records.

---

## 19. REST Controller

Create:

```text
BranchController
```

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

Use constructor injection.

Use Jakarta validation.

Delegate business behavior entirely to the service.

### Create branch

```http
POST /api/v1/branches
```

Return:

```http
201 Created
```

Add a Location header if consistent with existing controllers.

### List branches

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

Use existing pagination defaults and limits.

Sorting must be restricted to approved fields.

Suggested safe sort fields:

- name
- code
- city
- active
- createdAt
- updatedAt

Reject or safely map unsupported sort properties according to existing conventions.

### Get branch

```http
GET /api/v1/branches/{branchId}
```

Return:

- `200 OK` for a tenant-owned branch
- `404 Not Found` for unknown or cross-tenant branch

### Update branch

```http
PUT /api/v1/branches/{branchId}
```

Return:

```http
200 OK
```

Do not update tenant, code when immutable, or active status.

### Update status

```http
PATCH /api/v1/branches/{branchId}/status
```

Return:

```http
200 OK
```

Do not add a DELETE endpoint.

---

## 20. RBAC Authorization

Reuse the exact roles and authorities already present in the project.

Inspect existing `@PreAuthorize` patterns and security tests.

Apply the closest existing authorization model to:

| Operation     | Required conceptual access |
| ------------- | -------------------------- |
| Create        | Tenant administrator       |
| List          | Authorized tenant user     |
| Get           | Authorized tenant user     |
| Update        | Tenant administrator       |
| Change status | Tenant administrator       |

Do not invent new permission infrastructure unless the project already uses granular permissions.

Role authorization does not replace tenant scoping.

An administrator in Tenant A must not access Tenant B branches.

---

## 21. Error Handling

Reuse existing global exception handling and response formats.

Expected statuses:

| Condition                          | Status |
| ---------------------------------- | -----: |
| Invalid DTO                        |    400 |
| Invalid UUID                       |    400 |
| Missing or invalid token           |    401 |
| Insufficient role                  |    403 |
| Branch not found in current tenant |    404 |
| Cross-tenant branch access         |    404 |
| Duplicate code                     |    409 |
| Duplicate name                     |    409 |
| Successful create                  |    201 |
| Successful list/get/update/status  |    200 |

Do not catch exceptions in the controller that are already handled globally.

Do not expose stack traces or database details.

---

## 22. OpenAPI Documentation

Document every Branch endpoint using existing OpenAPI conventions.

Include:

- Endpoint summary
- Endpoint description
- JWT bearer security
- Authorization expectations
- Path parameters
- Query parameters
- Pagination behavior
- Search behavior
- Active filter behavior
- Request examples
- Response examples
- Validation errors
- Authentication errors
- Authorization errors
- Not-found errors
- Conflict errors

Confirm endpoints appear correctly in Swagger UI.

---

## 23. Automated Tests

Follow `AGENTS.md` and existing testing conventions.

Use PostgreSQL Testcontainers when repository integration or migration behavior depends on PostgreSQL.

Do not disable Spring Security.

### 23.1 Service unit tests

Cover at minimum:

1. Creates a branch successfully.
2. Uses the authenticated tenant.
3. Does not accept tenant ownership from request input.
4. Trims branch name.
5. Normalizes code to uppercase.
6. Normalizes country code to uppercase.
7. Defaults active to true.
8. Rejects duplicate code within the same tenant.
9. Rejects duplicate case-insensitive name within the same tenant.
10. Allows the same code in another tenant.
11. Allows the same name in another tenant.
12. Returns a tenant-owned branch.
13. Returns not found for unknown branch.
14. Returns not found for another tenant’s branch.
15. Updates editable fields.
16. Preserves tenant ownership during update.
17. Preserves immutable code during update.
18. Preserves active status during update.
19. Rejects duplicate name during update.
20. Deactivates a branch.
21. Reactivates a branch.
22. Handles idempotent status updates.
23. Returns paginated results.
24. Applies case-insensitive search.
25. Applies active filtering.
26. Handles blank search as no search.
27. Rejects or safely handles unsupported sorting.

Mock collaborators only.

Do not mock the service class under test.

### 23.2 Mapper tests

Verify:

1. Create request maps editable fields.
2. Tenant is not mapped from request.
3. ID is not mapped from request.
4. Active is not mapped from request.
5. Audit fields are not mapped from request.
6. Entity maps correctly to response.
7. Update mapping preserves tenant.
8. Update mapping preserves ID.
9. Update mapping preserves code when immutable.
10. Update mapping preserves active status.
11. Update mapping preserves audit fields.

### 23.3 Repository tests

Cover at minimum:

1. Persists a valid branch.
2. Rejects a branch without tenant.
3. Finds branch by ID and tenant ID.
4. Does not find branch using another tenant ID.
5. Detects duplicate code in one tenant.
6. Allows same code in different tenants.
7. Detects case-insensitive duplicate name in one tenant.
8. Allows same name in different tenants.
9. Lists only tenant-owned branches.
10. Filters active branches.
11. Filters inactive branches.
12. Searches name case-insensitively.
13. Searches code case-insensitively.
14. Applies pagination.
15. Applies safe sorting.
16. Enforces required fields.
17. Enforces tenant foreign key.
18. Enforces tenant/code unique constraint.
19. Enforces tenant/case-insensitive-name uniqueness.
20. Deleting a branch does not delete its tenant.

### 23.4 Controller or API integration tests

Cover at minimum:

1. Authorized administrator creates branch.
2. Create returns `201 Created`.
3. Request cannot assign tenant ID.
4. Missing name returns `400`.
5. Invalid code returns `400`.
6. Invalid email returns `400`.
7. Invalid country code returns `400`.
8. Duplicate code returns `409`.
9. Duplicate name returns `409`.
10. Unauthenticated create returns `401`.
11. Unauthorized role create returns `403`.
12. Authorized user lists branches.
13. List is tenant-scoped.
14. List is paginated.
15. Search by name works.
16. Search by code works.
17. Active filter works.
18. Inactive filter works.
19. Authorized user retrieves tenant-owned branch.
20. Unknown branch returns `404`.
21. Cross-tenant branch retrieval returns `404`.
22. Authorized administrator updates branch.
23. Update preserves code.
24. Update preserves status.
25. Cross-tenant update returns `404`.
26. Authorized administrator deactivates branch.
27. Authorized administrator reactivates branch.
28. Unauthorized role cannot change status.
29. Cross-tenant status change returns `404`.
30. Global success and error response structures are preserved.

Use existing JWT and authentication test helpers.

### 23.5 Migration tests

Verify:

1. Flyway migration succeeds on a clean PostgreSQL database.
2. `branches` table exists.
3. `tenant_id` is non-null.
4. Foreign key to tenants exists.
5. Active defaults correctly.
6. Tenant/code unique constraint exists.
7. Case-insensitive tenant/name unique index exists.
8. Required indexes exist.
9. Audit columns match `BaseEntity`.
10. Flyway validation passes.

---

## 24. Acceptance Criteria

Story 8 is accepted only when all criteria are satisfied.

### Persistence

- [ ] Branch entity exists.
- [ ] Branch extends `BaseEntity`.
- [ ] Every branch belongs to one tenant.
- [ ] Tenant association is mandatory.
- [ ] Branch IDs use UUIDs.
- [ ] Name is required and trimmed.
- [ ] Code is required and normalized.
- [ ] Country code is normalized.
- [ ] Active defaults to true.
- [ ] Flyway creates the schema.
- [ ] Tenant/code uniqueness is enforced.
- [ ] Tenant/name case-insensitive uniqueness is enforced.
- [ ] Same branch code is allowed in separate tenants.
- [ ] Same branch name is allowed in separate tenants.
- [ ] Required indexes exist.

### APIs

- [ ] Authorized administrator can create a branch.
- [ ] Authorized user can list branches.
- [ ] Listing is paginated.
- [ ] Listing supports search.
- [ ] Listing supports active filtering.
- [ ] Listing supports safe sorting.
- [ ] Authorized user can retrieve a branch.
- [ ] Authorized administrator can update a branch.
- [ ] Authorized administrator can deactivate a branch.
- [ ] Authorized administrator can reactivate a branch.
- [ ] No hard-delete endpoint exists.

### Tenant security

- [ ] Tenant identity comes from Story 7A trusted context.
- [ ] Branch request DTOs contain no tenant ID.
- [ ] Repository queries are tenant-scoped.
- [ ] Service lookups are tenant-scoped.
- [ ] Tenant A cannot retrieve Tenant B branches.
- [ ] Tenant A cannot update Tenant B branches.
- [ ] Tenant A cannot change Tenant B branch status.
- [ ] Cross-tenant resource IDs return `404`.
- [ ] Roles do not bypass tenant isolation.

### Architecture

- [ ] Package-by-feature is followed.
- [ ] Controllers contain no business logic.
- [ ] Services contain business rules.
- [ ] Repositories handle persistence.
- [ ] DTOs isolate API contracts.
- [ ] MapStruct handles mappings.
- [ ] Global exception handling is reused.
- [ ] Transactions are correctly applied.
- [ ] Existing tenant foundation is reused.
- [ ] `AGENTS.md` is followed.

### Quality

- [ ] Unit tests pass.
- [ ] Mapper tests pass where required.
- [ ] Repository tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] Tenant-isolation tests pass.
- [ ] Migration tests pass.
- [ ] Existing Story 7A tests remain green.
- [ ] Existing Sprint 1 tests remain green.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verification passes.
- [ ] Swagger documentation is correct.

---

## 25. Beginner-Friendly Manual QA

After implementation, provide detailed manual QA using Swagger UI.

### Test 1 — Start the environment

1. Start PostgreSQL and other required Docker services.
2. Start the Spring Boot application.
3. Confirm Flyway completes successfully.
4. Open Swagger UI.
5. Authenticate as a tenant administrator.

Expected:

- Application starts successfully.
- No Flyway validation errors occur.
- JWT authentication works.

### Test 2 — Create a branch

Call:

```http
POST /api/v1/branches
```

Request:

```json
{
    "name": "Nairobi CBD",
    "code": "nrb-cbd",
    "email": "nairobi@example.com",
    "phone": "+254712345678",
    "addressLine1": "Kimathi Street",
    "city": "Nairobi",
    "stateOrCounty": "Nairobi",
    "postalCode": "00100",
    "countryCode": "ke"
}
```

Expected:

- `201 Created`
- Code returned as `NRB-CBD`
- Country returned as `KE`
- Active is `true`
- UUID is returned
- No tenant ID was supplied

### Test 3 — Verify tenant assignment

Inspect the branch record in pgAdmin.

Expected:

- `tenant_id` matches the logged-in administrator’s tenant.
- Tenant ownership was assigned by the server.

### Test 4 — Missing name validation

Submit an empty or blank name.

Expected:

- `400 Bad Request`
- Standard validation error response

### Test 5 — Invalid code validation

Test:

```text
A
NRB CBD
NRB@CBD
```

Expected:

- Each returns `400 Bad Request`

### Test 6 — Duplicate code

Create another branch in the same tenant with:

```text
NRB-CBD
```

Expected:

- `409 Conflict`

### Test 7 — Duplicate name with different case

Create:

```text
nairobi cbd
```

Expected:

- `409 Conflict`

### Test 8 — Create another branch

Create:

```json
{
    "name": "Mombasa",
    "code": "MSA-01",
    "city": "Mombasa",
    "stateOrCounty": "Mombasa",
    "countryCode": "KE"
}
```

Expected:

- `201 Created`

### Test 9 — List branches

Call:

```http
GET /api/v1/branches?page=0&size=10&sort=name,asc
```

Expected:

- `200 OK`
- Pagination metadata appears
- Branches are sorted
- Only current-tenant branches appear

### Test 10 — Search by name

Call:

```http
GET /api/v1/branches?search=nairobi
```

Expected:

- Nairobi CBD appears

### Test 11 — Search by code

Call:

```http
GET /api/v1/branches?search=nrb
```

Expected:

- Nairobi CBD appears

### Test 12 — Retrieve by ID

Call:

```http
GET /api/v1/branches/{branchId}
```

Expected:

- `200 OK`
- Correct branch is returned

### Test 13 — Update branch

Call:

```http
PUT /api/v1/branches/{branchId}
```

Update contact or address information.

Expected:

- `200 OK`
- Editable fields change
- UUID does not change
- Tenant does not change
- Code does not change
- Active status does not change
- Audit modification fields update

### Test 14 — Deactivate branch

Call:

```http
PATCH /api/v1/branches/{branchId}/status
```

```json
{
    "active": false
}
```

Expected:

- `200 OK`
- Branch remains retrievable
- Active becomes false

### Test 15 — Filter inactive branches

Call:

```http
GET /api/v1/branches?active=false
```

Expected:

- Deactivated branch appears
- Active branches do not appear

### Test 16 — Reactivate branch

Call status endpoint with:

```json
{
    "active": true
}
```

Expected:

- `200 OK`
- Active becomes true

### Test 17 — Unauthorized write access

Authenticate as a user permitted to read but not administer branches.

Attempt create, update, and status operations.

Expected:

- Read operations follow RBAC rules
- Restricted operations return `403 Forbidden`

### Test 18 — Tenant isolation

Prepare Tenant A and Tenant B test users.

1. Create a branch as Tenant A.
2. Authenticate as Tenant B.
3. Attempt to retrieve Tenant A branch.
4. Attempt to update Tenant A branch.
5. Attempt to change Tenant A branch status.

Expected:

- Each operation returns `404 Not Found`
- No Tenant A data is exposed
- No Tenant A record is modified

### Test 19 — Same code in another tenant

As Tenant B, create:

```text
NRB-CBD
```

Expected:

- Creation succeeds because uniqueness is tenant-scoped

### Test 20 — Database constraints

Inspect PostgreSQL.

Confirm:

- Branch table exists
- Tenant foreign key exists
- Tenant ID is non-null
- Code unique constraint is tenant-scoped
- Name uniqueness is tenant-scoped and case-insensitive
- Active is non-null
- Audit fields exist
- Indexes exist

### Test 21 — Run quality checks

Run:

```bash
./mvnw test
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw verify
```

Use `mvnw.cmd` if required.

Expected:

- All commands pass

---

## 26. Quality Checks

Run all project-standard commands.

At minimum:

```bash
./mvnw test
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw verify
```

Also confirm:

- Flyway migration succeeds
- Flyway validation succeeds
- Swagger UI loads
- Existing authentication still works

Report actual results.

Do not claim success for commands not executed.

Do not suppress failures without a justified and documented reason.

---

## 27. Definition of Done

Story 8 is done only when:

- [ ] `AGENTS.md` was read and followed.
- [ ] Story 7A architecture was inspected and reused.
- [ ] Branch entity is implemented.
- [ ] Tenant ownership is mandatory.
- [ ] Flyway migration is complete.
- [ ] DTOs are complete.
- [ ] Mapper is complete.
- [ ] Repository is complete.
- [ ] Service is complete.
- [ ] Controller is complete.
- [ ] Authentication is required.
- [ ] RBAC is enforced.
- [ ] Tenant isolation is enforced.
- [ ] Client-controlled tenant assignment is impossible.
- [ ] Search works.
- [ ] Pagination works.
- [ ] Safe sorting works.
- [ ] Active filtering works.
- [ ] Update works.
- [ ] Deactivation works.
- [ ] Reactivation works.
- [ ] No hard-delete endpoint exists.
- [ ] Global exception handling is reused.
- [ ] OpenAPI is complete.
- [ ] Unit tests pass.
- [ ] Repository tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] Tenant-isolation tests pass.
- [ ] Migration tests pass.
- [ ] Existing Story 7A tests remain green.
- [ ] Existing Sprint 1 tests remain green.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verify passes.
- [ ] Manual QA instructions are complete.
- [ ] No Story 9 or later functionality was implemented.

---

## 28. Implementation Guardrails

Do not:

- Implement Customer Management.
- Implement Supplier Management.
- Implement Product Categories.
- Implement Product Management.
- Implement Inventory.
- Implement Purchases.
- Implement Sales.
- Implement Reports.
- Add another Tenant abstraction.
- Implement Tenant CRUD.
- Accept tenant IDs from clients.
- Accept arbitrary tenant headers.
- Add user-to-branch assignments.
- Add default or primary branch rules.
- Add branch-specific stock.
- Add warehouses.
- Add cash registers.
- Add POS terminals.
- Add hard deletion.
- Expose JPA entities directly.
- Put business logic in controllers.
- Access repositories from controllers.
- Use unrestricted `findById`.
- Disable security in tests.
- Bypass the current-tenant provider.
- Edit applied Flyway migrations.
- Depend on Hibernate schema generation.
- Rewrite unrelated Story 7A or Sprint 1 code.
- Suppress Spotless or Checkstyle failures.
- Claim tests passed when they were not run.

Keep all changes strictly limited to Story 8.

---

## 29. Final Response Requirements

After implementation, provide a structured report.

### Architecture reused

Report:

- Tenant entity and package
- User-to-Tenant association
- Authenticated principal
- Current-tenant provider
- JWT tenant behavior
- Existing RBAC conventions
- Existing pagination and exception conventions

### Implementation completed

Summarize:

- Branch entity
- Flyway migration
- DTOs
- Mapper
- Repository
- Service
- Controller
- Security
- OpenAPI
- Tests

### Files changed

List every created or modified file and explain why it changed.

### Key design decisions

Explain:

- Tenant ownership strategy
- Name uniqueness implementation
- Code normalization
- Code immutability
- Search implementation
- Pagination implementation
- Safe sorting
- Status transition behavior
- Cross-tenant not-found behavior

### Database changes

Report:

- Migration filename
- Table
- Columns
- Foreign keys
- Unique constraints
- Functional indexes
- Other indexes

### Test coverage

Report:

- Service tests
- Mapper tests
- Repository tests
- Integration tests
- Security tests
- Tenant-isolation tests
- Migration tests

### Verification results

Report actual outcomes for:

```text
Maven test
Spotless
Checkstyle
Maven verify
Flyway migration
Flyway validation
```

### Manual QA

Provide the complete beginner-friendly manual QA guide with example requests and expected responses.

### Remaining concerns

Clearly report:

- Failed checks
- Assumptions
- Skipped tests
- Compatibility concerns
- Incomplete acceptance criteria
- Follow-up recommendations

### Story 9 readiness

End by stating whether the project is ready to proceed to:

```text
Story 9 — Customer Management
```

The project is ready only if Story 8 acceptance criteria and quality gates pass.
