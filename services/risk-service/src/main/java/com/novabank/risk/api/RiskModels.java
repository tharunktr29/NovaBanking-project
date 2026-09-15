package com.novabank.risk.api;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.Instant; import java.util.List; import java.util.UUID;
public final class RiskModels { private RiskModels(){}
 public enum Decision { ALLOW, REVIEW, DENY }
 public record AssessmentRequest(@NotNull UUID customerId,@NotBlank String operationReference,@NotBlank String operationType,@NotNull @DecimalMin("0.01") BigDecimal amount,UUID accountId,UUID cardId,Boolean newExternalPayee,Integer recentFailures,Boolean recentlyChangedCard,Integer recentDisputes,Instant occurredAt,@NotNull UUID correlationId){}
 public record AssessmentResponse(UUID assessmentId,UUID customerId,String operationReference,int riskScore,Decision decision,List<String> triggeredRuleCodes,String customerSafeReason,String internalAnalystExplanation,String ruleVersion,Instant timestamp,UUID correlationId){}
 public record CaseActionRequest(String assignee,String status,String resolution,String note){}
 public record WatchlistRequest(@NotBlank String subjectType,@NotNull UUID subjectReference,@NotBlank @Size(max=160) String reason){}
}
