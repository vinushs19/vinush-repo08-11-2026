package com.college.resourcefinder.service;

import com.college.resourcefinder.dto.filesave.*;
import com.college.resourcefinder.entity.AuditLog;
import com.college.resourcefinder.entity.FileOperation;
import com.college.resourcefinder.entity.Resource;
import com.college.resourcefinder.entity.Role;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.exception.ResourceNotFoundException;
import com.college.resourcefinder.exception.StorageException;
import com.college.resourcefinder.repository.AuditLogRepository;
import com.college.resourcefinder.repository.FileOperationRepository;
import com.college.resourcefinder.repository.ResourceRepository;
import com.college.resourcefinder.util.SecurityUtils;
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

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service orchestrating deterministic, controlled file saving and organization.
 *
 * <p>Enforces fixed application storage boundaries, allowlisted destination categories,
 * collision-free server filenames, and transaction-safe compensating rollbacks.</p>
 */
@Service
public class ControlledFileSaveService {

    private static final Logger log = LoggerFactory.getLogger(ControlledFileSaveService.class);

    private static final Pattern SAFE_SUBFOLDER_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,50}$");

    public static final Map<String, String> APPROVED_CATEGORIES = Collections.unmodifiableMap(new LinkedHashMap<>() {{
        put("LECTURE_NOTES", "organized/lecture_notes");
        put("QUESTION_PAPERS", "organized/question_papers");
        put("ASSIGNMENTS", "organized/assignments");
        put("LAB_MANUALS", "organized/lab_manuals");
        put("STUDY_MATERIALS", "organized/study_materials");
        put("SYLLABUS", "organized/syllabus");
        put("ARCHIVES", "organized/archives");
    }});

    private final ResourceRepository resourceRepository;
    private final FileOperationRepository fileOperationRepository;
    private final StorageService storageService;
    private final AuditLogRepository auditLogRepository;

    public ControlledFileSaveService(ResourceRepository resourceRepository,
                                     FileOperationRepository fileOperationRepository,
                                     StorageService storageService,
                                     AuditLogRepository auditLogRepository) {
        this.resourceRepository = resourceRepository;
        this.fileOperationRepository = fileOperationRepository;
        this.storageService = storageService;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Returns the allowlisted list of approved destination categories.
     */
    public List<ApprovedCategoryResponse> getApprovedCategories() {
        List<ApprovedCategoryResponse> list = new ArrayList<>();
        list.add(new ApprovedCategoryResponse("LECTURE_NOTES", "Lecture Notes", "organized/lecture_notes", "Classroom lecture slides and unit-wise course notes"));
        list.add(new ApprovedCategoryResponse("QUESTION_PAPERS", "Question Papers", "organized/question_papers", "Previous university examination and internal test question papers"));
        list.add(new ApprovedCategoryResponse("ASSIGNMENTS", "Assignments", "organized/assignments", "Student homework assignments, lab assignments, and project prompts"));
        list.add(new ApprovedCategoryResponse("LAB_MANUALS", "Lab Manuals", "organized/lab_manuals", "Laboratory practical manuals, exercise guides, and software installation docs"));
        list.add(new ApprovedCategoryResponse("STUDY_MATERIALS", "Study Materials", "organized/study_materials", "Reference textbooks, e-books, research publications, and study kits"));
        list.add(new ApprovedCategoryResponse("SYLLABUS", "Syllabus", "organized/syllabus", "Curriculum structure, semester-wise syllabus, and course outcomes"));
        list.add(new ApprovedCategoryResponse("ARCHIVES", "Archives", "organized/archives", "Archived historical materials and previous curriculum iterations"));
        return list;
    }

    /**
     * Executes a controlled file-saving operation from an existing authorized resource
     * to a pre-approved application directory.
     */
    @Transactional
    public ControlledFileSaveResponse executeControlledSave(ControlledFileSaveRequest request,
                                                            User currentUser,
                                                            HttpServletRequest httpRequest) {
        assertAuthorizedStaffOrAdmin(currentUser);

        if (request.getResourceId() == null) {
            throw new IllegalArgumentException("Resource ID is required.");
        }

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + request.getResourceId()));

        if (!Boolean.TRUE.equals(resource.getIsActive()) || "DELETED".equalsIgnoreCase(resource.getStatus())) {
            throw new ResourceNotFoundException("Resource not found with ID: " + request.getResourceId());
        }

        assertCanOrganizeResource(resource, currentUser);

        String categoryKey = request.getDestinationCategory() != null ?
                request.getDestinationCategory().trim().toUpperCase() : null;

        if (categoryKey == null || !APPROVED_CATEGORIES.containsKey(categoryKey)) {
            throw new IllegalArgumentException("Invalid destination category: " + categoryKey +
                    ". Approved categories are: " + APPROVED_CATEGORIES.keySet());
        }

        String baseTargetDir = APPROVED_CATEGORIES.get(categoryKey);
        String finalSubDir = baseTargetDir;

        if (request.getSubFolderTag() != null && !request.getSubFolderTag().isBlank()) {
            String tag = request.getSubFolderTag().trim();
            if (!SAFE_SUBFOLDER_PATTERN.matcher(tag).matches()) {
                throw new IllegalArgumentException("Invalid subfolder tag: '" + tag +
                        "'. Only alphanumeric characters, hyphens, and underscores are allowed (1-50 chars).");
            }
            if (tag.contains("..") || tag.contains("/") || tag.contains("\\")) {
                throw new SecurityException("Path traversal attempt detected in subfolder tag.");
            }
            finalSubDir = baseTargetDir + "/" + tag;
        }

        boolean overwrite = Boolean.TRUE.equals(request.getOverwrite());
        if (overwrite) {
            boolean isAdmin = Role.ADMIN.name().equalsIgnoreCase(currentUser.getRole());
            boolean isOwner = resource.getUploadedBy() != null &&
                    resource.getUploadedBy().getUserId().equals(currentUser.getUserId());
            if (!isAdmin && !isOwner) {
                throw new AccessDeniedException("Unauthorized overwrite: Only administrators or the resource owner can overwrite files.");
            }
        }

        // Execute physical file copy to allowlisted destination
        StorageService.StoredFile stored = storageService.copyAndOrganizeFile(
                resource.getFilePath(),
                finalSubDir,
                resource.getOriginalFilename(),
                overwrite
        );

        // Persist operation metadata with compensating cleanup
        FileOperation op = new FileOperation(
                resource,
                currentUser,
                resource.getFilePath(),
                categoryKey,
                stored.relativeFilePath(),
                stored.uniqueFilename(),
                stored.fileSize(),
                stored.fileType(),
                "COPY_ORGANIZE",
                "SUCCESS",
                null
        );

        try {
            FileOperation savedOp = fileOperationRepository.save(op);

            logAudit(currentUser, "CONTROLLED_FILE_SAVE",
                    "Organized resource #" + resource.getResourceId() + " (" + resource.getTitle() +
                    ") to " + stored.relativeFilePath() + " under category " + categoryKey, httpRequest);

            return ControlledFileSaveResponse.fromEntity(savedOp);
        } catch (Exception ex) {
            log.error("Compensating cleanup: Database metadata persistence failed for controlled file save. Deleting newly written file: {}",
                    stored.relativeFilePath(), ex);
            storageService.deleteFile(stored.relativeFilePath());
            throw new StorageException("Failed to persist file operation metadata in database.", ex);
        }
    }

    /**
     * Lists paginated history of file operations.
     */
    @Transactional(readOnly = true)
    public Page<ControlledFileSaveResponse> listOperations(Long resourceId,
                                                          Long userId,
                                                          String category,
                                                          Pageable pageable,
                                                          User currentUser) {
        assertAuthorizedStaffOrAdmin(currentUser);

        Specification<FileOperation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (resourceId != null) {
                predicates.add(cb.equal(root.get("resource").get("resourceId"), resourceId));
            }

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("userId"), userId));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("destinationCategory")), category.trim().toUpperCase()));
            }

            // Non-admin faculty see their own operations or department operations
            if (Role.FACULTY.name().equalsIgnoreCase(currentUser.getRole())) {
                predicates.add(cb.equal(root.get("user").get("userId"), currentUser.getUserId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return fileOperationRepository.findAll(spec, pageable).map(ControlledFileSaveResponse::fromEntity);
    }

    /**
     * Retrieves details of a specific file operation.
     */
    @Transactional(readOnly = true)
    public ControlledFileSaveResponse getOperationById(Long operationId, User currentUser) {
        assertAuthorizedStaffOrAdmin(currentUser);

        FileOperation op = fileOperationRepository.findById(operationId)
                .orElseThrow(() -> new ResourceNotFoundException("File operation not found with ID: " + operationId));

        if (Role.FACULTY.name().equalsIgnoreCase(currentUser.getRole()) &&
                !op.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("You do not have permission to view this file operation.");
        }

        return ControlledFileSaveResponse.fromEntity(op);
    }

    private void assertAuthorizedStaffOrAdmin(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        if (Role.STUDENT.name().equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Access denied: Students are not authorized to execute controlled file-saving operations.");
        }
    }

    private void assertCanOrganizeResource(Resource resource, User user) {
        if (Role.ADMIN.name().equalsIgnoreCase(user.getRole())) {
            return; // Admins can organize any resource
        }

        // Faculty can organize their own resources or resources in their department
        boolean isOwner = resource.getUploadedBy() != null &&
                resource.getUploadedBy().getUserId().equals(user.getUserId());

        boolean isSameDept = false;
        if (user.getDepartment() != null && resource.getSubject() != null && resource.getSubject().getDepartment() != null) {
            isSameDept = user.getDepartment().getDepartmentId().equals(resource.getSubject().getDepartment().getDepartmentId());
        }

        if (!isOwner && !isSameDept) {
            throw new AccessDeniedException("You do not have permission to organize this resource.");
        }
    }

    private void logAudit(User user, String action, String details, HttpServletRequest request) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setUser(user);
            logEntry.setAction(action);
            logEntry.setStatus("SUCCESS");
            logEntry.setResourceType("FILE_OPERATION");
            logEntry.setResourceIdentifier(details);
            logEntry.setIpAddress(request != null ? SecurityUtils.getClientIp(request) : "LOCAL");
            logEntry.setDetails(details);
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}
