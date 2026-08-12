#!/usr/bin/env bash
set -euo pipefail

export PATH="/opt/kafka/bin:${PATH}"

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:9092}"

topics=(
  "customer.logged-in"
  "customer.profile-updated"
  "customer.preferences-updated"
  "suspicious-login-detected"
  "notification.requested"
  "account.nickname-updated"
  "payment.scheduled"
  "payment.completed"
  "payment.failed"
  "transfer.created"
  "transfer.completed"
  "transfer.failed"
  "transaction.created"
  "transaction.posted"
  "balance.updated"
  "card.activated"
  "card.locked"
  "card.unlocked"
  "statement.generated"
)

for topic in "${topics[@]}"; do
  kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions 3 \
    --replication-factor 1
done
