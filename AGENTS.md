# Backend Guide

## Required Stack
- Java: `Spring Boot 3.5.13`
- ORM / persistence: `MyBatis-Plus`
- Authentication / authorization: `Sa-Token`
- Database: `PostgreSQL`
- Cache: `Redis`
- Search: `Elasticsearch`
- Build tool: `Maven`

## Package Convention
- Base package: `com.zokomart.backend`
- Keep modules aligned to business boundaries:
  - `catalog`
  - `cart`
  - `order`
  - `payment`
  - `merchant`
  - `fulfillment`
  - `common`
  - `config`

## Implementation Rules
- Use controller → service → mapper layering unless a stronger local pattern is approved.
- Use `MyBatis-Plus` base mappers for CRUD scaffolding, but keep business rules in services.
- Use `Sa-Token` for login context, permission checks, and route protection.
- Use DTO / request / response objects instead of exposing persistence entities directly.
- Treat order, payment, inventory, and fulfillment state transitions as explicit business logic.

## Validation Rules
- Add Spring tests before implementing behavior changes.
- Prefer targeted `MockMvc` or slice tests first, then broader Spring Boot tests.
- Do not claim backend readiness without at least a passing bootstrapping test.
- Use repo-local Maven settings when running commands:
  - `C:\apache-maven-3.9.11\bin\mvn.cmd -f backend\pom.xml -gs backend\maven-settings.xml -s backend\maven-settings.xml test`
