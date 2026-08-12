package com.novabank.auth.service;

import com.novabank.auth.domain.AuditLog;
import com.novabank.auth.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(UUID userId, String action, String correlationId, String metadata) {
        auditLogRepository.save(new AuditLog(userId, action, correlationId, metadata));
    }
}
