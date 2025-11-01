package com.intelligentmarker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentmarker.model.AuditLog;
import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Audit Log Service
 * Records all sensitive operations to ensure traceability
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Record audit log
     * @param actor The actor performing the operation (null indicates system operation)
     * @param action Action type
     * @param entityType Entity type
     * @param entityId Entity ID
     * @param details Detailed information
     *
     * Uses REQUIRES_NEW to ensure execution in independent transaction, not affecting outer transaction
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void log(User actor, String action, String entityType, Long entityId, Map<String, Object> details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setActor(actor);
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            
            if (details != null && !details.isEmpty()) {
                String detailsJson = objectMapper.writeValueAsString(details);
                auditLog.setDetails(detailsJson);
            }
            
            // Build change description
            String description = buildDescription(action, entityType, entityId, details);
            auditLog.setChangeDescription(description);

            auditLogRepository.save(auditLog);

            log.info("Audit log created: {} - {} - {}", action, entityType, entityId);

        } catch (Exception e) {
            log.error("Failed to create audit log", e);
            // Audit log failure should not affect main business flow
        }
    }
    
    /**
     * Build readable change description
     */
    private String buildDescription(String action, String entityType, Long entityId, Map<String, Object> details) {
        StringBuilder desc = new StringBuilder();
        
        desc.append(action).append(" ").append(entityType).append(" #").append(entityId);
        
        if (details != null && !details.isEmpty()) {
            desc.append(": ");
            details.forEach((key, value) -> 
                desc.append(key).append("=").append(value).append(", ")
            );
        }
        
        return desc.toString();
    }
}

