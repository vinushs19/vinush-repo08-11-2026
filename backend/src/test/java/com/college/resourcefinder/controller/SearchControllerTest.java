package com.college.resourcefinder.controller;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SearchControllerTest {

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
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private StorageService storageService;

    private User studentMca;
    private User studentCs;
    private Department deptMca;
    private Department deptCs;
    private Subject subjDbms;
    private Subject subjJava;
    private Subject subjOs;
    private Unit unitDbms3;
    private Category catNotes;
    private Category catQp;
    private Category catAssign;
    private Resource resDbmsUnit3;
    private Resource resJavaSem2Qp;
    private Resource resOsAssignment;

    @BeforeEach
    void setUp() {
        deptMca = departmentRepository.findByDepartmentCode("MCA_SCH")
                .orElseGet(() -> departmentRepository.save(new Department("MCA_SCH", "Master of Computer Applications")));

        deptCs = departmentRepository.findByDepartmentCode("CS_SCH")
                .orElseGet(() -> departmentRepository.save(new Department("CS_SCH", "Computer Science")));

        studentMca = userRepository.findByEmail("student.mca@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Student MCA",
                        "student.mca@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.STUDENT.name(),
                        deptMca
                )));

        studentCs = userRepository.findByEmail("student.cs@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Student CS",
                        "student.cs@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.STUDENT.name(),
                        deptCs
                )));

        subjDbms = subjectRepository.findBySubjectCode("MCADBMS")
                .orElseGet(() -> subjectRepository.save(new Subject(
                        "MCADBMS",
                        "Database Management Systems",
                        1,
                        deptMca,
                        "Core DBMS syllabus"
                )));

        subjJava = subjectRepository.findBySubjectCode("MCAJAVA")
                .orElseGet(() -> subjectRepository.save(new Subject(
                        "MCAJAVA",
                        "Advanced Java Programming",
                        2,
                        deptMca,
                        "Java EE and frameworks"
                )));

        subjOs = subjectRepository.findBySubjectCode("CSOS")
                .orElseGet(() -> subjectRepository.save(new Subject(
                        "CSOS",
                        "Operating Systems",
                        1,
                        deptCs,
                        "Process scheduling and memory"
                )));

        unitDbms3 = unitRepository.findBySubject_SubjectIdAndUnitNumber(subjDbms.getSubjectId(), 3)
                .orElseGet(() -> unitRepository.save(new Unit(
                        subjDbms,
                        3,
                        "Normalization and Relational Calculus",
                        "3NF, BCNF, and queries"
                )));

        catNotes = categoryRepository.findByCategoryName("Lecture Notes")
                .orElseGet(() -> categoryRepository.save(new Category(
                        "Lecture Notes",
                        "Classroom lecture slides and PDF notes"
                )));

        catQp = categoryRepository.findByCategoryName("Previous Question Papers")
                .orElseGet(() -> categoryRepository.save(new Category(
                        "Previous Question Papers",
                        "Previous years university exam papers"
                )));

        catAssign = categoryRepository.findByCategoryName("Assignments")
                .orElseGet(() -> categoryRepository.save(new Category(
                        "Assignments",
                        "Practical laboratory exercises and problem sets"
                )));

        // Create and store resources
        MockMultipartFile f1 = new MockMultipartFile("file", "dbms_u3.pdf", "application/pdf", "%PDF-1.4 DBMS U3".getBytes());
        StorageService.StoredFile s1 = storageService.storeFile(f1, "MCA/SEM1/MCADBMS");

        resDbmsUnit3 = resourceRepository.save(new Resource(
                "DBMS Unit 3 Relational Normalization Notes",
                "Complete explanation of 1NF, 2NF, 3NF and BCNF with examples",
                s1.relativeFilePath(), s1.uniqueFilename(), s1.originalFilename(), s1.fileType(), s1.fileSize(),
                "Lecture notes", subjDbms, unitDbms3, catNotes, studentMca
        ));

        MockMultipartFile f2 = new MockMultipartFile("file", "java_sem2_2025.pdf", "application/pdf", "%PDF-1.4 Java QP".getBytes());
        StorageService.StoredFile s2 = storageService.storeFile(f2, "MCA/SEM2/MCAJAVA");

        resJavaSem2Qp = resourceRepository.save(new Resource(
                "Java Semester 2 Model Question Paper",
                "Final examination model paper with solved solutions",
                s2.relativeFilePath(), s2.uniqueFilename(), s2.originalFilename(), s2.fileType(), s2.fileSize(),
                "Previous examination papers", subjJava, null, catQp, studentMca
        ));

        MockMultipartFile f3 = new MockMultipartFile("file", "os_assign.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00});
        StorageService.StoredFile s3 = storageService.storeFile(f3, "CS/SEM1/CSOS");

        resOsAssignment = resourceRepository.save(new Resource(
                "Operating System Synchronization Assignment",
                "Semaphores and mutex deadlock exercises",
                s3.relativeFilePath(), s3.uniqueFilename(), s3.originalFilename(), s3.fileType(), s3.fileSize(),
                "Assignments", subjOs, null, catAssign, studentCs
        ));
    }

    // =========================================================================
    // AUTHORIZATION
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/search - unauthenticated request is rejected with 401")
    void shouldRejectUnauthenticatedSearch() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("query", "DBMS"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // DETERMINISTIC NATURAL QUERY NORMALIZATION & EXTRACTION
    // =========================================================================

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("Query 1: 'Find DBMS notes for Unit 3' correctly extracts Subject, Unit, and Category")
    void shouldProcessQuery1_DbmsNotesUnit3() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("query", "Find DBMS notes for Unit 3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.detectedCriteria.extractedSubjectToken", is("DBMS")))
                .andExpect(jsonPath("$.data.detectedCriteria.extractedUnitNumber", is(3)))
                .andExpect(jsonPath("$.data.detectedCriteria.extractedCategory", is("Lecture Notes")))
                .andExpect(jsonPath("$.data.results", not(empty())))
                .andExpect(jsonPath("$.data.results[0].title", containsString("DBMS Unit 3")));
    }

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("Query 2: 'Show Java question papers for Semester 2' correctly extracts Subject, Semester, and QP")
    void shouldProcessQuery2_JavaQuestionPapersSem2() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("query", "Show Java question papers for Semester 2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.detectedCriteria.extractedSubjectToken", is("Java")))
                .andExpect(jsonPath("$.data.detectedCriteria.extractedSemester", is(2)))
                .andExpect(jsonPath("$.data.detectedCriteria.extractedResourceType", is("Previous examination papers")))
                .andExpect(jsonPath("$.data.results", not(empty())))
                .andExpect(jsonPath("$.data.results[0].title", containsString("Java Semester 2")));
    }

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("Query 3: 'Find operating system assignments' correctly extracts Subject and Assignment category")
    void shouldProcessQuery3_OsAssignments() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("query", "Find operating system assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.detectedCriteria.extractedSubjectToken", is("Operating System")))
                .andExpect(jsonPath("$.data.detectedCriteria.extractedResourceType", is("Assignments")))
                .andExpect(jsonPath("$.data.results", not(empty())))
                .andExpect(jsonPath("$.data.results[0].title", containsString("Operating System Synchronization")));
    }

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("Query 4: 'Show study materials for my department' scopes results to user's assigned department")
    void shouldProcessQuery4_MyDepartmentScope() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("query", "Show study materials for my department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.detectedCriteria.departmentScopeApplied", is(true)))
                // Student MCA should only see MCA department resources
                .andExpect(jsonPath("$.data.results[*].departmentCode", everyItem(is("MCA_SCH"))));
    }

    // =========================================================================
    // MULTI-FACET FILTERING & PAGINATION
    // =========================================================================

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("Search with explicit facet filters: department, semester, and fileType")
    void shouldFilterWithExplicitFacets() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("departmentId", deptMca.getDepartmentId().toString())
                        .param("semester", "1")
                        .param("fileType", "pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", not(empty())))
                .andExpect(jsonPath("$.data.results[0].departmentCode", is("MCA_SCH")))
                .andExpect(jsonPath("$.data.results[0].semester", is(1)))
                .andExpect(jsonPath("$.data.results[0].fileType", is("pdf")));
    }

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("Zero results search returns 200 OK with empty results list")
    void shouldHandleZeroResultsGracefully() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("query", "NonExistentSubjectXYZ99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalResults", is(0)))
                .andExpect(jsonPath("$.data.results", empty()));
    }

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("Deactivated resources are strictly excluded from search results")
    void shouldExcludeDeactivatedResourcesFromSearch() throws Exception {
        resDbmsUnit3.setIsActive(false);
        resDbmsUnit3.setStatus("DELETED");
        resourceRepository.save(resDbmsUnit3);

        mockMvc.perform(get("/api/v1/search")
                        .param("query", "DBMS Unit 3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalResults", is(0)));
    }

    // =========================================================================
    // SEARCH HISTORY & PRIVACY TESTS
    // =========================================================================

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("Search query is automatically logged to user's private search history")
    void shouldLogSearchQueryToHistory() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("query", "Automated History Test Query"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/search/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].searchQuery", is("Automated History Test Query")));
    }

    @Test
    @WithMockUser(username = "student.cs@college.edu", roles = {"STUDENT"})
    @DisplayName("Search history is strictly private: User B cannot see User A's search history")
    void shouldEnforceHistoryPrivacyBetweenUsers() throws Exception {
        // Log query for Student MCA
        SearchHistory sh = new SearchHistory();
        sh.setUser(studentMca);
        sh.setSearchQuery("MCA Private Search");
        sh.setResultCount(5);
        searchHistoryRepository.save(sh);

        // Student CS queries their history; should not see MCA's query
        mockMvc.perform(get("/api/v1/search/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].searchQuery", not(hasItem("MCA Private Search"))));
    }

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("DELETE /api/v1/search/history/{id} - user can delete their own history entry")
    void shouldAllowUserToDeleteOwnHistoryEntry() throws Exception {
        SearchHistory sh = new SearchHistory();
        sh.setUser(studentMca);
        sh.setSearchQuery("Query to be deleted");
        sh.setResultCount(1);
        SearchHistory saved = searchHistoryRepository.save(sh);

        mockMvc.perform(delete("/api/v1/search/history/" + saved.getSearchId())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertEquals(0, searchHistoryRepository.findBySearchIdAndUser_UserId(saved.getSearchId(), studentMca.getUserId()).stream().count());
    }

    @Test
    @WithMockUser(username = "student.mca@college.edu", roles = {"STUDENT"})
    @DisplayName("DELETE /api/v1/search/history - user can clear all their search history")
    void shouldAllowUserToClearAllHistory() throws Exception {
        SearchHistory sh1 = new SearchHistory();
        sh1.setUser(studentMca);
        sh1.setSearchQuery("History 1");
        searchHistoryRepository.save(sh1);

        SearchHistory sh2 = new SearchHistory();
        sh2.setUser(studentMca);
        sh2.setSearchQuery("History 2");
        searchHistoryRepository.save(sh2);

        mockMvc.perform(delete("/api/v1/search/history")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertEquals(0, searchHistoryRepository.countByUser_UserId(studentMca.getUserId()));
    }
}
