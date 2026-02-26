# SISM JAVA PACKAGE KNOWLEDGE BASE

**Generated:** 2026-02-24

## OVERVIEW

Flat package structure for Spring Boot backend. 32 Java files in 11 packages under `com.sism`.

## STRUCTURE

```
com.sism/
├── config/              # 16 config files
├── controller/          # 18 REST controllers
├── service/             # 14 business services
├── repository/          # 32 JPA repositories
├── entity/              # 31 domain entities
├── dto/                 # Request DTOs
├── vo/                  # Response VOs
├── enums/               # Java enums
├── exception/           # Custom exceptions
├── util/                # Utilities
└── common/              # Common components
```

## WHERE TO LOOK

| Task | Location |
|------|----------|
| API endpoints | controller/ |
| Request models | dto/ |
| Response models | vo/ |
| Entity definitions | entity/ |
| Database queries | repository/ |
| Business logic | service/ |
| Security/config | config/ |

## CONVENTIONS

- Flat `com.sism` hierarchy (no nested domains)
- Controllers: RESTful, validation via DTOs
- Services: business logic, @Transactional
- Repositories: interface-based JPA, @Query or Criteria API
- Entities: JPA @Entity, @Id, @GeneratedValue, relationships
- DTOs/VOs: request/response separation, no logic
- Configuration: Spring @Configuration, @Bean, env injection
- Security: JWT filter chain, role-based access

## ANTI-PATTERNS

- No nested packages by domain (legacy ad-hoc patterns exist)
- Legacy task patterns incomplete migration

## NOTES

- 18 controllers expose 300+ endpoints
- 32 repositories with custom queries
- 31 entities with relationships
