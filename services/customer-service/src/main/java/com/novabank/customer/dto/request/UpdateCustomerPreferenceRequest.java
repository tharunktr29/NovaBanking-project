package com.novabank.customer.dto.request;

public record UpdateCustomerPreferenceRequest(
        boolean emailAlerts,
        boolean smsAlerts,
        boolean pushAlerts,
        boolean securityAlerts,
        boolean paymentAlerts,
        boolean lowBalanceAlerts,
        boolean paperlessStatements
) {
}
