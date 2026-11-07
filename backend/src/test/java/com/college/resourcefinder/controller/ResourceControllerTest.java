package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.resource.ResourceUpdateRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ResourceControllerTest {

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
    private UnitRepository unitRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private StorageService storageService;

    private User faculty1;
    private User faculty2;
    private User admin;
    private Department testDept;
    private Subject testSubject;
    private Unit testUnit;
    private Category testCategory;
    private Resource seededResource;

    @BeforeEach
    void setUp() {
        faculty1 = userRepository.findByEmail("faculty1.res@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Faculty One",
                        "faculty1.res@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.FACULTY.name(),
                        null
                )));

        faculty2 = userRepository.findByEmail("faculty2.res@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Faculty Two",
                        "faculty2.res@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.FACULTY.name(),
                        null
                )));

        admin = userRepository.findByEmail("admin.res@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Admin User",
                        "admin.res@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.ADMIN.name(),
                        null
                )));

        userRepository.findByEmail("student.res@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Student User",
                        "student.res@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.STUDENT.name(),
                        null
                )));

        testDept = departmentRepository.findByDepartmentCode("CS_RES")
                .orElseGet(() -> departmentRepository.save(new Department("CS_RES", "Computer Science Resources")));

        testSubject = subjectRepository.findBySubjectCode("CS9001")
                .orElseGet(() -> subjectRepository.save(new Subject(
                        "CS9001",
                        "Relational Database Systems",
                        1,
                        testDept,
                        "Core DBMS syllabus"
                )));

        testUnit = unitRepository.findBySubject_SubjectIdAndUnitNumber(testSubject.getSubjectId(), 1)
                .orElseGet(() -> unitRepository.save(new Unit(
                        testSubject,
                        1,
                        "Relational Model & ER",
                        "Introduction to ER diagrams"
                )));

        testCategory = categoryRepository.findByCategoryName("Lecture Notes")
                .orElseGet(() -> categoryRepository.save(new Category(
                        "Lecture Notes",
                        "Classroom lecture slides and PDF notes"
                )));

        // Create a test file in storage and seed an active Resource with valid %PDF header
        MockMultipartFile testFile = new MockMultipartFile(
                "file", "dbms_unit1.pdf", "application/pdf", "%PDF-1.4 Dummy PDF content for unit test".getBytes()
        );
        StorageService.StoredFile stored = storageService.storeFile(testFile, "TEST_DEPT/SEM1/CS9001");

        seededResource = resourceRepository.save(new Resource(
                "Database Systems Unit 1 Comprehensive Notes",
                "Full coverage of ER modeling, schema conversion and SQL",
                stored.relativeFilePath(),
                stored.uniqueFilename(),
                stored.originalFilename(),
                stored.fileType(),
                stored.fileSize(),
                "Lecture notes",
                testSubject,
                testUnit,
                testCategory,
                faculty1
        ));
    }

    // =========================================================================
    // AUTHORIZATION & RBAC TESTS
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/resources - unauthenticated request is rejected with 401")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @WithMockUser(username = "student.res@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/resources - student can list academic resources")
    void shouldAllowStudentToListResources() throws Exception {
        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].title", containsString("Database Systems")));
    }

    @Test
    @WithMockUser(username = "student.res@college.edu", roles = {"STUDENT"})
    @DisplayName("POST /api/v1/resources - student is forbidden (403) from uploading resources")
    void shouldForbidStudentFromUploadingResource() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "%PDF-1.4 Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resources")
                        .file(file)
                        .param("title", "Student Upload Attempt")
                        .param("subjectId", testSubject.getSubjectId().toString())
                        .param("categoryId", testCategory.getCategoryId().toString())
                        .param("resourceType", "Lecture notes")
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    // =========================================================================
    // UPLOAD & CREATION TESTS (PDF, DOCX, MAGIC BYTES)
    // =========================================================================

    @Test
    @WithMockUser(username = "faculty1.res@college.edu", roles = {"FACULTY"})
    @DisplayName("POST /api/v1/resources - faculty can upload valid PDF resource")
    void shouldAllowFacultyToUploadPdfResource() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "exam_solutions_2025.pdf", "application/pdf", "%PDF-1.4 Mock exam PDF bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resources")
                        .file(file)
                        .param("title", "Previous Year Question Paper Solutions 2025")
                        .param("description", "Complete solved question paper for mid-term exams")
                        .param("subjectId", testSubject.getSubjectId().toString())
                        .param("unitId", testUnit.getUnitId().toString())
                        .param("categoryId", testCategory.getCategoryId().toString())
                        .param("resourceType", "Previous examination papers")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Previous Year Question Paper Solutions 2025")))
                .andExpect(jsonPath("$.data.originalFilename", is("exam_solutions_2025.pdf")))
                .andExpect(jsonPath("$.data.fileType", is("pdf")))
                .andExpect(jsonPath("$.data.uploadedByName", is("Faculty One")))
                .andExpect(jsonPath("$.data.subjectCode", is("CS9001")))
                .andExpect(jsonPath("$.data.unitNumber", is(1)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.isActive", is(true)));
    }

    @Test
    @WithMockUser(username = "faculty1.res@college.edu", roles = {"FACULTY"})
    @DisplayName("POST /api/v1/resources - faculty can upload valid DOCX resource")
    void shouldAllowFacultyToUploadDocxResource() throws Exception {
        byte[] docxBytes = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "curriculum_assignment.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        mockMvc.perform(multipart("/api/v1/resources")
                        .file(file)
                        .param("title", "Database Design Lab Assignment")
                        .param("description", "Normalization practice problems")
                        .param("subjectId", testSubject.getSubjectId().toString())
                        .param("categoryId", testCategory.getCategoryId().toString())
                        .param("resourceType", "Assignments")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fileType", is("docx")))
                .andExpect(jsonPath("$.data.title", is("Database Design Lab Assignment")));
    }

    @Test
    @WithMockUser(username = "faculty1.res@college.edu", roles = {"FACULTY"})
    @DisplayName("POST /api/v1/resources - spoofed or corrupted PDF is rejected with 400 Bad Request")
    void shouldRejectSpoofedPdfFile() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "fake_document.pdf", "application/pdf",
                "NOT_REAL_PDF_HEADER_BYTES".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resources")
                        .file(badFile)
                        .param("title", "Corrupted PDF Upload")
                        .param("subjectId", testSubject.getSubjectId().toString())
                        .param("categoryId", testCategory.getCategoryId().toString())
                        .param("resourceType", "Lecture notes")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("INVALID_FILE")))
                .andExpect(jsonPath("$.message", containsString("File signature verification failed")));
    }

    @Test
    @WithMockUser(username = "faculty1.res@college.edu", roles = {"FACULTY"})
    @DisplayName("POST /api/v1/resources - disallowed extension (.exe) is rejected with 400 Bad Request")
    void shouldRejectDisallowedFileExtension() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream",
                "MZ executable content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resources")
                        .file(badFile)
                        .param("title", "Dangerous Binary")
                        .param("subjectId", testSubject.getSubjectId().toString())
                        .param("categoryId", testCategory.getCategoryId().toString())
                        .param("resourceType", "Lecture notes")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("INVALID_FILE")))
                .andExpect(jsonPath("$.message", containsString("not permitted")));
    }

    @Test
    @WithMockUser(username = "faculty1.res@college.edu", roles = {"FACULTY"})
    @DisplayName("POST /api/v1/resources - duplicate original filenames are safely isolated on disk")
    void shouldHandleDuplicateFilenamesSafely() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", "%PDF-1.4 Syllabus v1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", "%PDF-1.4 Syllabus v2 revised".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resources")
                        .file(file1)
                        .param("title", "Syllabus Initial")
                        .param("subjectId", testSubject.getSubjectId().toString())
                        .param("categoryId", testCategory.getCategoryId().toString())
                        .param("resourceType", "Lecture notes")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalFilename", is("syllabus.pdf")));

        mockMvc.perform(multipart("/api/v1/resources")
                        .file(file2)
                        .param("title", "Syllabus Revised")
                        .param("subjectId", testSubject.getSubjectId().toString())
                        .param("categoryId", testCategory.getCategoryId().toString())
                        .param("resourceType", "Lecture notes")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalFilename", is("syllabus.pdf")));
    }

    // =========================================================================
    // RESOURCE DETAIL & DOWNLOAD TESTS
    // =========================================================================

    @Test
    @WithMockUser(username = "student.res@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/resources/{id} - returns complete resource metadata")
    void shouldReturnResourceDetails() throws Exception {
        mockMvc.perform(get("/api/v1/resources/" + seededResource.getResourceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.resourceId", is(seededResource.getResourceId().intValue())))
                .andExpect(jsonPath("$.data.title", is(seededResource.getTitle())))
                .andExpect(jsonPath("$.data.departmentCode", is("CS_RES")))
                .andExpect(jsonPath("$.data.subjectCode", is("CS9001")))
                .andExpect(jsonPath("$.data.unitNumber", is(1)))
                .andExpect(jsonPath("$.data.uploadedByName", is("Faculty One")));
    }

    @Test
    @WithMockUser(username = "student.res@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/resources/{id}/download - streams file with attachment header and nosniff")
    void shouldStreamFileDownload() throws Exception {
        mockMvc.perform(get("/api/v1/resources/" + seededResource.getResourceId() + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=")))
                .andExpect(header().string("X-Content-Type-Options", is("nosniff")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser(username = "student.res@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/resources/{id}/preview - streams file with inline preview header")
    void shouldStreamFilePreview() throws Exception {
        mockMvc.perform(get("/api/v1/resources/" + seededResource.getResourceId() + "/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("inline; filename=")))
                .andExpect(header().string("X-Content-Type-Options", is("nosniff")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser(username = "student.res@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/resources/{id}/download - missing physical file returns safe 404 without path leak")
    void shouldHandleMissingPhysicalFileSafely() throws Exception {
        Resource phantomResource = resourceRepository.save(new Resource(
                "Phantom Resource",
                "File was purged from disk",
                "NON_EXISTENT_FOLDER/ghost_file.pdf",
                "ghost_file.pdf",
                "original_ghost.pdf",
                "pdf",
                100L,
                "Lecture notes",
                testSubject,
                null,
                testCategory,
                faculty1
        ));

        mockMvc.perform(get("/api/v1/resources/" + phantomResource.getResourceId() + "/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("The requested physical file is not available on the server")))
                // Ensure no local disk paths are exposed
                .andExpect(jsonPath("$.message", not(containsString("C:\\"))))
                .andExpect(jsonPath("$.message", not(containsString("D:\\"))));
    }

    @Test
    @WithMockUser(username = "student.res@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/resources/{id}/download - student cannot download deactivated resource")
    void shouldForbidStudentFromDownloadingDeactivatedResource() throws Exception {
        seededResource.setIsActive(false);
        seededResource.setStatus("DELETED");
        resourceRepository.save(seededResource);

        mockMvc.perform(get("/api/v1/resources/" + seededResource.getResourceId() + "/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================================
    // OWNERSHIP & MODIFICATION PERMISSIONS
    // =========================================================================

    @Test
    @WithMockUser(username = "faculty1.res@college.edu", roles = {"FACULTY"})
    @DisplayName("PUT /api/v1/resources/{id} - owner faculty can update their own resource")
    void shouldAllowOwnerFacultyToUpdateResource() throws Exception {
        ResourceUpdateRequest updateReq = new ResourceUpdateRequest(
                "Updated DBMS Unit 1 Title",
                "Updated description with more details",
                testSubject.getSubjectId(),
                testUnit.getUnitId(),
                testCategory.getCategoryId(),
                "Study materials"
        );

        mockMvc.perform(put("/api/v1/resources/" + seededResource.getResourceId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Updated DBMS Unit 1 Title")))
                .andExpect(jsonPath("$.data.resourceType", is("Study materials")));
    }

    @Test
    @WithMockUser(username = "faculty2.res@college.edu", roles = {"FACULTY"})
    @DisplayName("PUT /api/v1/resources/{id} - non-owner faculty is forbidden (403) from updating resource")
    void shouldForbidNonOwnerFacultyFromUpdatingResource() throws Exception {
        ResourceUpdateRequest updateReq = new ResourceUpdateRequest(
                "Unauthorized Tamper Attempt",
                "Trying to overwrite colleague's notes",
                testSubject.getSubjectId(),
                null,
                testCategory.getCategoryId(),
                "Study materials"
        );

        mockMvc.perform(put("/api/v1/resources/" + seededResource.getResourceId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @WithMockUser(username = "admin.res@college.edu", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/resources/{id} - admin can update any resource regardless of owner")
    void shouldAllowAdminToUpdateAnyResource() throws Exception {
        ResourceUpdateRequest updateReq = new ResourceUpdateRequest(
                "Admin Moderated Title",
                "Moderated by department chair",
                testSubject.getSubjectId(),
                testUnit.getUnitId(),
                testCategory.getCategoryId(),
                "Lecture notes"
        );

        mockMvc.perform(put("/api/v1/resources/" + seededResource.getResourceId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Admin Moderated Title")));
    }

    // =========================================================================
    // DELETION, SOFT-DEACTIVATION, & PERMANENT PURGE
    // =========================================================================

    @Test
    @WithMockUser(username = "faculty2.res@college.edu", roles = {"FACULTY"})
    @DisplayName("DELETE /api/v1/resources/{id} - non-owner faculty is forbidden from deleting")
    void shouldForbidNonOwnerFromDeleting() throws Exception {
        mockMvc.perform(delete("/api/v1/resources/" + seededResource.getResourceId())
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @WithMockUser(username = "faculty1.res@college.edu", roles = {"FACULTY"})
    @DisplayName("DELETE /api/v1/resources/{id} - owner faculty can safely soft-delete resource")
    void shouldAllowOwnerToSoftDeleteResource() throws Exception {
        mockMvc.perform(delete("/api/v1/resources/" + seededResource.getResourceId())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("DELETED")))
                .andExpect(jsonPath("$.data.isActive", is(false)));

        Resource inDb = resourceRepository.findById(seededResource.getResourceId()).orElseThrow();
        assertFalse(inDb.getIsActive());
    }

    @Test
    @WithMockUser(username = "admin.res@college.edu", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/resources/{id}/purge - admin permanently purges record and physical file")
    void shouldAllowAdminToPurgeResourceAndPhysicalFile() throws Exception {
        Long resId = seededResource.getResourceId();
        String relPath = seededResource.getFilePath();

        // Verify physical file exists before purge
        Path physFile = storageService.getRootLocation().resolve(relPath).normalize();
        assertTrue(Files.exists(physFile));

        mockMvc.perform(delete("/api/v1/resources/" + resId + "/purge")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("permanently purged")));

        // Verify DB record is deleted
        assertFalse(resourceRepository.findById(resId).isPresent());

        // Verify physical file is deleted (no orphan file)
        assertFalse(Files.exists(physFile));
    }

    @Test
    @WithMockUser(username = "faculty1.res@college.edu", roles = {"FACULTY"})
    @DisplayName("DELETE /api/v1/resources/{id}/purge - faculty is forbidden (403) from purging physical file")
    void shouldForbidFacultyFromPurgingFile() throws Exception {
        mockMvc.perform(delete("/api/v1/resources/" + seededResource.getResourceId() + "/purge")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // MULTI-FACET FILTERING
    // =========================================================================

    @Test
    @WithMockUser(username = "student.res@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/resources - filters by department, subject, semester, and keyword")
    void shouldFilterResourcesByTaxonomy() throws Exception {
        mockMvc.perform(get("/api/v1/resources")
                        .param("departmentId", testDept.getDepartmentId().toString())
                        .param("subjectId", testSubject.getSubjectId().toString())
                        .param("semester", "1")
                        .param("search", "Database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].subjectCode", is("CS9001")));
    }
}
