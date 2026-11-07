package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.recommendation.RecommendationResponse;
import com.college.resourcefinder.dto.resource.*;
import com.college.resourcefinder.dto.response.ApiResponse;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.service.AuthService;
import com.college.resourcefinder.service.RecommendationService;
import com.college.resourcefinder.service.ResourceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Academic Resource Repository REST Controller.
 * Base path: {@code /api/v1/resources}
 *
 * <p>Enforces role-based permissions: Students can list and view resources;
 * Faculty and Administrators can upload, edit, or delete resources according
 * to verified ownership rules.</p>
 */
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final RecommendationService recommendationService;
    private final AuthService authService;

    public ResourceController(ResourceService resourceService,
                              RecommendationService recommendationService,
                              AuthService authService) {
        this.resourceService = resourceService;
        this.recommendationService = recommendationService;
        this.authService = authService;
    }

    /**
     * Lists and filters academic resources with pagination and sorting.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ResourceResponse>>> listResources(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly,
            @RequestParam(required = false) Long uploadedBy,
            @PageableDefault(size = 20, sort = "uploadDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ResourceResponse> page = resourceService.listResources(
                departmentId, subjectId, semester, unitId, categoryId,
                resourceType, search, status, activeOnly, uploadedBy, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * Retrieves resource details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResource(@PathVariable Long id) {
        ResourceResponse response = resourceService.getResourceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Uploads an academic resource file with associated metadata.
     * Restricted to FACULTY and ADMIN roles.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(value = "unitId", required = false) Long unitId,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("resourceType") String resourceType,
            HttpServletRequest httpRequest) {

        ResourceUploadRequest request = new ResourceUploadRequest(
                title, description, subjectId, unitId, categoryId, resourceType
        );

        User currentUser = authService.getCurrentAuthenticatedUser();
        ResourceResponse created = resourceService.createResource(request, file, currentUser, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource uploaded successfully.", created));
    }

    /**
     * Updates permitted resource metadata.
     * Ownership or ADMIN role enforced.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceUpdateRequest request,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        ResourceResponse updated = resourceService.updateResource(id, request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully.", updated));
    }

    /**
     * Deactivates/soft-deletes a resource safely.
     * Ownership or ADMIN role enforced.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> deleteResource(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        ResourceResponse deleted = resourceService.deleteResource(id, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully.", deleted));
    }

    /**
     * Updates the status or active flag of a resource.
     * Ownership or ADMIN role enforced.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ResourceStatusRequest request,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        ResourceResponse updated = resourceService.updateStatus(id, request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Resource status updated successfully.", updated));
    }

    /**
     * Permanently purges a resource record and its physical storage file.
     * Restricted to ADMINISTRATORs. Prevents uncontrolled orphan files.
     */
    @DeleteMapping("/{id}/purge")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> purgeResource(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        resourceService.purgeResource(id, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Resource and physical file permanently purged.", null));
    }

    /**
     * Returns rule-based related-resource recommendations for the specified resource.
     * Recommendations are ranked by a deterministic relevance score computed from
     * shared metadata dimensions (subject, department, category, semester, unit,
     * resource type, and title keywords).
     *
     * <p>Only active, authorized resources are returned. The source resource
     * itself is always excluded from results.</p>
     *
     * @param id    the ID of the source resource
     * @param limit maximum number of recommendations (default 10, max 20)
     */
    @GetMapping("/{id}/recommendations")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getRecommendations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        List<RecommendationResponse> recommendations =
                recommendationService.getRecommendations(id, limit);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }

    /**
     * Streams physical file as an attachment download with defensive security headers.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@PathVariable Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        ResourceService.DownloadableFile df = resourceService.getDownloadableFile(id, currentUser);
        String filename = df.resource().getOriginalFilename() != null ?
                df.resource().getOriginalFilename() : df.resource().getFileName();
        MediaType mediaType = determineMediaType(df.resource().getFileType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizeFilenameForHeader(filename) + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(df.fileResource());
    }

    /**
     * Streams physical file inline for browser preview (e.g. PDF).
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<org.springframework.core.io.Resource> previewFile(@PathVariable Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        ResourceService.DownloadableFile df = resourceService.getDownloadableFile(id, currentUser);
        MediaType mediaType = determineMediaType(df.resource().getFileType());
        String filename = df.resource().getOriginalFilename() != null ?
                df.resource().getOriginalFilename() : df.resource().getFileName();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeFilenameForHeader(filename) + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=1800")
                .body(df.fileResource());
    }

    private MediaType determineMediaType(String fileType) {
        if (fileType == null) return MediaType.APPLICATION_OCTET_STREAM;
        return switch (fileType.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "pptx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "txt" -> MediaType.TEXT_PLAIN;
            case "csv" -> MediaType.parseMediaType("text/csv");
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private String sanitizeFilenameForHeader(String filename) {
        if (filename == null) return "resource_file";
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
