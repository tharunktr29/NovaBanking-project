package com.novabank.transaction.service;

import com.novabank.transaction.domain.BankTransaction;
import com.novabank.transaction.dto.TransactionQuery;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;

public final class TransactionSpecifications {
    private TransactionSpecifications() {
    }

    public static Specification<BankTransaction> byCustomerAndQuery(UUID customerId, TransactionQuery query) {
        return (root, criteriaQuery, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("customerId"), customerId));
            if (query.accountId() != null) {
                predicates.add(cb.equal(root.get("accountId"), query.accountId()));
            }
            if (query.status() != null) {
                predicates.add(cb.equal(root.get("status"), query.status()));
            }
            if (query.direction() != null) {
                predicates.add(cb.equal(root.get("direction"), query.direction()));
            }
            if (query.type() != null) {
                predicates.add(cb.equal(root.get("type"), query.type()));
            }
            if (query.category() != null) {
                predicates.add(cb.equal(root.get("category"), query.category()));
            }
            if (query.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("authorizedAt"), query.dateFrom().atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (query.dateTo() != null) {
                predicates.add(cb.lessThan(root.get("authorizedAt"), query.dateTo().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (query.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), query.minAmount()));
            }
            if (query.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), query.maxAmount()));
            }
            if (StringUtils.hasText(query.merchant()) || StringUtils.hasText(query.search())) {
                var merchant = root.join("merchant", JoinType.LEFT);
                if (StringUtils.hasText(query.merchant())) {
                    predicates.add(cb.like(cb.lower(merchant.get("name")), "%" + query.merchant().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(query.search())) {
                    var term = "%" + query.search().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("description")), term),
                            cb.like(cb.lower(merchant.get("normalizedName")), term)
                    ));
                }
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
