# Story 6.1 – User Module Foundation

## Context

You are implementing Story 6.1 for an enterprise-grade Inventory & POS SaaS.

Follow the repository's AGENTS.md exactly.

Do not change unrelated files.

Preserve the existing architecture.

The project uses:

- Java 17
- Spring Boot 4
- Spring Data JPA
- PostgreSQL
- Flyway
- Lombok
- UUID primary keys
- Package-by-feature architecture

BaseEntity and JPA auditing are already implemented.

---

## Objective

Implement the User domain foundation.

This story only establishes persistence.

Do NOT implement authentication, controllers, services, JWT, roles, or business logic.

---

## Files to Create

src/main/java/com/devrick/pos/user/entity/User.java

src/main/java/com/devrick/pos/user/repository/UserRepository.java

src/main/resources/db/migration/V2\_\_Create_users_table.sql

---

## Files to Modify

application.yml

(or the appropriate environment configuration)

Change:

spring.jpa.hibernate.ddl-auto

from

update

to

validate

---

## User Entity

Implement a JPA entity named User.

Requirements

- Extend BaseEntity.
- Table name: users.
- Use Lombok.
- Add a no-args constructor.
- Add an all-args constructor only if justified.
- Add a builder if appropriate.

Fields

firstName

- required
- max length 100

lastName

- required
- max length 100

email

- required
- unique
- max length 255

password

- required
- suitable for storing hashed passwords

enabled

- required
- default true

Use appropriate JPA annotations.

Do not include roles.

Do not include tenant fields.

Do not include phone number or address.

Do not include authentication logic.

---

## Repository

Create UserRepository.

Extend JpaRepository<User, UUID>.

Add:

Optional<User> findByEmail(String email);

boolean existsByEmail(String email);

Do not add unnecessary query methods.

---

## Flyway Migration

Create

V2\_\_Create_users_table.sql

The schema must match the entity exactly.

Requirements

- UUID primary key
- audit columns from BaseEntity
- version column
- deleted column
- unique constraint on email
- NOT NULL where appropriate

Create an index on email.

Use PostgreSQL syntax.

---

## Acceptance Criteria

Application starts successfully.

Flyway migration executes successfully.

Hibernate validation succeeds.

users table created correctly.

Repository compiles.

No Spotless violations.

No Checkstyle violations.

---

## Test Plan

### Automated

Repository Test

- Persist a user successfully.

- Verify findByEmail returns the expected user.

- Verify existsByEmail works.

- Verify duplicate email violates the unique constraint.

Integration Test

- Application context loads.

- Flyway migration runs.

- Hibernate validates schema.

### Manual Verification

1.

Run

./mvnw clean verify

2.

Start PostgreSQL.

3.

Run the application.

4.

Verify Flyway applies V2.

5.

Verify users table exists.

6.

Verify email unique constraint exists.

7.

Insert a user manually and confirm retrieval through the repository test.

---

## Definition of Done

✓ Project compiles.

✓ Spotless passes.

✓ Checkstyle passes.

✓ Tests pass.

✓ Flyway migration applied.

✓ Hibernate validates schema.

✓ No unrelated code modified.
