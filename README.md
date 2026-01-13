# SISM Backend

Strategic Indicator Management System - Spring Boot Backend

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Build Tool**: Maven
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA (Hibernate)
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Security**: Spring Security + JWT
- **Testing**: JUnit 5, Mockito, jqwik (Property-Based Testing)

## Project Structure

```
src/main/java/com/sism/
├── config/         # Configuration classes
├── controller/     # REST API controllers
├── service/        # Business logic layer
├── repository/     # Data access layer (JPA repositories)
├── entity/         # JPA entities
├── dto/            # Data Transfer Objects (request)
├── vo/             # Value Objects (response)
├── enums/          # Enum types
├── exception/      # Exception handling
├── common/         # Common utilities
└── util/           # Utility classes
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 12+

### Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE sism_dev;
```

2. Update database credentials in `src/main/resources/application-dev.yml`

### Build and Run

```bash
# Build the project
mvn clean install

# Run in development mode
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run in production mode
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### API Documentation

Once the application is running, access the Swagger UI at:
- http://localhost:8080/api/swagger-ui.html

API documentation JSON is available at:
- http://localhost:8080/api/v3/api-docs

## Configuration

### Environment Profiles

- **dev**: Development environment (local database)
- **prod**: Production environment (remote database at 175.24.139.148:8386)

### Environment Variables

For production deployment, set the following environment variables:

```bash
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_jwt_secret_key_at_least_256_bits
export ALLOWED_ORIGINS=https://your-frontend-domain.com
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=IndicatorServiceTest

# Run with coverage
mvn test jacoco:report
```

## Development Guidelines

1. Follow the layered architecture: Controller → Service → Repository
2. Use DTOs for request payloads and VOs for responses
3. Implement business logic in the Service layer
4. Use JPA repositories for data access
5. Handle exceptions with the global exception handler
6. Document APIs with OpenAPI annotations
7. Write both unit tests and property-based tests

## License

Proprietary - SISM Development Team
