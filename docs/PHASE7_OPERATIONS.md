# NovaBank Phase 7 operations

## Local observability

Start with `docker compose --profile app --profile observability up -d --build`. Grafana is at `http://localhost:3001` (host port 3000 was occupied during validation), Prometheus at `http://localhost:9090`, Tempo at `http://localhost:3200`, and Loki at `http://localhost:3100`. Copy `.env.example` to an ignored `.env` and change the local Grafana password. Alloy reads the Docker socket read-only; that socket still grants broad container metadata access and is strictly a local-development choice.

Trace investigation: locate an error in Grafana/Loki using `correlationId`, copy its `traceId`, and open the trace in Explore/Tempo. Logs intentionally exclude tokens, bodies, balances, personal data, statement content, and dispute explanations.

## Runbooks

- Payment/compensation failure: confirm idempotency key and ledger business-operation ID, inspect payment status history and trace, restore the downstream service, then allow the existing compensation path to finish. Never manually repeat a debit.
- Outbox backlog: check `novabank_outbox_*`, database availability, Kafka health, and publisher logs. Restore Kafka before considering replay. Do not edit outbox rows directly.
- Kafka dead letter: inspect headers and exception class without copying payloads. Fix the consumer, then use an approved operations-only replay process preserving the original event ID. Local arbitrary publishing is prohibited.
- Database-pool exhaustion: identify slow operations and pool wait time, reduce traffic, inspect PostgreSQL locks, and scale only after removing leaks.
- Statement failure: verify engagement database, encryption-key reference, and storage trace. Regenerate only through a future approved operations workflow.
- Notification failure: check preference state and local delivery metrics. Phase 7 does not contact an external email provider.
- Rollback: deploy the prior immutable image tags with Helm; do not roll database migrations backward unless a tested forward fix is unavailable.
- CI security failure: review the primary advisory, verify reachability, update dependencies first, and add a narrowly scoped suppression only with owner, expiry, and written rationale.

## Kubernetes and Helm

Run `helm lint deploy/helm/novabank` and `helm template novabank deploy/helm/novabank`. Supply the referenced Kubernetes Secret externally. Only web and the gateway are ingress targets; TLS terminates at the ingress controller. Production uses managed PostgreSQL, Kafka, Redis, and object storage rather than in-chart data services.

## Local scans and performance

Run OWASP Dependency Check and CycloneDX with `mvn org.owasp:dependency-check-maven:check org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom`. Run Gitleaks and Trivy from their pinned containers or CLIs. If an advisory database is unavailable, record the outage and rerun; do not silently suppress the scan. Read-only load testing: `k6 run tests/performance/novabank-readonly.js`.
