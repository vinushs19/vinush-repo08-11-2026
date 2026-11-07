package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.admin.*;
import com.college.resourcefinder.dto.response.ApiResponse;
import com.college.resourcefinder.service.AdminReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller exposing administrative reporting and audit inspection APIs.
 * Base path: {@code /api/v1/admin}
 * Strictly restricted to users with the {@code ROLE_ADMIN} authority.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    /**
     * Retrieves overall system metrics for the administrative dashboard.
     */
    @GetMapping("/reports/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardMetricsResponse>> getDashboardMetrics() {
        AdminDashboardMetricsResponse metrics = adminReportService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    /**
     * Reports resource distribution by academic department.
     */
    @GetMapping("/reports/departments")
    public ResponseEntity<ApiResponse<List<DepartmentResourceDistributionDto>>> getDepartmentDistribution() {
        List<DepartmentResourceDistributionDto> distribution = adminReportService.getDepartmentDistribution();
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    /**
     * Reports resource distribution by semester.
     */
    @GetMapping("/reports/semesters")
    public ResponseEntity<ApiResponse<List<SemesterResourceDistributionDto>>> getSemesterDistribution() {
        List<SemesterResourceDistributionDto> distribution = adminReportService.getSemesterDistribution();
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    /**
     * Reports resource distribution by category.
     */
    @GetMapping("/reports/categories")
    public ResponseEntity<ApiResponse<List<CategoryResourceDistributionDto>>> getCategoryDistribution() {
        List<CategoryResourceDistributionDto> distribution = adminReportService.getCategoryDistribution();
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    /**
     * Reports search activity analytics derived from actual user search history.
     */
    @GetMapping("/reports/search-activity")
    public ResponseEntity<ApiResponse<SearchActivitySummaryDto>> getSearchActivity(
            @RequestParam(defaultValue = "10") int limit) {
        SearchActivitySummaryDto summary = adminReportService.getSearchActivitySummary(limit);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Retrieves paginated audit logs with optional filtering by action, status, and date range.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponse> logs = adminReportService.getAuditLogs(action, status, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
