package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.attendance.*;
import com.college.resourcefinder.dto.response.ApiResponse;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.service.AttendanceService;
import com.college.resourcefinder.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Attendance Management REST Controller.
 * Base path: {@code /api/v1/attendance}
 *
 * <p>Enforces strict access control: Confidential staff attendance records are accessible
 * only to authorized staff (FACULTY) and administrators (ADMIN). Ordinary students are
 * strictly blocked from accessing staff attendance data.</p>
 */
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    public AttendanceController(AttendanceService attendanceService, AuthService authService) {
        this.attendanceService = attendanceService;
        this.authService = authService;
    }

    /**
     * Searches and lists staff attendance records with multi-criteria filtering and pagination.
     */
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<StaffAttendanceResponse>>> searchStaffAttendance(
            @RequestParam(required = false) String staffIdentifier,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        Page<StaffAttendanceResponse> page = attendanceService.searchStaffAttendance(
                staffIdentifier, departmentId, startDate, endDate, status, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * Calculates staff attendance metrics and percentage with safe zero-session handling.
     *
     * <p>Attendance Percentage = (Sessions Attended / Total Applicable Sessions) * 100</p>
     */
    @GetMapping("/staff/{staffIdentifier}/summary")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<StaffAttendanceCalculationSummary>> getStaffAttendanceSummary(
            @PathVariable String staffIdentifier,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        StaffAttendanceCalculationSummary summary = attendanceService.calculateStaffAttendanceSummary(
                staffIdentifier, startDate, endDate, currentUser);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Records a single staff attendance record.
     */
    @PostMapping("/staff")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<StaffAttendanceResponse>> recordStaffAttendance(
            @Valid @RequestBody StaffAttendanceEntryRequest request,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        StaffAttendanceResponse created = attendanceService.recordStaffAttendance(request, currentUser, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff attendance record saved successfully.", created));
    }

    /**
     * Batch imports multiple staff attendance records with origin tracking.
     */
    @PostMapping("/staff/import")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<BatchImportResultResponse>> batchImportStaffAttendance(
            @Valid @RequestBody StaffAttendanceBatchImportRequest request,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        BatchImportResultResponse result = attendanceService.batchImportStaffAttendance(request, currentUser, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Batch attendance import processed.", result));
    }

    /**
     * Updates an existing staff attendance record (Admin or original recorder).
     */
    @PutMapping("/staff/{id}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<StaffAttendanceResponse>> updateStaffAttendance(
            @PathVariable Long id,
            @Valid @RequestBody StaffAttendanceUpdateRequest request,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        StaffAttendanceResponse updated = attendanceService.updateStaffAttendance(id, request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Staff attendance record updated successfully.", updated));
    }

    /**
     * Deletes a staff attendance record (Admin only).
     */
    @DeleteMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStaffAttendance(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        attendanceService.deleteStaffAttendance(id, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Staff attendance record deleted successfully.", null));
    }
}
