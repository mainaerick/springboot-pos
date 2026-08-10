# Story 10 — Supplier Management

## Document Status

- **Sprint:** Sprint 2
- **Story:** 10
- **Module:** Supplier Management
- **Status:** Proposed
- **Implementation status:** Not started
- **Approval required before implementation:** Yes
- **Target path:** `docs/prompts/story-10-supplier-management.md`

---

## 1. Purpose

Implement tenant-owned Supplier Management for the Inventory and Point of Sale SaaS.

A supplier represents a business or individual from whom the tenant purchases products, materials, or services.

This story establishes the supplier master-data foundation required by future Purchase Orders, Purchases, Goods Receipts, Supplier Returns, Supplier Payments, Accounts Payable, Inventory, and Reporting modules.

---

## 2. Mandatory Project Rules

The implementation must comply with the repository’s root `AGENTS.md`.

Before implementation, inspect:

1. `AGENTS.md`
2. Story 7A — Tenant Foundation
3. Story 8 — Branch Management
4. Story 9 — Customer Management
5. `BaseEntity`
6. JPA auditing
7. Tenant entity and current-tenant provider
8. Authenticated principal
9. Existing RBAC roles and method security
10. DTO and validation conventions
11. MapStruct configuration
12. Repository query conventions
13. Pagination response conventions
14. Safe sorting implementation
15. Global exception handling
16. OpenAPI conventions
17. Flyway migration numbering
18. Testcontainers and integration-test infrastructure
19. Existing test fixtures and naming conventions

Reuse established Customer and Branch module patterns where appropriate.

Do not introduce a second tenant abstraction, pagination model, exception format, or security pattern.

---

## 3. User Story

As an authorized tenant user, I want to create and manage supplier records so that future purchases, goods receipts, returns, payments, and reports can be associated with the correct supplier.

---

## 4. Business Value

Supplier Management enables:

- Supplier-linked purchases
- Purchase orders
- Goods receipts
- Supplier returns
- Supplier pricing
- Supplier payment history
- Accounts payable
- Procurement reports
- Supplier performance analysis
- Inventory replenishment

This story implements supplier master data only.

---

## 5. Ownership Model

Every supplier must belong to exactly one tenant.

Suppliers are tenant-owned, not branch-owned.

A supplier may provide goods to multiple branches within the same tenant.

The model must follow:

```text
Tenant
├── Branches
├── Customers
└── Suppliers
```

Future purchase records will reference:

```text
supplier_id
branch_id
```

Supplier request DTOs must not accept:

- tenant ID
- tenant object
- branch ID as supplier ownership
- arbitrary ownership headers

Tenant ownership must come from the trusted authenticated tenant context created in Story 7A.

---

## 6. Scope

### 6.1 Included

Implement:

- Supplier entity
- Tenant-to-Supplier relationship
- Flyway migration
- Create supplier endpoint
- Paginated supplier-list endpoint
- Get supplier by UUID endpoint
- Update supplier endpoint
- Activate/deactivate supplier endpoint
- Request and response DTOs
- Jakarta Bean Validation
- MapStruct mapper
- Supplier repository
- Supplier service
- Tenant-scoped queries
- Tenant isolation
- Search
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

- Purchases
- Purchase orders
- Goods receipts
- Supplier invoices
- Supplier payments
- Accounts payable
- Supplier balances
- Supplier credit terms
- Supplier price lists
- Product-supplier associations
- Preferred suppliers
- Supplier rankings
- Supplier contracts
- Attachments
- Multiple supplier contacts
- Multiple addresses
- Supplier returns
- Supplier imports or exports
- Supplier merging
- Branch ownership
- Hard deletion
- Product Categories
- Product Management
- Inventory
- Sales
- Reports

---

## 7. Supplier Entity

Create a `Supplier` entity in the appropriate package-by-feature location.

The entity must extend the existing `BaseEntity`.

### 7.1 Fields

| Field          | Type               | Required | Rules                               |
| -------------- | ------------------ | -------: | ----------------------------------- |
| tenant         | Tenant             |      Yes | Trusted mandatory ownership         |
| code           | String             |      Yes | Uppercase, tenant-scoped unique     |
| name           | String             |      Yes | Trimmed, maximum 150 characters     |
| contactPerson  | String             |       No | Maximum 150 characters              |
| email          | String             |       No | Valid email, maximum 254 characters |
| phone          | String             |       No | Maximum 30 characters               |
| alternatePhone | String             |       No | Maximum 30 characters               |
| taxNumber      | String             |       No | Maximum 50 characters               |
| addressLine1   | String             |       No | Maximum 200 characters              |
| addressLine2   | String             |       No | Maximum 200 characters              |
| city           | String             |       No | Maximum 100 characters              |
| stateOrCounty  | String             |       No | Maximum 100 characters              |
| postalCode     | String             |       No | Maximum 30 characters               |
| countryCode    | String             |       No | Exactly two uppercase letters       |
| notes          | String             |       No | Maximum 1000 characters             |
| active         | Boolean or boolean |      Yes | Defaults to `true`                  |
| audit fields   | Existing types     |      Yes | Inherited from `BaseEntity`         |

Follow existing primitive/wrapper conventions.

### 7.2 Tenant relationship

Use the Tenant entity created in Story 7A.

Implement the project equivalent of:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

Requirements:

- Every supplier belongs to one tenant.
- One tenant may have many suppliers.
- Supplier deletion must not delete the tenant.
- Tenant persistence must not cascade through Supplier.
- Tenant ownership must not participate in `equals`, `hashCode`, or `toString`.
- Tenant ownership must not be assigned through request DTO mapping.
- Do not expose the Tenant entity through the Supplier API response.

---

## 8. Supplier Code Rules

The supplier code must:

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

Use this validation pattern or equivalent:

```regex
^[A-Z0-9][A-Z0-9_-]*$
```

Example:

```text
Input:  sup-0001
Stored: SUP-0001
```

Do not include `code` in `UpdateSupplierRequest`.

Automatic supplier-code generation is outside this story.

---

## 9. Supplier Name Rules

Supplier name must:

- Be required
- Be trimmed
- Not contain only whitespace
- Have a maximum length of 150 characters

Supplier name is not unique.

Multiple suppliers may legitimately have the same or similar names.

The UUID and supplier code distinguish supplier records.

---

## 10. Contact Information

### Contact person

When provided:

- Trim whitespace
- Maximum 150 characters

This represents the primary person contacted at the supplier.

Do not create a separate Supplier Contact entity in this story.

### Email

When provided:

- Trim whitespace
- Validate using the project’s standard email validation
- Maximum 254 characters
- Normalize to lowercase if this matches existing Customer Management behavior

Email is not unique.

### Phone numbers

When provided:

- Trim whitespace
- Maximum 30 characters
- Preserve useful international formatting such as `+254712345678`

Do not introduce full international telephone parsing unless already established.

Phone numbers are not unique.

### Tax number

When provided:

- Trim whitespace
- Maximum 50 characters
- Normalize case if consistent with Customer Management

Tax number is not unique in this story.

---

## 11. Address Information

Address fields are optional.

When provided:

- Trim surrounding whitespace
- Enforce maximum lengths
- Normalize country code to uppercase
- Validate country code as exactly two alphabetic characters

Do not create a separate address entity.

Do not support multiple supplier addresses in this story.

---

## 12. Active Status

New suppliers must default to active.

`CreateSupplierRequest` must not accept active status.

`UpdateSupplierRequest` must not change active status.

Status changes must use:

```http
PATCH /api/v1/suppliers/{supplierId}/status
```

Inactive suppliers:

- Remain retrievable
- Remain available for historical purchases and reports
- Can be reactivated
- Must not be physically deleted
- Should later be prevented from being selected for new purchases

The purchase-selection rule will be implemented in the Purchase story.

Do not expose a hard-delete endpoint.

---

## 13. Database Migration

Create the next correctly numbered Flyway migration.

Do not modify already-applied migrations.

Create a `suppliers` table according to existing database naming conventions.

### 13.1 Required database elements

Include:

- UUID primary key
- mandatory `tenant_id`
- supplier code
- supplier name
- optional contact fields
- optional address fields
- notes
- active status
- audit columns compatible with `BaseEntity`
- foreign key to tenants
- tenant-scoped code uniqueness
- useful indexes

### 13.2 Required constraints

At minimum:

```sql
UNIQUE (tenant_id, code)
```

Required non-null columns:

```text
id
tenant_id
code
name
active
required audit columns
```

### 13.3 Recommended indexes

Add useful indexes for:

- Tenant-scoped supplier listing
- Tenant and active filtering
- Tenant and code lookup
- Case-insensitive supplier-name search

Avoid redundant indexes already covered by the unique tenant/code constraint.

Do not rely on Hibernate schema generation.

---

## 14. DTOs

Create:

```text
CreateSupplierRequest
UpdateSupplierRequest
UpdateSupplierStatusRequest
SupplierResponse
```

Follow existing DTO class or record conventions.

### 14.1 CreateSupplierRequest

Fields:

```text
code
name
contactPerson
email
phone
alternatePhone
taxNumber
addressLine1
addressLine2
city
stateOrCounty
postalCode
countryCode
notes
```

Must not include:

```text
id
tenantId
tenant
branchId
active
audit fields
soft-delete fields
```

### 14.2 UpdateSupplierRequest

Editable fields:

```text
name
contactPerson
email
phone
alternatePhone
taxNumber
addressLine1
addressLine2
city
stateOrCounty
postalCode
countryCode
notes
```

Must not include:

```text
code
tenantId
tenant
active
audit fields
```

### 14.3 UpdateSupplierStatusRequest

Contains only:

```json
{
    "active": false
}
```

The active value must be required and non-null.

### 14.4 SupplierResponse

Return:

```text
id
code
name
contactPerson
email
phone
alternatePhone
taxNumber
address fields
countryCode
notes
active
permitted audit fields
```

Do not expose:

```text
Tenant entity
Hibernate relationships
Authentication data
Internal deletion metadata
```

Omit tenant ID unless existing public API conventions require it.

---

## 15. MapStruct

Create a Supplier mapper using the established MapStruct configuration.

Support:

```text
CreateSupplierRequest → Supplier
Supplier → SupplierResponse
UpdateSupplierRequest → existing Supplier
```

Request mappings must ignore:

```text
id
tenant
active
audit fields
soft-delete fields
```

Update mapping must preserve:

```text
id
tenant
code
active
audit fields
```

Use explicit MapStruct ignore declarations where necessary.

Do not use reflection-based property copying.

---

## 16. Supplier Repository

Create:

```text
SupplierRepository
```

using the existing repository base type.

All resource access must be tenant-scoped.

Expected equivalents:

```java
Optional<Supplier> findByIdAndTenantId(
        UUID supplierId,
        UUID tenantId);
```

```java
boolean existsByTenantIdAndCodeIgnoreCase(
        UUID tenantId,
        String code);
```

Use entity-association equivalents when required by the Tenant mapping.

Listing must support:

- Tenant scope
- Optional search
- Optional active status
- Pagination
- Safe sorting

Search must be case-insensitive and match:

```text
code
name
contactPerson
email
phone
alternatePhone
taxNumber
```

Null optional fields must not break search.

Use the established project approach, such as:

- JPA Specifications
- Criteria API
- custom JPQL
- derived queries
- custom repository implementation

Do not load all suppliers and filter in Java memory.

Do not use unrestricted `findById`.

---

## 17. Supplier Service

Create the service interface and implementation following existing conventions.

Expected operations are equivalent to:

```java
SupplierResponse createSupplier(CreateSupplierRequest request);
```

```java
PageResponse<SupplierResponse> getSuppliers(
        String search,
        Boolean active,
        Pageable pageable);
```

```java
SupplierResponse getSupplier(UUID supplierId);
```

```java
SupplierResponse updateSupplier(
        UUID supplierId,
        UpdateSupplierRequest request);
```

```java
SupplierResponse updateSupplierStatus(
        UUID supplierId,
        UpdateSupplierStatusRequest request);
```

Use the project’s existing pagination wrapper.

### 17.1 Create behavior

The service must:

1. Resolve the authenticated tenant.
2. Normalize supplier fields.
3. Check tenant-scoped supplier-code uniqueness.
4. Map the request to a new Supplier.
5. Assign the trusted Tenant.
6. Set active to true.
7. Persist the supplier.
8. Return the response.

### 17.2 List behavior

The service must:

1. Resolve the authenticated tenant ID.
2. Normalize optional search input.
3. Query only suppliers belonging to the tenant.
4. Apply optional active filtering.
5. Apply pagination.
6. Apply safe sorting.
7. Map the result to the project page response.

When `active` is absent, return active and inactive suppliers.

### 17.3 Get behavior

Load using:

```text
supplier ID + current tenant ID
```

Return not found for:

- Unknown supplier ID
- Supplier owned by another tenant

### 17.4 Update behavior

The service must:

1. Resolve the current tenant ID.
2. Load the supplier using tenant-scoped lookup.
3. Normalize editable fields.
4. Apply updates.
5. Preserve tenant ownership.
6. Preserve immutable supplier code.
7. Preserve active status.
8. Return the updated response.

### 17.5 Status behavior

The service must:

1. Resolve the current tenant ID.
2. Load supplier using tenant-scoped lookup.
3. Change only active status.
4. Support activation and deactivation.
5. Treat repeated same-status requests as idempotent success.
6. Return the updated response.

### 17.6 Transactions

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

Follow existing annotation-placement conventions.

---

## 18. Normalization

Apply normalization consistently with Customer Management.

Recommended behavior:

| Field          | Normalization                            |
| -------------- | ---------------------------------------- |
| code           | Trim and uppercase                       |
| name           | Trim                                     |
| contactPerson  | Trim                                     |
| email          | Trim and lowercase if established        |
| phone          | Trim                                     |
| alternatePhone | Trim                                     |
| taxNumber      | Trim and optionally uppercase            |
| address fields | Trim                                     |
| countryCode    | Trim and uppercase                       |
| notes          | Trim if consistent with existing modules |

Convert blank optional values to `null` if this is the established project convention.

Avoid storing meaningless empty strings when existing modules use null normalization.

---

## 19. Duplicate and Race-Condition Handling

Perform service-level duplicate-code validation to return a friendly conflict response.

The database tenant/code unique constraint remains the final authority.

Translate concurrent database uniqueness violations into:

```http
409 Conflict
```

Do not expose raw SQL, Hibernate, constraint, or PostgreSQL error details.

Do not reject duplicate names, emails, phone numbers, alternate phone numbers, or tax numbers.

---

## 20. REST API

Base path:

```http
/api/v1/suppliers
```

Required endpoints:

```http
POST  /api/v1/suppliers
GET   /api/v1/suppliers
GET   /api/v1/suppliers/{supplierId}
PUT   /api/v1/suppliers/{supplierId}
PATCH /api/v1/suppliers/{supplierId}/status
```

### 20.1 Create

```http
POST /api/v1/suppliers
```

Return:

```http
201 Created
```

Include a `Location` header if consistent with project conventions.

### 20.2 List

```http
GET /api/v1/suppliers
```

Support:

```text
page
size
sort
search
active
```

Suggested safe sorting fields:

```text
code
name
contactPerson
city
active
createdAt
updatedAt
```

Unsupported sorting fields must be rejected or safely mapped according to the existing project convention.

### 20.3 Get by ID

```http
GET /api/v1/suppliers/{supplierId}
```

Return:

- `200 OK` for a supplier owned by the current tenant
- `404 Not Found` for an unknown or cross-tenant supplier

### 20.4 Update

```http
PUT /api/v1/suppliers/{supplierId}
```

Return:

```http
200 OK
```

Code, tenant ownership, and active status must remain unchanged.

### 20.5 Status update

```http
PATCH /api/v1/suppliers/{supplierId}/status
```

Return:

```http
200 OK
```

Do not add a DELETE endpoint.

---

## 21. Authorization

Reuse the exact roles and authorization conventions already present in the project.

Conceptual access:

| Operation       | Conceptual access                                      |
| --------------- | ------------------------------------------------------ |
| Create supplier | Authorized procurement user, manager, or administrator |
| List suppliers  | Authorized tenant user                                 |
| Get supplier    | Authorized tenant user                                 |
| Update supplier | Authorized procurement user, manager, or administrator |
| Change status   | Manager or administrator                               |

Map these concepts to existing roles.

Do not introduce a new permission architecture solely for Supplier Management.

Tenant isolation must be enforced independently of RBAC.

A user with an administrator role in Tenant A must not access Tenant B suppliers.

---

## 22. Exception Handling

Reuse existing global exception handling.

Expected statuses:

| Situation                              | Status |
| -------------------------------------- | -----: |
| Invalid request                        |    400 |
| Invalid UUID                           |    400 |
| Missing or invalid authentication      |    401 |
| Insufficient role                      |    403 |
| Supplier unavailable in current tenant |    404 |
| Cross-tenant supplier access           |    404 |
| Duplicate supplier code                |    409 |
| Created                                |    201 |
| Successful list/get/update/status      |    200 |

Do not create supplier-specific error formats.

Do not expose internal database details.

---

## 23. OpenAPI Documentation

Document every Supplier endpoint using existing OpenAPI conventions.

Include:

- Endpoint summaries
- Endpoint descriptions
- JWT bearer security
- Authorization expectations
- Path parameters
- Query parameters
- Pagination behavior
- Searchable fields
- Active filter
- Safe sorting
- Request examples
- Success responses
- Validation errors
- Authentication errors
- Authorization errors
- Not-found errors
- Conflict errors

Confirm the endpoints appear correctly in Swagger UI.

---

## 24. Automated Tests

Follow `AGENTS.md` and established test conventions.

Do not disable Spring Security.

### 24.1 Service unit tests

Cover at minimum:

1. Creates a supplier successfully.
2. Uses the authenticated tenant.
3. Does not accept tenant ownership from request data.
4. Trims supplier name.
5. Normalizes supplier code.
6. Normalizes country code.
7. Normalizes optional fields according to project conventions.
8. Defaults active to true.
9. Rejects duplicate code in the same tenant.
10. Allows the same code in another tenant.
11. Allows duplicate supplier names.
12. Allows duplicate emails.
13. Allows duplicate phones.
14. Allows duplicate tax numbers.
15. Returns a tenant-owned supplier.
16. Returns not found for an unknown supplier.
17. Returns not found for another tenant’s supplier.
18. Updates editable fields.
19. Preserves tenant ownership.
20. Preserves supplier code.
21. Preserves active status.
22. Deactivates a supplier.
23. Reactivates a supplier.
24. Handles an idempotent status change.
25. Returns paginated suppliers.
26. Applies case-insensitive search.
27. Applies active filtering.
28. Handles blank search.
29. Handles unsupported sorting according to project conventions.

### 24.2 Mapper tests

Verify:

1. Create request maps editable fields.
2. Tenant is ignored.
3. ID is ignored.
4. Active is ignored.
5. Audit fields are ignored.
6. Supplier maps correctly to response.
7. Update preserves tenant.
8. Update preserves ID.
9. Update preserves code.
10. Update preserves active status.
11. Update preserves audit fields.

### 24.3 Repository tests

Cover at minimum:

1. Persists a valid supplier.
2. Rejects a supplier without a tenant.
3. Finds supplier by ID and tenant.
4. Does not find supplier under another tenant.
5. Detects duplicate supplier code in one tenant.
6. Allows the same code in separate tenants.
7. Allows duplicate names.
8. Allows duplicate emails.
9. Allows duplicate phone numbers.
10. Allows duplicate tax numbers.
11. Lists only tenant-owned suppliers.
12. Filters active suppliers.
13. Filters inactive suppliers.
14. Searches by code.
15. Searches by name.
16. Searches by contact person.
17. Searches by email.
18. Searches by phone.
19. Searches by alternate phone.
20. Searches by tax number.
21. Applies pagination.
22. Applies safe sorting.
23. Enforces required fields.
24. Enforces tenant foreign key.
25. Enforces tenant/code uniqueness.
26. Deleting a supplier does not delete the tenant.

### 24.4 Controller or integration tests

Cover at minimum:

1. Authorized user creates a supplier.
2. Create returns `201 Created`.
3. Request cannot assign tenant ID.
4. Missing code returns `400`.
5. Invalid code returns `400`.
6. Missing name returns `400`.
7. Invalid email returns `400`.
8. Invalid country code returns `400`.
9. Duplicate code returns `409`.
10. Duplicate name is allowed.
11. Duplicate email is allowed.
12. Duplicate phone is allowed.
13. Duplicate tax number is allowed.
14. Unauthenticated create returns `401`.
15. Unauthorized create returns `403`.
16. Authorized user lists suppliers.
17. List is tenant-scoped.
18. List is paginated.
19. Search works for all supported fields.
20. Active filter works.
21. Authorized user retrieves a supplier.
22. Unknown supplier returns `404`.
23. Cross-tenant get returns `404`.
24. Authorized user updates a supplier.
25. Update preserves code.
26. Update preserves active status.
27. Cross-tenant update returns `404`.
28. Authorized manager deactivates a supplier.
29. Authorized manager reactivates a supplier.
30. Unauthorized status change returns `403`.
31. Cross-tenant status change returns `404`.
32. Response and error structures match existing conventions.

### 24.5 Migration tests

Verify:

1. Migration succeeds on clean PostgreSQL.
2. `suppliers` table exists.
3. Tenant ID is non-null.
4. Tenant foreign key exists.
5. Active defaults correctly.
6. Tenant/code unique constraint exists.
7. Required indexes exist.
8. Audit columns match `BaseEntity`.
9. Column lengths and nullability match the entity.
10. Flyway validation succeeds.

---

## 25. Acceptance Criteria

### Persistence

- [ ] Supplier entity exists.
- [ ] Supplier extends `BaseEntity`.
- [ ] Every supplier belongs to one tenant.
- [ ] Supplier code is required.
- [ ] Supplier code is normalized to uppercase.
- [ ] Supplier code is unique within a tenant.
- [ ] Supplier code is immutable.
- [ ] The same code is allowed in different tenants.
- [ ] Supplier name is required.
- [ ] Duplicate supplier names are allowed.
- [ ] Email is optional and not unique.
- [ ] Phone is optional and not unique.
- [ ] Tax number is optional and not unique.
- [ ] Country code is normalized.
- [ ] Active defaults to true.
- [ ] Flyway creates the supplier schema.
- [ ] Required constraints and indexes exist.

### API

- [ ] Authorized users can create suppliers.
- [ ] Authorized users can list suppliers.
- [ ] Listing is paginated.
- [ ] Listing supports search.
- [ ] Listing supports active filtering.
- [ ] Listing supports safe sorting.
- [ ] Authorized users can retrieve suppliers.
- [ ] Authorized users can update suppliers.
- [ ] Authorized managers can deactivate suppliers.
- [ ] Authorized managers can reactivate suppliers.
- [ ] No DELETE endpoint exists.

### Tenant security

- [ ] Tenant identity comes from trusted authenticated context.
- [ ] Request DTOs contain no tenant ID.
- [ ] Repository queries are tenant-scoped.
- [ ] Service operations are tenant-scoped.
- [ ] Tenant A cannot retrieve Tenant B suppliers.
- [ ] Tenant A cannot update Tenant B suppliers.
- [ ] Tenant A cannot change Tenant B supplier status.
- [ ] Cross-tenant IDs return `404`.
- [ ] RBAC does not bypass tenant isolation.

### Architecture

- [ ] Package-by-feature is followed.
- [ ] Controllers contain no business logic.
- [ ] Services contain business rules.
- [ ] Repositories handle persistence.
- [ ] DTOs isolate the API contract.
- [ ] MapStruct handles mapping.
- [ ] Existing tenant provider is reused.
- [ ] Global exception handling is reused.
- [ ] Transactions are correctly applied.
- [ ] `AGENTS.md` is followed.

### Quality

- [ ] Service tests pass.
- [ ] Mapper tests pass where required.
- [ ] Repository tests pass.
- [ ] API integration tests pass.
- [ ] Security tests pass.
- [ ] Tenant-isolation tests pass.
- [ ] Migration tests pass.
- [ ] Existing Story 9 tests remain green.
- [ ] Existing Story 8 tests remain green.
- [ ] Existing Story 7A tests remain green.
- [ ] Existing Sprint 1 tests remain green.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verification passes.
- [ ] OpenAPI documentation is correct.

---

## 26. Beginner-Friendly Manual QA

### Test 1 — Start the application

1. Start PostgreSQL and all required Docker services.
2. Start the Spring Boot application.
3. Confirm Flyway completes successfully.
4. Open Swagger UI.
5. Authenticate as an authorized tenant user.

Expected:

- Application starts.
- Supplier endpoints appear.
- JWT authentication works.

### Test 2 — Create a supplier

Call:

```http
POST /api/v1/suppliers
```

Request:

```json
{
    "code": "sup-0001",
    "name": "Nairobi Medical Supplies Ltd",
    "contactPerson": "Mary Wambui",
    "email": "sales@nairobimedical.example.com",
    "phone": "+254712345678",
    "alternatePhone": "+254733333333",
    "taxNumber": "P051234567X",
    "addressLine1": "Enterprise Road",
    "city": "Nairobi",
    "stateOrCounty": "Nairobi",
    "postalCode": "00100",
    "countryCode": "ke",
    "notes": "Primary medical consumables supplier"
}
```

Expected:

- `201 Created`
- Code is returned as `SUP-0001`
- Country code is returned as `KE`
- Active is `true`
- UUID is returned
- No tenant ID was supplied

### Test 3 — Verify tenant ownership

Inspect the supplier in PostgreSQL.

Expected:

- `tenant_id` matches the authenticated user’s tenant.
- Tenant ownership was assigned by the server.

### Test 4 — Required validation

Try requests missing:

```text
code
name
```

Expected:

- `400 Bad Request`
- Existing validation error structure is used

### Test 5 — Invalid code

Try:

```text
A
SUP 001
SUP@001
```

Expected:

- `400 Bad Request`

### Test 6 — Invalid email and country

Try invalid values such as:

```text
email: not-an-email
countryCode: KEN
```

Expected:

- `400 Bad Request`

### Test 7 — Duplicate code

Create another supplier in the same tenant with:

```text
SUP-0001
```

Expected:

- `409 Conflict`

### Test 8 — Duplicate contact details

Create another supplier with a different code but the same:

```text
name
email
phone
taxNumber
```

Expected:

- Creation succeeds
- These fields are not unique identifiers

### Test 9 — Create a second supplier

Create:

```json
{
    "code": "FRESH-FARMS",
    "name": "Fresh Farms Produce",
    "contactPerson": "Peter Kamau",
    "phone": "+254700000000",
    "city": "Limuru",
    "countryCode": "KE"
}
```

Expected:

- `201 Created`

### Test 10 — List suppliers

Call:

```http
GET /api/v1/suppliers?page=0&size=10&sort=name,asc
```

Expected:

- `200 OK`
- Pagination metadata exists
- Results are sorted
- Only current-tenant suppliers appear

### Test 11 — Search suppliers

Test:

```http
GET /api/v1/suppliers?search=medical
GET /api/v1/suppliers?search=SUP-0001
GET /api/v1/suppliers?search=Mary
GET /api/v1/suppliers?search=254712
GET /api/v1/suppliers?search=P051234567X
```

Expected:

- Correct suppliers appear for each supported search field

### Test 12 — Retrieve supplier

Call:

```http
GET /api/v1/suppliers/{supplierId}
```

Expected:

- `200 OK`
- Correct supplier is returned

### Test 13 — Update supplier

Call:

```http
PUT /api/v1/suppliers/{supplierId}
```

Update contact information, address, or notes.

Expected:

- `200 OK`
- Editable fields change
- Supplier code remains unchanged
- Tenant ownership remains unchanged
- Active status remains unchanged
- Audit modification data changes

### Test 14 — Deactivate supplier

Call:

```http
PATCH /api/v1/suppliers/{supplierId}/status
```

```json
{
    "active": false
}
```

Expected:

- `200 OK`
- Supplier remains retrievable
- Active becomes false

### Test 15 — Filter inactive suppliers

Call:

```http
GET /api/v1/suppliers?active=false
```

Expected:

- Inactive supplier appears
- Active suppliers do not appear

### Test 16 — Reactivate supplier

Send:

```json
{
    "active": true
}
```

Expected:

- Supplier becomes active

### Test 17 — Authorization

Authenticate as a user without supplier-write authorization.

Attempt create, update, and status operations.

Expected:

- Allowed reads follow RBAC rules
- Restricted writes return `403 Forbidden`

### Test 18 — Tenant isolation

1. Create a supplier in Tenant A.
2. Authenticate as Tenant B.
3. Attempt to retrieve the Tenant A supplier.
4. Attempt to update the Tenant A supplier.
5. Attempt to change the Tenant A supplier’s status.

Expected:

- All return `404 Not Found`
- No Tenant A data is exposed
- No Tenant A supplier is changed

### Test 19 — Same code in another tenant

As Tenant B, create a supplier using:

```text
SUP-0001
```

Expected:

- Creation succeeds because code uniqueness is tenant-scoped

### Test 20 — Database inspection

Confirm:

- `suppliers` table exists
- `tenant_id` is mandatory
- Foreign key exists
- Supplier code uniqueness is tenant-scoped
- Active is non-null
- Audit fields exist
- Required indexes exist

### Test 21 — Quality checks

Run:

```bash
./mvnw test
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw verify
```

Use `mvnw.cmd` where appropriate.

Expected:

- All commands pass

---

## 27. Definition of Done

Story 10 is complete only when:

- [ ] `AGENTS.md` was followed.
- [ ] Story 7A tenant architecture was reused.
- [ ] Story 8 and Story 9 patterns were reviewed.
- [ ] Supplier entity is implemented.
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
- [ ] Supplier search works.
- [ ] Active filtering works.
- [ ] Pagination works.
- [ ] Safe sorting works.
- [ ] Supplier update works.
- [ ] Deactivation works.
- [ ] Reactivation works.
- [ ] Supplier code remains immutable.
- [ ] No hard-delete endpoint exists.
- [ ] Global exception handling is reused.
- [ ] OpenAPI documentation is complete.
- [ ] Unit tests pass.
- [ ] Repository tests pass.
- [ ] API integration tests pass.
- [ ] Security tests pass.
- [ ] Tenant-isolation tests pass.
- [ ] Migration tests pass.
- [ ] Existing tests remain green.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verification passes.
- [ ] Manual QA is completed.
- [ ] No Story 11 or later functionality was implemented.

---

## 28. Implementation Guardrails

Do not:

- Implement Product Categories.
- Implement Product Management.
- Implement Inventory.
- Implement Purchases.
- Implement Sales.
- Implement supplier payments.
- Implement accounts payable.
- Implement supplier balances.
- Implement supplier price lists.
- Implement product-supplier relationships.
- Add branch ownership to Supplier.
- Add automatic supplier-code generation.
- Add hard deletion.
- Accept tenant IDs from clients.
- Accept arbitrary tenant headers.
- Expose JPA entities directly.
- Put business logic in controllers.
- Access repositories directly from controllers.
- Use unrestricted `findById`.
- Disable security in tests.
- Edit applied Flyway migrations.
- Depend on Hibernate schema generation.
- Rewrite unrelated Story 9, Story 8, Story 7A, or Sprint 1 code.
- Suppress quality violations.
- Claim tests passed when they were not run.

Keep all changes strictly limited to Story 10.

---

## 29. Expected Deliverables

Expected equivalents include:

```text
src/main/java/com/devrick/pos/supplier/entity/Supplier.java
src/main/java/com/devrick/pos/supplier/dto/CreateSupplierRequest.java
src/main/java/com/devrick/pos/supplier/dto/UpdateSupplierRequest.java
src/main/java/com/devrick/pos/supplier/dto/UpdateSupplierStatusRequest.java
src/main/java/com/devrick/pos/supplier/dto/SupplierResponse.java
src/main/java/com/devrick/pos/supplier/mapper/SupplierMapper.java
src/main/java/com/devrick/pos/supplier/repository/SupplierRepository.java
src/main/java/com/devrick/pos/supplier/service/SupplierService.java
src/main/java/com/devrick/pos/supplier/service/SupplierServiceImpl.java
src/main/java/com/devrick/pos/supplier/controller/SupplierController.java
src/main/resources/db/migration/V<next>__create_suppliers.sql
src/test/java/com/devrick/pos/supplier/...
```

Use the exact paths and naming conventions already established in the repository.

---

## 30. Approval Gate

Do not implement Story 10 until this specification has been reviewed and explicitly approved.

After approval, generate a dedicated Codex implementation prompt beginning directly with:

```text
Implement Story 10 — Supplier Management
```

The Codex prompt must require:

1. Reading `AGENTS.md`
2. Reading this Story 10 specification
3. Inspecting Story 7A, Story 8, and Story 9 implementations
4. Implementing only Story 10
5. Running all automated tests
6. Running Spotless
7. Running Checkstyle
8. Running Maven verification
9. Providing beginner-friendly manual QA
10. Reporting every modified file and actual verification result
