# Story 6 - User Management Module

## Document Information

| Field          | Value                                |
| -------------- | ------------------------------------ |
| Project        | Inventory & POS SaaS                 |
| Module         | User Management                      |
| Story          | 6                                    |
| Status         | In Development                       |
| Backend        | Java 17 + Spring Boot                |
| Database       | PostgreSQL                           |
| Migration Tool | Flyway                               |
| ORM            | Spring Data JPA                      |
| Mapping        | MapStruct                            |
| Testing        | JUnit 5 + Mockito + Spring Boot Test |

---

# 1. Overview

## Purpose

This story implements the complete User Management module for the Inventory & POS SaaS platform.

The User module represents employees and system users who interact with the platform.

Examples:

- Business owners
- Managers
- Cashiers
- Inventory staff
- Accountants
- Administrators

Authentication and authorization are intentionally excluded from this story and will be implemented in Story 7.

---

# 2. Learning Objectives

This story introduces enterprise Spring Boot development patterns.

By completing this story, the developer should understand:

- Feature-based package architecture
- DTO-based API design
- Entity isolation
- Service layer responsibilities
- Repository responsibilities
- MapStruct usage
- Bean validation
- Exception handling
- REST API design
- Pagination
- Automated testing
- QA verification

---

# 3. Architectural Principles

The implementation must follow these rules.

## 3.1 Layer Separation

The application follows:

Controller
|
|
v
Service
|
|
v
Repository
|
|
v
Database

Responsibilities:

---

## Controller Layer

Responsible for:

- HTTP requests
- Request validation
- HTTP responses
- API documentation

Must NOT contain:

- Business logic
- Database access
- Complex transformations

---

## Service Layer

Responsible for:

- Business rules
- Validation beyond simple field validation
- Transactions
- Entity operations
- Coordinating repositories and mappers

Examples:

- Checking duplicate emails
- Normalizing emails
- Deciding whether an operation is allowed

---

## Repository Layer

Responsible for:

- Database communication
- Query execution

Must NOT contain:

- Business decisions
- Validation rules

---

## Mapper Layer

Responsible for:

- Entity to DTO conversion
- DTO to Entity conversion

Must NOT contain:

- Business logic
- Repository calls
- Validation

---

# 4. Current Project Context

The following components already exist:

src/main/java/com/devrick/pos

├── user
│ ├── controller
│ ├── dto
│ ├── entity
│ ├── mapper
│ ├── repository
│ └── service
│
├── common
│
├── config
│
├── exception
│
└── security

Existing implementation:

Completed:

- User entity
- User repository
- Flyway initial schema
- BaseEntity
- Auditing configuration
- Coding standards
- MapStruct dependency

---

# 5. Story Scope

## Included

This story implements:

### User Management

- Create user
- Retrieve user
- Retrieve users
- Update user
- Disable user

---

### API Features

- REST endpoints
- Validation
- Pagination
- Sorting
- Error handling

---

### Testing

- Unit tests
- Repository tests
- Controller tests
- Integration tests

---

## Excluded

The following belong to Story 7:

- Login
- JWT
- Refresh tokens
- Password encryption
- Roles
- Permissions
- Spring Security configuration

---

# 6. User Domain Rules

## User Lifecycle

Users are never physically deleted.

The lifecycle is:

Created
|
|
v
Enabled
|
|
v
Disabled

Disable instead of delete.

---

# 7. Database Requirements

## Users Table

The users table must contain:

users

id
first_name
last_name
email
password
enabled

created_at
updated_at
created_by
updated_by
version
deleted

---

# 8. Database Rules

## Primary Key

Use UUID.

Example:

id UUID PRIMARY KEY

---

## Email

Email must:

- Be required
- Be unique
- Have an index

Example:

email VARCHAR(255) NOT NULL UNIQUE

---

## Soft Delete

Do not remove users.

Use:

deleted BOOLEAN

Future stories may implement full soft-delete filtering.

---

# 9. Package Structure

Final structure:

user/

├── controller
│ └── UserController.java
│
├── dto
│ ├── CreateUserRequest.java
│ ├── UpdateUserRequest.java
│ └── UserResponse.java
│
├── entity
│ └── User.java
│
├── mapper
│ └── UserMapper.java
│
├── repository
│ └── UserRepository.java
│
└── service
├── UserService.java
└── impl
└── UserServiceImpl.java

---

# 10. Exception Structure

Exceptions should follow:

exception/

├── GlobalExceptionHandler.java
├── ErrorResponse.java
│
└── user
├── UserNotFoundException.java
└── DuplicateEmailException.java

---

# 11. Coding Standards

Follow AGENTS.md.

Required:

- Java 17
- Constructor injection only
- No field injection
- No exposing entities
- DTO responses only
- MapStruct for mapping
- Flyway for database changes
- Unit tests for business logic

---

# 12. Definition of Done

Story 6 is complete when:

[ ] User CRUD API exists

[ ] Validation works

[ ] Exceptions are handled globally

[ ] Pagination works

[ ] Tests pass

[ ] No entity is exposed

[ ] Code passes formatting

[ ] Code passes static checks

[ ] QA checklist completed

[ ] Documentation updated

# 13. DTO Design Requirements

The API must never expose JPA entities directly.

All communication between the API layer and clients must use DTOs.

DTOs should use Java Records where possible.

---

# 13.1 CreateUserRequest

Location:

user/dto/CreateUserRequest.java

Purpose:

Represents data required to create a new user.

---

## Fields

```java
String firstName;

String lastName;

String email;

String password;
Validation Rules
firstName

Required.

Rules:

@NotBlank
@Size(min = 2, max = 100)
lastName

Required.

Rules:

@NotBlank
@Size(min = 2, max = 100)
email

Required.

Rules:

@NotBlank
@Email
@Size(max = 255)
password

Required.

Rules:

@NotBlank
@Size(min = 8, max = 100)

Note:

Password encryption is not part of Story 6.

Story 7 will introduce:

PasswordEncoder
BCrypt
Authentication flow
13.2 UpdateUserRequest

Location:

user/dto/UpdateUserRequest.java

Purpose:

Represents editable user information.

Fields
String firstName;

String lastName;

String email;

Boolean enabled;
Important Rules

Password cannot be updated here.

Password management belongs to authentication/security.

13.3 UserResponse

Location:

user/dto/UserResponse.java

Purpose:

Safe representation returned to API consumers.

Allowed Fields
UUID id;

String firstName;

String lastName;

String email;

Boolean enabled;

Instant createdAt;

Instant updatedAt;
Forbidden Fields

Never expose:

password

createdBy

updatedBy

version

deleted
14. MapStruct Requirements

Location:

user/mapper/UserMapper.java

The mapper must use:

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
Mapping Methods

Required:

User toEntity(CreateUserRequest request);
UserResponse toResponse(User user);
void updateEntity(
    UpdateUserRequest request,
    @MappingTarget User user
);
Mapper Rules

The mapper must:

DO:

Convert objects
Map fields

DO NOT:

Validate data
Call repositories
Throw business exceptions
Normalize emails
Hash passwords
15. Service Layer Requirements

Location:

user/service
15.1 UserService Interface

Create:

UserService.java

Required methods:

UserResponse create(CreateUserRequest request);

Creates a new user.

UserResponse getById(UUID id);

Retrieves a user.

Page<UserResponse> getAll(Pageable pageable);

Retrieves users using pagination.

UserResponse update(
    UUID id,
    UpdateUserRequest request
);

Updates user details.

void disable(UUID id);

Disables a user.

15.2 UserServiceImpl

Location:

user/service/impl/UserServiceImpl.java

Requirements:

Use constructor injection.

Example:

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

}

Dependencies:

Required:

UserRepository

UserMapper
16. Business Rules
16.1 Create User Flow

Implementation sequence:

Receive request

        ↓

Normalize email

        ↓

Check duplicate email

        ↓

Map DTO to Entity

        ↓

Save entity

        ↓

Map entity to response

        ↓

Return response
Email Normalization

Before checking uniqueness:

email =
email.trim()
     .toLowerCase(Locale.ROOT);

Example:

Input:

 Erick@Example.COM

Stored:

erick@example.com
Duplicate Email Rule

Before saving:

userRepository.existsByEmail(email)

If true:

Throw:

DuplicateEmailException
16.2 Get User Flow

Process:

Receive ID

↓

Search repository

↓

If missing

throw UserNotFoundException

↓

Return DTO

Repository:

findById(id)
16.3 Update User Flow

Process:

Find existing user

↓

Normalize email

↓

Check email change

↓

Update fields

↓

Save

↓

Return response

If email changes:

Check:

existsByEmail(email)

If duplicate:

Throw:

DuplicateEmailException
16.4 Disable User Flow

Do not delete.

Update:

enabled=false

Process:

Find user

↓

Set enabled false

↓

Save

↓

Return
17. Repository Requirements

Location:

user/repository/UserRepository.java

Required:

public interface UserRepository
extends JpaRepository<User, UUID>

Methods:

Optional<User> findByEmail(String email);
boolean existsByEmail(String email);

Do not add business logic.

18. REST Controller Requirements

Location:

user/controller/UserController.java

Base mapping:

/api/v1/users

Controller rules:

Must:

Use DTOs
Use @Valid
Return ResponseEntity
Delegate to service

Must not:

Access repository
Contain business rules
19. API Endpoints
Create User

Request:

POST /api/v1/users

Request:

{
 "firstName":"Eric",
 "lastName":"Maina",
 "email":"eric@example.com",
 "password":"Password123"
}

Response:

Status:

201 CREATED

Response:

{
 "id":"uuid",
 "firstName":"Eric",
 "lastName":"Maina",
 "email":"eric@example.com",
 "enabled":true
}
Get User

Request:

GET /api/v1/users/{id}

Success:

200 OK

Missing:

404 NOT_FOUND
List Users

Request:

GET /api/v1/users

Supports:

?page=0
&size=20
&sort=firstName,asc

Response:

Spring Page format:

{
 "content":[ ],
 "pageable":{},
 "totalElements":0
}
Update User

Request:

PUT /api/v1/users/{id}

Success:

200 OK
Disable User

Request:

PATCH /api/v1/users/{id}/disable

Success:

204 NO_CONTENT
20. Exception Handling

Create:

GlobalExceptionHandler

Using:

@RestControllerAdvice

Handle:

UserNotFoundException

DuplicateEmailException

MethodArgumentNotValidException

Exception
21. Error Response Model

Create:

exception/ErrorResponse.java

Fields:

Instant timestamp;

int status;

String error;

String message;

String path;

Validation errors may extend:

Map<String,String> fieldErrors;

Example:

{
 "timestamp":"2026-08-01T10:00:00Z",
 "status":404,
 "error":"NOT_FOUND",
 "message":"User not found",
 "path":"/api/v1/users/123"
}
```

# 22. Testing Strategy

This story must include automated tests and manual verification.

Testing pyramid:

```
                 Integration Tests
               /-------------------\
             Controller Tests
           /-------------------------\
          Service Unit Tests
        /-----------------------------\
       Repository Tests
```

Testing philosophy:

- Unit tests verify business rules.
- Repository tests verify persistence.
- Controller tests verify API behaviour.
- Integration tests verify the application works as a whole.

---

# 23. Unit Tests

Location

```
src/test/java/com/devrick/pos/user/service
```

Frameworks

- JUnit 5
- Mockito
- AssertJ

Mock:

- UserRepository
- UserMapper

Never mock the service under test.

---

## Test Cases

### Create User

Verify:

- valid request creates a user
- mapper called
- repository save called
- response returned

---

### Duplicate Email

Given:

Existing email

When:

Create request received

Then:

DuplicateEmailException thrown

Repository save never called

---

### Email Normalization

Input

```
 Eric@Example.COM
```

Expected

```
eric@example.com
```

Verify repository receives normalized email.

---

### Get User

Verify:

- existing user returned
- response mapped correctly

---

### Missing User

Verify

```
UserNotFoundException
```

is thrown.

---

### Update User

Verify:

- existing values updated
- mapper invoked
- repository save invoked

---

### Disable User

Verify:

```
enabled=false
```

Verify entity saved.

---

# 24. Repository Tests

Location

```
src/test/java/com/devrick/pos/user/repository
```

Use:

@DataJpaTest

Prefer Testcontainers if already configured.

Otherwise use embedded database only for repository tests.

---

## Test Cases

Verify:

- save()
- findById()
- findByEmail()
- existsByEmail()

---

Duplicate email

Expected

Database constraint violation.

---

# 25. Controller Tests

Location

```
src/test/java/com/devrick/pos/user/controller
```

Use

@WebMvcTest(UserController.class)

Mock

UserService

---

## Create Endpoint

POST

```
/api/v1/users
```

Verify

HTTP 201

Verify JSON response.

---

## Validation

Missing email

Expected

HTTP 400

---

Invalid email

Expected

HTTP 400

---

Missing password

Expected

HTTP 400

---

## Get User

Verify

HTTP 200

---

Unknown ID

Verify

HTTP 404

---

## List Users

Verify

HTTP 200

Verify pagination fields.

---

## Update User

Verify

HTTP 200

---

## Disable User

Verify

HTTP 204

---

# 26. Integration Tests

Location

```
src/test/java/com/devrick/pos/integration
```

Use

@SpringBootTest

Recommended

Testcontainers PostgreSQL

---

Verify

ApplicationContext loads.

---

Verify

Flyway migration executes.

---

Verify

UserRepository functions.

---

Verify

REST endpoints function.

---

# 27. Manual QA Guide

This section is written for developers who are new to QA.

Complete every step before marking Story 6 complete.

---

## Step 1

Run

```bash
./mvnw clean verify
```

Expected

```
BUILD SUCCESS
```

---

## Step 2

Review Console

Expected

No:

- ERROR
- StackTrace
- Bean creation failures

---

## Step 3

Start PostgreSQL

Verify database accepts connections.

---

## Step 4

Run application

```bash
./mvnw spring-boot:run
```

Expected

Application starts successfully.

Flyway completes successfully.

Tomcat starts.

---

## Step 5

Verify Database

Open

pgAdmin

or

DBeaver

Run

```sql
SELECT *
FROM users;
```

Expected

Table exists.

---

## Step 6

Open Swagger

If SpringDoc is installed

```
http://localhost:8080/swagger-ui/index.html
```

Verify User endpoints exist.

---

## Step 7

Create User

POST

```
/api/v1/users
```

Payload

```json
{
    "firstName": "Eric",
    "lastName": "Maina",
    "email": "ERIC@example.com",
    "password": "Password123"
}
```

Expected

HTTP 201

---

Verify database

Email stored as

```
eric@example.com
```

---

## Step 8

Duplicate Email

Repeat same request.

Expected

HTTP 409 Conflict

Response

```
DuplicateEmailException
```

---

## Step 9

Get User

```
GET /api/v1/users/{id}
```

Expected

HTTP 200

No password in response.

---

## Step 10

Unknown User

Request

Unknown UUID

Expected

HTTP 404

Correct ErrorResponse returned.

---

## Step 11

Update User

PUT

```
/api/v1/users/{id}
```

Update first name.

Expected

HTTP 200

Database updated.

---

## Step 12

Disable User

PATCH

```
/api/v1/users/{id}/disable
```

Expected

HTTP 204

Database

```
enabled=false
```

---

## Step 13

Pagination

```
GET /api/v1/users?page=0&size=10
```

Expected

Paged response.

---

## Step 14

Sorting

```
GET /api/v1/users?sort=firstName,asc
```

Verify ordering.

---

## Step 15

Review Logs

Expected

No warnings.

No exceptions.

---

# 28. Common Problems

## MapStruct Implementation Missing

Check

- annotation processor enabled
- processor dependency
- clean build

Run

```bash
./mvnw clean compile
```

---

## Flyway Error

Check

Migration versions.

Never edit an already executed migration.

Create a new migration.

---

## Validation Not Running

Verify

```
@Valid
```

exists on controller parameters.

---

## Duplicate Email Not Detected

Verify

Email normalization occurs before

```
existsByEmail()
```

---

## Pagination Not Working

Verify controller accepts

```
Pageable
```

---

# 29. Logging Requirements

Use SLF4J.

Never use

```
System.out.println()
```

Log:

- create user
- update user
- disable user

Never log:

- passwords
- secrets
- JWTs

---

# 30. Performance Considerations

Do not fetch unnecessary relationships.

Return DTOs only.

Use pagination.

Avoid loading the full user table.

---

# 31. Security Considerations

Passwords remain plain values only until Story 7.

Never expose passwords.

Never log passwords.

Do not implement authentication here.

---

# 32. Coding Standards Checklist

✓ Constructor injection

✓ DTO responses

✓ MapStruct

✓ Bean Validation

✓ Global exception handling

✓ Repository free of business logic

✓ Small service methods

✓ Spotless compliant

✓ Checkstyle compliant

---

# 33. Acceptance Criteria

Story 6 is accepted when:

✓ User CRUD implemented

✓ Pagination works

✓ Validation works

✓ Exception handling works

✓ Repository tests pass

✓ Service tests pass

✓ Controller tests pass

✓ Integration tests pass

✓ Manual QA completed

✓ BUILD SUCCESS

---

# 34. Definition of Done

Before committing verify:

- [ ] Application compiles
- [ ] Spotless passes
- [ ] Checkstyle passes
- [ ] All automated tests pass
- [ ] Manual QA completed
- [ ] Swagger documentation verified (if enabled)
- [ ] No entity exposed through the API
- [ ] No passwords returned in responses
- [ ] Email normalization verified
- [ ] Duplicate email protection verified
- [ ] Code reviewed
- [ ] Ready for merge

---

# 35. Instructions for Codex

You are implementing Story 6 for an enterprise Inventory & POS SaaS.

Read AGENTS.md before making any changes.

Follow the package-by-feature architecture.

Do not modify unrelated modules.

Use constructor injection.

Use Java Records for DTOs.

Use MapStruct for object mapping.

Keep business logic inside the service layer.

Keep controllers thin.

Do not expose JPA entities.

Write comprehensive tests before considering the implementation complete.

Do not leave TODOs or placeholder implementations.

Run the equivalent of:

```bash
./mvnw clean verify
```

Ensure all checks pass before finishing.

Only modify files required for Story 6.
