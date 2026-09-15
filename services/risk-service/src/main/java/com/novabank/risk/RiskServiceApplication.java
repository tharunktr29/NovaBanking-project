package com.novabank.risk;
import com.novabank.risk.config.RiskProperties;
import org.springframework.boot.SpringApplication; import org.springframework.boot.autoconfigure.SpringBootApplication; import org.springframework.boot.context.properties.EnableConfigurationProperties; import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication @EnableScheduling @EnableConfigurationProperties(RiskProperties.class)
public class RiskServiceApplication { public static void main(String[] args){SpringApplication.run(RiskServiceApplication.class,args);} }
