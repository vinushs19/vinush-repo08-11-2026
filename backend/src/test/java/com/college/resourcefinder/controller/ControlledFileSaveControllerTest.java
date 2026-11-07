package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.filesave.ControlledFileSaveRequest;
import com.college.resourcefinder.entity.*;
import com.college.resourcefinder.repository.*;
import com.college.resourcefinder.service.StorageService;
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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControlledFileSaveControllerTest {

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
    private SubjectRepository subjectRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private FileOperationRepository fileOperationRepository;

    @Autowired
    private StorageService storageService;

    private User admin;
    private User faculty1;
    private User faculty2;
    private User student;
    private Department csDept;
    private Department eceDept;
    private Subject csSubject;
    private Subject eceSubject;
    private Category testCategory;
    private Resource physicalResource;

    @BeforeEach
    void setUp() throws Exception {
        admin = userRepository.findByEmail("admin.fs@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Admin FS",
                        "admin.fs@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.ADMIN.name(),
                        null
                )));

        csDept = departmentRepository.findByDepartmentCode("CS_FS")
                .orElseGet(() -> departmentRepository.save(new Department("CS_FS", "Computer Science File Save")));

        eceDept = departmentRepository.findByDepartmentCode("ECE_FS")
                .orElseGet(() -> departmentRepository.save(new Department("ECE_FS", "ECE File Save")));

        faculty1 = userRepository.findByEmail("faculty1.fs@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Faculty One FS",
                        "faculty1.fs@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.FACULTY.name(),
                        csDept
                )));

        faculty2 = userRepository.findByEmail("faculty2.fs@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Faculty Two FS",
                        "faculty2.fs@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.FACULTY.name(),
                        eceDept
                )));

        student = userRepository.findByEmail("student.fs@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Student FS",
                        "student.fs@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.STUDENT.name(),
                        csDept
                )));

        csSubject = subjectRepository.findBySubjectCode("CS_FS_101")
                .orElseGet(() -> subjectRepository.save(new Subject(
                        "CS_FS_101", "Database Systems", 1, csDept, "Core DBMS")));

        eceSubject = subjectRepository.findBySubjectCode("ECE_FS_101")
                .orElseGet(() -> subjectRepository.save(new Subject(
                        "ECE_FS_101", "Signals", 1, eceDept, "Core Signals")));

        testCategory = categoryRepository.findByCategoryName("Notes_FS")
                .orElseGet(() -> categoryRepository.save(new Category("Notes_FS", "Notes")));

        // Create a real physical file in test storage root for physicalResource
        String relativeSourcePath = "source_tests/test_doc.pdf";
        Path physicalSourcePath = storageService.getRootLocation().resolve(relativeSourcePath);
        Files.createDirectories(physicalSourcePath.getParent());
        Files.write(physicalSourcePath, "%PDF-1.4 Mock Physical File Content for Controlled Save".getBytes());

        physicalResource = resourceRepository.save(new Resource(
                "Database Architecture Notes",
                "DBMS Notes for controlled save testing",
                relativeSourcePath,
                "test_doc.pdf",
                "original_dbms.pdf",
                "pdf",
                (long) "%PDF-1.4 Mock Physical File Content for Controlled Save".length(),
                "Lecture notes",
                csSubject,
                null,
                testCategory,
                faculty1
        ));
    }

    // =========================================================================
    // 1. Success & Real File Execution Tests
    // =========================================================================

    @Test
    @WithMockUser(username = "faculty1.fs@college.edu", roles = {"FACULTY"})
    @DisplayName("Controlled save copies physical file, enforces allowlisted directory, and stores operation metadata")
    void executeControlledSave_validRequest_copiesFileAndRecordsMetadata() throws Exception {
        ControlledFileSaveRequest request = new ControlledFileSaveRequest(
                physicalResource.getResourceId(),
                "QUESTION_PAPERS",
                "MCA_SEM1",
                false,
                "Organizing end semester paper"
        );

        mockMvc.perform(post("/api/v1/files/controlled-save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resourceId").value(physicalResource.getResourceId()))
                .andExpect(jsonPath("$.data.destinationCategory").value("QUESTION_PAPERS"))
                .andExpect(jsonPath("$.data.destinationPath", containsString("organized/question_papers/MCA_SEM1")))
                .andExpect(jsonPath("$.data.destinationFilename", startsWith("saved_")))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        // Verify physical file copied to disk
        var ops = fileOperationRepository.findByResource_ResourceId(physicalResource.getResourceId());
        assertTrue(ops.size() >= 1);
        FileOperation op = ops.get(ops.size() - 1);
        assertTrue(storageService.fileExists(op.getDestinationPath()));
    }

    // =========================================================================
    // 2. Access Control Tests
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated request to controlled save returns 401 Unauthorized")
    void executeControlledSave_unauthenticated_returns401() throws Exception {
        ControlledFileSaveRequest request = new ControlledFileSaveRequest(
                physicalResource.getResourceId(), "QUESTION_PAPERS", null, false, null);

        mockMvc.perform(post("/api/v1/files/controlled-save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student.fs@college.edu", roles = {"STUDENT"})
    @DisplayName("Student role cannot execute controlled save - returns 403 Forbidden")
    void executeControlledSave_studentRole_returns403() throws Exception {
        ControlledFileSaveRequest request = new ControlledFileSaveRequest(
                physicalResource.getResourceId(), "QUESTION_PAPERS", null, false, null);

        mockMvc.perform(post("/api/v1/files/controlled-save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "faculty2.fs@college.edu", roles = {"FACULTY"})
    @DisplayName("Faculty cannot organize resources from another department without ownership - returns 403 Forbidden")
    void executeControlledSave_unauthorizedFaculty_returns403() throws Exception {
        ControlledFileSaveRequest request = new ControlledFileSaveRequest(
                physicalResource.getResourceId(), "QUESTION_PAPERS", null, false, null);

        mockMvc.perform(post("/api/v1/files/controlled-save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin.fs@college.edu", roles = {"ADMIN"})
    @DisplayName("Administrator can organize any resource across any department")
    void executeControlledSave_adminRole_canOrganizeAnyResource() throws Exception {
        ControlledFileSaveRequest request = new ControlledFileSaveRequest(
                physicalResource.getResourceId(), "STUDY_MATERIALS", "ADMIN_BATCH", false, "Admin archiving");

        mockMvc.perform(post("/api/v1/files/controlled-save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.destinationCategory").value("STUDY_MATERIALS"));
    }

    // =========================================================================
    // 3. Security & Path Traversal Prevention
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.fs@college.edu", roles = {"ADMIN"})
    @DisplayName("Path traversal sequences in subfolder tag (../) are rejected with 400 Bad Request")
    void executeControlledSave_pathTraversalInSubfolder_rejected() throws Exception {
        ControlledFileSaveRequest request = new ControlledFileSaveRequest(
                physicalResource.getResourceId(), "QUESTION_PAPERS", "../../../traversal", false, null);

        mockMvc.perform(post("/api/v1/files/controlled-save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin.fs@college.edu", roles = {"ADMIN"})
    @DisplayName("Unallowlisted destination category is rejected with 400 Bad Request")
    void executeControlledSave_unallowlistedCategory_rejected() throws Exception {
        ControlledFileSaveRequest request = new ControlledFileSaveRequest(
                physicalResource.getResourceId(), "ILLEGAL_DESTINATION", null, false, null);

        mockMvc.perform(post("/api/v1/files/controlled-save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 4. Missing Resource & Missing Physical File Tests
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.fs@college.edu", roles = {"ADMIN"})
    @DisplayName("Non-existent resource returns 404 Not Found")
    void executeControlledSave_nonExistentResource_returns404() throws Exception {
        ControlledFileSaveRequest request = new ControlledFileSaveRequest(
                999999L, "QUESTION_PAPERS", null, false, null);

        mockMvc.perform(post("/api/v1/files/controlled-save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin.fs@college.edu", roles = {"ADMIN"})
    @DisplayName("Missing physical file on server returns 404 without leaking server paths")
    void executeControlledSave_missingPhysicalFile_returns404() throws Exception {
        Resource missingFileResource = resourceRepository.save(new Resource(
                "Ghost Document", "Points to missing file", "ghost/missing.pdf", "missing.pdf",
                "missing.pdf", "pdf", 100L, "Lecture notes", csSubject, null, testCategory, admin));

        ControlledFileSaveRequest request = new ControlledFileSaveRequest(
                missingFileResource.getResourceId(), "QUESTION_PAPERS", null, false, null);

        mockMvc.perform(post("/api/v1/files/controlled-save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not available on the server")));
    }

    // =========================================================================
    // 5. Allowlist Categories & History Query Tests
    // =========================================================================

    @Test
    @WithMockUser(username = "faculty1.fs@college.edu", roles = {"FACULTY"})
    @DisplayName("Approved categories endpoint returns allowlisted categories")
    void getApprovedCategories_returnsAllowlistedCategories() throws Exception {
        mockMvc.perform(get("/api/v1/files/controlled-save/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$.data[*].code", hasItems("LECTURE_NOTES", "QUESTION_PAPERS", "ASSIGNMENTS")));
    }

    @Test
    @WithMockUser(username = "faculty1.fs@college.edu", roles = {"FACULTY"})
    @DisplayName("History endpoint returns paginated file operations")
    void listOperations_returnsPaginatedHistory() throws Exception {
        mockMvc.perform(get("/api/v1/files/controlled-save/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
