# SISM BACKEND KNOWLEDGE BASE

**Generated:** 2026-02-24
**Commit:** auto-generated

## OVERVIEW

Spring Boot 3.2.0 backend for Strategic Indicator Management System. Java 17, Maven build, PostgreSQL database, Flyway migrations.

## STRUCTURE

```
sism-backend/
├── src/main/java/com/sism/
│   ├── config/              # 16 config files
│   ├── controller/          # 18 REST controllers
│   ├── service/             # 14 business services
│   ├── repository/          # 32 JPA repositories
│   ├── entity/              # 31 domain entities
│   ├── dto/                 # Request DTOs
│   ├── vo/                  # Response VOs
│   ├── enums/               # Java enums
│   ├── exception/           # Custom exceptions
│   └── util/                # Utilities
├── database/
│   ├── migrations/          # Flyway scripts
│   ├── seeds/               # Seed data
│   └── scripts/             # DB maintenance
├── scripts/                 # Data sync, deployment
├── docs/                    # Audit reports
└── .github/workflows/       # CI/CD
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| API routes | src/main/java/com/sism/controller/ | 18 controllers, 300+ endpoints |
| Business logic | src/main/java/com/sism/service/ | Service classes, @Transactional |
| Data access | src/main/java/com/sism/repository/ | JPA repositories, custom queries |
| Entity definitions | src/main/java/com/sism/entity/ | 31 JPA entities, relationships |
| Config | src/main/java/com/sism/config/ | Security, CORS, rate limiting |
| Migrations | database/migrations/ | Flyway V1.0, V2, V3 |

## CONVENTIONS

- Layered: Controller → Service → Repository
- DTO/VO separation (strict)
- Flat package structure (no nested domains)
- Dual Repository: UserRepository (custom) + SysUserRepository (basic)
- Flyway migrations (idempotent scripts)
- H2 for unit tests, PostgreSQL for integration
- Global exception handling, custom validation
- Security: JWT auth, rate limiting, CORS, headers

## ANTI-PATTERNS

- @deprecated: IndicatorLevel enum (lines 21, 28), TableRenameListener (DEPRECATED)
- TODO legacy: 7+ removed database field references in services
- Legacy ad-hoc task patterns (Indicator → Task → AdhocTask incomplete)

## COMMANDS

```bash
cd sism-backend
mvn clean install            # Build with tests
mvn spring-boot:run          # Start dev
mvn test                     # Run tests
mvn flyway:info              # Check migrations
mvn verify                   # Quality checks
```

## NOTES

- Test coverage: 64% (342/536 tests), JaCoCo
- Dual repository pattern: separate concerns, but may be overkill
- Migration scripts in `database/migrations/` (not `src/main/resources/`)
- CI: tests allowed to fail non-blocking (ci.yml: `mvn test -B || true`)
