package com.novabank.risk.config;
import org.springframework.boot.context.properties.ConfigurationProperties; import java.math.BigDecimal;
@ConfigurationProperties("novabank.risk")
public record RiskProperties(String ruleVersion, BigDecimal highValueReview, BigDecimal highValueDeny, int velocityCount, int velocityWindowMinutes, String paymentServiceUri) {}
