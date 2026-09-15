package com.novabank.engagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import com.novabank.shared.operations.OperationalMetrics;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import java.time.Duration;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.time.*;
import java.util.*;

@Service
public class EngagementService {
  private final JdbcTemplate db; private final DocumentCryptoService crypto; private final RestClient accounts; private final OperationalMetrics metrics;
  public EngagementService(JdbcTemplate db, DocumentCryptoService crypto,OperationalMetrics metrics,@Value("${novabank.account-service-uri}") String uri){this.db=db;this.crypto=crypto;this.metrics=metrics;var settings=ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofSeconds(2)).withReadTimeout(Duration.ofSeconds(3));this.accounts=RestClient.builder().baseUrl(uri).requestFactory(ClientHttpRequestFactories.get(settings)).build();}
  public List<Statement> statements(UUID customer){return db.query("select id,account_id,period_start,period_end,opening_balance,closing_balance,currency,created_at from statements where customer_id=? order by period_end desc",(r,n)->statement(r),customer);}
  @Transactional public Statement createStatement(UUID customer,StatementRequest x){
    if(x.periodEnd().isBefore(x.periodStart())||x.periodStart().isBefore(x.periodEnd().minusYears(1))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid statement period");
    UUID id=UUID.randomUUID(); Instant now=Instant.now(); String body="NOVABANK FICTIONAL STATEMENT\nAccount: "+x.accountId()+"\nPeriod: "+x.periodStart()+" to "+x.periodEnd()+"\nOpening balance: "+x.openingBalance()+" "+x.currency()+"\nClosing balance: "+x.closingBalance()+" "+x.currency()+"\n\nDemo document - not a bank record.\n";
    var encrypted=crypto.encrypt(body.getBytes(StandardCharsets.UTF_8));
    db.update("insert into statements values (?,?,?,?,?,?,?,?,?,?,?)",id,customer,x.accountId(),x.periodStart(),x.periodEnd(),x.openingBalance(),x.closingBalance(),x.currency(),encrypted.nonce(),encrypted.content(),now);
    notify(customer,"STATEMENT","Statement ready","Your "+x.periodEnd()+" statement is ready."); metrics.success("statement.generate"); return new Statement(id,x.accountId(),x.periodStart(),x.periodEnd(),x.openingBalance(),x.closingBalance(),x.currency(),now);
  }
  public Document document(UUID customer,UUID id){return db.query("select period_end,document_nonce,document_content from statements where id=? and customer_id=?",r->{if(!r.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Statement not found");return new Document("novabank-statement-"+r.getObject(1)+".txt",crypto.decrypt(r.getBytes(2),r.getBytes(3)));},id,customer);}
  public List<Dispute> disputes(UUID customer){return db.query("select id,transaction_id,account_id,reason,details,amount,currency,status,provisional_credit_id,created_at,updated_at from disputes where customer_id=? order by created_at desc",(r,n)->dispute(r),customer);}
  @Transactional public Dispute createDispute(UUID customer,DisputeRequest x,String auth,String correlation){
    UUID id=UUID.randomUUID(); Instant now=Instant.now();
    try {db.update("insert into disputes values (?,?,?,?,?,?,?,?,?,?,?,?)",id,customer,x.transactionId(),x.accountId(),x.reason(),x.details().trim(),x.amount(),x.currency(),"SUBMITTED",null,now,now);} catch(Exception e){throw new ResponseStatusException(HttpStatus.CONFLICT,"A dispute already exists for this transaction");}
    notify(customer,"DISPUTE","Dispute submitted","We received dispute "+id+" and will review it."); metrics.success("dispute.create");
    if (x.amount().compareTo(new BigDecimal("500.00")) <= 0) return credit(customer,id,auth,correlation);
    return disputes(customer).stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();
  }
  @Transactional public Dispute credit(UUID customer,UUID id,String auth,String correlation){
    var d=disputes(customer).stream().filter(v->v.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Dispute not found"));
    if(d.provisionalCreditId()!=null)return d; UUID operation=UUID.randomUUID();
    var request=Map.of("businessOperationId",operation,"type","PROVISIONAL_CREDIT","reference","Provisional credit for dispute "+id,"sourceAccountId","00000000-0000-0000-0000-00000000dd01","destinationAccountId",d.accountId(),"amount",d.amount(),"currency",d.currency());
    accounts.post().uri("/api/internal/ledger/transactions").header(HttpHeaders.AUTHORIZATION,auth).header("X-Correlation-Id",correlation==null?UUID.randomUUID().toString():correlation).body(request).retrieve().toBodilessEntity();
    db.update("update disputes set status='PROVISIONAL_CREDIT_ISSUED',provisional_credit_id=?,updated_at=? where id=? and customer_id=?",operation,Instant.now(),id,customer);
    notify(customer,"DISPUTE","Provisional credit issued","A provisional credit was applied."); metrics.success("dispute.provisional_credit"); return disputes(customer).stream().filter(v->v.id().equals(id)).findFirst().orElseThrow();
  }
  public List<Notification> notifications(UUID customer){return db.query("select id,category,title,message,read_at,created_at from notifications where customer_id=? order by created_at desc limit 100",(r,n)->new Notification(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getObject(5,Instant.class),r.getObject(6,Instant.class)),customer);}
  public void read(UUID customer,UUID id){if(db.update("update notifications set read_at=coalesce(read_at,?) where id=? and customer_id=?",Instant.now(),id,customer)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found");}
  public Preferences preferences(UUID customer){var list=db.query("select in_app,email,security,payments,statements,disputes from notification_preferences where customer_id=?",(r,n)->new Preferences(r.getBoolean(1),r.getBoolean(2),r.getBoolean(3),r.getBoolean(4),r.getBoolean(5),r.getBoolean(6)),customer);return list.isEmpty()?new Preferences(true,true,true,true,true,true):list.getFirst();}
  public Preferences preferences(UUID customer,Preferences p){db.update("insert into notification_preferences values (?,?,?,?,?,?,?,?) on conflict(customer_id) do update set in_app=excluded.in_app,email=excluded.email,security=excluded.security,payments=excluded.payments,statements=excluded.statements,disputes=excluded.disputes,updated_at=excluded.updated_at",customer,p.inApp(),p.email(),p.security(),p.payments(),p.statements(),p.disputes(),Instant.now());return p;}
  private void notify(UUID customer,String category,String title,String message){if(preferences(customer).inApp()){db.update("insert into notifications values (?,?,?,?,?,?,?)",UUID.randomUUID(),customer,category,title,message,null,Instant.now());metrics.success("notification.in_app");}}
  private Statement statement(ResultSet r)throws java.sql.SQLException{return new Statement(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,LocalDate.class),r.getObject(4,LocalDate.class),r.getBigDecimal(5),r.getBigDecimal(6),r.getString(7),r.getObject(8,Instant.class));}
  private Dispute dispute(ResultSet r)throws java.sql.SQLException{return new Dispute(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getString(4),r.getString(5),r.getBigDecimal(6),r.getString(7),r.getString(8),r.getObject(9,UUID.class),r.getObject(10,Instant.class),r.getObject(11,Instant.class));}
  public record Statement(UUID id,UUID accountId,LocalDate periodStart,LocalDate periodEnd,BigDecimal openingBalance,BigDecimal closingBalance,String currency,Instant createdAt){}
  public record StatementRequest(UUID accountId,LocalDate periodStart,LocalDate periodEnd,BigDecimal openingBalance,BigDecimal closingBalance,String currency){}
  public record Document(String filename,byte[] content){}
  public record Dispute(UUID id,UUID transactionId,UUID accountId,String reason,String details,BigDecimal amount,String currency,String status,UUID provisionalCreditId,Instant createdAt,Instant updatedAt){}
  public record DisputeRequest(UUID transactionId,UUID accountId,String reason,String details,BigDecimal amount,String currency){}
  public record Notification(UUID id,String category,String title,String message,Instant readAt,Instant createdAt){}
  public record Preferences(boolean inApp,boolean email,boolean security,boolean payments,boolean statements,boolean disputes){}
}
