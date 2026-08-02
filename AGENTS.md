# AGENTS.md

# Inventory & POS SaaS

Enterprise Spring Boot Development Guide for AI Coding Agents

---

# Purpose

This repository contains an enterprise-grade multi-tenant Inventory & Point of Sale SaaS application.

Every code change should prioritize:

- Maintainability
- Readability
- Testability
- Security
- Scalability
- Clean Architecture

This project is also a learning project.

The developer is learning enterprise Spring Boot, therefore every implementation should be:

- production-ready
- well structured
- easy to understand
- heavily commented where business logic is complex

Avoid clever code.

Prefer readable code.

---

# Technology Stack

Backend

- Java 17
- Spring Boot 4.x
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- Lombok
- Maven

Testing

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers (where applicable)

Future

- Redis
- Docker
- Kubernetes
- React
- JWT Authentication

---

# Architecture

Follow Package-by-Feature.

Example

```
user
├── controller
├── dto
├── entity
├── mapper
├── repository
└── service
```

Do NOT create package-by-layer structures.

Each feature should remain self-contained.

---

# Layer Responsibilities

## Controller

Responsibilities

- REST endpoints
- Request validation
- HTTP responses

Controllers must remain thin.

Never place business logic inside controllers.

---

## Service

Responsibilities

- Business logic
- Transactions
- Validation that belongs to business rules

Services should coordinate repositories.

---

## Repository

Responsibilities

Database access only.

No business logic.

---

## Entity

Represents database tables.

Never expose entities directly through the API.

---

## DTO

Used for API communication.

Always expose DTOs.

Never expose entities.

---

## Mapper

Use MapStruct.

Never manually map entities unless unavoidable.

---

# Dependency Injection

Always use constructor injection.

Never use field injection.

Correct

```java
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository repository;
}
```

Avoid

```java
@Autowired
private UserRepository repository;
```

---

# Validation

Use Jakarta Bean Validation.

Examples

- @NotBlank
- @Email
- @NotNull
- @Size
- @Positive

Validation belongs on DTOs.

---

# Exception Handling

Use GlobalExceptionHandler.

Return consistent ErrorResponse objects.

Never return stack traces.

---

# Database

Use PostgreSQL.

Database schema changes must use Flyway.

Never edit an executed migration.

Always create a new migration.

Correct

```
V2__add_security_columns.sql
```

Wrong

Editing

```
V1__Initial_schema.sql
```

---

# JPA

Prefer LAZY loading.

Avoid EAGER unless absolutely required.

Avoid N+1 query problems.

Use pagination for collections.

---

# Auditing

Use Spring Data JPA Auditing.

Populate:

- createdAt
- updatedAt
- createdBy
- updatedBy

Never update these fields manually.

---

# Security

Passwords

- Never store plain text passwords.
- Always hash using BCrypt.

JWT

- Validation belongs in filters.
- Controllers should never parse JWTs.

Never log:

- passwords
- JWTs
- refresh tokens
- secrets

---

# Logging

Use SLF4J.

Never use

```java
System.out.println()
```

Log:

- startup
- business events
- warnings
- recoverable failures

Do not log sensitive information.

---

# Coding Standards

Methods should be small.

Classes should have one responsibility.

Prefer composition over inheritance.

Prefer immutability where practical.

Avoid static utility classes unless appropriate.

Use expressive names.

Avoid abbreviations.

---

# Records

Prefer Java Records for DTOs.

Example

```java
public record UserResponse(
    UUID id,
    String firstName,
    String email
) {}
```

---

# MapStruct

Use MapStruct for mapping.

Keep mapping logic inside mapper interfaces.

---

# API Design

REST conventions

GET

```
/users
```

POST

```
/users
```

PUT

```
/users/{id}
```

PATCH

```
/users/{id}
```

DELETE

```
/users/{id}
```

Use proper HTTP status codes.

200

201

204

400

401

403

404

409

500

---

# Testing Standards

Every feature should include tests.

Types

- Unit Tests
- Repository Tests
- Controller Tests
- Integration Tests

Test business behaviour.

Do not test framework internals.

---

# Manual QA

Whenever implementing a feature, always provide:

1. Bruno requests
2. Expected HTTP status
3. Expected response
4. Database verification
5. Common failure scenarios

Assume the developer is a beginner.

Explain each verification step.

---

# Beginner-Friendly Explanations

The developer is learning enterprise Spring Boot.

Whenever implementing a feature:

- Explain WHY the code exists.
- Explain HOW Spring uses it.
- Explain WHERE files belong.
- Explain WHAT happens internally.

Do not assume prior Spring Security knowledge.

Teach before coding.

---

# Code Comments

Avoid commenting obvious code.

Comment:

- business rules
- architectural decisions
- complex algorithms

---

# Maven

Before completing work ensure:

```
./mvnw clean verify
```

passes.

If Spotless is configured

Run

```
./mvnw spotless:apply
```

then

```
./mvnw spotless:check
```

If Checkstyle is configured

Run

```
./mvnw checkstyle:check
```

---

# Git

Keep commits focused.

One feature per commit.

Suggested commit format

```
feat(user): implement user management
```

```
feat(auth): implement jwt authentication
```

```
fix(user): prevent duplicate email
```

---

# Pull Requests

Each PR should include

- Summary
- Files changed
- Testing completed
- Screenshots (if UI)
- Known limitations

---

# Story Implementation

The project follows Story-based development.

Before implementing code:

Read the relevant story document.

Example

```
docs/prompts/story-07-authentication.md
```

Treat the story document as the implementation specification.

Do not invent additional requirements.

---

# Large Stories

Some stories are intentionally divided into phases.

Example

Story 7

Phase 1

Phase 2

Phase 3

When asked to implement a phase:

- Read the entire story document.
- Implement ONLY the requested phase.
- Do not start future phases.
- Do not create placeholder implementations.
- Finish the current phase completely.

---

# After Every Phase

Provide

1. Files created
2. Files modified
3. Architectural explanation
4. Beginner explanation
5. Manual QA guide
6. Remaining work

---

# Code Quality

Before finishing:

- No compiler warnings where practical
- No unused imports
- No dead code
- No TODO placeholders
- No duplicated logic

---

# Performance

Avoid unnecessary database calls.

Use pagination.

Avoid loading large collections.

Keep endpoints efficient.

---

# Definition of Done

A task is complete only when:

- Code compiles
- Tests pass
- Spotless passes
- Checkstyle passes
- Manual QA documented
- Beginner explanation provided
- Story acceptance criteria satisfied

---

# AI Agent Behaviour

When implementing features:

1. Read AGENTS.md first.
2. Read the requested story document.
3. Explain the implementation plan before making changes.
4. Stay within the requested scope.
5. Do not modify unrelated modules.
6. Prefer production-ready implementations over quick fixes.
7. If a requirement is ambiguous, state the assumption rather than guessing.
8. Keep changes small and reviewable.

The goal is not only to generate working code, but also to help the developer learn enterprise software engineering practices.
