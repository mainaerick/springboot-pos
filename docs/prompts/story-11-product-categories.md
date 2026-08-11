# Story 11 — Product Categories

## Document Status

- **Sprint:** Sprint 2
- **Story:** 11
- **Module:** Product Categories
- **Status:** Proposed
- **Implementation status:** Not started
- **Approval required before implementation:** Yes
- **Target path:** `docs/prompts/story-11-product-categories.md`

---

## 1. Purpose

Implement tenant-owned Product Category Management for the Inventory and Point of Sale SaaS.

A product category provides a reusable classification for products belonging to a tenant.

Examples:

```text
Beverages
Electronics
Medicines
Hardware
Bakery
Cleaning Supplies
```

Categories will later be referenced by Product Management and reporting.

---

## 2. Mandatory Project Rules

Before implementation, inspect:

1. `AGENTS.md`
2. Story 7A — Tenant Foundation
3. Story 8 — Branch Management
4. Story 9 — Customer Management
5. Story 10 — Supplier Management
6. `BaseEntity`
7. JPA auditing
8. Tenant entity
9. Current tenant provider
10. Existing RBAC
11. Existing DTO conventions
12. Existing MapStruct configuration
13. Existing repository patterns
14. Existing pagination model
15. Existing safe sorting behavior
16. Existing global exception handling
17. Existing OpenAPI conventions
18. Flyway migration numbering
19. Existing integration test and Testcontainers conventions

Follow `AGENTS.md` as the authoritative project standard.

Reuse existing architectural patterns rather than creating parallel abstractions.

---

## 3. User Story

As an authorized tenant user, I want to create and manage product categories so that products can later be consistently grouped for organization, filtering, reporting, and inventory operations.

---

## 4. Business Value

Product categories enable:

- Product catalog organization
- Product filtering
- Category-based reports
- Category-based inventory analysis
- Cleaner product searches
- Category-specific dashboards
- Future pricing or promotion rules

This story implements category master data only.

---

## 5. Ownership Model

Every product category belongs to exactly one tenant.

Categories are tenant-owned, not branch-owned.

The model is:

```text
Tenant
├── Branches
├── Customers
├── Suppliers
└── Product Categories
```

A category may be used by products stocked across multiple branches.

Branch ownership must not be added to categories.

Tenant ownership must always come from trusted authenticated context.

---

## 6. Scope

### 6.1 Included

Implement:

- ProductCategory entity
- Tenant-to-category relationship
- Flyway migration
- Create category endpoint
- Paginated category-list endpoint
- Get category by UUID endpoint
- Update category endpoint
- Activate/deactivate category endpoint
- Request DTOs
- Response DTO
- Jakarta Bean Validation
- MapStruct mapper
- ProductCategory repository
- ProductCategory service
- Tenant-scoped queries
- Case-insensitive search
- Active-status filtering
- Pagination
- Safe sorting
- RBAC integration
- Global exception integration
- OpenAPI documentation
- Automated tests
- Tenant-isolation tests
- Beginner-friendly manual QA

### 6.2 Excluded

Do not implement:

- Product Management
- Nested or hierarchical categories
- Parent/child category relationships
- Category trees
- Category images
- Category icons
- Category-specific pricing
- Promotions
- Inventory
- Purchases
- Sales
- Supplier-category relationships
- Branch-category relationships
- Hard deletion

---

## 7. ProductCategory Entity

Create:

```text
ProductCategory
```

in the appropriate package-by-feature location.

The entity must extend `BaseEntity`.

### 7.1 Fields

| Field        | Type            | Required | Rules                                          |
| ------------ | --------------- | -------: | ---------------------------------------------- |
| tenant       | Tenant          |      Yes | Trusted tenant ownership                       |
| code         | String          |      Yes | Uppercase, tenant-scoped unique                |
| name         | String          |      Yes | Trimmed, tenant-scoped case-insensitive unique |
| description  | String          |       No | Maximum 500 characters                         |
| active       | Boolean/boolean |      Yes | Defaults to `true`                             |
| audit fields | Existing types  |      Yes | Inherited                                      |

---

## 8. Tenant Relationship

Use the existing Tenant entity.

Equivalent mapping:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

Requirements:

- Every category belongs to one tenant.
- Tenant ownership is immutable through public category APIs.
- Category persistence must not cascade tenant creation or deletion.
- Tenant must not participate in `equals`, `hashCode`, or `toString`.
- The Tenant entity must not be exposed through the API response.

---

## 9. Category Code Rules

Category code must:

- Be required
- Be trimmed
- Be normalized to uppercase
- Have a minimum length of 2
- Have a maximum length of 40
- Start with a letter or digit
- Contain only uppercase letters, digits, hyphens, and underscores
- Be unique within the tenant
- Be allowed in another tenant
- Be immutable after creation

Recommended validation:

```regex
^[A-Z0-9][A-Z0-9_-]*$
```

Examples:

```text
BEVERAGES
ELECTRONICS
MEDICINES
HOME-CARE
```

Input:

```text
beverages
```

must be stored as:

```text
BEVERAGES
```

Do not include code in `UpdateProductCategoryRequest`.

---

## 10. Category Name Rules

Category name must:

- Be required
- Be trimmed
- Not consist only of whitespace
- Have a maximum length of 120
- Be unique within the current tenant using a case-insensitive comparison
- Be allowed in another tenant

Invalid within one tenant:

```text
Beverages
beverages
BEVERAGES
```

These represent the same category name.

Valid across tenants:

```text
Tenant A → Beverages
Tenant B → Beverages
```

---

## 11. Description Rules

Description is optional.

When supplied:

- Trim surrounding whitespace
- Maximum 500 characters

Do not introduce rich text or HTML support.

---

## 12. Active Status

New categories must default to active.

Create requests must not accept active status.

General updates must not change active status.

Status changes must use:

```http
PATCH /api/v1/product-categories/{categoryId}/status
```

Inactive categories:

- Remain retrievable
- Remain available for historical product relationships
- Can be reactivated
- Should later be prevented from being assigned to new products when appropriate

Do not physically delete categories.

---

## 13. Database Migration

Create the next correctly numbered Flyway migration.

Do not edit applied migrations.

Create a table equivalent to:

```text
product_categories
```

following existing naming conventions.

### 13.1 Required columns

Include:

- UUID primary key
- `tenant_id`
- `code`
- `name`
- `description`
- `active`
- BaseEntity audit columns

### 13.2 Required constraints

At minimum:

```sql
UNIQUE (tenant_id, code)
```

Also enforce tenant-scoped case-insensitive name uniqueness using an approved PostgreSQL approach, preferably a functional unique index equivalent to:

```sql
CREATE UNIQUE INDEX ...
ON product_categories (tenant_id, LOWER(name));
```

Required non-null fields:

```text
id
tenant_id
code
name
active
required audit columns
```

### 13.3 Indexes

Add useful indexes for:

- tenant listing
- tenant + active filtering
- tenant + code lookup
- tenant + case-insensitive name lookup

Avoid redundant indexes.

---

## 14. DTOs

Create:

```text
CreateProductCategoryRequest
UpdateProductCategoryRequest
UpdateProductCategoryStatusRequest
ProductCategoryResponse
```

### 14.1 CreateProductCategoryRequest

Fields:

```text
code
name
description
```

Must not include:

```text
id
tenantId
tenant
active
audit fields
```

### 14.2 UpdateProductCategoryRequest

Fields:

```text
name
description
```

Must not include:

```text
code
tenant
tenantId
active
audit fields
```

### 14.3 UpdateProductCategoryStatusRequest

Contains only:

```json
{
    "active": false
}
```

Active must be non-null.

### 14.4 ProductCategoryResponse

Return:

```text
id
code
name
description
active
permitted audit fields
```

Do not expose Tenant.

---

## 15. MapStruct

Create the category mapper using existing MapStruct configuration.

Support:

```text
CreateProductCategoryRequest → ProductCategory
ProductCategory → ProductCategoryResponse
UpdateProductCategoryRequest → existing ProductCategory
```

Request mapping must ignore:

```text
id
tenant
active
audit fields
```

Update mapping must preserve:

```text
id
tenant
code
active
audit fields
```

---

## 16. Repository

Create:

```text
ProductCategoryRepository
```

All specific-resource access must be tenant-scoped.

Expected equivalents:

```java
Optional<ProductCategory> findByIdAndTenantId(
        UUID categoryId,
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

For update validation, support excluding the current category.

Listing must support:

- Tenant scope
- Optional search
- Optional active filter
- Pagination
- Safe sorting

Search should match:

```text
code
name
description
```

Search must be case-insensitive.

Do not load all records and filter in Java.

Do not use unrestricted `findById`.

---

## 17. Service Layer

Create the service interface and implementation following existing project conventions.

Expected operations:

```java
ProductCategoryResponse createCategory(
        CreateProductCategoryRequest request);
```

```java
PageResponse<ProductCategoryResponse> getCategories(
        String search,
        Boolean active,
        Pageable pageable);
```

```java
ProductCategoryResponse getCategory(UUID categoryId);
```

```java
ProductCategoryResponse updateCategory(
        UUID categoryId,
        UpdateProductCategoryRequest request);
```

```java
ProductCategoryResponse updateCategoryStatus(
        UUID categoryId,
        UpdateProductCategoryStatusRequest request);
```

### 17.1 Create behavior

The service must:

1. Resolve current tenant.
2. Trim and normalize fields.
3. Check tenant-scoped duplicate code.
4. Check tenant-scoped duplicate name.
5. Map the request.
6. Assign the trusted Tenant.
7. Set active to true.
8. Persist.
9. Return response.

### 17.2 List behavior

The service must:

1. Resolve current tenant ID.
2. Normalize search input.
3. Query only current-tenant categories.
4. Apply optional active filtering.
5. Apply pagination.
6. Apply safe sorting.
7. Map to the project pagination response.

### 17.3 Get behavior

Load using:

```text
categoryId + tenantId
```

Return not found for unknown or cross-tenant categories.

### 17.4 Update behavior

The service must:

1. Resolve tenant ID.
2. Load category using tenant-scoped lookup.
3. Normalize fields.
4. Validate duplicate name excluding current category.
5. Update editable fields.
6. Preserve code.
7. Preserve tenant ownership.
8. Preserve active status.
9. Return response.

### 17.5 Status behavior

Change only active status.

Support:

```text
active → inactive
inactive → active
```

Same-state operations should be idempotent.

### 17.6 Transactions

Use write transactions for:

```text
create
update
status change
```

Use read-only transactions for:

```text
list
get
```

---

## 18. Duplicate Handling

Service-level checks should provide friendly conflict responses.

Database constraints remain the final authority.

Duplicate code or name must return:

```http
409 Conflict
```

Concurrent constraint violations must also be converted into the existing conflict response format.

Do not expose raw database exception information.

---

## 19. REST API

Base path:

```http
/api/v1/product-categories
```

Required endpoints:

```http
POST  /api/v1/product-categories
GET   /api/v1/product-categories
GET   /api/v1/product-categories/{categoryId}
PUT   /api/v1/product-categories/{categoryId}
PATCH /api/v1/product-categories/{categoryId}/status
```

### List query parameters

Support:

```text
page
size
sort
search
active
```

Suggested safe sorting:

```text
code
name
active
createdAt
updatedAt
```

---

## 20. Authorization

Reuse existing RBAC.

Conceptually:

| Operation       | Access                                     |
| --------------- | ------------------------------------------ |
| Create category | Manager/Admin or appropriate existing role |
| List categories | Authorized tenant user                     |
| Get category    | Authorized tenant user                     |
| Update category | Manager/Admin                              |
| Change status   | Manager/Admin                              |

Use actual project roles.

Tenant isolation remains mandatory independently of roles.

---

## 21. Error Handling

Reuse global exception handling.

Expected statuses:

| Situation                       | Status |
| ------------------------------- | -----: |
| Invalid request                 |    400 |
| Invalid UUID                    |    400 |
| Missing authentication          |    401 |
| Insufficient authorization      |    403 |
| Unknown/current-tenant mismatch |    404 |
| Duplicate code                  |    409 |
| Duplicate name                  |    409 |
| Created                         |    201 |
| Success                         |    200 |

---

## 22. OpenAPI

Document all category APIs.

Include:

- summaries
- descriptions
- JWT security
- authorization
- path parameters
- pagination parameters
- search behavior
- active filtering
- example requests
- success responses
- validation errors
- authentication errors
- authorization errors
- not-found responses
- conflict responses

---

## 23. Automated Tests

Follow `AGENTS.md`.

### 23.1 Service tests

Cover at minimum:

1. Creates category successfully.
2. Uses authenticated tenant.
3. Normalizes code.
4. Trims name.
5. Defaults active to true.
6. Rejects duplicate code in same tenant.
7. Allows same code in another tenant.
8. Rejects case-insensitive duplicate name in same tenant.
9. Allows same name in another tenant.
10. Gets tenant-owned category.
11. Cross-tenant get returns not found.
12. Updates name.
13. Updates description.
14. Preserves code.
15. Preserves tenant.
16. Preserves active status.
17. Rejects duplicate name during update.
18. Deactivates.
19. Reactivates.
20. Handles idempotent status.
21. Paginates.
22. Searches.
23. Filters active.
24. Handles blank search.
25. Handles safe sorting.

### 23.2 Mapper tests

Verify:

- request mapping
- tenant ignored
- ID ignored
- active ignored
- response mapping
- update preserves code
- update preserves tenant
- update preserves active
- audit fields preserved

### 23.3 Repository tests

Cover:

1. Persists valid category.
2. Rejects missing tenant.
3. Finds by ID and tenant.
4. Rejects cross-tenant lookup.
5. Enforces tenant/code uniqueness.
6. Allows same code across tenants.
7. Enforces tenant/name case-insensitive uniqueness.
8. Allows same name across tenants.
9. Lists only tenant categories.
10. Filters active.
11. Filters inactive.
12. Searches by code.
13. Searches by name.
14. Searches by description.
15. Paginates.
16. Sorts.
17. Enforces foreign key.
18. Deleting category does not delete tenant.

### 23.4 Controller/integration tests

Cover:

1. Authorized create.
2. `201 Created`.
3. Tenant cannot be assigned by request.
4. Missing code returns `400`.
5. Invalid code returns `400`.
6. Missing name returns `400`.
7. Duplicate code returns `409`.
8. Duplicate name returns `409`.
9. Unauthenticated request returns `401`.
10. Unauthorized write returns `403`.
11. Authorized list.
12. Tenant-scoped list.
13. Pagination.
14. Search.
15. Active filtering.
16. Authorized get.
17. Unknown category returns `404`.
18. Cross-tenant get returns `404`.
19. Authorized update.
20. Update preserves code.
21. Update preserves status.
22. Cross-tenant update returns `404`.
23. Authorized deactivate.
24. Authorized reactivate.
25. Unauthorized status update returns `403`.
26. Cross-tenant status returns `404`.

### 23.5 Migration tests

Verify:

- table exists
- tenant foreign key exists
- tenant ID non-null
- code unique constraint exists
- case-insensitive name unique index exists
- active default works
- audit fields exist
- Flyway validation succeeds

---

## 24. Acceptance Criteria

### Persistence

- [ ] ProductCategory entity exists.
- [ ] Extends `BaseEntity`.
- [ ] Tenant ownership is mandatory.
- [ ] Code is required.
- [ ] Code is uppercase-normalized.
- [ ] Code is tenant-scoped unique.
- [ ] Code is immutable.
- [ ] Name is required.
- [ ] Name is tenant-scoped case-insensitive unique.
- [ ] Same code/name may exist in another tenant.
- [ ] Active defaults to true.
- [ ] Migration creates required schema and indexes.

### API

- [ ] Create works.
- [ ] List works.
- [ ] Pagination works.
- [ ] Search works.
- [ ] Active filtering works.
- [ ] Safe sorting works.
- [ ] Get works.
- [ ] Update works.
- [ ] Deactivate works.
- [ ] Reactivate works.
- [ ] No DELETE endpoint exists.

### Security

- [ ] Tenant comes from trusted context.
- [ ] Request contains no tenant ID.
- [ ] Tenant-scoped repository access is used.
- [ ] Cross-tenant get returns `404`.
- [ ] Cross-tenant update returns `404`.
- [ ] Cross-tenant status change returns `404`.
- [ ] RBAC cannot bypass tenant isolation.

### Quality

- [ ] Unit tests pass.
- [ ] Repository tests pass.
- [ ] Integration tests pass.
- [ ] Tenant-isolation tests pass.
- [ ] Migration tests pass.
- [ ] Existing Story 10 tests remain green.
- [ ] Existing Story 9 tests remain green.
- [ ] Existing Story 8 tests remain green.
- [ ] Existing Story 7A tests remain green.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verify passes.
- [ ] OpenAPI documentation is correct.

---

## 25. Beginner-Friendly Manual QA

### Create category

```http
POST /api/v1/product-categories
```

```json
{
    "code": "beverages",
    "name": "Beverages",
    "description": "Drinks and beverage products"
}
```

Expected:

```text
201 Created
code = BEVERAGES
active = true
```

### Duplicate code

Create another category using:

```text
BEVERAGES
```

Expected:

```text
409 Conflict
```

### Duplicate name

Create:

```json
{
    "code": "DRINKS",
    "name": "beverages"
}
```

Expected:

```text
409 Conflict
```

### Create second category

```json
{
    "code": "ELECTRONICS",
    "name": "Electronics"
}
```

Expected:

```text
201 Created
```

### List categories

```http
GET /api/v1/product-categories?page=0&size=10&sort=name,asc
```

Expected:

- paginated response
- sorted categories
- current-tenant categories only

### Search

```http
GET /api/v1/product-categories?search=bev
```

Expected:

- Beverages returned

### Update

```http
PUT /api/v1/product-categories/{id}
```

```json
{
    "name": "Soft Drinks & Beverages",
    "description": "Beverages and related products"
}
```

Expected:

- name updated
- code unchanged
- tenant unchanged
- active unchanged

### Deactivate

```http
PATCH /api/v1/product-categories/{id}/status
```

```json
{
    "active": false
}
```

Expected:

- category remains retrievable
- active becomes false

### Tenant isolation

1. Create category in Tenant A.
2. Authenticate as Tenant B.
3. Attempt get/update/status using Tenant A UUID.

Expected:

```text
404 Not Found
```

### Same category in another tenant

As Tenant B create:

```json
{
    "code": "BEVERAGES",
    "name": "Beverages"
}
```

Expected:

```text
201 Created
```

### Quality checks

Run:

```bash
./mvnw test
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw verify
```

All must pass.

---

## 26. Definition of Done

Story 11 is complete only when:

- [ ] `AGENTS.md` was followed.
- [ ] Existing tenant architecture was reused.
- [ ] ProductCategory entity exists.
- [ ] Flyway migration exists.
- [ ] DTOs exist.
- [ ] Mapper exists.
- [ ] Repository exists.
- [ ] Service exists.
- [ ] Controller exists.
- [ ] Tenant isolation works.
- [ ] RBAC works.
- [ ] Validation works.
- [ ] Duplicate code handling works.
- [ ] Duplicate name handling works.
- [ ] Search works.
- [ ] Pagination works.
- [ ] Safe sorting works.
- [ ] Active filtering works.
- [ ] Update works.
- [ ] Deactivation works.
- [ ] Reactivation works.
- [ ] No hierarchy was introduced.
- [ ] No hard deletion was introduced.
- [ ] OpenAPI is complete.
- [ ] Automated tests pass.
- [ ] Existing tests remain green.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verify passes.
- [ ] Manual QA is completed.
- [ ] Story 12 was not implemented.

---

## 27. Implementation Guardrails

Do not:

- Implement Product Management.
- Implement nested categories.
- Add `parentCategory`.
- Add recursive tree APIs.
- Implement Inventory.
- Implement Purchases.
- Implement Sales.
- Add category images.
- Add branch ownership.
- Add hard deletion.
- Accept tenant IDs from clients.
- Use unrestricted `findById`.
- Expose JPA entities.
- Put business logic in controllers.
- Disable security in tests.
- Edit previous Flyway migrations.
- Depend on Hibernate schema generation.
- Rewrite unrelated modules.
- Suppress quality failures.

---

## 28. Approval Gate

Do not implement Story 11 until this specification is explicitly approved.

After approval, generate the dedicated Codex prompt beginning with:

```text
Implement Story 11 — Product Categories
```
