# ADR-001: Coding Standards

## Status

Accepted

---

## Context

The POS SaaS project is expected to grow into an enterprise-scale application with multiple developers and business modules.

To maintain consistency, coding standards are established before business logic implementation.

---

## Decisions

### Package Structure

Package by Feature.

Example:

- user
- auth
- inventory
- sales

---

### Dependency Injection

Constructor Injection only.

Field injection is prohibited.

---

### DTOs

Use Java Records whenever possible.

---

### Entities

Entities are never exposed directly through REST APIs.

DTOs are mandatory.

---

### Controllers

Controllers contain no business logic.

Business logic belongs in Services.

---

### Repositories

Repositories are responsible only for persistence.

---

### Logging

Use SLF4J.

System.out.println() is prohibited.

---

### Formatting

Automatic formatting is performed using Spotless.

---

### Static Analysis

Checkstyle validates code quality during builds.

---

### REST

REST endpoints use plural nouns.

Example:

/api/users

/api/products

/api/customers

---

### Git

Follow Conventional Commits.

Example:

feat(auth): add login endpoint

fix(user): validate duplicate email

docs(adr): add coding standards

---

## Consequences

The project remains consistent regardless of contributor.

Code reviews focus on architecture and business logic rather than formatting.