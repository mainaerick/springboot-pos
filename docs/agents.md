# AGENTS.md

# Enterprise Engineering Guide

This document defines the engineering standards, architectural decisions, coding conventions, and development workflow for this repository.

Every AI coding agent (Codex, ChatGPT, Cursor, Claude Code, GitHub Copilot, etc.) must follow this document when implementing features.

This repository is intended to become a production-ready, enterprise-grade, multi-tenant Inventory & Point of Sale (POS) SaaS platform. The objective is to build software that could be deployed to real customers, not tutorial-level code.

When implementing any feature, always optimize for:

- Maintainability
- Scalability
- Security
- Readability
- Testability
- Performance
- Extensibility

Never optimize only for writing the least amount of code.

---

# 1. Project Overview

Project Name

Enterprise Multi-Tenant Inventory & POS SaaS

Target Customers

- Retail Shops
- Restaurants
- Pharmacies
- Hardware Stores
- Wholesalers
- Supermarkets

Future Modules

- Authentication
- User Management
- Branch Management
- Product Management
- Inventory
- Purchases
- Sales
- Customers
- Suppliers
- Expenses
- Reports
- Barcode Management
- Offline Synchronization
- Mobile API
- MPesa Integration
- Notifications
- AI Demand Forecasting
- Dashboard & Analytics

The project should always be designed so that future modules can be added without requiring architectural rewrites.

---

# 2. Technology Stack

Backend

- Java 17
- Spring Boot 4.x
- Spring Security
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Redis (future)
- JWT Authentication
- Docker
- Testcontainers
- Maven

Frontend

- React
- TypeScript
- Vite
- Tailwind CSS

Testing

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers

Developer Tooling

- Spotless
- Checkstyle
- EditorConfig

---

# 3. Engineering Principles

Always follow SOLID principles.

Prefer clean architecture over clever code.

Prefer readability over brevity.

Prefer explicit code over magic.

Favor maintainability over premature optimization.

Avoid unnecessary abstractions.

Do not introduce complexity unless there is a measurable benefit.

Every implementation should be understandable by a new developer joining the team.

---

# 4. Architecture

Follow Package-by-Feature architecture.

Correct example

user/
auth/
inventory/
sales/
purchase/
supplier/
customer/
report/

Each feature owns its own components.

Example

user

- controller
- dto
- entity
- mapper
- repository
- service

Do NOT organize the project by technical layer.

Avoid structures like

controller/
entity/
repository/
service/

at the application root.

---

# 5. Module Independence

Each module should have minimal coupling with other modules.

Expose functionality through services instead of directly accessing repositories across modules whenever possible.

Modules should be independently maintainable.

---

# 6. Dependency Injection

Always use constructor injection.

Preferred

@RequiredArgsConstructor

Never use

@Autowired field injection

Example

Good

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

}

Bad

@Autowired
private UserRepository repository;

---

# 7. DTO Standards

Use Java Records whenever possible.

Request DTOs

CreateUserRequest

UpdateUserRequest

LoginRequest

Response DTOs

UserResponse

LoginResponse

Never expose JPA entities through REST APIs.

Controllers return DTOs only.

Entities remain internal.

---

# 8. Entity Standards

All entities extend BaseEntity.

Every entity inherits

- UUID id
- createdAt
- updatedAt
- createdBy
- updatedBy
- version
- deleted

Never duplicate these fields.

Primary keys use UUID.

Never use auto-increment Long IDs.

Use

GenerationType.UUID

All timestamps use

Instant

Never LocalDateTime unless explicitly required.

---

# 9. Database Standards

Flyway is the single source of truth.

Hibernate must never create or modify production schemas.

Always use

spring.jpa.hibernate.ddl-auto=validate

Never

create

create-drop

update

Every schema change requires a Flyway migration.

Migration naming

V1\_\_Initial_schema.sql

V2\_\_Create_users_table.sql

V3\_\_Create_products_table.sql

Never modify an already-applied migration.

Create a new migration instead.

Database naming

Tables

snake_case

plural

Examples

users

products

sales

purchase_orders

Columns

snake_case

Examples

first_name

created_at

updated_by

Foreign Keys

Always name constraints explicitly where practical.

Indexes

Create indexes for

- foreign keys
- frequently searched columns
- unique business identifiers

---

# 10. Auditing

Use Spring Data JPA Auditing.

All entities inherit audit fields from BaseEntity.

Use

@CreatedDate

@LastModifiedDate

@CreatedBy

@LastModifiedBy

Never manually populate audit fields inside services.

---

# 11. Soft Delete

Prefer soft deletes.

Records should generally not be physically removed.

Use

deleted = true

Historical data must remain available.

---

# 12. Optimistic Locking

All entities should support optimistic locking.

Use

@Version

This prevents lost updates.

---

# 13. Repository Rules

Repositories only perform persistence.

Allowed

findByEmail()

existsByEmail()

findById()

save()

Not allowed

Business calculations

Validation

Sending emails

Generating reports

Repositories should never contain business logic.

---

# 14. Service Rules

Services contain business logic.

Services coordinate

Repositories

Validation

Transactions

Domain rules

External integrations

Controllers should remain thin.

---

# 15. Controller Rules

Controllers

Receive requests

Validate input

Call services

Return DTOs

Controllers must never contain business logic.

REST endpoints use plural nouns.

Correct

/api/users

/api/products

/api/customers

/api/sales

Incorrect

/api/user

/api/product

---

# 16. Validation

Validate all incoming requests.

Use Jakarta Validation.

Examples

@NotBlank

@NotNull

@Email

@Positive

@Size

Never trust client input.

---

# 17. Security Standards

Passwords

Always hash passwords.

Never store plain text passwords.

Never log passwords.

Never log JWT tokens.

Never expose stack traces.

Follow least privilege.

Authentication

JWT

Authorization

RBAC

Security must be implemented in the service and security layers.

---

# 18. Logging Standards

Use SLF4J.

Never use

System.out.println()

Use log levels correctly.

TRACE

Very detailed diagnostics.

DEBUG

Development diagnostics.

INFO

Business events.

WARN

Recoverable issues.

ERROR

Unexpected failures.

Never log

Passwords

Secrets

JWTs

Database credentials

API keys

---

# 19. Exception Handling

Use custom exceptions.

Example

UserNotFoundException

DuplicateEmailException

Global exception handling must use

@RestControllerAdvice

Never return null to indicate failure.

Never swallow exceptions silently.

---

# 20. Mapping

Separate Entities from DTOs.

Use dedicated mapper classes.

Never map inside controllers.

Never expose entities.

---

# 21. Configuration

Use configuration classes.

Organize by concern.

Example

config/

audit/

security/

web/

cache/

openapi/

Avoid placing every configuration class directly inside config.

---

# 22. Package Naming

Packages

lowercase

Classes

PascalCase

Methods

camelCase

Variables

camelCase

Constants

UPPER_SNAKE_CASE

Avoid abbreviations.

Prefer expressive names.

---

# 23. Method Design

Methods should

Do one thing.

Be small.

Have meaningful names.

Avoid deeply nested logic.

Extract reusable private methods where appropriate.

---

# 24. Code Formatting

Formatting is automatic.

Use

Spotless

EditorConfig

Checkstyle

Never manually fight the formatter.

---

# 25. Comments

Code should be self-explanatory.

Do not comment obvious code.

Comment

Business rules

Algorithms

Architectural decisions

Trade-offs

Use JavaDoc for public APIs when appropriate.

---

# 26. Documentation

Architecture changes require documentation.

Update ADRs whenever architectural decisions change.

Document

Business rules

Complex workflows

External integrations

Database decisions

---

# 27. Multi-Tenancy

Always consider future multi-tenancy.

Avoid writing code that assumes a single business.

Entities should eventually support tenant ownership.

Do not hardcode tenant-specific assumptions.

---

# 28. Performance

Avoid N+1 queries.

Prefer pagination.

Use indexes.

Load only required data.

Avoid eager loading unless justified.

Think about scalability.

---

# 29. Testing Standards

Every feature should include testing.

Types

Unit Tests

Integration Tests

Repository Tests

Controller Tests where appropriate

Use

JUnit 5

Mockito

Spring Boot Test

Testcontainers

Mock external integrations.

Never skip tests to make builds pass.

---

# 30. Build Quality

Every build should pass

Spotless

Checkstyle

Compilation

Tests

Flyway

No warnings should be ignored without justification.

---

# 31. Git Standards

Branch names

feature/user-registration

feature/inventory

bugfix/login

refactor/security

Commit messages

Follow Conventional Commits.

Examples

feat(user): create user entity

feat(auth): implement JWT authentication

fix(user): validate duplicate email

refactor(common): simplify mapper

docs(adr): add auditing decision

test(user): add repository tests

---

# 32. Pull Request Standards

Keep pull requests focused.

Do not mix unrelated refactoring with feature work.

Every pull request should

Compile successfully.

Pass tests.

Follow formatting rules.

Follow architecture.

---

# 33. AI Agent Responsibilities

When implementing any feature

Understand the existing architecture first.

Reuse existing components.

Avoid duplicate code.

Follow package-by-feature.

Maintain consistency.

Do not introduce unnecessary libraries.

Do not perform unrelated refactoring.

Respect existing coding standards.

If requirements are ambiguous

Choose the most maintainable enterprise solution.

---

# 34. AI Prompt Contract

Story-specific prompts override this document only for the requested implementation.

If a prompt conflicts with this document

Follow the story prompt.

Preserve overall architecture.

Never silently change established standards.

Explain significant architectural trade-offs.

---

# 35. Enterprise Mindset

Before implementing any feature ask

Is it secure?

Is it scalable?

Is it maintainable?

Is it testable?

Is it extensible?

Is it multi-tenant ready?

Does it preserve backward compatibility?

Would this implementation be acceptable in a production SaaS system?

Avoid tutorial shortcuts.

Prefer enterprise-grade solutions.

---

# 36. Definition of Done

A feature is complete only when

✓ Code compiles

✓ Spotless passes

✓ Checkstyle passes

✓ Tests pass

✓ Flyway migration succeeds

✓ Documentation updated

✓ ADR updated if architecture changed

✓ No unrelated code modified

✓ Feature follows project architecture

---

# 37. Things the AI Must Never Do

Never

- Disable tests to make builds pass.
- Remove validation.
- Expose JPA entities through REST.
- Hardcode secrets.
- Store passwords in plain text.
- Log sensitive information.
- Introduce unrelated refactoring.
- Break package-by-feature architecture.
- Use field injection.
- Replace Flyway with Hibernate schema generation.
- Ignore Spotless violations.
- Ignore Checkstyle violations.
- Introduce unnecessary dependencies.
- Modify previously executed Flyway migrations.
- Implement features outside the requested scope unless explicitly instructed.

When in doubt, choose the solution that best aligns with enterprise software engineering practices, long-term maintainability, and production readiness.
