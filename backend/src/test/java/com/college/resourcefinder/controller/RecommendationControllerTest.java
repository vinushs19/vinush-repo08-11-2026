package com.college.resourcefinder.controller;

import com.college.resourcefinder.entity.*;
import com.college.resourcefinder.repository.*;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private User student;
    private Department csDept;
    private Department eceDept;
    private Subject dbmsSubject;
    private Subject osSubject;
    private Subject eceSubject;
    private Unit dbmsUnit1;
    private Unit dbmsUnit2;
    private Category notesCategory;
    private Category qpCategory;

    private Resource sourceResource;
    private Resource sameSubjectAndUnitRes;
    private Resource sameSubjectDiffUnitRes;
    private Resource sameDeptDiffSubjectRes;
    private Resource diffDeptRes;
    private Resource inactiveCandidateRes;

    @BeforeEach
    void setUp() {
        student = userRepository.findByEmail("student.rec@college.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "Student Rec",
                        "student.rec@college.edu",
                        passwordEncoder.encode("Password@123"),
                        Role.STUDENT.name(),
                        null
                )));

        csDept = departmentRepository.findByDepartmentCode("CS_REC")
                .orElseGet(() -> departmentRepository.save(new Department("CS_REC", "Computer Science Recommendations")));

        eceDept = departmentRepository.findByDepartmentCode("ECE_REC")
                .orElseGet(() -> departmentRepository.save(new Department("ECE_REC", "Electronics Recommendations")));

        dbmsSubject = subjectRepository.findBySubjectCode("CS_REC_DBMS")
                .orElseGet(() -> subjectRepository.save(new Subject(
                        "CS_REC_DBMS",
                        "Database Management Systems",
                        2,
                        csDept,
                        "Core DBMS syllabus"
                )));

        osSubject = subjectRepository.findBySubjectCode("CS_REC_OS")
                .orElseGet(() -> subjectRepository.save(new Subject(
                        "CS_REC_OS",
                        "Operating Systems",
                        2,
                        csDept,
                        "Core OS syllabus"
                )));

        eceSubject = subjectRepository.findBySubjectCode("ECE_REC_VLSI")
                .orElseGet(() -> subjectRepository.save(new Subject(
                        "ECE_REC_VLSI",
                        "VLSI Design",
                        4,
                        eceDept,
                        "VLSI syllabus"
                )));

        dbmsUnit1 = unitRepository.findBySubject_SubjectIdAndUnitNumber(dbmsSubject.getSubjectId(), 1)
                .orElseGet(() -> unitRepository.save(new Unit(dbmsSubject, 1, "Relational Model", "Intro to Relational Model")));

        dbmsUnit2 = unitRepository.findBySubject_SubjectIdAndUnitNumber(dbmsSubject.getSubjectId(), 2)
                .orElseGet(() -> unitRepository.save(new Unit(dbmsSubject, 2, "SQL and Normalization", "Normal forms")));

        notesCategory = categoryRepository.findByCategoryName("Notes_Rec")
                .orElseGet(() -> categoryRepository.save(new Category("Notes_Rec", "Lecture Notes")));

        qpCategory = categoryRepository.findByCategoryName("QP_Rec")
                .orElseGet(() -> categoryRepository.save(new Category("QP_Rec", "Question Papers")));

        // Source Resource: DBMS Unit 1, Notes_Rec, "Lecture notes", Title: "Relational Algebra Notes"
        sourceResource = resourceRepository.save(new Resource(
                "Relational Algebra Notes",
                "Detailed notes on relational algebra",
                "storage/rec/dbms_u1_source.pdf",
                "dbms_u1_source.pdf",
                "source_relational.pdf",
                "pdf",
                1024L,
                "Lecture notes",
                dbmsSubject,
                dbmsUnit1,
                notesCategory,
                student
        ));

        // Candidate 1: High similarity (Same subject, same unit, same dept, same sem, same cat, same type, title keyword "Algebra")
        sameSubjectAndUnitRes = resourceRepository.save(new Resource(
                "Advanced Relational Algebra Practice",
                "Practice set for algebra",
                "storage/rec/dbms_u1_cand1.pdf",
                "dbms_u1_cand1.pdf",
                "practice_algebra.pdf",
                "pdf",
                2048L,
                "Lecture notes",
                dbmsSubject,
                dbmsUnit1,
                notesCategory,
                student
        ));

        // Candidate 2: Medium similarity (Same subject, diff unit, same dept, same sem, diff category, diff type)
        sameSubjectDiffUnitRes = resourceRepository.save(new Resource(
                "DBMS SQL Question Paper",
                "Exam paper for DBMS",
                "storage/rec/dbms_u2_cand2.pdf",
                "dbms_u2_cand2.pdf",
                "dbms_qp.pdf",
                "pdf",
                4096L,
                "Previous examination papers",
                dbmsSubject,
                dbmsUnit2,
                qpCategory,
                student
        ));

        // Candidate 3: Lower similarity (Diff subject, same dept, same sem, same cat, same type)
        sameDeptDiffSubjectRes = resourceRepository.save(new Resource(
                "Operating System Process Management Notes",
                "OS notes",
                "storage/rec/os_u1_cand3.pdf",
                "os_u1_cand3.pdf",
                "os_notes.pdf",
                "pdf",
                3072L,
                "Lecture notes",
                osSubject,
                null,
                notesCategory,
                student
        ));

        // Candidate 4: Disjoint (Diff dept, diff subject, diff sem, diff cat)
        diffDeptRes = resourceRepository.save(new Resource(
                "VLSI Circuit Design Diagrams",
                "VLSI diagrams",
                "storage/rec/vlSI_cand4.pdf",
                "vlsi_cand4.pdf",
                "vlsi.pdf",
                "pdf",
                5120L,
                "Study materials",
                eceSubject,
                null,
                qpCategory,
                student
        ));

        // Candidate 5: Inactive resource that matches same subject/unit (MUST be excluded)
        inactiveCandidateRes = new Resource(
                "Deactivated Relational Notes",
                "Deactivated note",
                "storage/rec/deact.pdf",
                "deact.pdf",
                "deact.pdf",
                "pdf",
                1000L,
                "Lecture notes",
                dbmsSubject,
                dbmsUnit1,
                notesCategory,
                student
        );
        inactiveCandidateRes.setIsActive(false);
        inactiveCandidateRes.setStatus("DELETED");
        resourceRepository.save(inactiveCandidateRes);
    }

    @Test
    @DisplayName("Unauthenticated request to recommendations returns 401 Unauthorized")
    void getRecommendations_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/resources/" + sourceResource.getResourceId() + "/recommendations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student.rec@college.edu", roles = {"STUDENT"})
    @DisplayName("Recommendations returns 200 with ranked list, excluding source and inactive resources")
    void getRecommendations_validSource_returnsRankedList() throws Exception {
        mockMvc.perform(get("/api/v1/resources/" + sourceResource.getResourceId() + "/recommendations")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))))
                // Verify source resource itself is NOT in recommendations
                .andExpect(jsonPath("$.data[*].resource.resourceId", not(hasItem(sourceResource.getResourceId().intValue()))))
                // Verify inactive resource is NOT in recommendations
                .andExpect(jsonPath("$.data[*].resource.resourceId", not(hasItem(inactiveCandidateRes.getResourceId().intValue()))))
                // Candidate 1 (same subject + same unit + same dept + same sem + same cat + same type + shared keyword) has highest score
                .andExpect(jsonPath("$.data[0].resource.resourceId").value(sameSubjectAndUnitRes.getResourceId().intValue()))
                .andExpect(jsonPath("$.data[0].relevanceScore", greaterThan(50)))
                .andExpect(jsonPath("$.data[0].matchedCriteria", hasItem("Same subject")))
                .andExpect(jsonPath("$.data[0].matchedCriteria", hasItem("Same unit")));
    }

    @Test
    @WithMockUser(username = "student.rec@college.edu", roles = {"STUDENT"})
    @DisplayName("Candidate 1 has higher relevance score than Candidate 2 due to unit and category match")
    void getRecommendations_relevanceOrdering_isDeterministic() throws Exception {
        var result = mockMvc.perform(get("/api/v1/resources/" + sourceResource.getResourceId() + "/recommendations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].resource.resourceId").value(sameSubjectAndUnitRes.getResourceId().intValue()))
                .andExpect(jsonPath("$.data[1].resource.resourceId").value(sameSubjectDiffUnitRes.getResourceId().intValue()))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        int score0 = com.jayway.jsonpath.JsonPath.read(responseJson, "$.data[0].relevanceScore");
        int score1 = com.jayway.jsonpath.JsonPath.read(responseJson, "$.data[1].relevanceScore");
        org.junit.jupiter.api.Assertions.assertTrue(score0 > score1,
                "Candidate 1 score (" + score0 + ") must exceed Candidate 2 score (" + score1 + ")");
    }

    @Test
    @WithMockUser(username = "student.rec@college.edu", roles = {"STUDENT"})
    @DisplayName("Limit parameter properly bounds returned recommendation count")
    void getRecommendations_boundedByLimit() throws Exception {
        mockMvc.perform(get("/api/v1/resources/" + sourceResource.getResourceId() + "/recommendations")
                        .param("limit", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].resource.resourceId").value(sameSubjectAndUnitRes.getResourceId().intValue()));
    }

    @Test
    @WithMockUser(username = "student.rec@college.edu", roles = {"STUDENT"})
    @DisplayName("Non-existent resource returns 404 Not Found")
    void getRecommendations_nonExistentResource_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/resources/999999/recommendations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "student.rec@college.edu", roles = {"STUDENT"})
    @DisplayName("Isolated resource with no matching dimensions returns empty list with 200 OK")
    void getRecommendations_isolatedResource_returnsEmptyList() throws Exception {
        // Create an isolated resource in a standalone department with no peers
        Department isolatedDept = departmentRepository.save(new Department("ISOLATED_DEPT", "Isolated Department"));
        Subject isolatedSubject = subjectRepository.save(new Subject("ISO101", "Isolated Subject", 8, isolatedDept, "None"));
        Category isolatedCategory = categoryRepository.save(new Category("IsolatedCat", "Isolated Category"));

        Resource isolatedResource = resourceRepository.save(new Resource(
                "Completely Unique Quantum Computing Treatise",
                "No other resource shares anything",
                "storage/rec/isolated.pdf",
                "isolated.pdf",
                "isolated.pdf",
                "pdf",
                100L,
                "E-books and reference documents",
                isolatedSubject,
                null,
                isolatedCategory,
                student
        ));

        mockMvc.perform(get("/api/v1/resources/" + isolatedResource.getResourceId() + "/recommendations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
