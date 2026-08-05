# Story 9 — Customer Management

## Document Status

- **Sprint:** Sprint 2
- **Story:** 9
- **Module:** Customer Management
- **Status:** Proposed
- **Implementation status:** Not started
- **Approval required before implementation:** Yes
- **Target path:** `docs/prompts/story-09-customer-management.md`

---

## 1. Purpose

Implement tenant-owned Customer Management for the Inventory and Point of Sale SaaS.

A customer represents an individual or business that purchases goods or services from the tenant.

This story establishes the customer master-data model and secure customer APIs required by future Sales, POS, Payments, Returns, Loyalty, Credit, and Reporting modules.

---

## 2. Mandatory Project Rules

All implementation must comply with the root `AGENTS.md`.

Before implementation, inspect:

1. `AGENTS.md`
2. Story 7A — Tenant Foundation
3. Story 8 — Branch Management
4. `BaseEntity`
5. JPA auditing
6. Current-tenant provider
7. Authenticated principal
8. Existing RBAC roles
9. DTO and validation conventions
10. MapStruct configuration
11. Pagination conventions
12. Repository query conventions
13. Global exception handling
14. OpenAPI conventions
15. Flyway migration numbering
16. Testcontainers and integration-test configuration
17. Existing test naming and fixture conventions

Reuse the exact tenant architecture created in Story 7A.

Do not introduce another tenant abstraction.

---

## 3. User Story

As an authorized tenant user, I want to create and manage customer records so that purchases, sales, invoices, payments, returns, and reports can later be associated with the correct customer.

---

## 4. Business Value

Customer Management enables:

- Customer-linked sales
- Purchase-history reporting
- Customer receipts and invoices
- Returns and refunds
- Loyalty programs
- Credit accounts
- Customer balances
- Customer communication
- Customer analytics

This story implements only customer master data.

---

## 5. Ownership Model

Every customer must belong to exactly one tenant.

Customers are tenant-owned, not branch-owned.

A customer may transact at multiple branches belonging to the same tenant.

The model must follow:

```text
Tenant
├── Branches
└── Customers
```

Future sales will reference both a customer and the branch where the sale occurred.

Customer request DTOs must not accept:

- tenant ID
- tenant object
- branch ID as customer ownership
- arbitrary ownership headers

Tenant ownership must be obtained from the authenticated tenant context created in Story 7A.

---

## 6. Scope

### 6.1 Included

Implement:

- Customer entity
- Customer type enum
- Tenant-to-Customer relationship
- Flyway migration
- Create customer endpoint
- Paginated customer-list endpoint
- Get customer by UUID endpoint
- Update customer endpoint
- Activate/deactivate customer endpoint
- Request and response DTOs
- Jakarta Bean Validation
- MapStruct mapper
- Customer repository
- Customer service
- Tenant-scoped queries
- Tenant isolation
- Search
- Customer-type filtering
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

- Sales
- POS transactions
- Invoices
- Payments
- Customer balances
- Customer credit
- Credit limits
- Loyalty points
- Rewards
- Customer groups
- Customer pricing
- Customer discounts
- Customer contacts
- Multiple addresses
- Customer documents
- Attachments
- Marketing preferences
- SMS or email notifications
- Branch ownership
- Walk-in system customer
- Customer imports
- Customer exports
- Customer merging
- Hard deletion
- Supplier Management
- Product Management
- Inventory

---

## 7. Customer Type

Create an enum following existing project conventions:

```text
CustomerType
```

Values:

```text
INDIVIDUAL
BUSINESS
```

Do not add additional values without approved business requirements.

Persist the enum as a string.

Do not use ordinal enum persistence.

---

## 8. Customer Entity

Create a `Customer` entity in the appropriate package-by-feature location.

The entity must extend the existing `BaseEntity`.

### 8.1 Required fields

| Field         | Type               | Required | Rules                               |
| ------------- | ------------------ | -------: | ----------------------------------- |
| tenant        | Tenant             |      Yes | Trusted mandatory ownership         |
| type          | CustomerType       |      Yes | `INDIVIDUAL` or `BUSINESS`          |
| code          | String             |      Yes | Uppercase, tenant-scoped unique     |
| name          | String             |      Yes | Trimmed, maximum 150 characters     |
| email         | String             |       No | Valid email, maximum 254 characters |
| phone         | String             |       No | Maximum 30 characters               |
| taxNumber     | String             |       No | Maximum 50 characters               |
| addressLine1  | String             |       No | Maximum 200 characters              |
| addressLine2  | String             |       No | Maximum 200 characters              |
| city          | String             |       No | Maximum 100 characters              |
| stateOrCounty | String             |       No | Maximum 100 characters              |
| postalCode    | String             |       No | Maximum 30 characters               |
| countryCode   | String             |       No | Exactly two uppercase letters       |
| notes         | String             |       No | Maximum 1000 characters             |
| active        | Boolean or boolean |      Yes | Defaults to `true`                  |
| audit fields  | Existing types     |      Yes | Inherited from `BaseEntity`         |

Follow existing primitive/wrapper conventions.

### 8.2 Tenant relationship

Use the Tenant entity created in Story 7A.

Implement the existing project equivalent of:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

Requirements:

- Customer deletion must not delete the tenant.
- Tenant ownership must not be included in equality calculations.
- Tenant must not be exposed through entity serialization.
- Request mapping must never assign tenant ownership.
- No cascading tenant persistence or deletion.

---

## 9. Customer Code Rules

The customer code must:

- Be required
- Be trimmed
- Be normalized to uppercase
- Have a minimum length of 2
- Have a maximum length of 40
- Start with a letter or digit
- Contain only uppercase letters, digits, hyphens, or underscores
- Be unique within the tenant
- Be allowed in another tenant
- Be immutable after creation

Use this validation pattern or equivalent:

```regex
^[A-Z0-9][A-Z0-9_-]*$
```

Example:

```text
Input:  cus-0001
Stored: CUS-0001
```

Do not include `code` in `UpdateCustomerRequest`.

Automatic customer-code generation is outside this story.

---

## 10. Customer Name Rules

The customer name must:

- Be required
- Be trimmed
- Not contain only whitespace
- Have a maximum length of 150

Customer names are not required to be unique.

Different customers may legitimately have the same name.

Example:

```text
John Kamau
John Kamau
```

The UUID and customer code distinguish them.

---

## 11. Contact Fields

### Email

When provided:

- Trim whitespace
- Validate as an email address
- Maximum 254 characters
- Prefer lowercase normalization if consistent with existing project conventions

Email is not required to be unique.

### Phone

When provided:

- Trim whitespace
- Maximum 30 characters
- Preserve valid international formatting such as `+254712345678`

Do not implement full international phone parsing unless the project already contains a standard library or validator.

Phone is not required to be unique.

### Tax number

When provided:

- Trim whitespace
- Maximum 50 characters
- Normalize case if consistent with existing conventions

Tax number uniqueness is not required in this story.

---

## 12. Address Fields

Address fields are optional.

When provided:

- Trim surrounding whitespace
- Apply maximum lengths
- Normalize `countryCode` to uppercase
- Validate country code as exactly two alphabetic characters

Do not implement a separate address entity in this story.

Do not implement multiple customer addresses.

---

## 13. Active Status

New customers must default to active.

Create requests must not accept active status.

General updates must not change active status.

Status changes must use:

```http
PATCH /api/v1/customers/{customerId}/status
```

Inactive customers:

- Remain retrievable
- Remain available for historical records
- Can be reactivated
- Must not be physically deleted

Do not expose a hard-delete endpoint.

---

## 14. Database Migration

Create the next correctly numbered Flyway migration.

Do not modify already-applied migrations.

Create a `customers` table according to existing naming conventions.

### 14.1 Required database elements

Include:

- UUID primary key
- mandatory `tenant_id`
- customer type
- customer code
- name
- optional contact fields
- optional address fields
- notes
- active status
- audit columns compatible with `BaseEntity`
- foreign key to tenants
- tenant-scoped code uniqueness
- useful indexes

### 14.2 Required constraints

At minimum:

```sql
UNIQUE (tenant_id, code)
```

Required non-null columns:

```text
id
tenant_id
type
code
name
active
required audit columns
```

Use a check constraint for customer type only if that is consistent with existing migration conventions.

### 14.3 Recommended indexes

Add useful indexes for:

- tenant-scoped listing
- tenant and active filtering
- tenant and type filtering
- tenant and code lookup
- case-insensitive customer-name search

Avoid redundant indexes already covered by unique constraints.

Do not rely on Hibernate schema generation.

---

## 15. DTOs

Create:

```text
CreateCustomerRequest
UpdateCustomerRequest
UpdateCustomerStatusRequest
CustomerResponse
```

Follow existing DTO class or record conventions.

### 15.1 CreateCustomerRequest

Fields:

```text
type
code
name
email
phone
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
deletion fields
```

### 15.2 UpdateCustomerRequest

Editable fields:

```text
type
name
email
phone
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

### 15.3 UpdateCustomerStatusRequest

Contains only:

```json
{
    "active": false
}
```

The active value must be required and non-null.

### 15.4 CustomerResponse

Return:

```text
id
type
code
name
email
phone
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
Authentication information
Internal deletion data
```

Omit tenant ID unless existing public response conventions require it.

---

## 16. MapStruct

Create a Customer mapper using the established MapStruct configuration.

Support:

```text
CreateCustomerRequest → Customer
Customer → CustomerResponse
UpdateCustomerRequest → existing Customer
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

Use explicit ignore mappings where necessary.

Do not use reflection-based property-copy utilities.

---

## 17. Customer Repository

Create:

```text
CustomerRepository
```

using the existing repository base type.

All resource access must be tenant-scoped.

Expected equivalents:

```java
Optional<Customer> findByIdAndTenantId(
        UUID customerId,
        UUID tenantId);
```

```java
boolean existsByTenantIdAndCodeIgnoreCase(
        UUID tenantId,
        String code);
```

Use entity-association equivalents where appropriate.

Listing must support:

- Tenant scope
- Optional search
- Optional customer type
- Optional active status
- Pagination
- Safe sorting

Search must be case-insensitive and match:

```text
code
name
email
phone
taxNumber
```

Null fields must not break search queries.

Use the simplest approach consistent with existing architecture:

- JPA Specifications
- Criteria API
- derived queries
- custom repository query

Do not load all customers and filter in memory.

Do not use unrestricted `findById`.

---

## 18. Customer Service

Create the service interface and implementation according to existing conventions.

Expected operations are equivalent to:

```java
CustomerResponse createCustomer(CreateCustomerRequest request);
```

```java
PageResponse<CustomerResponse> getCustomers(
        String search,
        CustomerType type,
        Boolean active,
        Pageable pageable);
```

```java
CustomerResponse getCustomer(UUID customerId);
```

```java
CustomerResponse updateCustomer(
        UUID customerId,
        UpdateCustomerRequest request);
```

```java
CustomerResponse updateCustomerStatus(
        UUID customerId,
        UpdateCustomerStatusRequest request);
```

Use the existing pagination wrapper.

### 18.1 Create behavior

The service must:

1. Resolve the authenticated tenant.
2. Normalize customer fields.
3. Check tenant-scoped customer-code uniqueness.
4. Map the request to a Customer.
5. Assign the trusted Tenant.
6. Set active to true.
7. Persist the customer.
8. Return the response.

### 18.2 List behavior

The service must:

1. Resolve the current tenant ID.
2. Normalize optional search.
3. Apply tenant scoping.
4. Apply optional type filtering.
5. Apply optional active filtering.
6. Apply pagination.
7. Apply safe sorting.
8. Map the result to the project page response.

When no active filter is provided, return active and inactive customers.

When no type filter is supplied, return both customer types.

### 18.3 Get behavior

Load using:

```text
customer ID + current tenant ID
```

Return not found for:

- Unknown UUID
- Customer belonging to another tenant

### 18.4 Update behavior

The service must:

1. Resolve current tenant ID.
2. Load the customer using a tenant-scoped query.
3. Normalize editable fields.
4. Apply updates to the managed entity.
5. Preserve tenant ownership.
6. Preserve immutable customer code.
7. Preserve active status.
8. Return the updated response.

### 18.5 Status behavior

The service must:

1. Resolve current tenant ID.
2. Load customer using tenant-scoped lookup.
3. Change only active status.
4. Support activation and deactivation.
5. Treat the same-status request as idempotent success.
6. Return the response.

### 18.6 Transactions

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

Follow existing annotation placement conventions.

---

## 19. Duplicate and Race-Condition Handling

Perform a service-level duplicate-code check for friendly errors.

The database unique constraint remains the final authority.

Translate concurrent tenant/code unique-constraint violations into the project’s standard:

```http
409 Conflict
```

Do not expose Hibernate or PostgreSQL exception details.

Do not perform duplicate email, phone, tax number, or name rejection.

---

## 20. REST API

Base path:

```http
/api/v1/customers
```

Required endpoints:

```http
POST  /api/v1/customers
GET   /api/v1/customers
GET   /api/v1/customers/{customerId}
PUT   /api/v1/customers/{customerId}
PATCH /api/v1/customers/{customerId}/status
```

### 20.1 Create

```http
POST /api/v1/customers
```

Return:

```http
201 Created
```

Include a Location header if consistent with project conventions.

### 20.2 List

```http
GET /api/v1/customers
```

Support:

```text
page
size
sort
search
type
active
```

Suggested safe sort fields:

```text
code
name
type
city
active
createdAt
updatedAt
```

Unsupported sort fields must be rejected or safely mapped according to existing conventions.

### 20.3 Get

```http
GET /api/v1/customers/{customerId}
```

Return:

- `200 OK` for a tenant-owned customer
- `404 Not Found` for unknown or cross-tenant customer

### 20.4 Update

```http
PUT /api/v1/customers/{customerId}
```

Return:

```http
200 OK
```

Code, tenant ownership, and active status must remain unchanged.

### 20.5 Status

```http
PATCH /api/v1/customers/{customerId}/status
```

Return:

```http
200 OK
```

Do not add a DELETE endpoint.

---

## 21. Authorization

Reuse the exact roles and authorization conventions already in the repository.

Conceptual access:

| Operation       | Conceptual access                                |
| --------------- | ------------------------------------------------ |
| Create customer | Authorized sales user, manager, or administrator |
| List customers  | Authorized tenant user                           |
| Get customer    | Authorized tenant user                           |
| Update customer | Authorized sales user, manager, or administrator |
| Change status   | Manager or administrator                         |

Map these concepts to existing roles.

Do not introduce a new permission architecture unless one already exists.

Tenant isolation must be enforced independently of role checks.

---

## 22. Exception Handling

Reuse the existing global exception framework.

Expected statuses:

| Situation                         | Status |
| --------------------------------- | -----: |
| Invalid request                   |    400 |
| Invalid UUID                      |    400 |
| Missing or invalid authentication |    401 |
| Insufficient role                 |    403 |
| Customer not found in tenant      |    404 |
| Cross-tenant customer access      |    404 |
| Duplicate customer code           |    409 |
| Created                           |    201 |
| Successful list/get/update/status |    200 |

Do not create customer-specific error-body formats.

---

## 23. OpenAPI

Document all Customer endpoints using existing conventions.

Include:

- Endpoint summaries
- Descriptions
- JWT bearer security
- Authorization expectations
- Path parameters
- Query parameters
- Pagination behavior
- Searchable fields
- Type filter
- Active filter
- Request examples
- Success responses
- Validation errors
- Authentication errors
- Authorization errors
- Not-found errors
- Conflict errors

Confirm all endpoints appear correctly in Swagger UI.

---

## 24. Automated Tests

Follow `AGENTS.md` and existing test conventions.

Do not disable Spring Security.

### 24.1 Service unit tests

Cover at minimum:

1. Creates an individual customer.
2. Creates a business customer.
3. Uses the authenticated tenant.
4. Does not accept tenant ownership from the request.
5. Trims customer name.
6. Normalizes customer code.
7. Normalizes country code.
8. Defaults active to true.
9. Rejects duplicate code within the same tenant.
10. Allows the same code in another tenant.
11. Allows duplicate customer names.
12. Allows duplicate email addresses.
13. Allows duplicate phone numbers.
14. Returns a tenant-owned customer.
15. Returns not found for unknown customer.
16. Returns not found for another tenant’s customer.
17. Updates editable fields.
18. Preserves tenant ownership.
19. Preserves customer code.
20. Preserves active status.
21. Deactivates a customer.
22. Reactivates a customer.
23. Handles idempotent status change.
24. Returns paginated customers.
25. Applies search.
26. Applies type filtering.
27. Applies active filtering.
28. Handles blank search.
29. Handles unsupported sorting according to project rules.

### 24.2 Mapper tests

Verify:

1. Create request maps editable fields.
2. Tenant is ignored.
3. ID is ignored.
4. Active is ignored.
5. Audit fields are ignored.
6. Customer maps correctly to response.
7. Update preserves tenant.
8. Update preserves ID.
9. Update preserves code.
10. Update preserves active status.
11. Update preserves audit fields.

### 24.3 Repository tests

Cover at minimum:

1. Persists a valid customer.
2. Rejects a customer without a tenant.
3. Finds customer by ID and tenant.
4. Does not find customer under another tenant.
5. Detects duplicate customer code in one tenant.
6. Allows same code in separate tenants.
7. Allows duplicate names.
8. Allows duplicate emails.
9. Allows duplicate phone numbers.
10. Lists only tenant-owned customers.
11. Filters individual customers.
12. Filters business customers.
13. Filters active customers.
14. Filters inactive customers.
15. Searches by code.
16. Searches by name.
17. Searches by email.
18. Searches by phone.
19. Searches by tax number.
20. Applies pagination.
21. Applies safe sorting.
22. Enforces required fields.
23. Enforces tenant foreign key.
24. Enforces tenant/code unique constraint.
25. Deleting a customer does not delete the tenant.

### 24.4 Controller or integration tests

Cover at minimum:

1. Authorized user creates an individual customer.
2. Authorized user creates a business customer.
3. Create returns `201 Created`.
4. Request cannot assign tenant ID.
5. Missing type returns `400`.
6. Invalid type returns `400`.
7. Missing code returns `400`.
8. Invalid code returns `400`.
9. Missing name returns `400`.
10. Invalid email returns `400`.
11. Invalid country code returns `400`.
12. Duplicate code returns `409`.
13. Duplicate name is allowed.
14. Duplicate email is allowed.
15. Duplicate phone is allowed.
16. Unauthenticated create returns `401`.
17. Unauthorized create returns `403`.
18. Authorized user lists customers.
19. List is tenant-scoped.
20. List is paginated.
21. Search works for all supported fields.
22. Type filter works.
23. Active filter works.
24. Authorized user retrieves a customer.
25. Unknown customer returns `404`.
26. Cross-tenant get returns `404`.
27. Authorized user updates a customer.
28. Update preserves code.
29. Update preserves active status.
30. Cross-tenant update returns `404`.
31. Authorized manager deactivates customer.
32. Authorized manager reactivates customer.
33. Unauthorized status change returns `403`.
34. Cross-tenant status change returns `404`.
35. Response and error formats match project conventions.

### 24.5 Migration tests

Verify:

1. Migration succeeds on clean PostgreSQL.
2. `customers` table exists.
3. Tenant ID is non-null.
4. Tenant foreign key exists.
5. Customer type is persisted correctly.
6. Active defaults correctly.
7. Tenant/code unique constraint exists.
8. Required indexes exist.
9. Audit columns match `BaseEntity`.
10. Flyway validation succeeds.

---

## 25. Acceptance Criteria

### Persistence

- [ ] Customer entity exists.
- [ ] Customer extends `BaseEntity`.
- [ ] Every customer belongs to one tenant.
- [ ] Customer type uses string enum persistence.
- [ ] Customer code is required.
- [ ] Customer code is tenant-scoped unique.
- [ ] Customer code is immutable.
- [ ] Customer name is required.
- [ ] Duplicate customer names are allowed.
- [ ] Email is optional and not unique.
- [ ] Phone is optional and not unique.
- [ ] Country code is normalized.
- [ ] Active defaults to true.
- [ ] Flyway creates the customer schema.
- [ ] Required constraints and indexes exist.

### API

- [ ] Authorized users can create customers.
- [ ] Authorized users can list customers.
- [ ] Listing is paginated.
- [ ] Listing supports search.
- [ ] Listing supports customer-type filtering.
- [ ] Listing supports active filtering.
- [ ] Listing supports safe sorting.
- [ ] Authorized users can retrieve customers.
- [ ] Authorized users can update customers.
- [ ] Authorized managers can deactivate customers.
- [ ] Authorized managers can reactivate customers.
- [ ] No DELETE endpoint exists.

### Tenant security

- [ ] Tenant identity comes from trusted authentication context.
- [ ] Request DTOs contain no tenant ID.
- [ ] Repository operations are tenant-scoped.
- [ ] Service operations are tenant-scoped.
- [ ] Tenant A cannot retrieve Tenant B customers.
- [ ] Tenant A cannot update Tenant B customers.
- [ ] Tenant A cannot change Tenant B customer status.
- [ ] Cross-tenant IDs return `404`.
- [ ] RBAC does not bypass tenant isolation.

### Architecture

- [ ] Package-by-feature is followed.
- [ ] Controller contains no business logic.
- [ ] Service contains business rules.
- [ ] Repository handles persistence.
- [ ] DTOs isolate the REST contract.
- [ ] MapStruct handles mapping.
- [ ] Existing tenant provider is reused.
- [ ] Global exception handling is reused.
- [ ] Transactions are correct.
- [ ] `AGENTS.md` is followed.

### Quality

- [ ] Unit tests pass.
- [ ] Mapper tests pass where required.
- [ ] Repository tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] Tenant-isolation tests pass.
- [ ] Migration tests pass.
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

1. Start PostgreSQL and required Docker services.
2. Start the Spring Boot application.
3. Confirm Flyway succeeds.
4. Open Swagger UI.
5. Authenticate as an authorized tenant user.

Expected:

- Application starts.
- Customer endpoints appear.
- JWT authorization works.

### Test 2 — Create an individual customer

```http
POST /api/v1/customers
```

```json
{
    "type": "INDIVIDUAL",
    "code": "cus-0001",
    "name": "Jane Wanjiku",
    "email": "jane@example.com",
    "phone": "+254712345678",
    "addressLine1": "Kimathi Street",
    "city": "Nairobi",
    "stateOrCounty": "Nairobi",
    "postalCode": "00100",
    "countryCode": "ke",
    "notes": "Prefers SMS communication"
}
```

Expected:

- `201 Created`
- Code is `CUS-0001`
- Country code is `KE`
- Active is `true`
- UUID is returned
- No tenant ID was supplied

### Test 3 — Create a business customer

```json
{
    "type": "BUSINESS",
    "code": "SUNRISE-HOTEL",
    "name": "Sunrise Hotel Ltd",
    "email": "accounts@sunrise.example.com",
    "phone": "+254700000000",
    "taxNumber": "P051234567X",
    "city": "Nairobi",
    "countryCode": "KE"
}
```

Expected:

- `201 Created`
- Type is `BUSINESS`

### Test 4 — Verify tenant assignment

Inspect PostgreSQL.

Expected:

- Each customer has the authenticated user’s tenant ID.
- Tenant ownership was assigned server-side.

### Test 5 — Required-field validation

Try requests missing:

```text
type
code
name
```

Expected:

- Each returns `400 Bad Request`
- Standard validation response is used

### Test 6 — Invalid code validation

Try:

```text
A
CUS 001
CUS@001
```

Expected:

- `400 Bad Request`

### Test 7 — Duplicate code

Create another customer with:

```text
CUS-0001
```

Expected:

- `409 Conflict`

### Test 8 — Duplicate contact details

Create another customer using Jane’s email or phone but a different code.

Expected:

- Creation succeeds
- Email and phone are not unique identifiers

### Test 9 — List customers

```http
GET /api/v1/customers?page=0&size=10&sort=name,asc
```

Expected:

- `200 OK`
- Pagination metadata exists
- Results are sorted
- Only the current tenant’s customers appear

### Test 10 — Search

Test:

```http
GET /api/v1/customers?search=jane
GET /api/v1/customers?search=CUS-0001
GET /api/v1/customers?search=254712
GET /api/v1/customers?search=P051234567X
```

Expected:

- Matching customers appear for each supported field

### Test 11 — Filter by type

```http
GET /api/v1/customers?type=INDIVIDUAL
```

Expected:

- Only individual customers appear

Then:

```http
GET /api/v1/customers?type=BUSINESS
```

Expected:

- Only business customers appear

### Test 12 — Get by UUID

```http
GET /api/v1/customers/{customerId}
```

Expected:

- `200 OK`
- Correct customer is returned

### Test 13 — Update customer

```http
PUT /api/v1/customers/{customerId}
```

Update name, phone, email, address, or notes.

Expected:

- `200 OK`
- Editable fields change
- Code remains unchanged
- Tenant remains unchanged
- Active remains unchanged
- Audit modification data changes

### Test 14 — Deactivate customer

```http
PATCH /api/v1/customers/{customerId}/status
```

```json
{
    "active": false
}
```

Expected:

- `200 OK`
- Customer remains retrievable
- Active becomes false

### Test 15 — Filter inactive customers

```http
GET /api/v1/customers?active=false
```

Expected:

- Deactivated customers appear
- Active customers do not appear

### Test 16 — Reactivate customer

Send:

```json
{
    "active": true
}
```

Expected:

- Customer becomes active

### Test 17 — Authorization

Authenticate as a user without customer-write permissions.

Attempt create, update, and status operations.

Expected:

- Allowed reads succeed according to RBAC
- Restricted operations return `403 Forbidden`

### Test 18 — Tenant isolation

1. Create a customer under Tenant A.
2. Authenticate as Tenant B.
3. Try to retrieve the Tenant A customer.
4. Try to update the Tenant A customer.
5. Try to change the Tenant A customer’s status.

Expected:

- All return `404 Not Found`
- No Tenant A data is exposed or changed

### Test 19 — Same code in another tenant

As Tenant B, create a customer with:

```text
CUS-0001
```

Expected:

- Creation succeeds

### Test 20 — Database inspection

Confirm:

- `customers` table exists
- tenant ID is mandatory
- foreign key exists
- type is stored as text
- code uniqueness is tenant-scoped
- active is non-null
- audit columns exist
- indexes exist

### Test 21 — Quality checks

Run:

```bash
./mvnw test
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw verify
```

Expected:

- All commands pass

---

## 27. Definition of Done

Story 9 is complete only when:

- [ ] `AGENTS.md` was followed.
- [ ] Story 7A tenant architecture was reused.
- [ ] Story 8 conventions were reviewed.
- [ ] Customer entity is implemented.
- [ ] Customer type is implemented.
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
- [ ] Customer search works.
- [ ] Type filtering works.
- [ ] Active filtering works.
- [ ] Pagination works.
- [ ] Safe sorting works.
- [ ] Customer update works.
- [ ] Deactivation works.
- [ ] Reactivation works.
- [ ] Code remains immutable.
- [ ] No hard-delete endpoint exists.
- [ ] Global exception handling is reused.
- [ ] OpenAPI documentation is complete.
- [ ] Unit tests pass.
- [ ] Repository tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] Tenant-isolation tests pass.
- [ ] Migration tests pass.
- [ ] Existing tests remain green.
- [ ] Spotless passes.
- [ ] Checkstyle passes.
- [ ] Maven verify passes.
- [ ] Manual QA is completed.
- [ ] No Story 10 or later functionality was implemented.

---

## 28. Implementation Guardrails

Do not:

- Implement Supplier Management.
- Implement Product Categories.
- Implement Product Management.
- Implement Inventory.
- Implement Purchases.
- Implement Sales.
- Implement customer payments.
- Implement customer credit.
- Implement loyalty.
- Implement customer groups.
- Add branch ownership to Customer.
- Add automatic customer-code sequencing.
- Add a walk-in system customer.
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
- Rewrite unrelated Story 8, Story 7A, or Sprint 1 code.
- Suppress quality violations.
- Claim tests passed when they were not run.

Keep all changes strictly limited to Story 9.

---

## 29. Expected Deliverables

Expected equivalents include:

```text
src/main/java/com/devrick/pos/customer/entity/Customer.java
src/main/java/com/devrick/pos/customer/entity/CustomerType.java
src/main/java/com/devrick/pos/customer/dto/CreateCustomerRequest.java
src/main/java/com/devrick/pos/customer/dto/UpdateCustomerRequest.java
src/main/java/com/devrick/pos/customer/dto/UpdateCustomerStatusRequest.java
src/main/java/com/devrick/pos/customer/dto/CustomerResponse.java
src/main/java/com/devrick/pos/customer/mapper/CustomerMapper.java
src/main/java/com/devrick/pos/customer/repository/CustomerRepository.java
src/main/java/com/devrick/pos/customer/service/CustomerService.java
src/main/java/com/devrick/pos/customer/service/CustomerServiceImpl.java
src/main/java/com/devrick/pos/customer/controller/CustomerController.java
src/main/resources/db/migration/V<next>__create_customers.sql
src/test/java/com/devrick/pos/customer/...
```

Use exact paths consistent with the repository.

---

## 30. Approval Gate

Do not implement Story 9 until this specification has been reviewed and explicitly approved.

After approval, generate a separate Codex implementation prompt that begins directly with:

```text
Implement Story 9 — Customer Management
```

The Codex prompt must require:

1. Reading `AGENTS.md`
2. Reading this Story 9 specification
3. Inspecting existing tenant and branch architecture
4. Implementing only Story 9
5. Running all automated tests
6. Running Spotless
7. Running Checkstyle
8. Running Maven verification
9. Providing beginner-friendly manual QA
10. Reporting every modified file and actual verification result
