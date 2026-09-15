package com.novabank.risk.service;
import com.novabank.risk.api.RiskModels.*; import com.novabank.risk.config.RiskProperties; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Component;
import java.time.*; import java.util.*;
@Component public class RiskEngine {
 private final RiskProperties p; private final JdbcTemplate jdbc;
 public RiskEngine(RiskProperties p,JdbcTemplate jdbc){this.p=p;this.jdbc=jdbc;}
 public Result evaluate(AssessmentRequest r){var rules=new ArrayList<String>(); int score=0;
  if(r.amount().compareTo(p.highValueDeny())>=0){rules.add("HIGH_VALUE_PAYMENT");score+=100;} else if(r.amount().compareTo(p.highValueReview())>=0){rules.add("HIGH_VALUE_PAYMENT");score+=40;}
  Integer velocity=jdbc.queryForObject("select count(*) from risk_assessments where customer_id=? and created_at>=?",Integer.class,r.customerId(),java.sql.Timestamp.from(Instant.now().minus(Duration.ofMinutes(p.velocityWindowMinutes())))); if(velocity!=null&&velocity>=p.velocityCount()){rules.add("PAYMENT_VELOCITY");score+=25;}
  if(Boolean.TRUE.equals(r.newExternalPayee())){rules.add("NEW_EXTERNAL_PAYEE");score+=25;} if(r.recentFailures()!=null&&r.recentFailures()>=3){rules.add("REPEATED_FAILURES");score+=30;}
  var hour=(r.occurredAt()==null?Instant.now():r.occurredAt()).atZone(ZoneOffset.UTC).getHour(); if(hour<5){rules.add("UNUSUAL_PAYMENT_TIME");score+=15;}
  if(Boolean.TRUE.equals(r.recentlyChangedCard())){rules.add("RECENT_CARD_CHANGE");score+=25;} if(r.recentDisputes()!=null&&r.recentDisputes()>=3){rules.add("MULTIPLE_DISPUTES");score+=35;}
  Integer watched=jdbc.queryForObject("select count(*) from watchlist_entries where active=true and ((subject_type='ACCOUNT' and subject_reference=?) or (subject_type='CARD' and subject_reference=?))",Integer.class,r.accountId(),r.cardId()); if(watched!=null&&watched>0){rules.add("INTERNAL_WATCHLIST");score+=100;}
  score=Math.min(score,100); Decision d=score>=80?Decision.DENY:score>=40?Decision.REVIEW:Decision.ALLOW;
  String safe=d==Decision.ALLOW?"Payment checks completed":d==Decision.REVIEW?"This payment needs an additional security review":"This payment could not be completed after security checks";
  return new Result(score,d,List.copyOf(rules),safe,rules.isEmpty()?"No configured rules triggered":"Triggered deterministic rules: "+String.join(", ",rules)); }
 public record Result(int score,Decision decision,List<String> rules,String safeReason,String explanation){}
}
