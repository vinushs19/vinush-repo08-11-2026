package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.master.*;
import com.college.resourcefinder.entity.Department;
import com.college.resourcefinder.entity.Semester;
import com.college.resourcefinder.entity.Subject;
import com.college.resourcefinder.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MasterDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Department seededDepartment;
    private Semester seededSemester;
    private Subject seededSubject;

    @BeforeEach
    void setUp() {
        seededDepartment = departmentRepository.save(new Department(
                "MD",
                "Master Data Department"
        ));

        seededSemester = semesterRepository.save(new Semester(
                1,
                "Semester 1",
                "ODD"
        ));

        seededSubject = subjectRepository.save(new Subject(
                "CS8101",
                "Advanced Java",
                1,
                seededDepartment,
                "Advanced Java Programming course"
        ));
    }

    // =========================================================================
    // AUTHORIZATION & RBAC TESTS
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/master/departments - unauthenticated request should return 401 Unauthorized")
    void shouldDenyUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/master/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/master/departments - student should be permitted to read departments")
    void shouldAllowStudentToReadDepartments() throws Exception {
        mockMvc.perform(get("/api/v1/master/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())));
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("POST /api/v1/master/departments - student should be forbidden (403) from creating departments")
    void shouldForbidStudentFromCreatingDepartment() throws Exception {
        DepartmentRequest request = new DepartmentRequest("PHY", "Physics", true);

        mockMvc.perform(post("/api/v1/master/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @WithMockUser(username = "faculty@college.edu", roles = {"FACULTY"})
    @DisplayName("POST /api/v1/master/departments - faculty should be forbidden (403) from creating departments")
    void shouldForbidFacultyFromCreatingDepartment() throws Exception {
        DepartmentRequest request = new DepartmentRequest("CHM", "Chemistry", true);

        mockMvc.perform(post("/api/v1/master/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    // =========================================================================
    // DEPARTMENT CRUD & CONSTRAINTS
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/master/departments - admin creates department successfully")
    void shouldAllowAdminToCreateDepartment() throws Exception {
        DepartmentRequest request = new DepartmentRequest(
                "BIO",
                "Biotechnology",
                true
        );

        mockMvc.perform(post("/api/v1/master/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.departmentCode", is("BIO")))
                .andExpect(jsonPath("$.data.departmentName", is("Biotechnology")))
                .andExpect(jsonPath("$.data.isActive", is(true)));
    }

    @Test
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/master/departments - duplicate department code is rejected with 400")
    void shouldRejectDuplicateDepartmentCode() throws Exception {
        DepartmentRequest request = new DepartmentRequest(
                "MD", // seededDepartment uses MD
                "Duplicate Dept",
                true
        );

        mockMvc.perform(post("/api/v1/master/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    @DisplayName("PATCH /api/v1/master/departments/{id}/status - deactivate department soft-delete")
    void shouldDeactivateDepartment() throws Exception {
        StatusUpdateRequest request = new StatusUpdateRequest(false);

        mockMvc.perform(patch("/api/v1/master/departments/" + seededDepartment.getDepartmentId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isActive", is(false)));
    }

    // =========================================================================
    // SUBJECT CRUD & CONSTRAINTS
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/master/subjects - admin creates subject successfully")
    void shouldAllowAdminToCreateSubject() throws Exception {
        SubjectRequest request = new SubjectRequest(
                "CS8102",
                "Data Structures and Algorithms",
                1,
                seededDepartment.getDepartmentId(),
                "Core DSA syllabus",
                true
        );

        mockMvc.perform(post("/api/v1/master/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.subjectCode", is("CS8102")))
                .andExpect(jsonPath("$.data.departmentCode", is("MD")));
    }

    @Test
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/master/subjects - duplicate subject code rejected with 400")
    void shouldRejectDuplicateSubjectCode() throws Exception {
        SubjectRequest request = new SubjectRequest(
                "CS8101", // already used
                "Another Java Subject",
                1,
                seededDepartment.getDepartmentId(),
                "Duplicate code test",
                true
        );

        mockMvc.perform(post("/api/v1/master/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    // =========================================================================
    // UNIT CRUD & CONSTRAINTS
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/master/subjects/{subjectId}/units - creates unit and rejects duplicates")
    void shouldManageUnitsForSubject() throws Exception {
        UnitRequest unit1 = new UnitRequest(
                1,
                "Introduction to OOP and Java",
                "Classes, objects, inheritance, polymorphism",
                true
        );

        mockMvc.perform(post("/api/v1/master/subjects/" + seededSubject.getSubjectId() + "/units")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unit1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.unitNumber", is(1)))
                .andExpect(jsonPath("$.data.unitTitle", is("Introduction to OOP and Java")));

        // Duplicate unit 1 for the same subject should be rejected
        mockMvc.perform(post("/api/v1/master/subjects/" + seededSubject.getSubjectId() + "/units")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unit1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists for subject")));
    }

    // =========================================================================
    // CATEGORY CRUD & CONSTRAINTS
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/master/categories - creates category and prevents duplicates")
    void shouldManageCategories() throws Exception {
        CategoryRequest cat = new CategoryRequest(
                "Lecture Slides",
                "PowerPoint and PDF presentations used during classroom lectures",
                true
        );

        mockMvc.perform(post("/api/v1/master/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.categoryName", is("Lecture Slides")));

        // Duplicate category name rejected
        mockMvc.perform(post("/api/v1/master/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    // =========================================================================
    // SEMESTERS
    // =========================================================================

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/master/semesters - returns semester list")
    void shouldListSemesters() throws Exception {
        mockMvc.perform(get("/api/v1/master/semesters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", not(empty())));
    }
}
