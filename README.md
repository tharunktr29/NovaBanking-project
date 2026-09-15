# NovaBank

NovaBank is a production-style fictional online banking platform for learning, interview preparation, and portfolio use. It does not integrate with real banks, card networks, payment processors, or money movement systems, and it must not process real financial data.

## Current phase

Phase 7 is implemented as a Java 21/Spring Boot microservices application with production-readiness tooling around the existing Phase 1–6 banking features.

- `shared-kernel` provides shared API errors, correlation, and `BankingEvent` contracts only.
- `auth-service` handles registration, login, JWT access tokens, refresh-token rotation, logout, password reset, mock MFA, password changes, refresh-token revocation, and login activity.
- `customer-service` owns customer profiles, addresses, KYC status, preferences, and profile/preference events.
- `account-service` owns checking/savings accounts, nicknames, current balances, available balances, and account events.
- `transaction-service` owns posted/pending transactions, filtering, sorting, pagination, details, CSV export, idempotent event ingestion, and transactional outbox publishing.
- `card-service` owns debit/credit cards, card details, activation, lock/unlock, replacement requests, spending controls, credit summaries, lifecycle history, idempotent commands, and transactional outbox publishing.
- `payment-service` owns internal transfers, credit-card payments, simulated external payments/payees, one-time schedules, cancellation, payment history, idempotency, and transactional outbox publishing.
- `engagement-service` owns encrypted fictional statements, customer-scoped document downloads, dispute intake, provisional credits, in-app notifications, and notification preferences.
- `api-gateway` routes `/api/**`, validates JWTs, forwards authorization headers, preserves correlation IDs, configures CORS, and applies Redis-backed rate limiting.

## Architecture

```text
apps/web                     React + Vite frontend, port 5173
services/api-gateway         Spring Cloud Gateway edge service, port 8080
services/auth-service        Authentication service, port 8081
services/customer-service    Customer profile/preferences service, port 8082
services/account-service     Account/balance service, port 8083
services/transaction-service Transaction history service, port 8084
services/card-service        Card management service, port 8085
services/payment-service     Payment and transfer service, port 8086
services/engagement-service  Statements, disputes, notifications, port 8087
shared-kernel                Shared DTO/error/event contracts only
infra/postgres/init          Local database bootstrap
infra/kafka                  Local Kafka topic bootstrap
```

Shared modules must not contain JPA entities or business logic owned by a service.

## Ports and databases

| Component | Port | Database |
| --- | ---: | --- |
| Web app | 5173 | n/a |
| API Gateway | 8080 | n/a |
| Auth Service | 8081 | `novabank_auth` |
| Customer Service | 8082 | `novabank_customer` |
| Account Service | 8083 | `novabank_account` |
| Transaction Service | 8084 | `novabank_transaction` |
| Card Service | 8085 | `novabank_card` |
| Payment Service | 8086 | `novabank_payment` |
| PostgreSQL | 5432 | service-owned databases |
| Redis | 6379 | gateway rate limiting |
| Kafka | 9092 | platform events |

## Card-service overview

The card service owns only fictional, masked demo card data. It never stores or exposes real PANs, CVV/CVC, PINs, magnetic-stripe data, tokens, or encryption keys.

Supported customer APIs:

- `GET /api/cards`
- `GET /api/cards/{cardId}`
- `GET /api/cards/{cardId}/history`
- `POST /api/cards/{cardId}/activate`
- `POST /api/cards/{cardId}/lock`
- `POST /api/cards/{cardId}/unlock`
- `POST /api/cards/{cardId}/replacement-requests`
- `GET /api/cards/{cardId}/controls`
- `PUT /api/cards/{cardId}/controls`

Supported card states:

- `PENDING_ACTIVATION`
- `ACTIVE`
- `LOCKED`
- `REPLACEMENT_REQUESTED`
- `EXPIRED`
- `CLOSED`

State-changing card APIs require an `Idempotency-Key` header. Repeating the same key with the same request replays the original response; reusing the key with different request content returns a conflict. Successful mutations write the card change, lifecycle history, idempotency record, and outbox event in one database transaction.

Seeded demo cards belong to customer `11111111-1111-1111-1111-111111111111`:

- active debit card linked to checking account `44444444-4444-4444-4444-444444444444`
- pending-activation debit card linked to savings account `55555555-5555-5555-5555-555555555555`
- active credit card with fictional credit details

## Kafka topics

Every event follows the shared `BankingEvent` envelope: event id, type, version, timestamp, correlation id, customer id, aggregate id, and payload.

Current local topics include:

- `customer.logged-in`
- `customer.profile-updated`
- `customer.preferences-updated`
- `suspicious-login-detected`
- `notification.requested`
- `account.nickname-updated`
- `transaction.created`
- `transaction.posted`
- `balance.updated`
- `card.created`
- `card.activated`
- `card.locked`
- `card.unlocked`
- `card.replacement-requested`
- `card.controls-updated`
- `payment.scheduled`
- `payment.completed`
- `payment.failed`
- `transfer.created`
- `transfer.completed`
- `transfer.failed`
- `statement.generated`

Card event payloads intentionally include only safe metadata such as card id, linked account id, card type, previous/new status, action, and occurrence time. They do not include masked numbers, last-four values, cardholder names, credit amounts, replacement details, or customer personal data.

## Phase 5 payments and ledger

Customer APIs include `POST /api/transfers/internal`, `POST /api/payments/credit-card`, `POST /api/payments/external`, payment list/detail/history and cancellation endpoints, plus fictional payee CRUD under `/api/payees`. Commands use database-backed idempotency. One-time scheduled orders are claimed safely by the payment scheduler.

Account-service remains the balance source of truth. It posts immutable, balanced ledger transactions with unique business-operation IDs. Internal transfers debit and credit customer accounts atomically; card and simulated external payments balance against hidden clearing accounts. Card application or simulated settlement failure triggers an idempotent reversal. Transaction-service records history-only projections and does not apply balances a second time.

External payments are local simulations. NovaBank never contacts ACH, card, wire, or other financial networks.

## Local run

Start the full application:

```bash
mvn.cmd clean verify
cd apps\web
npm.cmd test
npm.cmd run build
cd ..\..
docker compose --profile app up -d --build
docker compose --profile app ps
```

Or start infrastructure only:

```bash
docker compose up -d postgres redis kafka kafka-init
```

Demo credentials are seeded by Flyway:

- username: `demo.user`
- email: `demo.user@novabank.test`
- password: `NovaBankDemo!2026`

Open the React app at:

```text
http://localhost:5173
```

Backend APIs are available through the gateway at:

```text
http://localhost:8080
```

## Manual Phase 5 smoke test

1. Log in as `demo.user`.
2. Open the dashboard Cards tab.
3. Confirm debit and credit cards load from PostgreSQL.
4. Confirm only masked card numbers appear.
5. Select the pending debit card and activate it.
6. Select an active card and lock it.
7. Unlock the locked card.
8. Change spending controls and verify they remain after refresh.
9. Request a replacement with one of the supported reason codes.
10. Confirm invalid lifecycle transitions are rejected safely.
11. Repeat a command with the same idempotency key through the API and confirm it does not create duplicate changes.
12. Confirm another customer cannot retrieve the card by ID.
13. Confirm account, profile, login activity, and transaction screens still work.
14. Inspect card Kafka events and confirm no sensitive card data is present.
15. Open Transfers and complete an internal transfer; confirm each balance changes once.
16. Retry the same API command with its idempotency key and confirm no duplicate movement.
17. Pay the fictional credit card and confirm funding and credit balances change once.
18. Create and cancel a one-time scheduled payment.
19. Run a simulated external payment and confirm payment history is projected without a second balance update.

## Security notes and limitations

- Defaults in this repository are development-only and must not be used as production secrets.
- Access tokens are short-lived JWTs signed with an environment-configured secret.
- Refresh tokens are hashed, rotated, and revoked on password change.
- Card-service derives customer identity from the verified JWT subject and never trusts customer IDs from the browser.
- Unauthorized or cross-customer card access returns a safe not-found response.
- Debit card balance display uses account-service as the source of truth; card-service does not duplicate checking/savings balances.
- Credit card data is fictional and does not implement payment processing, network authorization, statements, rewards, disputes, PIN display, digital wallets, or external payment-network workflows.

## Phase 6 statements, disputes, and notifications

Phase 6 adds fictional statement generation with AES-GCM encrypted database documents, customer-scoped downloads, dispute intake and status tracking, idempotent ledger-backed provisional credits, in-app notifications, and notification preferences. Email delivery remains a local simulation: preferences are persisted, but NovaBank does not contact external mail providers.

## Phase 7 operations and observability

Phase 7 adds Prometheus metrics, OpenTelemetry tracing, configurable JSON logs, Tempo, Loki, Alloy, provisioned Grafana dashboards, example alert rules, bounded Kafka retry/dead-letter handling, timeouts and circuit-breaker configuration, graceful shutdown, health probes, hardened non-root images, CI security/quality checks, Helm resources, safe read-only k6 tests, AWS reference documentation, and operational runbooks. It adds no new banking functionality.

Start application and observability profiles:

```bash
docker compose --profile app --profile observability up -d --build
docker compose --profile app --profile observability ps
```

Local URLs:

- NovaBank: `http://localhost:5173`
- Gateway: `http://localhost:8080`
- Grafana: `http://localhost:3001` (3000 was already occupied in the verified Windows environment)
- Prometheus: `http://localhost:9090`
- Tempo: `http://localhost:3200`
- Loki: `http://localhost:3100`
- OTLP gRPC/HTTP: `4317` / `4318`

Copy `.env.example` to an ignored `.env` and replace the local Grafana password. Prometheus endpoints are scraped directly on the internal Compose network and are not routed through the public gateway. Full local trace sampling defaults to 100%; the Helm production example uses 10%. See `docs/PHASE7_OPERATIONS.md` and `docs/AWS_REFERENCE_DEPLOYMENT.md`.

Known limitations: the local email channel is simulated; the local Docker socket is mounted read-only into Alloy but still exposes container metadata; production requires managed data services, externally supplied secrets, TLS, and environment-specific alert tuning. Phase 8 is intentionally not implemented.
