package com.college.resourcefinder.service;

import com.college.resourcefinder.entity.AuditLog;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dedicated service for persisting audit log entries in an independent transaction.
 * Uses Propagation.REQUIRES_NEW so security audit events (e.g., failed logins,
 * authorization rejections, password change failures) persist even if the calling
 * transaction is rolled back due to an exception.
 */
@Service
public class AuditLoggingService {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingService.class);

    private final AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AuditLoggingService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists an audit log in an isolated new transaction.
     *
     * @param user               the user associated with the event, or null for anonymous/failed logins
     * @param action             the event action name (e.g., LOGIN, PASSWORD_CHANGE)
     * @param status             SUCCESS, FAILURE, ACCOUNT_DISABLED, etc.
     * @param resourceType       the category of resource (e.g., USER, RESOURCE, ATTENDANCE)
     * @param resourceIdentifier the target identifier (e.g., user email, resource ID)
     * @param ipAddress          the client's IP address
     * @param details            descriptive metadata or reason
     * @return the saved AuditLog entry, or null if an error occurred
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logAudit(User user,
                             String action,
                             String status,
                             String resourceType,
                             String resourceIdentifier,
                             String ipAddress,
                             String details) {
        try {
            AuditLog logEntry = new AuditLog();

            if (user != null && user.getUserId() != null) {
                // Attach a managed reference within this new persistence context
                User managedUser = entityManager.getReference(User.class, user.getUserId());
                logEntry.setUser(managedUser);
            } else {
                logEntry.setUser(null);
            }

            logEntry.setAction(action);
            logEntry.setStatus(status);
            logEntry.setResourceType(resourceType != null ? resourceType : "SECURITY");
            logEntry.setResourceIdentifier(resourceIdentifier != null ? resourceIdentifier : "SYSTEM");
            logEntry.setIpAddress(ipAddress != null ? ipAddress : "LOCAL");
            logEntry.setDetails(details);

            return auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to persist audit log for action '{}': {}", action, ex.getMessage());
            return null;
        }
    }
}
