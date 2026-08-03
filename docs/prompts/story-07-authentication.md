# Story 7 - Authentication & Authorization

## Document Information

| Field | Value |
|--------|-------|
| Project | Inventory & POS SaaS |
| Module | Authentication & Authorization |
| Story | 7 |
| Status | In Development |
| Backend | Spring Boot 4.x |
| Java | 17 |
| Security | Spring Security |
| Authentication | JWT |
| Password Encoding | BCrypt |
| Testing | JUnit 5 + Mockito + Spring Boot Test |

---

# 1. Overview

## Purpose

This story implements enterprise authentication and authorization for the Inventory & POS SaaS platform.

By the end of this story the application will:

- Authenticate users
- Issue JWT Access Tokens
- Issue Refresh Tokens
- Protect API endpoints
- Support role-based authorization
- Secure user passwords using BCrypt
- Replace the temporary `permitAll()` configuration from Story 6

---

# 2. Business Context

Every employee using the POS system must identify themselves before accessing business data.

Examples include:

- Cashiers
- Managers
- Store Owners
- Accountants
- Administrators

Authentication verifies **who the user is**.

Authorization determines **what the user is allowed to do**.

---

# 3. Learning Objectives

After completing this story you should understand:

- Spring Security architecture
- Authentication vs Authorization
- SecurityFilterChain
- AuthenticationManager
- UserDetailsService
- PasswordEncoder
- BCrypt hashing
- JWT Access Tokens
- Refresh Tokens
- Security Filters
- Method Security
- Role-Based Access Control (RBAC)

---

# 4. Authentication vs Authorization

These concepts are different.

## Authentication

Authentication answers:

> Who are you?

Example:

```
Username
Password
```

↓

Validate credentials

↓

User authenticated

---

## Authorization

Authorization answers:

> What are you allowed to do?

Examples:

Cashier

✓ Create Sale

✓ Print Receipt

✗ Create User

✗ Delete Product

---

Manager

✓ Reports

✓ Inventory

✓ Approve Purchases

---

Administrator

✓ Everything

---

# 5. High Level Authentication Flow

The login process should work as follows:

```
                POST /api/v1/auth/login
                           │
                           ▼
                Email + Password
                           │
                           ▼
            AuthenticationManager
                           │
          ┌────────────────┴──────────────┐
          │                               │
          ▼                               ▼
 Credentials Valid                Credentials Invalid
          │                               │
          ▼                               ▼
Generate JWT Access Token          HTTP 401 Unauthorized
Generate Refresh Token
          │
          ▼
Return Tokens to Client
```

---

# 6. Request Flow After Login

Every secured request should follow this flow:

```
Client

│

│ Authorization: Bearer <JWT>

▼

Spring Security Filter Chain

▼

JWT Authentication Filter

▼

Validate Token

▼

Load User Details

▼

Security Context

▼

Controller

▼

Service

▼

Repository
```

Important:

The controller should never manually validate a JWT.

That responsibility belongs entirely to Spring Security.

---

# 7. Why JWT?

Without JWT:

Every request requires:

- username
- password

Problems:

- Credentials travel constantly
- Poor performance
- Poor user experience

With JWT:

Login once.

Receive token.

Use token for future requests.

---

Example

```
POST /login
```

↓

Receive

```
eyJhbGciOiJIUzI1NiIsInR5cCI...
```

↓

Future requests

```
Authorization: Bearer eyJhbGc...
```

---

# 8. Access Token vs Refresh Token

We will implement two different tokens.

## Access Token

Purpose

Access protected APIs.

Lifetime

Short.

Example:

15 minutes.

---

## Refresh Token

Purpose

Request a new Access Token.

Lifetime

Long.

Example:

7 days.

---

Flow

```
Login

↓

Access Token

↓

Expires

↓

Refresh Token

↓

New Access Token
```

---

# 9. Roles

The application should support the following roles.

```
SUPER_ADMIN

ADMIN

MANAGER

CASHIER

INVENTORY_CLERK

ACCOUNTANT
```

Store these as an enum.

Example:

```
common/enums/Role.java
```

---

# 10. Permissions Matrix

| Feature | Super Admin | Admin | Manager | Cashier | Inventory | Accountant |
|----------|------------|--------|----------|----------|------------|-------------|
| Manage Users | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Manage Branches | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Create Sales | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| Inventory | ✓ | ✓ | ✓ | ✗ | ✓ | ✗ |
| Reports | ✓ | ✓ | ✓ | ✗ | ✗ | ✓ |

We won't implement every permission now, but the design should support them.

---

# 11. Security Architecture

Final package structure:

```
security/

├── config
│   └── SecurityConfig.java
│
├── controller
│   └── AuthenticationController.java
│
├── dto
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RefreshTokenRequest.java
│   └── RefreshTokenResponse.java
│
├── filter
│   └── JwtAuthenticationFilter.java
│
├── jwt
│   ├── JwtService.java
│   ├── JwtProperties.java
│   └── TokenType.java
│
├── service
│   └── CustomUserDetailsService.java
│
├── exception
│
└── util
```

---

# 12. Components Explained

## SecurityConfig

Purpose

Configures Spring Security.

Responsibilities

- Register filters
- Configure authentication
- Define public endpoints
- Protect secured endpoints

---

## AuthenticationController

Purpose

Expose authentication endpoints.

Examples

```
POST /api/v1/auth/login

POST /api/v1/auth/refresh

POST /api/v1/auth/logout

GET /api/v1/auth/me
```

---

## JwtAuthenticationFilter

Purpose

Runs before every secured request.

Responsibilities

- Read Authorization header
- Extract JWT
- Validate JWT
- Load authenticated user
- Populate SecurityContext

---

## JwtService

Responsible for

- Generate Access Token
- Generate Refresh Token
- Validate Tokens
- Extract Claims

It should contain no controller logic.

---

## CustomUserDetailsService

Spring Security requires a class capable of loading users.

Responsibilities

- Find user by email
- Convert User entity into Spring Security UserDetails

---

## PasswordEncoder

Responsibilities

- Hash passwords
- Verify passwords

Never decrypt passwords.

BCrypt is a one-way hashing algorithm.

---

# 13. Security Principles

Throughout this story follow these rules.

✓ Never store plain passwords.

✓ Never expose passwords.

✓ Never log passwords.

✓ Never manually compare passwords.

✓ Always use PasswordEncoder.

✓ JWT validation belongs in the filter.

✓ Business logic belongs in services.

✓ Controllers remain thin.

---

# 14. Story Deliverables

By the end of Story 7 the application should support:

- Login
- Logout
- Access Tokens
- Refresh Tokens
- BCrypt password hashing
- Protected APIs
- Role-based authorization
- Current user endpoint
- Authentication tests
- Authorization tests

---

# 15. Definition of Done

Story 7 is complete when:

- [ ] Login endpoint implemented
- [ ] BCrypt integrated
- [ ] JWT generation working
- [ ] JWT validation working
- [ ] Refresh token flow working
- [ ] SecurityFilterChain configured
- [ ] UserDetailsService implemented
- [ ] Protected endpoints secured
- [ ] Roles implemented
- [ ] Automated tests passing
- [ ] Manual QA completed


# 16. Database Changes

Authentication requires additional fields in the users table.

## Password Storage

Passwords must never be stored as plain text.

Instead of:

```
Password123
```

Store:

```
$2a$10$5EyxgQv...
```

This value is produced by BCrypt.

---

## Role

Each user must have one role.

Example

```
SUPER_ADMIN

ADMIN

MANAGER

CASHIER

INVENTORY_CLERK

ACCOUNTANT
```

Store as a String Enum.

Example

```java
@Enumerated(EnumType.STRING)
private Role role;
```

---

## Enabled

Login is only allowed when

```
enabled = true
```

Disabled users cannot authenticate.

---

# 17. Flyway Migration

Create a new migration.

```
V2__add_security_columns.sql
```

Migration should:

- Add role column
- Ensure password column exists
- Set default role for existing users
- Update existing passwords if required for local development

Never edit

```
V1__Initial_schema.sql
```

Always create a new migration.

---

# 18. DTO Requirements

Location

```
security/dto
```

---

## LoginRequest

Fields

```java
String email;

String password;
```

Validation

```
@NotBlank

@Email
```

Password

```
@NotBlank
```

---

## LoginResponse

Fields

```java
String accessToken;

String refreshToken;

String tokenType;

long expiresIn;
```

Example

```json
{
    "accessToken":"eyJhbGc...",
    "refreshToken":"eyJhbGc...",
    "tokenType":"Bearer",
    "expiresIn":900
}
```

---

## RefreshTokenRequest

Fields

```java
String refreshToken;
```

---

## RefreshTokenResponse

Fields

```java
String accessToken;

long expiresIn;
```

---

# 19. Authentication API

Base URL

```
/api/v1/auth
```

---

## Login

```
POST /api/v1/auth/login
```

Body

```json
{
    "email":"eric@example.com",
    "password":"Password123"
}
```

Success

```
200 OK
```

Response

```
LoginResponse
```

---

Invalid credentials

```
401 Unauthorized
```

---

## Refresh

```
POST /api/v1/auth/refresh
```

Body

```json
{
    "refreshToken":"eyJhbGc..."
}
```

Success

```
200 OK
```

Returns new Access Token.

---

Invalid token

```
401 Unauthorized
```

---

## Logout

```
POST /api/v1/auth/logout
```

Initially this endpoint may simply return success.

Later stories can implement token blacklisting if required.

---

## Current User

```
GET /api/v1/auth/me
```

Returns

Current authenticated user.

No password.

---

# 20. Security Configuration

Location

```
security/config/SecurityConfig.java
```

Replace the temporary

```
permitAll()
```

configuration.

---

Public Endpoints

Allow anonymous access to:

```
POST /api/v1/auth/login

POST /api/v1/auth/refresh
```

Everything else requires authentication.

---

Configuration responsibilities

- Disable CSRF
- Stateless sessions
- Register JWT filter
- Register AuthenticationProvider
- Configure PasswordEncoder
- Configure AuthenticationManager

---

# 21. Password Encoder

Register

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Purpose

Hash passwords.

---

Creating User

Instead of

```java
user.setPassword(request.password());
```

Do

```java
user.setPassword(
    passwordEncoder.encode(request.password())
);
```

---

Authentication

Never compare

```java
storedPassword.equals(password)
```

Always

```java
passwordEncoder.matches(
    rawPassword,
    encodedPassword
)
```

---

# 22. UserDetailsService

Location

```
security/service
```

Create

```
CustomUserDetailsService
```

Responsibilities

- Find user by email
- Throw UsernameNotFoundException if missing
- Convert User entity into UserDetails

Never return Entity directly.

---

# 23. JWT Service

Location

```
security/jwt
```

Responsibilities

Generate

- Access Token
- Refresh Token

Validate

- Signature
- Expiration
- Username

Extract

- Username
- Claims

Do not mix JWT logic into controllers.

---

# 24. JWT Authentication Filter

Purpose

Runs before controllers.

Flow

```
Incoming Request

↓

Read Authorization Header

↓

Bearer Token?

↓

YES

↓

Extract Token

↓

Validate

↓

Load User

↓

Security Context

↓

Continue Filter Chain
```

If invalid

Return

```
401 Unauthorized
```

---

# 25. Authentication Service

Responsibilities

Login Flow

```
Receive LoginRequest

↓

AuthenticationManager.authenticate()

↓

Credentials Valid?

↓

Generate Tokens

↓

Return LoginResponse
```

Do not manually verify passwords.

AuthenticationManager handles credential verification.

---

# 26. Authorization

Use

```
@EnableMethodSecurity
```

Examples

```java
@PreAuthorize("hasRole('ADMIN')")
```

```java
@PreAuthorize("hasRole('MANAGER')")
```

```java
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
```

Initially protect

User Management endpoints.

Example

```
POST /users

ADMIN only
```

---

# 27. Updating Story 6

Modify User creation.

Old

```
Store password directly
```

New

```
Encode password before saving.
```

No controller changes required.

Only service layer.

---

# 28. Exception Handling

Handle

AuthenticationException

↓

401

---

AccessDeniedException

↓

403

---

ExpiredJwtException

↓

401

---

UsernameNotFoundException

↓

401

---

Response format

Reuse

```
ErrorResponse
```

from Story 6.

Keep API responses consistent.

---

# 29. Logging

Log

✓ Successful login

✓ Failed login

✓ Refresh Token request

✓ Logout

Never log

✗ Password

✗ JWT

✗ Refresh Token

---

# 30. Coding Standards

Controllers

Small.

No JWT logic.

---

Services

Authentication logic only.

---

JWT Service

Token logic only.

---

Filter

Authentication only.

---

Repositories

Database access only.

---

# 31. Security Best Practices

Always

✓ HTTPS in production

✓ BCrypt

✓ Short-lived Access Tokens

✓ Longer Refresh Tokens

✓ Stateless sessions

Never

✗ Store passwords in plain text

✗ Return passwords

✗ Log tokens

✗ Store JWT in Local Storage for production browser applications (discussion deferred to frontend architecture)

---

# 32. Acceptance Criteria

Story 7 implementation is accepted when

✓ Login works

✓ Password hashing works

✓ JWT generated

✓ JWT validated

✓ Protected endpoint rejects anonymous user

✓ Refresh Token works

✓ Current User endpoint works

✓ Roles enforced

✓ Error responses consistent


# 33. Testing Strategy

Authentication is security-critical functionality.

Every component must be tested.

Testing Pyramid

```
                  Integration Tests
                /-------------------\
              Controller Tests
            /-------------------------\
           Service Unit Tests
         /-----------------------------\
        Repository Tests
```

Testing goals

- Verify login works
- Verify passwords are hashed
- Verify JWT generation
- Verify JWT validation
- Verify authorization
- Verify protected endpoints
- Verify refresh token flow

---

# 34. Unit Tests

Location

```
src/test/java/com/devrick/pos/security
```

Frameworks

- JUnit 5
- Mockito
- AssertJ

---

## AuthenticationService Tests

Mock

- AuthenticationManager
- JwtService
- UserRepository
- PasswordEncoder

---

### Successful Login

Given

Existing user

Valid password

Verify

- authenticate() called
- JWT generated
- Refresh token generated
- LoginResponse returned

---

### Invalid Password

Given

Incorrect password

Expected

```
BadCredentialsException
```

Response

```
401 Unauthorized
```

---

### Disabled User

Given

```
enabled=false
```

Expected

Authentication fails.

---

## JwtService Tests

Verify

Generate Access Token

Verify

Token is not empty.

---

Verify

Username extracted correctly.

---

Verify

Token expires correctly.

---

Verify

Expired token rejected.

---

Verify

Invalid signature rejected.

---

## PasswordEncoder Tests

Verify

```
encode()
```

returns a BCrypt hash.

Verify

```
matches()
```

returns

```
true
```

for the original password.

Verify

Different passwords return

```
false
```

---

# 35. Controller Tests

Location

```
src/test/java/com/devrick/pos/security/controller
```

Use

```
@WebMvcTest
```

Mock

AuthenticationService

---

## Login Endpoint

```
POST /api/v1/auth/login
```

Expected

```
200 OK
```

Verify

Response contains

- accessToken
- refreshToken
- expiresIn

---

## Invalid Login

Expected

```
401 Unauthorized
```

---

## Validation

Missing email

↓

400

---

Missing password

↓

400

---

Invalid email

↓

400

---

## Refresh Endpoint

Expected

```
200 OK
```

New Access Token returned.

---

Invalid Refresh Token

↓

401

---

## Current User Endpoint

Authenticated

↓

200

Anonymous

↓

401

---

# 36. Integration Tests

Location

```
src/test/java/com/devrick/pos/integration
```

Use

```
@SpringBootTest
```

Prefer

Testcontainers PostgreSQL

---

Verify

ApplicationContext loads.

---

Verify

Flyway migration executes.

---

Verify

Login works.

---

Verify

JWT secures endpoints.

---

Verify

Refresh token flow.

---

# 37. Manual QA Guide

Complete every step before marking Story 7 complete.

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

Run application

```bash
./mvnw spring-boot:run
```

Verify

Application starts.

No bean errors.

Flyway succeeds.

---

## Step 3

Verify Password Hashing

Create a user.

Check database.

Password should look similar to

```
$2a$10$7L....
```

It should never be

```
Password123
```

---

## Step 4

Login

Request

```
POST /api/v1/auth/login
```

Body

```json
{
    "email":"eric@example.com",
    "password":"Password123"
}
```

Expected

```
200 OK
```

Response

```json
{
  "accessToken":"...",
  "refreshToken":"...",
  "tokenType":"Bearer",
  "expiresIn":900
}
```

Copy the Access Token.

---

## Step 5

Call Protected Endpoint

Request

```
GET /api/v1/users
```

Header

```
Authorization

Bearer <AccessToken>
```

Expected

```
200 OK
```

---

## Step 6

No Token

Remove Authorization header.

Expected

```
401 Unauthorized
```

---

## Step 7

Invalid Token

Header

```
Bearer abc123
```

Expected

```
401 Unauthorized
```

---

## Step 8

Expired Token

Use an expired token.

Expected

```
401 Unauthorized
```

---

## Step 9

Refresh Token

Call

```
POST /api/v1/auth/refresh
```

Expected

New Access Token returned.

---

## Step 10

Logout

```
POST /api/v1/auth/logout
```

Expected

```
200 OK
```

or

```
204 No Content
```

depending on implementation.

---

## Step 11

Role Verification

Login as

```
ADMIN
```

Verify

```
POST /users
```

works.

---

Login as

```
CASHIER
```

Verify

```
POST /users
```

returns

```
403 Forbidden
```

---

# 38. Common Problems

## 401 Unauthorized

Possible causes

- Missing Authorization header
- Invalid JWT
- Expired JWT
- Wrong signing key

---

## 403 Forbidden

Authentication succeeded.

Authorization failed.

User lacks required role.

---

## Password Never Matches

Possible cause

Comparing strings.

Wrong

```java
password.equals(hash)
```

Correct

```java
passwordEncoder.matches(
    rawPassword,
    encodedPassword
)
```

---

## JwtAuthenticationFilter Never Runs

Check

```
SecurityFilterChain
```

Verify filter registered.

---

## UserDetailsService Never Called

Check

AuthenticationProvider configuration.

---

## Every Endpoint Returns 401

Verify

```
POST /api/v1/auth/login
```

is permitted.

---

# 39. Logging

Log

✓ Login success

✓ Login failure

✓ Refresh request

✓ Logout

Never log

✗ Password

✗ Access Token

✗ Refresh Token

---

# 40. Performance

JWT validation should not query unnecessary tables.

Keep tokens small.

Avoid unnecessary database lookups.

---

# 41. Security Checklist

✓ BCrypt

✓ Stateless sessions

✓ CSRF disabled for REST API

✓ JWT validation filter

✓ Passwords never returned

✓ Refresh Token implemented

✓ Role checks

✓ AuthenticationManager used

✓ PasswordEncoder used

---

# 42. Acceptance Criteria

Story accepted when

✓ Login successful

✓ Invalid login rejected

✓ BCrypt hashing working

✓ JWT generation working

✓ JWT validation working

✓ Refresh endpoint working

✓ Protected endpoints secured

✓ Roles enforced

✓ Current user endpoint working

✓ Automated tests passing

✓ Manual QA completed

---

# 43. Definition of Done

Before merging

- [ ] BUILD SUCCESS
- [ ] Spotless passes
- [ ] Checkstyle passes
- [ ] Unit tests pass
- [ ] Controller tests pass
- [ ] Integration tests pass
- [ ] Passwords hashed
- [ ] JWT verified
- [ ] Refresh token tested
- [ ] Protected endpoints tested
- [ ] Unauthorized requests return 401
- [ ] Forbidden requests return 403
- [ ] Manual QA completed
- [ ] Code reviewed

---

# 44. Instructions for Codex

You are implementing Story 7 (Authentication & Authorization) for an enterprise Inventory & POS SaaS.

Read AGENTS.md before making changes.

Follow the package-by-feature architecture.

Do not modify unrelated modules.

Implement:

- BCrypt password hashing
- JWT authentication
- Refresh token flow
- SecurityFilterChain
- AuthenticationManager
- AuthenticationProvider
- CustomUserDetailsService
- JwtAuthenticationFilter
- AuthenticationController
- AuthenticationService
- Login and refresh DTOs
- Role-based authorization
- Method security
- Current user endpoint

Use constructor injection.

Keep controllers thin.

Keep JWT logic inside JwtService.

Keep authentication logic inside AuthenticationService.

Do not expose JPA entities through the API.

Update Story 6 user creation to hash passwords using PasswordEncoder.

Write comprehensive:

- Unit tests
- Controller tests
- Integration tests

Run:

```bash
./mvnw clean verify
```

Ensure:

- Spotless passes
- Checkstyle passes
- All tests pass

Do not leave TODOs or placeholder implementations.

Only modify files required for Story 7.
