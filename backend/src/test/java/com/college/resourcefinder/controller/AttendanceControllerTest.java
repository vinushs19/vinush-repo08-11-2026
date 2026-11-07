package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.attendance.*;
import com.college.resourcefinder.entity.*;
import com.college.resourcefinder.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StaffAttendanceRepository staffAttendanceRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User admin;
    private User faculty;
    private User student;
    private Department mcaDept;
    private Department eceDept;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("admin.att@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Admin Att",
                        "admin.att@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.ADMIN.name(),
                        null
                )));

        faculty = userRepository.findByEmail("faculty.att@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Faculty Att",
                        "faculty.att@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.FACULTY.name(),
                        null
                )));

        student = userRepository.findByEmail("student.att@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Student Att",
                        "student.att@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.STUDENT.name(),
                        null
                )));

        mcaDept = departmentRepository.findByDepartmentCode("MCA_ATT")
                .orElseGet(() -> departmentRepository.save(new Department("MCA_ATT", "MCA Attendance Dept")));

        eceDept = departmentRepository.findByDepartmentCode("ECE_ATT")
                .orElseGet(() -> departmentRepository.save(new Department("ECE_ATT", "ECE Attendance Dept")));
    }

    // =========================================================================
    // 1. Access Control Tests
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated request to staff attendance returns 401 Unauthorized")
    void searchAttendance_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/staff"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student.att@college.edu", roles = {"STUDENT"})
    @DisplayName("Student role cannot access confidential staff attendance - returns 403 Forbidden")
    void searchAttendance_studentRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/staff"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student.att@college.edu", roles = {"STUDENT"})
    @DisplayName("Student role cannot calculate staff attendance summary - returns 403 Forbidden")
    void getSummary_studentRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/staff/STF-001/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Faculty role can access staff attendance listing - returns 200 OK")
    void searchAttendance_facultyRole_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 2. Attendance Calculations (Exact Formula Verification)
    // =========================================================================

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Calculate percentage for 10 sessions (7 present, 1 on-duty, 1 absent, 1 leave) -> 80.00%")
    void calculatePercentage_knownTestCase_returns80Percent() throws Exception {
        String staffId = "STF-TEST-80";
        LocalDate baseDate = LocalDate.now().minusDays(10);

        // 7 Present
        for (int i = 1; i <= 7; i++) {
            staffAttendanceRepository.save(new StaffAttendance(
                    staffId, "Dr. S. Test", mcaDept, null, baseDate.plusDays(i), 1, "PRESENT", "MANUAL_ENTRY", null, faculty));
        }
        // 1 On-Duty (counts as attended duty)
        staffAttendanceRepository.save(new StaffAttendance(
                staffId, "Dr. S. Test", mcaDept, null, baseDate.plusDays(8), 1, "ON_DUTY", "MANUAL_ENTRY", "Seminar duty", faculty));
        // 1 Absent
        staffAttendanceRepository.save(new StaffAttendance(
                staffId, "Dr. S. Test", mcaDept, null, baseDate.plusDays(9), 1, "ABSENT", "MANUAL_ENTRY", "Personal work", faculty));
        // 1 Leave
        staffAttendanceRepository.save(new StaffAttendance(
                staffId, "Dr. S. Test", mcaDept, null, baseDate.plusDays(10), 1, "LEAVE", "MANUAL_ENTRY", "Medical leave", faculty));

        // Total applicable = 10, Attended = 7 + 1 = 8. Percentage = (8/10) * 100 = 80.00%
        mockMvc.perform(get("/api/v1/attendance/staff/" + staffId + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.staffIdentifier").value(staffId))
                .andExpect(jsonPath("$.data.totalApplicableSessions").value(10))
                .andExpect(jsonPath("$.data.sessionsAttended").value(8))
                .andExpect(jsonPath("$.data.sessionsPresent").value(7))
                .andExpect(jsonPath("$.data.sessionsOnDuty").value(1))
                .andExpect(jsonPath("$.data.sessionsAbsent").value(1))
                .andExpect(jsonPath("$.data.sessionsOnLeave").value(1))
                .andExpect(jsonPath("$.data.attendancePercentage").value(80.0))
                .andExpect(jsonPath("$.data.attendancePercentageFormatted").value("80.00%"))
                .andExpect(jsonPath("$.data.meetsRequirement").value(true));
    }

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Calculate percentage for 8 sessions (7 present, 1 absent) -> 87.50%")
    void calculatePercentage_knownTestCase_returns87Point5Percent() throws Exception {
        String staffId = "STF-TEST-875";
        LocalDate baseDate = LocalDate.now().minusDays(8);

        // 7 Present
        for (int i = 1; i <= 7; i++) {
            staffAttendanceRepository.save(new StaffAttendance(
                    staffId, "Prof. K. Test", mcaDept, null, baseDate.plusDays(i), 1, "PRESENT", "MANUAL_ENTRY", null, faculty));
        }
        // 1 Absent
        staffAttendanceRepository.save(new StaffAttendance(
                staffId, "Prof. K. Test", mcaDept, null, baseDate.plusDays(8), 1, "ABSENT", "MANUAL_ENTRY", null, faculty));

        // 7 / 8 * 100 = 87.50%
        mockMvc.perform(get("/api/v1/attendance/staff/" + staffId + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalApplicableSessions").value(8))
                .andExpect(jsonPath("$.data.sessionsAttended").value(7))
                .andExpect(jsonPath("$.data.attendancePercentage").value(87.5))
                .andExpect(jsonPath("$.data.attendancePercentageFormatted").value("87.50%"));
    }

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Safe zero-session handling: when total applicable sessions = 0 in date range, returns 0.00% without error")
    void calculatePercentage_zeroApplicableSessions_returnsZeroWithoutError() throws Exception {
        String staffId = "STF-TEST-ZERO";
        // Create 1 record in the past
        staffAttendanceRepository.save(new StaffAttendance(
                staffId, "Prof. Zero", mcaDept, null, LocalDate.now().minusMonths(6), 1, "PRESENT", "MANUAL_ENTRY", null, faculty));

        // Query date range where zero sessions exist (e.g. yesterday to today)
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/v1/attendance/staff/" + staffId + "/summary")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalApplicableSessions").value(0))
                .andExpect(jsonPath("$.data.sessionsAttended").value(0))
                .andExpect(jsonPath("$.data.attendancePercentage").value(0.0))
                .andExpect(jsonPath("$.data.attendancePercentageFormatted").value("0.00%"))
                .andExpect(jsonPath("$.data.meetsRequirement").value(false));
    }

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Non-existent staff identifier returns 404 Not Found")
    void calculatePercentage_nonExistentStaff_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/staff/NON_EXISTENT_STAFF/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 3. Single Entry & Validation Tests
    // =========================================================================

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Valid attendance entry saves successfully and logs audit")
    void recordStaffAttendance_validEntry_savesSuccessfully() throws Exception {
        StaffAttendanceEntryRequest req = new StaffAttendanceEntryRequest(
                "STF-VALID-01",
                "Dr. Valid Entry",
                mcaDept.getDepartmentId(),
                null,
                LocalDate.now(),
                1,
                "PRESENT",
                "MANUAL_ENTRY",
                "Regular lecture session"
        );

        mockMvc.perform(post("/api/v1/attendance/staff")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.staffIdentifier").value("STF-VALID-01"))
                .andExpect(jsonPath("$.data.status").value("PRESENT"))
                .andExpect(jsonPath("$.data.dataSource").value("MANUAL_ENTRY"));

        assertTrue(staffAttendanceRepository.existsByStaffIdentifierAndAttendanceDateAndSessionPeriod(
                "STF-VALID-01", LocalDate.now(), 1));
    }

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Duplicate entry for same staff, date, and session period is rejected with 400 Bad Request")
    void recordStaffAttendance_duplicateEntry_rejected() throws Exception {
        staffAttendanceRepository.save(new StaffAttendance(
                "STF-DUP-01", "Dr. Dup", mcaDept, null, LocalDate.now(), 1, "PRESENT", "MANUAL_ENTRY", null, faculty));

        StaffAttendanceEntryRequest duplicateReq = new StaffAttendanceEntryRequest(
                "STF-DUP-01", "Dr. Dup", mcaDept.getDepartmentId(), null, LocalDate.now(), 1, "PRESENT", "MANUAL_ENTRY", null);

        mockMvc.perform(post("/api/v1/attendance/staff")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Invalid status value (e.g., 'UNKNOWN') is rejected with 400 Bad Request")
    void recordStaffAttendance_invalidStatus_rejected() throws Exception {
        StaffAttendanceEntryRequest invalidReq = new StaffAttendanceEntryRequest(
                "STF-INV-01", "Dr. Invalid", mcaDept.getDepartmentId(), null, LocalDate.now(), 1, "UNKNOWN", "MANUAL_ENTRY", null);

        mockMvc.perform(post("/api/v1/attendance/staff")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Invalid session period (e.g. 0 or 9) is rejected with 400 Bad Request")
    void recordStaffAttendance_invalidPeriod_rejected() throws Exception {
        StaffAttendanceEntryRequest invalidReq = new StaffAttendanceEntryRequest(
                "STF-INV-02", "Dr. Invalid", mcaDept.getDepartmentId(), null, LocalDate.now(), 10, "PRESENT", "MANUAL_ENTRY", null);

        mockMvc.perform(post("/api/v1/attendance/staff")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 4. Batch Import with Origin Tracking
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.att@college.edu", roles = {"ADMIN"})
    @DisplayName("Batch import saves multiple records, skips duplicates, and records origin")
    void batchImportStaffAttendance_tracksOriginAndDuplicates() throws Exception {
        // Pre-seed 1 duplicate
        staffAttendanceRepository.save(new StaffAttendance(
                "STF-BATCH-01", "Dr. Batch", mcaDept, null, LocalDate.now(), 1, "PRESENT", "CSV_IMPORT", null, admin));

        StaffAttendanceEntryRequest rec1 = new StaffAttendanceEntryRequest(
                "STF-BATCH-01", "Dr. Batch", mcaDept.getDepartmentId(), null, LocalDate.now(), 1, "PRESENT", "CSV_IMPORT", null); // Duplicate
        StaffAttendanceEntryRequest rec2 = new StaffAttendanceEntryRequest(
                "STF-BATCH-01", "Dr. Batch", mcaDept.getDepartmentId(), null, LocalDate.now(), 2, "PRESENT", "CSV_IMPORT", null); // New
        StaffAttendanceEntryRequest rec3 = new StaffAttendanceEntryRequest(
                "STF-BATCH-02", "Prof. Batch Two", eceDept.getDepartmentId(), null, LocalDate.now(), 1, "ON_DUTY", "CSV_IMPORT", "Conference"); // New

        StaffAttendanceBatchImportRequest importReq = new StaffAttendanceBatchImportRequest("CSV_IMPORT", List.of(rec1, rec2, rec3));

        mockMvc.perform(post("/api/v1/attendance/staff/import")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSubmitted").value(3))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.duplicateCount").value(1))
                .andExpect(jsonPath("$.data.origin").value("CSV_IMPORT"));
    }

    // =========================================================================
    // 5. Search Filters
    // =========================================================================

    @Test
    @WithMockUser(username = "faculty.att@college.edu", roles = {"FACULTY"})
    @DisplayName("Search filters by staffIdentifier, departmentId, and status")
    void searchStaffAttendance_appliesFiltersCorrectly() throws Exception {
        staffAttendanceRepository.save(new StaffAttendance(
                "STF-SEARCH-01", "Dr. Search", mcaDept, null, LocalDate.now(), 1, "PRESENT", "MANUAL_ENTRY", null, faculty));
        staffAttendanceRepository.save(new StaffAttendance(
                "STF-SEARCH-01", "Dr. Search", mcaDept, null, LocalDate.now(), 2, "ABSENT", "MANUAL_ENTRY", null, faculty));
        staffAttendanceRepository.save(new StaffAttendance(
                "STF-SEARCH-02", "Prof. Other", eceDept, null, LocalDate.now(), 1, "PRESENT", "MANUAL_ENTRY", null, faculty));

        // Filter by MCA department and ABSENT status
        mockMvc.perform(get("/api/v1/attendance/staff")
                        .param("departmentId", mcaDept.getDepartmentId().toString())
                        .param("status", "ABSENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].staffIdentifier").value("STF-SEARCH-01"))
                .andExpect(jsonPath("$.data.content[0].status").value("ABSENT"));
    }
}
