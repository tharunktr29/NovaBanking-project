package com.novabank.shared.operations;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
public final class OutboxMetricsBinder implements MeterBinder {
 private final JdbcTemplate jdbc;
 public OutboxMetricsBinder(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public void bindTo(MeterRegistry registry){
  Gauge.builder("novabank.outbox.unpublished",this,b->b.count()).description("Unpublished outbox records").register(registry);
  Gauge.builder("novabank.outbox.oldest.seconds",this,b->b.oldest()).baseUnit("seconds").description("Age of oldest unpublished outbox record").register(registry);
 }
 double count(){try{Long value=jdbc.queryForObject("select count(*) from outbox_events where status <> 'PUBLISHED'",Long.class);return value==null?0:value;}catch(DataAccessException ex){return 0;}}
 double oldest(){try{Instant value=jdbc.queryForObject("select min(created_at) from outbox_events where status <> 'PUBLISHED'",Instant.class);return value==null?0:Math.max(0,java.time.Duration.between(value,Instant.now()).toSeconds());}catch(DataAccessException ex){return 0;}}
}
