package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.request.UserStatusUpdateRequest;
import com.college.resourcefinder.dto.response.ApiResponse;
import com.college.resourcefinder.dto.response.UserProfileResponse;
import com.college.resourcefinder.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administrator User Management API controller.
 * Base path: {@code /api/v1/admin/users}
 * Accessible strictly to users possessing the {@code ROLE_ADMIN} authority.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Lists registered users with pagination and sorting.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> listUsers(
            @PageableDefault(size = 20, sort = "userId", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserProfileResponse> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Retrieves detailed profile for a specific user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUser(@PathVariable Long id) {
        UserProfileResponse user = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Updates account status (active/deactivated) or reassigns user role (FACULTY, ADMIN, STUDENT).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody UserStatusUpdateRequest updateRequest,
            HttpServletRequest request) {
        UserProfileResponse updated = adminUserService.updateUserStatus(id, updateRequest, request);
        return ResponseEntity.ok(ApiResponse.success("User account updated successfully.", updated));
    }
}
