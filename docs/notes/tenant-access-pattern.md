# Tenant Access Pattern

Use this pattern for every future tenant-owned module.

## Read tenant context

Always resolve the current tenant from trusted Spring Security state.

- Use `CurrentTenantProvider`
- Never read tenant ownership from request bodies, query parameters, or client headers
- Never trust a client-supplied `tenantId`

## Scope repository queries

Every tenant-owned lookup should include the authenticated tenant.

Recommended patterns:

```java
Optional<Entity> findByIdAndTenantId(UUID id, UUID tenantId);
Page<Entity> findAllByTenantId(UUID tenantId, Pageable pageable);
boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
```

## Service layer rule

Services should:

- resolve the current tenant once
- pass the tenant ID into repository methods
- return `404 Not Found` for cross-tenant access
- reject suspended or inactive tenants through security

## Entity rule

Tenant-owned entities should:

- contain an explicit `tenant` association
- use `@ManyToOne(fetch = FetchType.LAZY, optional = false)`
- avoid exposing tenant ownership in API DTOs unless the story explicitly requires it

## Authentication rule

Authenticated principals should carry trusted tenant identity so the application does not need to query the database on every request.

The current implementation uses:

- `AuthenticatedUser`
- `SecurityCurrentTenantProvider`
- tenant-aware JWT validation

## What not to do

- Do not add tenant switching
- Do not accept tenant IDs from normal user requests
- Do not use unrestricted repository access for tenant-owned data
- Do not introduce a second tenant model
