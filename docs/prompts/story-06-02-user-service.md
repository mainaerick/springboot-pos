# Story 6.2 – User Service Layer

## Background

You are working on an enterprise-grade multi-tenant Inventory & POS SaaS.

Follow the repository's `AGENTS.md` exactly.

This story builds on Story 6.1, where the User entity, repository, and Flyway migration were implemented.

Do not modify unrelated files.

---

# Objective

Implement the User Service layer.

The service layer is responsible for business logic only.

Do NOT implement:

- Authentication
- Authorization
- Controllers
- REST endpoints
- JWT
- Spring Security configuration

---

# Existing Architecture

The project currently includes:

- Java 17
- Spring Boot 4
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- Lombok
- Package-by-feature architecture
- BaseEntity
- JPA Auditing

---

# Files to Create

src/main/java/com/devrick/pos/user/service/UserService.java

src/main/java/com/devrick/pos/user/service/impl/UserServiceImpl.java

src/main/java/com/devrick/pos/user/dto/CreateUserRequest.java

src/main/java/com/devrick/pos/user/dto/UpdateUserRequest.java

src/main/java/com/devrick/pos/user/dto/UserResponse.java

src/main/java/com/devrick/pos/user/mapper/UserMapper.java

src/main/java/com/devrick/pos/exception/user/DuplicateEmailException.java

src/main/java/com/devrick/pos/exception/user/UserNotFoundException.java

src/test/java/com/devrick/pos/user/service/UserServiceImplTest.java

---

# DTO Requirements

Use Java Records.

CreateUserRequest

Fields

- firstName
- lastName
- email
- password

Add Jakarta Validation annotations where appropriate.

Examples:

- @NotBlank
- @Email
- @Size

---

UpdateUserRequest

Fields

- firstName
- lastName
- email
- enabled

Password updates will be implemented in a future story.

---

UserResponse

Return only safe information.

Include:

- id
- firstName
- lastName
- email
- enabled
- createdAt
- updatedAt

Never expose:

- password
- version
- deleted
- createdBy
- updatedBy

---

# Mapper

Create a MapStruct mapper.

Requirements

```java
@Mapper(componentModel = "spring")
```

Methods

```java
User toEntity(CreateUserRequest request);

UserResponse toResponse(User user);

void updateEntity(UpdateUserRequest request,
                  @MappingTarget User user);
```

No business logic inside the mapper.

No repository access.

No password hashing.

---

# Service Interface

Create UserService.

Methods

```java
UserResponse create(CreateUserRequest request);

UserResponse getById(UUID id);

List<UserResponse> getAll();

UserResponse update(UUID id,
                    UpdateUserRequest request);

void disable(UUID id);
```

---

# Service Implementation

Use constructor injection.

Business Rules

## Create

Normalize email

```java
email.trim().toLowerCase(Locale.ROOT)
```

Check

```java
existsByEmail(...)
```

If email already exists

throw

```java
DuplicateEmailException
```

Map DTO → Entity.

Save.

Return UserResponse.

---

## Get By Id

If user does not exist

throw

```java
UserNotFoundException
```

---

## Get All

Return DTO list.

Never return entities.

---

## Update

Find existing user.

Normalize email.

If email changes

verify uniqueness.

Use MapStruct

```java
updateEntity(...)
```

Save.

Return DTO.

---

## Disable

Soft disable only.

```java
user.setEnabled(false);
```

Do not delete.

---

# Exception Requirements

DuplicateEmailException

Message example

```
Email already exists: john@example.com
```

---

UserNotFoundException

Message example

```
User not found: <uuid>
```

---

# Coding Standards

Follow AGENTS.md.

Use constructor injection only.

No field injection.

No wildcard imports.

Keep methods small.

Prefer early returns.

Do not expose JPA entities.

Service contains business logic.

Mapper performs mapping only.

Repository performs persistence only.

---

# Unit Tests

Use

- JUnit 5
- Mockito

Test Cases

## Create User

✓ Success

✓ Duplicate email throws DuplicateEmailException

✓ Email normalization works

---

## Get User

✓ Existing user returned

✓ Missing user throws UserNotFoundException

---

## Update User

✓ Update succeeds

✓ Duplicate email rejected

---

## Disable User

✓ enabled becomes false

---

## Mapper

Verify DTO mapping.

---

# Acceptance Criteria

Application compiles.

Spotless passes.

Checkstyle passes.

All tests pass.

No entities exposed outside the service layer.

MapStruct generates implementations successfully.

Business rules implemented inside the service layer.

Repository contains no business logic.

---

# Manual Verification

Run

```bash
./mvnw clean verify
```

Run the application.

Create several users using service tests.

Verify

- duplicate emails fail
- email normalization works
- updates work
- disabling a user sets enabled=false
- no password appears in UserResponse

---

# Definition of Done

- UserService implemented
- UserServiceImpl implemented
- DTOs implemented as Java records
- MapStruct mapper implemented
- Domain exceptions implemented
- Unit tests passing
- Spotless passing
- Checkstyle passing
- No unrelated files modified
