Story 9 Architecture and Business Requirements

1. Why Customer Management exists

A customer represents a person or business that purchases goods or services from a tenant.

Examples:

Individual customer:

- Jane Wanjiku
- Phone: +254712345678

Business customer:

- Sunrise Hotel Ltd
- Tax number: P051234567X

Customers will later be referenced by:

Customer
├── Sales
├── Invoices
├── Payments
├── Returns
├── Loyalty
├── Credit accounts
└── Reports

Story 9 should establish only the customer master record. It should not implement sales, payments, loyalty, or credit-account transactions.

2. Customer ownership

Customers should belong to the tenant, not directly to a branch.

Recommended relationship:

Tenant
├── Branch A
├── Branch B
└── Customers

A customer may buy from multiple branches:

Jane Wanjiku
├── Purchase at Nairobi CBD
├── Purchase at Westlands
└── Purchase at Mombasa

If customers were branch-owned, the same customer could be duplicated at every location, making reporting, loyalty, balances, and purchase history difficult.

Therefore:

Customer → Tenant

not:

Customer → Branch

Sales records will later contain both:

sale.customer_id
sale.branch_id

This identifies the customer and the branch where the transaction occurred.

3. Customer types

The initial model should support:

INDIVIDUAL
BUSINESS

This distinction is useful because individuals and companies often require different information.

Individual customer

Typical fields:

Name
Phone
Email
Address
Business customer

Typical fields:

Business name
Phone
Email
Tax number
Address

To keep the model manageable, Story 9 should use a common name field rather than separate first name, last name, and company name fields.

Examples:

Customer type: INDIVIDUAL
Name: Jane Wanjiku

Customer type: BUSINESS
Name: Sunrise Hotel Ltd

More detailed legal-contact structures can be introduced later if required.

4. Recommended customer fields
   Field Purpose
   id UUID inherited from BaseEntity
   tenant Trusted tenant owner
   type INDIVIDUAL or BUSINESS
   code Stable tenant-local customer identifier
   name Customer or business display name
   email Optional contact email
   phone Optional primary phone
   taxNumber Optional tax or registration number
   addressLine1 Optional address
   addressLine2 Optional secondary address
   city Optional city or town
   stateOrCounty Optional county or region
   postalCode Optional postal code
   countryCode Optional two-letter country code
   notes Optional internal notes
   active Whether the customer may be used for new transactions
   audit fields Inherited from BaseEntity
5. Customer code

A customer code provides a stable business identifier.

Examples:

CUS-0001
JANE-001
SUNRISE-HOTEL

Recommended rules:

Required
Tenant-scoped unique
Normalized to uppercase
Between 2 and 40 characters
Letters, numbers, underscores, and hyphens only
Immutable after creation

Recommended validation:

^[A-Z0-9][A-Z0-9_-]\*$

For this story, the client may provide the code.

Automatic sequence generation introduces additional concerns:

Race conditions
Per-tenant numbering
Prefix configuration
Database locking
Import compatibility

That can be added later as a dedicated configurable numbering feature.

6. Email and phone uniqueness

Email and phone should not initially have hard tenant-scoped uniqueness constraints.

Reasons include:

Family members may share a phone number.
Businesses may share a reception email.
A customer may be imported with incomplete data.
Multiple customer contacts may use the same company contact details.

The system may provide duplicate warnings later, but Story 9 should enforce uniqueness only for:

tenant + customer code

The customer UUID remains the technical identity.

7. Required contact information

Only the following should be mandatory:

type
code
name

Email, phone, and address should remain optional.

This supports walk-in or minimally registered customers without forcing staff to invent contact data.

A dedicated system walk-in customer may be introduced during Story 15 — Sales/POS, where its lifecycle rules can be properly designed.

8. Customer lifecycle

Customers should be deactivated rather than deleted.

An inactive customer:

Remains available in historical sales
Remains visible in reports
Cannot normally be selected for new transactions
Can be reactivated

Story 9 should provide:

PATCH /api/v1/customers/{customerId}/status

It should not expose hard deletion.

9. Tenant isolation

All customer operations must use the authenticated tenant from Story 7A.

Safe lookup:

customerRepository.findByIdAndTenantId(customerId, tenantId);

Unsafe lookup:

customerRepository.findById(customerId);

A Tenant A user must not:

View Tenant B customers
Update Tenant B customers
Change Tenant B customer status
Detect whether a Tenant B customer code exists

Cross-tenant IDs should return 404 Not Found.

10. API design

Recommended base path:

/api/v1/customers

Endpoints:

POST /api/v1/customers
GET /api/v1/customers
GET /api/v1/customers/{customerId}
PUT /api/v1/customers/{customerId}
PATCH /api/v1/customers/{customerId}/status

The list endpoint should support:

page
size
sort
search
type
active

Search should match:

code
name
email
phone
taxNumber

All filtering and pagination must occur in PostgreSQL, not in Java memory.

11. Suggested authorization

Use the existing RBAC roles and conventions.

Conceptually:

Operation Access
Create customer Authorized sales/admin user
List customers Authorized tenant user
View customer Authorized tenant user
Update customer Authorized sales/admin user
Activate/deactivate Tenant administrator or authorized manager

Do not introduce a new permission framework solely for Story 9.

Tenant isolation remains mandatory even for administrators.

12. DTO design

Recommended DTOs:

CreateCustomerRequest
UpdateCustomerRequest
UpdateCustomerStatusRequest
CustomerResponse

Separate DTOs prevent clients from controlling:

Tenant ownership
UUID
Active status during creation
Audit data
Immutable customer code during update 13. How Spring processes customer creation

For:

POST /api/v1/customers

the flow is:

HTTP request
↓
Spring Security validates JWT
↓
Authenticated principal contains tenant identity
↓
DispatcherServlet selects CustomerController
↓
Jackson creates CreateCustomerRequest
↓
Jakarta Validation validates the DTO
↓
Method authorization checks the role
↓
CustomerService resolves the current tenant
↓
Service normalizes the customer code and country
↓
Repository checks tenant-scoped code uniqueness
↓
MapStruct creates Customer entity
↓
Service assigns trusted Tenant entity
↓
Repository persists the customer
↓
Hibernate executes SQL
↓
PostgreSQL enforces constraints
↓
Transaction commits
↓
CustomerResponse is returned

The controller should only manage HTTP concerns. Tenant ownership and business rules belong in the service.
