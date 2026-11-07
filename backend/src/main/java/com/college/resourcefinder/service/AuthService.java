package com.college.resourcefinder.service;

import com.college.resourcefinder.dto.request.LoginRequest;
import com.college.resourcefinder.dto.request.ProfileUpdateRequest;
import com.college.resourcefinder.dto.request.RegisterRequest;
import com.college.resourcefinder.dto.response.UserProfileResponse;
import com.college.resourcefinder.entity.AuditLog;
import com.college.resourcefinder.entity.Department;
import com.college.resourcefinder.entity.Role;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.exception.ResourceNotFoundException;
import com.college.resourcefinder.repository.AuditLogRepository;
import com.college.resourcefinder.repository.DepartmentRepository;
import com.college.resourcefinder.repository.UserRepository;
import com.college.resourcefinder.security.CustomUserDetails;
import com.college.resourcefinder.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user authentication, registration, profile retrieval, and credential updates.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthService(UserRepository userRepository,
                       DepartmentRepository departmentRepository,
                       AuditLogRepository auditLogRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Authenticates user credentials, binds session to SecurityContext, and writes an audit record.
     */
    @Transactional
    public UserProfileResponse authenticate(LoginRequest loginRequest,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        String email = loginRequest.getEmail().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
            );

            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            logAudit(user, "LOGIN", "SUCCESS", request, "User authenticated via session.");
            return UserProfileResponse.fromEntity(user);

        } catch (DisabledException ex) {
            logAudit(null, "LOGIN", "ACCOUNT_DISABLED", request, "Login attempt for disabled account: " + email);
            throw new BadCredentialsException("Your account has been deactivated. Please contact an administrator.");
        } catch (BadCredentialsException ex) {
            logAudit(null, "LOGIN", "FAILURE", request, "Invalid credentials attempted for: " + email);
            throw new BadCredentialsException("Invalid email address or password.");
        }
    }

    /**
     * Registers a new student account.
     * Enforces unique email, hashes password with BCrypt, and strictly defaults role to STUDENT.
     */
    @Transactional
    public UserProfileResponse register(RegisterRequest registerRequest, HttpServletRequest request) {
        String email = registerRequest.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with email '" + email + "' already exists.");
        }

        Department department = null;
        if (registerRequest.getDepartmentId() != null) {
            department = departmentRepository.findById(registerRequest.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", registerRequest.getDepartmentId()));
        }

        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());

        // Strictly enforce Role.STUDENT for public registration
        User newUser = new User(
                registerRequest.getName().trim(),
                email,
                hashedPassword,
                Role.STUDENT.name(),
                department
        );

        User savedUser = userRepository.save(newUser);
        logAudit(savedUser, "REGISTER", "SUCCESS", request, "New STUDENT account registered.");

        return UserProfileResponse.fromEntity(savedUser);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentAuthenticatedUser();
        return UserProfileResponse.fromEntity(user);
    }

    /**
     * Updates display name, department, or rotates password for the authenticated user.
     */
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(ProfileUpdateRequest updateRequest,
                                                        HttpServletRequest request) {
        User user = getCurrentAuthenticatedUser();

        user.setName(updateRequest.getName().trim());

        if (updateRequest.getDepartmentId() != null) {
            Department department = departmentRepository.findById(updateRequest.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", updateRequest.getDepartmentId()));
            user.setDepartment(department);
        }

        // Handle password change if requested
        if (updateRequest.getNewPassword() != null && !updateRequest.getNewPassword().isBlank()) {
            if (updateRequest.getCurrentPassword() == null || updateRequest.getCurrentPassword().isBlank()) {
                throw new IllegalArgumentException("Current password is required to change password.");
            }

            if (!passwordEncoder.matches(updateRequest.getCurrentPassword(), user.getPassword())) {
                logAudit(user, "PASSWORD_CHANGE", "FAILURE", request, "Incorrect current password supplied.");
                throw new IllegalArgumentException("Current password does not match our records.");
            }

            user.setPassword(passwordEncoder.encode(updateRequest.getNewPassword()));
            logAudit(user, "PASSWORD_CHANGE", "SUCCESS", request, "Password successfully rotated.");
        }

        User updatedUser = userRepository.save(user);
        logAudit(updatedUser, "PROFILE_UPDATE", "SUCCESS", request, "User profile updated.");

        return UserProfileResponse.fromEntity(updatedUser);
    }

    /**
     * Helper to obtain the entity of the authenticated user from the SecurityContext.
     */
    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BadCredentialsException("No authenticated user found in session.");
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void logAudit(User user, String action, String status, HttpServletRequest request, String details) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setUser(user);
            logEntry.setAction(action);
            logEntry.setStatus(status);
            logEntry.setResourceType("USER");
            logEntry.setResourceIdentifier(user != null ? user.getEmail() : "ANONYMOUS");
            logEntry.setIpAddress(request != null ? SecurityUtils.getClientIp(request) : "LOCAL");
            logEntry.setDetails(details);
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to write audit log for action {}: {}", action, ex.getMessage());
        }
    }
}
