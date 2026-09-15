# NovaBank Phase 8: risk operations

> NovaBank is a fictional demonstration system. It is not a production fraud, AML, KYC, sanctions-screening, or regulatory-compliance product. Rules and data are intentionally simplified and fictional.

## Architecture and decision flow

`risk-service` runs on port 8088 with its own `novabank_risk` PostgreSQL database. Payment service calls `POST /api/risk/internal/assessments` before any ledger post, including when a scheduled payment becomes due. The risk service never changes balances. A deterministic result is `ALLOW`, `REVIEW`, or `DENY`: allow proceeds; review persists `UNDER_REVIEW` without moving money; deny persists `DECLINED` without a ledger call. Analyst approval calls the payment service under a pessimistic payment lock. Completed approvals and rejected reviews are idempotent, so retries cannot post twice.

## Rules

The versioned demo rules are `HIGH_VALUE_PAYMENT`, `PAYMENT_VELOCITY`, `NEW_EXTERNAL_PAYEE`, `REPEATED_FAILURES`, `UNUSUAL_PAYMENT_TIME`, `RECENT_CARD_CHANGE`, `INTERNAL_WATCHLIST`, and `MULTIPLE_DISPUTES`. Scores and thresholds are configuration, not machine learning. Assessments store the rule version, triggered codes, score, safe customer reason, analyst explanation, operation/customer references, timestamp, correlation ID, and a unique assessment ID. Internal explanations and rule configuration are restricted to back-office roles.

## Cases, roles, and security

Alerts create cases with `OPEN`, `ASSIGNED`, `INVESTIGATING`, `AWAITING_CUSTOMER`, `RESOLVED`, or `ESCALATED` lifecycle states and `FALSE_POSITIVE`, `PAYMENT_APPROVED`, `PAYMENT_REJECTED`, `ACCOUNT_RESTRICTED`, or `CARD_RESTRICTED` resolutions. Notes, status history, and audit history are append-only; no update/delete API exists. Evidence references and watchlist subjects store references rather than sensitive values.

Backend authorization defines `CUSTOMER`, `SUPPORT_AGENT`, `FRAUD_ANALYST`, and `OPERATIONS_ADMIN`. Customers cannot call risk-operations APIs or see the navigation. Support has read access; analysts and operations staff investigate cases, while payment approval/rejection is additionally enforced in payment service. Demo users use BCrypt through the existing password encoder: `support.agent`, `fraud.analyst`, and `operations.admin`, each with the local-only password documented in `DataSeeder`.

## Events and reliability

The transactional risk outbox records `risk.assessment.completed`, `risk.alert.created`, `risk.case.created`, `risk.case.assigned`, `risk.case.resolved`, `risk.payment.approved`, and `risk.payment.rejected`, with correlation/causation fields. `processed_events` provides a duplicate-delivery boundary and `risk.events.DLT` is provisioned. Payment operation references and assessment IDs are unique; payment rows are locked during review decisions and the ledger continues using its immutable unique business-operation reference.

## APIs and UI

- `POST /api/risk/internal/assessments` — idempotent service screening.
- `GET /api/risk/assessments/operation/{reference}` — assessment explanation for authorized staff.
- `GET /api/risk/cases?status=&priority=` — filtered queue.
- `POST /api/risk/cases/{id}/actions` — assignment, append-only note, resolution, approval/rejection.
- `GET|POST /api/risk/watchlist` — list/add internal entries.

The protected React `/risk-operations` area supplies queue filters, loading/empty/error states, an investigation dialog, append-only notes, sensitive-action confirmation, and duplicate-action prevention. Customer payment history receives only a safe status and customer-safe reason.

## Observability and operation

Prometheus scrapes port 8088. Metrics cover assessments, decision outcomes, and evaluation latency; the Grafana `NovaBank Risk Operations` dashboard visualizes them. Alerts cover service availability, review backlog, latency, and unresolved high-priority cases. Labels and structured logs must not contain customer IDs, account/card numbers, tokens, notes, or rule thresholds.

Start locally with `docker compose --profile app up -d --build` and `docker compose --profile observability up -d`. Check `docker compose ps`, `http://localhost:8088/actuator/health`, Prometheus targets, and Grafana provisioning. Verify low value allow, configured high value review, analyst approve once, analyst reject with no ledger entry, repeated idempotency keys, and customer authorization denial.

## Demo limitations

This phase uses deterministic illustrative signals supplied by NovaBank services. It does not connect to bureaus, card networks, identity providers, sanctions lists, device intelligence, or external notification delivery. Production use would require threat modeling, privacy/legal review, segregation of duties, resilient event publication/consumption, key management, retention controls, and independently validated models/rules.
