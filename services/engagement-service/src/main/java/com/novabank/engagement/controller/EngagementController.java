package com.novabank.engagement.controller;

import com.novabank.engagement.service.EngagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class EngagementController {
  private final EngagementService service;
  public EngagementController(EngagementService service){this.service=service;}
  private UUID customer(Authentication a){return UUID.fromString(a.getName());}
  @GetMapping("/api/statements") Object statements(Authentication a){return service.statements(customer(a));}
  @PostMapping("/api/statements") Object statement(Authentication a,@RequestBody EngagementService.StatementRequest request){return service.createStatement(customer(a),request);}
  @GetMapping("/api/statements/{id}/document") ResponseEntity<byte[]> document(Authentication a,@PathVariable UUID id){var d=service.document(customer(a),id);return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+d.filename()+"\"").body(d.content());}
  @GetMapping("/api/disputes") Object disputes(Authentication a){return service.disputes(customer(a));}
  @PostMapping("/api/disputes") Object dispute(Authentication a,@RequestBody EngagementService.DisputeRequest request,HttpServletRequest http){return service.createDispute(customer(a),request,http.getHeader(HttpHeaders.AUTHORIZATION),http.getHeader("X-Correlation-Id"));}
  @GetMapping("/api/notifications") Object notifications(Authentication a){return service.notifications(customer(a));}
  @PostMapping("/api/notifications/{id}/read") ResponseEntity<Void> read(Authentication a,@PathVariable UUID id){service.read(customer(a),id);return ResponseEntity.noContent().build();}
  @GetMapping("/api/notification-preferences") Object preferences(Authentication a){return service.preferences(customer(a));}
  @PutMapping("/api/notification-preferences") Object preferences(Authentication a,@RequestBody EngagementService.Preferences p){return service.preferences(customer(a),p);}
}
