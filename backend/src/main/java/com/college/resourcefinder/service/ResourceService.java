package com.college.resourcefinder.service;

import com.college.resourcefinder.dto.resource.*;
import com.college.resourcefinder.entity.*;
import com.college.resourcefinder.exception.ResourceNotFoundException;
import com.college.resourcefinder.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Service orchestrating academic resource repository operations,
 * metadata management, role-based ownership boundaries, and safe deactivations.
 */
@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepository resourceRepository;
    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final StorageService storageService;

    public ResourceService(ResourceRepository resourceRepository,
                           SubjectRepository subjectRepository,
                           UnitRepository unitRepository,
                           CategoryRepository categoryRepository,
                           AuditLogRepository auditLogRepository,
                           StorageService storageService) {
        this.resourceRepository = resourceRepository;
        this.subjectRepository = subjectRepository;
        this.unitRepository = unitRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.storageService = storageService;
    }

    /**
     * Uploads an academic resource file and persists its metadata.
     */
    @Transactional
    public ResourceResponse createResource(ResourceUploadRequest request,
                                           MultipartFile file,
                                           User currentUser,
                                           HttpServletRequest httpRequest) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required to upload resources.");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findById(request.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + request.getUnitId()));
            if (!unit.getSubject().getSubjectId().equals(subject.getSubjectId())) {
                throw new IllegalArgumentException("Unit " + unit.getUnitNumber() + " does not belong to subject " + subject.getSubjectCode());
            }
        }

        // Subdirectory partitioning: e.g. "MCA/SEM1/CS8101"
        String deptCode = (subject.getDepartment() != null) ? subject.getDepartment().getDepartmentCode() : "GENERAL";
        String subDir = deptCode + "/SEM" + subject.getSemester() + "/" + subject.getSubjectCode();

        StorageService.StoredFile stored = storageService.storeFile(file, subDir);

        try {
            Resource resource = new Resource(
                    request.getTitle(),
                    request.getDescription(),
                    stored.relativeFilePath(),
                    stored.uniqueFilename(),
                    stored.originalFilename(),
                    stored.fileType(),
                    stored.fileSize(),
                    request.getResourceType(),
                    subject,
                    unit,
                    category,
                    currentUser
            );

            Resource saved = resourceRepository.save(resource);
            logAudit(currentUser, "RESOURCE_UPLOAD", "Uploaded resource #" + saved.getResourceId() + " (" + saved.getTitle() + ")", httpRequest);

            return ResourceResponse.fromEntity(saved);
        } catch (Exception ex) {
            log.error("Compensating cleanup: Database persistence failed for resource upload. Purging written file: {}", stored.relativeFilePath(), ex);
            storageService.deleteFile(stored.relativeFilePath());
            throw ex;
        }
    }

    /**
     * Retrieves a single resource by ID.
     */
    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));
        return ResourceResponse.fromEntity(resource);
    }

    /**
     * Updates permitted resource metadata. Restricts modifications to owner or admin.
     */
    @Transactional
    public ResourceResponse updateResource(Long resourceId,
                                           ResourceUpdateRequest request,
                                           User currentUser,
                                           HttpServletRequest httpRequest) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        assertCanModify(resource, currentUser);

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findById(request.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + request.getUnitId()));
            if (!unit.getSubject().getSubjectId().equals(subject.getSubjectId())) {
                throw new IllegalArgumentException("Unit " + unit.getUnitNumber() + " does not belong to subject " + subject.getSubjectCode());
            }
        }

        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setSubject(subject);
        resource.setUnit(unit);
        resource.setCategory(category);
        resource.setResourceType(request.getResourceType());

        Resource updated = resourceRepository.save(resource);
        logAudit(currentUser, "RESOURCE_UPDATE", "Updated metadata for resource #" + resourceId, httpRequest);

        return ResourceResponse.fromEntity(updated);
    }

    /**
     * Soft-deactivates a resource safely to preserve relational integrity.
     */
    @Transactional
    public ResourceResponse deleteResource(Long resourceId,
                                           User currentUser,
                                           HttpServletRequest httpRequest) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        assertCanModify(resource, currentUser);

        resource.setIsActive(false);
        resource.setStatus("DELETED");

        Resource updated = resourceRepository.save(resource);
        logAudit(currentUser, "RESOURCE_DELETE", "Soft-deleted resource #" + resourceId, httpRequest);

        return ResourceResponse.fromEntity(updated);
    }

    /**
     * Updates resource status (e.g., ACTIVE, ARCHIVED, DELETED).
     */
    @Transactional
    public ResourceResponse updateStatus(Long resourceId,
                                         ResourceStatusRequest request,
                                         User currentUser,
                                         HttpServletRequest httpRequest) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        assertCanModify(resource, currentUser);

        if (request.getStatus() != null) {
            resource.setStatus(request.getStatus());
            if ("DELETED".equalsIgnoreCase(request.getStatus())) {
                resource.setIsActive(false);
            }
        }
        if (request.getIsActive() != null) {
            resource.setIsActive(request.getIsActive());
        }

        Resource updated = resourceRepository.save(resource);
        logAudit(currentUser, "RESOURCE_STATUS_UPDATE", "Status of resource #" + resourceId + " set to " + resource.getStatus(), httpRequest);

        return ResourceResponse.fromEntity(updated);
    }

    /**
     * Queries and filters academic resources with pagination and multi-parameter taxonomy facets.
     */
    @Transactional(readOnly = true)
    public Page<ResourceResponse> listResources(Long departmentId,
                                                Long subjectId,
                                                Integer semester,
                                                Long unitId,
                                                Long categoryId,
                                                String resourceType,
                                                String search,
                                                String status,
                                                Boolean activeOnly,
                                                Long uploadedBy,
                                                Pageable pageable) {
        Specification<Resource> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Boolean.TRUE.equals(activeOnly)) {
                predicates.add(cb.isTrue(root.get("isActive")));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("subject").get("department").get("departmentId"), departmentId));
            }

            if (subjectId != null) {
                predicates.add(cb.equal(root.get("subject").get("subjectId"), subjectId));
            }

            if (semester != null) {
                predicates.add(cb.equal(root.get("subject").get("semester"), semester));
            }

            if (unitId != null) {
                predicates.add(cb.equal(root.get("unit").get("unitId"), unitId));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            if (resourceType != null && !resourceType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("resourceType")), resourceType.trim().toLowerCase()));
            }

            if (uploadedBy != null) {
                predicates.add(cb.equal(root.get("uploadedBy").get("userId"), uploadedBy));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return resourceRepository.findAll(spec, pageable).map(ResourceResponse::fromEntity);
    }

    /**
     * Loads the physical file for streaming/download after security and existence verification.
     */
    @Transactional(readOnly = true)
    public DownloadableFile getDownloadableFile(Long resourceId, User currentUser) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        if (!Boolean.TRUE.equals(resource.getIsActive()) || "DELETED".equalsIgnoreCase(resource.getStatus())) {
            boolean isAdmin = currentUser != null && Role.ADMIN.name().equalsIgnoreCase(currentUser.getRole());
            boolean isOwner = currentUser != null && resource.getUploadedBy() != null &&
                    resource.getUploadedBy().getUserId().equals(currentUser.getUserId());
            if (!isAdmin && !isOwner) {
                throw new ResourceNotFoundException("Resource not found with ID: " + resourceId);
            }
        }

        org.springframework.core.io.Resource fileResource = storageService.loadAsResource(resource.getFilePath());
        return new DownloadableFile(fileResource, resource);
    }

    /**
     * Permanently purges a resource record and its physical storage file.
     * Restricted to ADMINISTRATORs. Ensures no uncontrolled orphan files remain.
     */
    @Transactional
    public void purgeResource(Long resourceId, User currentUser, HttpServletRequest httpRequest) {
        if (currentUser == null || !Role.ADMIN.name().equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Only administrators can permanently purge resources and physical files.");
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        String filePath = resource.getFilePath();
        resourceRepository.delete(resource);

        // Compensating physical file deletion to ensure no orphan files remain
        boolean fileDeleted = storageService.deleteFile(filePath);
        log.info("Purged resource #{} from database. Physical file deleted: {}", resourceId, fileDeleted);

        logAudit(currentUser, "RESOURCE_PURGE", "Permanently purged resource #" + resourceId + " (" + filePath + ")", httpRequest);
    }

    /**
     * Verifies that the current user has permission to modify or delete the given resource.
     * Permission is granted to ADMINISTRATORs or to the user who originally uploaded the file.
     */
    public void assertCanModify(Resource resource, User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required.");
        }

        boolean isAdmin = Role.ADMIN.name().equalsIgnoreCase(currentUser.getRole());
        boolean isOwner = resource.getUploadedBy() != null &&
                resource.getUploadedBy().getUserId().equals(currentUser.getUserId());

        if (!isAdmin && !isOwner) {
            log.warn("Access denied: User #{} ({}) attempted to modify resource #{} owned by #{}",
                    currentUser.getUserId(), currentUser.getEmail(), resource.getResourceId(),
                    resource.getUploadedBy() != null ? resource.getUploadedBy().getUserId() : "null");
            throw new AccessDeniedException("You do not have permission to modify or delete this resource.");
        }
    }

    private void logAudit(User user, String action, String details, HttpServletRequest request) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setUser(user);
            logEntry.setAction(action);
            logEntry.setStatus("SUCCESS");
            logEntry.setResourceType("RESOURCE");
            logEntry.setResourceIdentifier(details);
            logEntry.setIpAddress(request != null ? com.college.resourcefinder.util.SecurityUtils.getClientIp(request) : "LOCAL");
            logEntry.setDetails(details);
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    public record DownloadableFile(
            org.springframework.core.io.Resource fileResource,
            Resource resource
    ) {}
}
