package com.college.resourcefinder.service;

import com.college.resourcefinder.dto.request.UserStatusUpdateRequest;
import com.college.resourcefinder.dto.response.UserProfileResponse;
import com.college.resourcefinder.entity.AuditLog;
import com.college.resourcefinder.entity.Role;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.exception.ResourceNotFoundException;
import com.college.resourcefinder.repository.AuditLogRepository;
import com.college.resourcefinder.repository.UserRepository;
import com.college.resourcefinder.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user accounts, role escalations, and account deactivation for administrators.
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminUserService(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserProfileResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserProfileResponse.fromEntity(user);
    }

    @Transactional
    public UserProfileResponse updateUserStatus(Long userId,
                                                UserStatusUpdateRequest updateRequest,
                                                HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Safeguard default admin account from accidental deactivation
        if ("admin@college.edu".equalsIgnoreCase(user.getEmail()) &&
                Boolean.FALSE.equals(updateRequest.getIsActive())) {
            throw new IllegalArgumentException("The primary administrator account cannot be deactivated.");
        }

        if (updateRequest.getIsActive() != null) {
            user.setIsActive(updateRequest.getIsActive());
        }

        if (updateRequest.getRole() != null && !updateRequest.getRole().isBlank()) {
            String requestedRole = updateRequest.getRole().trim().toUpperCase();
            if (!Role.isValid(requestedRole)) {
                throw new IllegalArgumentException("Invalid role specified: " + updateRequest.getRole());
            }
            user.setRole(requestedRole);
        }

        User updatedUser = userRepository.save(user);

        logAudit(updatedUser, "ADMIN_USER_UPDATE", "SUCCESS", request,
                "Updated user " + updatedUser.getEmail() + " status: active=" + updatedUser.getIsActive() +
                ", role=" + updatedUser.getRole());

        return UserProfileResponse.fromEntity(updatedUser);
    }

    private void logAudit(User targetUser, String action, String status, HttpServletRequest request, String details) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setUser(targetUser);
            logEntry.setAction(action);
            logEntry.setStatus(status);
            logEntry.setResourceType("USER");
            logEntry.setResourceIdentifier(targetUser != null ? targetUser.getEmail() : "N/A");
            logEntry.setIpAddress(request != null ? SecurityUtils.getClientIp(request) : "LOCAL");
            logEntry.setDetails(details);
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to write audit log in AdminUserService: {}", ex.getMessage());
        }
    }
}
