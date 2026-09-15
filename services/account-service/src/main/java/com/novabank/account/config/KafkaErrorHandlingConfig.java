package com.novabank.account.config;
import com.novabank.account.exception.AccountException;
import com.novabank.shared.operations.OperationalMetrics;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
@Configuration public class KafkaErrorHandlingConfig {
 @Bean DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object,Object> template, OperationalMetrics metrics){
  var recoverer=new DeadLetterPublishingRecoverer(template,(record,error)->{metrics.deadLetter();return new TopicPartition(record.topic()+".DLT",record.partition());});
  var handler=new DefaultErrorHandler(recoverer,new FixedBackOff(1000L,3L));
  handler.addNotRetryableExceptions(AccountException.class,IllegalArgumentException.class,DataIntegrityViolationException.class);handler.setCommitRecovered(true);return handler;
 }
}
