package com.novabank.risk.api;
import com.novabank.risk.api.RiskModels.*; import com.novabank.risk.service.RiskOperationsService; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/risk") public class RiskController {private final RiskOperationsService service; public RiskController(RiskOperationsService s){service=s;}
 @PostMapping("/internal/assessments") AssessmentResponse assess(@Valid @RequestBody AssessmentRequest r){return service.assess(r);}
 @GetMapping("/assessments/operation/{ref}") AssessmentResponse assessment(@PathVariable String ref){return service.findByOperation(ref).orElseThrow();}
 @GetMapping("/cases") List<Map<String,Object>> cases(@RequestParam(required=false)String status,@RequestParam(required=false)String priority){return service.cases(status,priority);}
 @PostMapping("/cases/{id}/actions") @ResponseStatus(HttpStatus.NO_CONTENT) void act(@PathVariable UUID id,@RequestBody CaseActionRequest r,Authentication a,@RequestHeader(HttpHeaders.AUTHORIZATION)String auth,@RequestHeader(value="X-Correlation-ID",required=false)UUID corr){service.act(id,r,a.getName(),auth,corr==null?UUID.randomUUID():corr);}
 @GetMapping("/watchlist") List<Map<String,Object>> watch(){return service.watchlist();}
 @PostMapping("/watchlist") ResponseEntity<Map<String,UUID>> watch(@Valid @RequestBody WatchlistRequest r,Authentication a,@RequestHeader(value="X-Correlation-ID",required=false)UUID corr){return ResponseEntity.status(201).body(Map.of("id",service.watch(r,a.getName(),corr==null?UUID.randomUUID():corr)));}
}
