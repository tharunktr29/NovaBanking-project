# NovaBank

NovaBank is a production-style mock online banking platform for learning, interview preparation, and portfolio use.

This project intentionally uses fictional customers, accounts, cards, transactions, and payments. It does not integrate with real banking systems and must not process real money.

## Current phase

Phase 1 is implemented as a runnable foundation:

- Maven parent project using Java 21
- `shared-kernel` for cross-service API/error/event contracts
- `auth-service` for demo registration, login, JWT access tokens, refresh-token rotation, logout, forgot/reset password simulation, failed-login lockout, audit records, and Kafka login events
- `api-gateway` for `/api/**` routing, JWT validation, correlation IDs, CORS, standardized auth errors, and Redis-backed rate-limit configuration
- Docker Compose for PostgreSQL, Redis, Kafka, and Kafka topic creation
- React/Vite/TypeScript frontend with NovaBank login, forgot-password, MFA, and starter dashboard routes

## Architecture

```text
apps/web                     React + Vite frontend
services/api-gateway         Spring Cloud Gateway edge service, port 8080
services/auth-service        Spring Boot authentication service, port 8081
shared-kernel                Shared DTO/error/event contracts only
infra/postgres/init          Local database bootstrap
infra/kafka                  Local topic bootstrap
```

Shared modules must not contain JPA entities or business logic owned by a service.

## Ports

| Component | Port |
| --- | ---: |
| Web app | 5173 |
| API Gateway | 8080 |
| Auth Service | 8081 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 |

## Database strategy

Each microservice owns its own database or schema. Phase 1 creates `novabank_auth`. Later phases should add separate databases such as `novabank_customer`, `novabank_account`, `novabank_transaction`, and so on. Services must communicate through APIs/events, not direct cross-service database joins.

## Kafka topic plan

Phase 1 creates the topic list needed across the platform:

- `customer.logged-in`
- `suspicious-login-detected`
- `notification.requested`
- `transaction.created`
- `transaction.posted`
- `balance.updated`
- `card.activated`
- `card.locked`
- `card.unlocked`
- `payment.scheduled`
- `payment.completed`
- `payment.failed`
- `transfer.created`
- `transfer.completed`
- `transfer.failed`
- `statement.generated`

Every event follows the shared `BankingEvent` envelope: event id, type, version, timestamp, correlation id, customer id, aggregate id, and payload.

## Local run

Start infrastructure:

```bash
docker compose up -d postgres redis kafka kafka-init
```

Run backend services:

```bash
mvn clean verify
mvn -pl services/auth-service spring-boot:run
mvn -pl services/api-gateway spring-boot:run
```

Run frontend:

```bash
cd apps/web
npm install
npm run dev
```

Demo credentials are seeded by Flyway:

- username: `demo.user`
- email: `demo.user@novabank.test`
- password: `NovaBankDemo!2026`

## Security notes

- Never commit real secrets. Defaults in this repository are development-only.
- Access tokens are short-lived JWTs signed with an environment-configured secret.
- Refresh tokens are stored hashed and rotated on refresh.
- Login errors are intentionally generic.
- Sensitive values such as passwords and tokens are never logged.
