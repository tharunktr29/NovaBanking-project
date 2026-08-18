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
  "payment.processing"
  "payment.completed"
  "payment.failed"
  "payment.cancelled"
  "payment.events"
  "internal-transfer.completed"
  "credit-card-payment.applied"
  "external-payment.completed"
  "account-funding.requested"
  "account-funding.completed"
  "account-funding.rejected"
  "payment-reversal.requested"
  "payment-reversal.completed"
  "payment-reversal.failed"
  "transfer.created"
  "transfer.completed"
  "transfer.failed"
  "transaction.created"
  "transaction.posted"
  "balance.updated"
  "card.created"
  "card.activated"
  "card.locked"
  "card.unlocked"
  "card.replacement-requested"
  "card.controls-updated"
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
