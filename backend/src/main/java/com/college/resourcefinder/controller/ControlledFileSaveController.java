package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.filesave.*;
import com.college.resourcefinder.dto.response.ApiResponse;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.service.AuthService;
import com.college.resourcefinder.service.ControlledFileSaveService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlled File-Saving and Organization REST Controller.
 * Base path: {@code /api/v1/files/controlled-save}
 *
 * <p>Enforces strict server-side storage boundaries, allowlisted categories,
 * collision-free naming, and transaction-safe compensating cleanup.</p>
 */
@RestController
@RequestMapping("/api/v1/files/controlled-save")
public class ControlledFileSaveController {

    private final ControlledFileSaveService controlledFileSaveService;
    private final AuthService authService;

    public ControlledFileSaveController(ControlledFileSaveService controlledFileSaveService,
                                        AuthService authService) {
        this.controlledFileSaveService = controlledFileSaveService;
        this.authService = authService;
    }

    /**
     * Retrieves the allowlisted list of approved destination categories.
     */
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ApprovedCategoryResponse>>> getApprovedCategories() {
        List<ApprovedCategoryResponse> categories = controlledFileSaveService.getApprovedCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Executes a controlled file-saving operation for an authorized resource.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<ControlledFileSaveResponse>> executeControlledSave(
            @Valid @RequestBody ControlledFileSaveRequest request,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        ControlledFileSaveResponse response = controlledFileSaveService.executeControlledSave(request, currentUser, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource file successfully organized and saved.", response));
    }

    /**
     * Retrieves paginated history of controlled file-saving operations.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ControlledFileSaveResponse>>> listOperations(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        Page<ControlledFileSaveResponse> page = controlledFileSaveService.listOperations(
                resourceId, userId, category, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * Retrieves details of a specific file operation by ID.
     */
    @GetMapping("/{operationId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<ControlledFileSaveResponse>> getOperationById(@PathVariable Long operationId) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        ControlledFileSaveResponse response = controlledFileSaveService.getOperationById(operationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
