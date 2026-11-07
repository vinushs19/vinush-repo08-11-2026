package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.request.UserStatusUpdateRequest;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User studentUser;
    private Department testDept;
    private Category testCategory;
    private Subject testSubject;
    private Unit testUnit;

    @BeforeEach
    void setUp() {
        testDept = departmentRepository.save(new Department("MCA_ADM", "Master of Computer Applications Admin"));

        adminUser = new User();
        adminUser.setEmail("admin.reporting@college.edu");
        adminUser.setName("System Admin");
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser.setRole("ADMIN");
        adminUser.setDepartment(testDept);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);

        studentUser = new User();
        studentUser.setEmail("student.regular@college.edu");
        studentUser.setName("Regular Student");
        studentUser.setPassword(passwordEncoder.encode("StudentPass123!"));
        studentUser.setRole("STUDENT");
        studentUser.setDepartment(testDept);
        studentUser.setIsActive(true);
        studentUser = userRepository.save(studentUser);

        testCategory = categoryRepository.save(new Category("Exam Papers", "Previous year exam questions"));

        testSubject = new Subject();
        testSubject.setSubjectCode("MCA1001");
        testSubject.setSubjectName("Distributed Systems");
        testSubject.setSemester(1);
        testSubject.setDepartment(testDept);
        testSubject = subjectRepository.save(testSubject);

        testUnit = unitRepository.save(new Unit(testSubject, 1, "Cloud Architectures", "Cloud overview"));

        Resource resource = new Resource();
        resource.setTitle("Distributed Systems Architecture Notes");
        resource.setDescription("Unit 1 cloud overview");
        resource.setFilePath("MCA_ADM/SEM1/MCA1001/notes.pdf");
        resource.setFileName("res_notes123.pdf");
        resource.setOriginalFilename("notes.pdf");
        resource.setFileType("application/pdf");
        resource.setFileSize(2048L);
        resource.setResourceType("Lecture Notes");
        resource.setSubject(testSubject);
        resource.setUnit(testUnit);
        resource.setCategory(testCategory);
        resource.setUploadedBy(adminUser);
        resource.setStatus("ACTIVE");
        resource.setIsActive(true);
        resourceRepository.save(resource);

        SearchHistory search = new SearchHistory();
        search.setUser(studentUser);
        search.setSearchQuery("Distributed Systems Notes");
        search.setResultCount(1);
        searchHistoryRepository.save(search);

        AuditLog auditLog = new AuditLog();
        auditLog.setUser(adminUser);
        auditLog.setAction("RESOURCE_UPLOAD");
        auditLog.setStatus("SUCCESS");
        auditLog.setResourceType("RESOURCE");
        auditLog.setResourceIdentifier("Distributed Systems Architecture Notes");
        auditLog.setIpAddress("127.0.0.1");
        auditLog.setDetails("Uploaded notes for Unit 1");
        auditLogRepository.save(auditLog);
    }

    @Test
    @DisplayName("Admin dashboard metrics returns accurate real database figures")
    @WithMockUser(username = "admin.reporting@college.edu", roles = {"ADMIN"})
    void getDashboardMetrics_asAdmin_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.adminCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.studentCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalResources", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.activeResources", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.storageUsedBytes", greaterThanOrEqualTo(2048)))
                .andExpect(jsonPath("$.data.totalSearchQueries", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Student is forbidden from viewing admin dashboard metrics")
    @WithMockUser(username = "student.regular@college.edu", roles = {"STUDENT"})
    void getDashboardMetrics_asStudent_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Faculty is forbidden from viewing admin dashboard metrics")
    @WithMockUser(username = "faculty@college.edu", roles = {"FACULTY"})
    void getDashboardMetrics_asFaculty_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Department resource distribution returns real breakdown")
    @WithMockUser(username = "admin.reporting@college.edu", roles = {"ADMIN"})
    void getDepartmentDistribution_asAdmin_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/departments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.departmentCode == 'MCA_ADM')].resourceCount").value(hasItem(1)))
                .andExpect(jsonPath("$.data[?(@.departmentCode == 'MCA_ADM')].storageBytes").value(hasItem(2048)));
    }

    @Test
    @DisplayName("Semester resource distribution returns valid semester breakdown")
    @WithMockUser(username = "admin.reporting@college.edu", roles = {"ADMIN"})
    void getSemesterDistribution_asAdmin_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/semesters")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.semesterNumber == 1)].resourceCount").value(hasItem(1)));
    }

    @Test
    @DisplayName("Category resource distribution returns valid category breakdown")
    @WithMockUser(username = "admin.reporting@college.edu", roles = {"ADMIN"})
    void getCategoryDistribution_asAdmin_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.categoryName == 'Exam Papers')].resourceCount").value(hasItem(1)));
    }

    @Test
    @DisplayName("Search activity report returns top search queries from actual history")
    @WithMockUser(username = "admin.reporting@college.edu", roles = {"ADMIN"})
    void getSearchActivity_asAdmin_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/search-activity?limit=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSearches", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.topQueries[?(@.query == 'Distributed Systems Notes')].count").value(hasItem(1)));
    }

    @Test
    @DisplayName("Audit logs endpoint returns paginated audit records")
    @WithMockUser(username = "admin.reporting@college.edu", roles = {"ADMIN"})
    void getAuditLogs_asAdmin_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].action").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].ipAddress").isNotEmpty());
    }

    @Test
    @DisplayName("Audit logs endpoint filters by action and status correctly")
    @WithMockUser(username = "admin.reporting@college.edu", roles = {"ADMIN"})
    void getAuditLogs_withFilters_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs?action=RESOURCE_UPLOAD&status=SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].action").value("RESOURCE_UPLOAD"))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Admin updates user account status and role with audit trail")
    @WithMockUser(username = "admin.reporting@college.edu", roles = {"ADMIN"})
    void updateUserStatus_asAdmin_success() throws Exception {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setIsActive(false);
        request.setRole("FACULTY");

        mockMvc.perform(patch("/api/v1/admin/users/" + studentUser.getUserId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.role").value("FACULTY"));
    }
}
