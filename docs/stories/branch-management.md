Branch Management Architecture

Branch Management should come first in Sprint 2 because most later business records will belong to a branch.

A branch represents a physical or operational business location, such as:

Nairobi CBD Store
Westlands Pharmacy
Mombasa Warehouse
Main Restaurant
Online Sales Branch

Later modules will reference branches:

Tenant / Organization
|
├── Branch
│ ├── Inventory
│ ├── Purchases
│ ├── Sales
│ ├── Expenses
│ └── Employees
|
├── Customers
├── Suppliers
└── Products

Products may be shared across an organization, but their stock levels, purchases, and sales usually vary by branch.

For example:

Product: Coca-Cola 500ml

Nairobi Branch stock: 120
Mombasa Branch stock: 75
Kisumu Branch stock: 30

This is why Branch Management must exist before Inventory, Purchases, and Sales.

1. Important multi-tenancy requirement

Because this is a multi-tenant SaaS, branches must never exist globally without ownership.

Every branch must belong to exactly one tenant or organization.

Conceptually:

Tenant
└── Branches

The database should eventually enforce this relationship:

tenant_id
branch_id

A user authenticated under Tenant A must never:

View Tenant B branches
Update Tenant B branches
Delete Tenant B branches
Detect whether a Tenant B branch code exists
Assign records to Tenant B branches
Required architecture check

Before implementing Story 8, Codex must inspect the existing project and determine how tenant ownership is currently represented.

It may be represented by something such as:

Tenant
Organization
Company
Business
tenantId stored on the user
A tenant context resolved from the JWT

The implementation must reuse the existing tenant model and terminology.

If Sprint 1 does not yet contain tenant ownership or tenant-context resolution, Codex must not invent an insecure temporary workaround. That missing foundation should be reported before Branch Management is implemented.

Branch Management can be designed now, but secure implementation depends on a reliable tenant identifier.

2. Branch business requirements

A branch should contain enough information to identify and operate a business location without becoming an oversized entity.

Recommended initial fields:

Field Purpose
id UUID primary key inherited from BaseEntity
tenantId or tenant relationship Identifies the branch owner
name Human-readable branch name
code Short business identifier such as NRB-CBD
email Optional branch contact email
phone Optional branch phone number
addressLine1 Main physical address
addressLine2 Optional additional address
city Branch city or town
stateOrCounty County, state, or region
postalCode Optional postal code
countryCode ISO-style country code such as KE
active Determines whether the branch may be used operationally
audit fields Created and modified timestamps/users from BaseEntity

The first version should avoid storing complex operational configuration such as:

Receipt templates
Tax settings
Payment integrations
Branch-specific pricing
Warehouse bins
Cash registers
Opening hours
Fiscal devices

Those belong in later stories or separate configuration modules.

3. Branch identity rules
   Branch name

The name is displayed to users.

Examples:

Nairobi CBD
Westlands
Mombasa Warehouse

A branch name should:

Be required
Be trimmed
Have a reasonable maximum length
Be unique within the same tenant
Be allowed to exist in another tenant

For example:

Tenant A → Main Branch
Tenant B → Main Branch

This is valid.

But this should not be valid:

Tenant A → Main Branch
Tenant A → Main Branch

Case-insensitive uniqueness is preferable:

Main Branch
main branch
MAIN BRANCH

These should be treated as the same name within one tenant.

Branch code

The branch code is a stable business identifier.

Examples:

MAIN
NRB-CBD
MSA-01
WAREHOUSE

A branch code should:

Be required
Be normalized to uppercase
Not contain spaces
Support letters, numbers, hyphens, and possibly underscores
Be unique within the tenant
Not be changed casually after creation

Recommended pattern:

^[A-Z0-9][A-Z0-9_-]\*$

Recommended length:

2–30 characters

A code is useful because names can change while internal identifiers should remain recognizable.

For example:

Old name: Nairobi Store
New name: Nairobi CBD Flagship Store
Code: NRB-CBD

The branch UUID remains the technical identity, while the code remains the business identity.

4. Active and inactive branches

Branches should normally be deactivated rather than physically deleted.

An inactive branch:

Remains available for historical reports
Keeps its previous sales and purchase relationships
Cannot normally receive new sales, purchases, inventory movements, or assignments
Can be reactivated by an authorized administrator

Example:

Westlands Branch closed in 2027.

Historical sales must still show:
Sale #1234 → Westlands Branch

Deleting the branch would risk breaking historical reporting and referential integrity.

Therefore, Story 8 should distinguish between:

Deactivation

and:

Deletion

For the first implementation, deactivation is the safer primary lifecycle operation.

A hard-delete endpoint should not be exposed unless AGENTS.md or the existing architecture explicitly requires it.

If the project already supports soft deletion, the branch should follow the same established pattern.

5. Default or primary branch

Many POS systems need a primary branch, but introducing it has important business rules:

Can each tenant have only one primary branch?
Can the primary branch be deactivated?
What happens when the primary branch is deleted?
Is the first branch automatically primary?
Can users belong to several branches?

Unless these rules are already documented, Story 8 should not introduce isDefault or isPrimary.

A primary branch can be added later through a dedicated story when user-to-branch assignment and tenant settings are designed.

This keeps Story 8 focused.

6. Authorization design

Authentication answers:

Who is making the request?

Authorization answers:

Is this user allowed to perform this branch operation?

Tenant isolation answers:

Does this branch belong to the authenticated user’s tenant?

All three must be applied.

Suggested permissions or roles depend on the existing Sprint 1 RBAC implementation.

Conceptually:

Operation Suggested authorization
Create branch Tenant administrator
List branches Authorized tenant user
View branch Authorized tenant user
Update branch Tenant administrator or branch-management permission
Activate/deactivate Tenant administrator
Delete, if supported Highest-level tenant administrator

The implementation must reuse existing role names. It should not invent new role constants without confirming the established authorization model.

Controller authorization may use:

@PreAuthorize(...)

However, method-level role checks alone are insufficient.

The service must still ensure that the requested branch belongs to the authenticated tenant.

For example, this is unsafe:

branchRepository.findById(branchId)

This is safer:

branchRepository.findByIdAndTenantId(branchId, tenantId)

The repository query itself helps prevent cross-tenant access.

7. API design

Recommended resource path:

/api/v1/branches

Suggested endpoints:

POST /api/v1/branches
GET /api/v1/branches
GET /api/v1/branches/{branchId}
PUT /api/v1/branches/{branchId}
PATCH /api/v1/branches/{branchId}/status

A hard-delete endpoint is intentionally excluded from the initial recommendation.

Create branch
POST /api/v1/branches

Example request:

{
"name": "Nairobi CBD",
"code": "NRB-CBD",
"email": "nairobi@example.com",
"phone": "+254712345678",
"addressLine1": "Kimathi Street",
"addressLine2": null,
"city": "Nairobi",
"stateOrCounty": "Nairobi",
"postalCode": "00100",
"countryCode": "KE"
}

The client must not submit:

{
"tenantId": "...",
"createdBy": "...",
"active": true
}

These values should be controlled by the server.

The tenant should come from the authenticated context, not from user input.

New branches should default to active.

List branches
GET /api/v1/branches

Recommended query parameters:

?page=0
&size=20
&sort=name,asc
&search=nairobi
&active=true

The endpoint should be paginated from the beginning. A tenant may eventually have many branches, and using pagination establishes a reusable pattern for upcoming modules.

Get branch
GET /api/v1/branches/{branchId}

It should return only a branch belonging to the authenticated tenant.

For cross-tenant IDs, returning 404 Not Found is usually safer than 403 Forbidden, because it does not reveal that another tenant’s resource exists.

Update branch
PUT /api/v1/branches/{branchId}

The initial implementation should use full update semantics.

The service must:

Find the branch within the authenticated tenant.
Validate name uniqueness if the name changed.
Validate code uniqueness if code changes are permitted.
Apply updates through MapStruct or explicit controlled mapping.
Save the managed entity.
Return the updated response.
Change status
PATCH /api/v1/branches/{branchId}/status

Example request:

{
"active": false
}

A dedicated status endpoint is clearer than allowing any user who can update contact details to deactivate a branch accidentally.

8. DTO architecture

The entity should not be exposed directly through the REST API.

Recommended DTOs:

CreateBranchRequest
UpdateBranchRequest
UpdateBranchStatusRequest
BranchResponse
BranchSummaryResponse

Depending on existing project patterns, BranchSummaryResponse may be deferred until it is needed.

Why separate create and update DTOs?

Create and update operations do not always have the same rules.

For example:

active is server-controlled during creation.
Certain identifiers may become immutable after creation.
Future update requests may allow fewer fields.
Validation messages may differ.

Using one large DTO for every operation tends to create unclear validation and accidental over-posting.

9. Entity design

Conceptual entity:

@Entity
@Table(
name = "branches",
uniqueConstraints = {
@UniqueConstraint(
name = "uk_branches_tenant_code",
columnNames = {"tenant_id", "code"}
)
}
)
public class Branch extends BaseEntity {
// fields
}

The exact entity must follow AGENTS.md and existing entity conventions.

Important database constraints should include:

tenant_id not null
name not null
code not null
active not null
Unique tenant and normalized code
Appropriate column lengths

Name uniqueness should also be enforced at the database level where practical.

Application validation provides friendly errors, but database constraints protect correctness against race conditions.

10. Why each class is needed
    Branch

Represents the persisted branch domain entity.

Its responsibility is branch state and persistence mapping. It should not contain HTTP concerns.

CreateBranchRequest

Defines data accepted when creating a branch.

It protects the entity from direct external binding and contains request validation.

UpdateBranchRequest

Defines fields that may be changed through the update endpoint.

It prevents clients from changing server-controlled fields.

UpdateBranchStatusRequest

Represents the activate/deactivate command.

It gives the status transition its own validation and authorization boundary.

BranchResponse

Defines the API representation returned to clients.

It prevents accidental exposure of internal relationships or sensitive tenant information.

BranchMapper

Converts:

CreateBranchRequest → Branch
Branch → BranchResponse
UpdateBranchRequest → existing Branch

MapStruct generates mapping code during compilation. It avoids repetitive manual conversion while remaining type-safe.

BranchRepository

Provides database access for branches.

It should include tenant-aware queries rather than relying only on unrestricted findById.

BranchService

Defines branch use cases.

It creates a stable contract between controllers and business logic.

BranchServiceImpl

Implements:

Tenant scoping
Uniqueness checks
Normalization
Transaction boundaries
Not-found handling
Status changes
BranchController

Handles HTTP concerns:

Routes
Request deserialization
Bean Validation
Authorization annotations
Status codes
OpenAPI documentation

It should not contain business logic.

Flyway migration

Creates the branch table and database constraints in a repeatable, version-controlled manner.

JPA should not be responsible for modifying the production schema.

Tests

Tests prove that branch behavior, tenant isolation, validation, and authorization remain correct as the project evolves.

11. How Spring processes a branch request internally

Consider:

POST /api/v1/branches

Spring processes it approximately as follows:

HTTP Request
↓
Servlet container receives request
↓
Spring Security filter chain runs
↓
JWT filter reads and validates token
↓
Authentication is placed in SecurityContext
↓
DispatcherServlet receives request
↓
RequestMappingHandlerMapping finds BranchController method
↓
Jackson converts JSON into CreateBranchRequest
↓
Bean Validation checks DTO annotations
↓
Authorization check is evaluated
↓
BranchController calls BranchService
↓
Service obtains authenticated tenant context
↓
Service checks duplicate branch name/code
↓
MapStruct maps request into Branch
↓
Spring Data repository persists the entity
↓
Hibernate generates SQL
↓
PostgreSQL validates database constraints
↓
Transaction commits
↓
MapStruct maps Branch into BranchResponse
↓
Jackson serializes response into JSON

If validation fails, the controller method should not execute. The global exception handler should convert the validation exception into the project’s standard API error response.

12. Transaction design

Create, update, and status-change operations should run inside transactions.

For example:

@Transactional
public BranchResponse createBranch(CreateBranchRequest request) {
// business operation
}

Read operations may use:

@Transactional(readOnly = true)

A transaction ensures that the business operation succeeds or fails as one unit.

During an update, Hibernate performs dirty checking:

The branch is loaded as a managed entity.
The mapper or service changes its fields.
Hibernate detects those changes.
Hibernate generates an UPDATE during flush or commit.

Calling save() for an already managed entity may be optional, although project conventions should determine whether it is used consistently.

13. Validation levels

Validation should exist at multiple layers.

DTO validation

Examples:

@NotBlank
@Size(max = 120)
private String name;
@NotBlank
@Pattern(regexp = "^[A-Z0-9][A-Z0-9_-]\*$")
private String code;

This gives fast, understandable client errors.

Service validation

Examples:

Duplicate branch code within tenant
Duplicate branch name within tenant
Cross-tenant resource access
Invalid status transition
Prohibition against deactivating a protected branch, if later required
Database validation

Examples:

NOT NULL
Column lengths
Foreign key to tenant
Unique tenant/code constraint

No single validation layer replaces the others.

14. Error behavior

The story should reuse the global exception framework from Sprint 1.

Expected cases include:

Situation Response
Invalid input 400 Bad Request
Missing or invalid token 401 Unauthorized
Insufficient permission 403 Forbidden
Branch not found within tenant 404 Not Found
Duplicate branch name or code 409 Conflict
Successful creation 201 Created
Successful retrieval/update 200 OK

Error bodies must follow the project’s existing standard rather than creating a branch-specific error structure.

15. Testing strategy

Story 8 should test more than happy paths.

Unit tests

Service tests should cover:

Creating a branch
Normalizing the branch code
Rejecting a duplicate code
Rejecting a duplicate name
Returning a branch
Updating a branch
Deactivating a branch
Reactivating a branch
Branch not found
Cross-tenant branch treated as not found

Mapper tests should be included if required by AGENTS.md.

Repository tests

Repository tests should verify:

Tenant-scoped lookup
Tenant-scoped code uniqueness
Tenant-scoped name uniqueness
Same code permitted across different tenants
Pagination and search behavior
Active filtering

Repository tests should use the project’s established database testing strategy. If Testcontainers is already configured, PostgreSQL Testcontainers is preferable to H2 because PostgreSQL behavior and constraints are part of the application architecture.

Controller or integration tests

These should verify:

Authentication requirements
Authorization requirements
Request validation
HTTP status codes
JSON response structure
Tenant isolation
Global exception response format
