package com.college.resourcefinder.service;

import com.college.resourcefinder.dto.attendance.*;
import com.college.resourcefinder.entity.*;
import com.college.resourcefinder.exception.ResourceNotFoundException;
import com.college.resourcefinder.repository.*;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing staff attendance records, batch imports, origin tracking,
 * and deterministic attendance percentage calculations.
 *
 * <p>Attendance Calculation Formula:
 * {@code Attendance Percentage = (Sessions Attended / Total Applicable Sessions) * 100}
 * where {@code Sessions Attended = PRESENT + ON_DUTY}.</p>
 *
 * <p>Confidentiality: Staff attendance records are strictly restricted from
 * student access.</p>
 */
@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final StaffAttendanceRepository staffAttendanceRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AttendanceService(StaffAttendanceRepository staffAttendanceRepository,
                             DepartmentRepository departmentRepository,
                             UserRepository userRepository,
                             AuditLogRepository auditLogRepository) {
        this.staffAttendanceRepository = staffAttendanceRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Records a single staff attendance record with uniqueness checks and audit tracking.
     */
    @Transactional
    public StaffAttendanceResponse recordStaffAttendance(StaffAttendanceEntryRequest request,
                                                         User currentUser,
                                                         HttpServletRequest httpRequest) {
        assertAuthorizedStaffOrAdmin(currentUser);

        Department dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        User staffUser = null;
        if (request.getUserId() != null) {
            staffUser = userRepository.findById(request.getUserId()).orElse(null);
        }

        // Enforce duplicate protection via composite unique check
        if (staffAttendanceRepository.existsByStaffIdentifierAndAttendanceDateAndSessionPeriod(
                request.getStaffIdentifier().trim(), request.getAttendanceDate(), request.getSessionPeriod())) {
            throw new IllegalArgumentException("Attendance record already exists for staff '" +
                    request.getStaffIdentifier() + "' on date " + request.getAttendanceDate() +
                    " period " + request.getSessionPeriod());
        }

        StaffAttendance record = new StaffAttendance(
                request.getStaffIdentifier().trim().toUpperCase(),
                request.getStaffName().trim(),
                dept,
                staffUser,
                request.getAttendanceDate(),
                request.getSessionPeriod(),
                request.getStatus().trim().toUpperCase(),
                request.getDataSource() != null ? request.getDataSource().trim().toUpperCase() : "MANUAL_ENTRY",
                request.getRemarks(),
                currentUser
        );

        StaffAttendance saved = staffAttendanceRepository.save(record);
        logAudit(currentUser, "STAFF_ATTENDANCE_RECORD",
                "Recorded " + saved.getStatus() + " for staff " + saved.getStaffIdentifier() +
                " on " + saved.getAttendanceDate() + " (Period " + saved.getSessionPeriod() + ")", httpRequest);

        return StaffAttendanceResponse.fromEntity(saved);
    }

    /**
     * Batch imports multiple staff attendance records with origin tracking and duplicate avoidance.
     */
    @Transactional
    public BatchImportResultResponse batchImportStaffAttendance(StaffAttendanceBatchImportRequest request,
                                                                User currentUser,
                                                                HttpServletRequest httpRequest) {
        assertAuthorizedStaffOrAdmin(currentUser);

        String origin = request.getDefaultDataSource() != null ?
                request.getDefaultDataSource().trim().toUpperCase() : "CSV_IMPORT";

        List<StaffAttendanceEntryRequest> records = request.getRecords();
        int total = records != null ? records.size() : 0;
        int success = 0;
        int duplicates = 0;
        int failures = 0;
        List<String> errors = new ArrayList<>();
        List<StaffAttendanceResponse> imported = new ArrayList<>();

        if (records == null || records.isEmpty()) {
            return new BatchImportResultResponse(0, 0, 0, 0, origin, errors, imported);
        }

        for (int i = 0; i < records.size(); i++) {
            StaffAttendanceEntryRequest item = records.get(i);
            int rowNum = i + 1;

            try {
                if (item.getStaffIdentifier() == null || item.getStaffIdentifier().isBlank()) {
                    failures++;
                    errors.add("Row " + rowNum + ": Staff identifier is missing.");
                    continue;
                }
                if (item.getAttendanceDate() == null) {
                    failures++;
                    errors.add("Row " + rowNum + ": Attendance date is missing.");
                    continue;
                }
                if (item.getDepartmentId() == null) {
                    failures++;
                    errors.add("Row " + rowNum + ": Department ID is missing.");
                    continue;
                }

                Department dept = departmentRepository.findById(item.getDepartmentId()).orElse(null);
                if (dept == null) {
                    failures++;
                    errors.add("Row " + rowNum + ": Department not found with ID " + item.getDepartmentId());
                    continue;
                }

                int period = item.getSessionPeriod() != null ? item.getSessionPeriod() : 1;
                String staffId = item.getStaffIdentifier().trim().toUpperCase();

                if (staffAttendanceRepository.existsByStaffIdentifierAndAttendanceDateAndSessionPeriod(
                        staffId, item.getAttendanceDate(), period)) {
                    duplicates++;
                    errors.add("Row " + rowNum + ": Duplicate record for " + staffId + " on " + item.getAttendanceDate() + " P" + period);
                    continue;
                }

                User staffUser = null;
                if (item.getUserId() != null) {
                    staffUser = userRepository.findById(item.getUserId()).orElse(null);
                }

                String dataSource = (item.getDataSource() != null && !item.getDataSource().isBlank()) ?
                        item.getDataSource().trim().toUpperCase() : origin;

                StaffAttendance entity = new StaffAttendance(
                        staffId,
                        item.getStaffName() != null ? item.getStaffName().trim() : staffId,
                        dept,
                        staffUser,
                        item.getAttendanceDate(),
                        period,
                        item.getStatus() != null ? item.getStatus().trim().toUpperCase() : "PRESENT",
                        dataSource,
                        item.getRemarks(),
                        currentUser
                );

                StaffAttendance saved = staffAttendanceRepository.save(entity);
                imported.add(StaffAttendanceResponse.fromEntity(saved));
                success++;
            } catch (Exception ex) {
                failures++;
                errors.add("Row " + rowNum + ": " + ex.getMessage());
            }
        }

        logAudit(currentUser, "STAFF_ATTENDANCE_BATCH_IMPORT",
                "Batch imported " + success + " records via " + origin + " (Duplicates: " + duplicates + ", Failures: " + failures + ")",
                httpRequest);

        return new BatchImportResultResponse(total, success, duplicates, failures, origin, errors, imported);
    }

    /**
     * Queries and filters staff attendance records with pagination.
     * Enforces strict confidentiality: students are blocked.
     */
    @Transactional(readOnly = true)
    public Page<StaffAttendanceResponse> searchStaffAttendance(String staffIdentifier,
                                                               Long departmentId,
                                                               LocalDate startDate,
                                                               LocalDate endDate,
                                                               String status,
                                                               Pageable pageable,
                                                               User currentUser) {
        assertAuthorizedStaffOrAdmin(currentUser);

        Specification<StaffAttendance> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (staffIdentifier != null && !staffIdentifier.isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("staffIdentifier")),
                        "%" + staffIdentifier.trim().toUpperCase() + "%"));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("departmentId"), departmentId));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), endDate));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return staffAttendanceRepository.findAll(spec, pageable).map(StaffAttendanceResponse::fromEntity);
    }

    /**
     * Calculates staff attendance metrics and percentage with safe zero-session handling.
     *
     * <p>Formula:
     * {@code Attendance Percentage = (Sessions Attended / Total Applicable Sessions) * 100}
     * where {@code Sessions Attended = PRESENT + ON_DUTY}.</p>
     */
    @Transactional(readOnly = true)
    public StaffAttendanceCalculationSummary calculateStaffAttendanceSummary(String staffIdentifier,
                                                                             LocalDate startDate,
                                                                             LocalDate endDate,
                                                                             User currentUser) {
        assertAuthorizedStaffOrAdmin(currentUser);

        String cleanStaffId = staffIdentifier.trim().toUpperCase();

        List<StaffAttendance> records = staffAttendanceRepository.findByStaffIdentifier(cleanStaffId);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No attendance records found for staff identifier: " + cleanStaffId);
        }

        StaffAttendance sample = records.get(0);
        String staffName = sample.getStaffName();
        String deptCode = sample.getDepartment() != null ? sample.getDepartment().getDepartmentCode() : "N/A";
        String deptName = sample.getDepartment() != null ? sample.getDepartment().getDepartmentName() : "N/A";

        long applicable = staffAttendanceRepository.countApplicableSessions(cleanStaffId, startDate, endDate);
        long attended = staffAttendanceRepository.countAttendedSessions(cleanStaffId, startDate, endDate);
        long present = staffAttendanceRepository.countByStatus(cleanStaffId, "PRESENT", startDate, endDate);
        long onDuty = staffAttendanceRepository.countByStatus(cleanStaffId, "ON_DUTY", startDate, endDate);
        long absent = staffAttendanceRepository.countByStatus(cleanStaffId, "ABSENT", startDate, endDate);
        long leave = staffAttendanceRepository.countByStatus(cleanStaffId, "LEAVE", startDate, endDate);

        double percentage = 0.0;
        if (applicable > 0) {
            double raw = ((double) attended / (double) applicable) * 100.0;
            percentage = Math.round(raw * 100.0) / 100.0;
        }

        return new StaffAttendanceCalculationSummary(
                cleanStaffId, staffName, deptCode, deptName,
                startDate, endDate, applicable, attended,
                present, onDuty, absent, leave, percentage
        );
    }

    /**
     * Updates an existing attendance record (Admin or original recorder).
     */
    @Transactional
    public StaffAttendanceResponse updateStaffAttendance(Long id,
                                                         StaffAttendanceUpdateRequest request,
                                                         User currentUser,
                                                         HttpServletRequest httpRequest) {
        assertAuthorizedStaffOrAdmin(currentUser);

        StaffAttendance record = staffAttendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff attendance record not found with ID: " + id));

        boolean isAdmin = Role.ADMIN.name().equalsIgnoreCase(currentUser.getRole());
        boolean isRecorder = record.getRecordedBy() != null && record.getRecordedBy().getUserId().equals(currentUser.getUserId());

        if (!isAdmin && !isRecorder) {
            throw new AccessDeniedException("You do not have permission to modify this attendance record.");
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            record.setStatus(request.getStatus().trim().toUpperCase());
        }
        if (request.getRemarks() != null) {
            record.setRemarks(request.getRemarks());
        }

        StaffAttendance updated = staffAttendanceRepository.save(record);
        logAudit(currentUser, "STAFF_ATTENDANCE_UPDATE",
                "Updated attendance #" + id + " to " + updated.getStatus(), httpRequest);

        return StaffAttendanceResponse.fromEntity(updated);
    }

    /**
     * Deletes a staff attendance record (Admin only).
     */
    @Transactional
    public void deleteStaffAttendance(Long id, User currentUser, HttpServletRequest httpRequest) {
        if (currentUser == null || !Role.ADMIN.name().equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Only administrators can delete attendance records.");
        }

        StaffAttendance record = staffAttendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff attendance record not found with ID: " + id));

        staffAttendanceRepository.delete(record);
        logAudit(currentUser, "STAFF_ATTENDANCE_DELETE",
                "Deleted attendance #" + id + " for staff " + record.getStaffIdentifier(), httpRequest);
    }

    /**
     * Enforces that students cannot access confidential staff attendance records.
     */
    private void assertAuthorizedStaffOrAdmin(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        if (Role.STUDENT.name().equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Access denied: Confidential staff attendance records cannot be accessed by students.");
        }
    }

    private void logAudit(User user, String action, String details, HttpServletRequest request) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setUser(user);
            logEntry.setAction(action);
            logEntry.setStatus("SUCCESS");
            logEntry.setResourceType("STAFF_ATTENDANCE");
            logEntry.setResourceIdentifier(details);
            logEntry.setIpAddress(request != null ? SecurityUtils.getClientIp(request) : "LOCAL");
            logEntry.setDetails(details);
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}
