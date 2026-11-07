package com.college.resourcefinder.service;

import com.college.resourcefinder.dto.master.*;
import com.college.resourcefinder.entity.*;
import com.college.resourcefinder.exception.ResourceNotFoundException;
import com.college.resourcefinder.repository.*;
import com.college.resourcefinder.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing academic master data: Departments, Semesters, Subjects, Units, and Categories.
 * Provides searchable, paginated queries, unique constraint enforcement, and soft-deactivation.
 */
@Service
public class MasterDataService {

    private static final Logger log = LoggerFactory.getLogger(MasterDataService.class);

    private final DepartmentRepository departmentRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthService authService;

    public MasterDataService(DepartmentRepository departmentRepository,
                             SemesterRepository semesterRepository,
                             SubjectRepository subjectRepository,
                             UnitRepository unitRepository,
                             CategoryRepository categoryRepository,
                             AuditLogRepository auditLogRepository,
                             AuthService authService) {
        this.departmentRepository = departmentRepository;
        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.unitRepository = unitRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.authService = authService;
    }

    // =========================================================================
    // 1. DEPARTMENTS
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getDepartments(String search, Boolean activeOnly, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            String term = search.trim();
            if (Boolean.TRUE.equals(activeOnly)) {
                return departmentRepository
                        .findByIsActiveTrueAndDepartmentNameContainingIgnoreCaseOrIsActiveTrueAndDepartmentCodeContainingIgnoreCase(
                                term, term, pageable)
                        .map(DepartmentResponse::fromEntity);
            }
            return departmentRepository
                    .findByDepartmentNameContainingIgnoreCaseOrDepartmentCodeContainingIgnoreCase(term, term, pageable)
                    .map(DepartmentResponse::fromEntity);
        }

        if (Boolean.TRUE.equals(activeOnly)) {
            return departmentRepository.findAll((root, query, cb) -> cb.isTrue(root.get("isActive")), pageable)
                    .map(DepartmentResponse::fromEntity);
        }

        return departmentRepository.findAll(pageable).map(DepartmentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getActiveDepartmentsList() {
        return departmentRepository.findByIsActiveTrue().stream()
                .map(DepartmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        return DepartmentResponse.fromEntity(dept);
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request, HttpServletRequest httpRequest) {
        String code = request.getDepartmentCode().trim().toUpperCase();
        if (departmentRepository.existsByDepartmentCode(code)) {
            throw new IllegalArgumentException("A department with code '" + code + "' already exists.");
        }

        Department department = new Department(code, request.getDepartmentName().trim());
        if (request.getIsActive() != null) {
            department.setIsActive(request.getIsActive());
        }

        Department saved = departmentRepository.save(department);
        logAudit("CREATE_DEPARTMENT", "Created department " + saved.getDepartmentCode(), httpRequest);
        return DepartmentResponse.fromEntity(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request, HttpServletRequest httpRequest) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));

        String code = request.getDepartmentCode().trim().toUpperCase();
        if (departmentRepository.existsByDepartmentCodeAndDepartmentIdNot(code, id)) {
            throw new IllegalArgumentException("Another department with code '" + code + "' already exists.");
        }

        dept.setDepartmentCode(code);
        dept.setDepartmentName(request.getDepartmentName().trim());
        if (request.getIsActive() != null) {
            dept.setIsActive(request.getIsActive());
        }

        Department updated = departmentRepository.save(dept);
        logAudit("UPDATE_DEPARTMENT", "Updated department " + updated.getDepartmentCode(), httpRequest);
        return DepartmentResponse.fromEntity(updated);
    }

    @Transactional
    public DepartmentResponse updateDepartmentStatus(Long id, boolean isActive, HttpServletRequest httpRequest) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));

        dept.setIsActive(isActive);
        Department saved = departmentRepository.save(dept);
        logAudit("STATUS_DEPARTMENT", "Changed department " + saved.getDepartmentCode() + " status to " + isActive, httpRequest);
        return DepartmentResponse.fromEntity(saved);
    }

    // =========================================================================
    // 2. SEMESTERS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<SemesterResponse> getSemesters(Boolean activeOnly) {
        List<Semester> list = Boolean.TRUE.equals(activeOnly)
                ? semesterRepository.findByIsActiveTrueOrderBySemesterNumberAsc()
                : semesterRepository.findAllByOrderBySemesterNumberAsc();
        return list.stream().map(SemesterResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public SemesterResponse getSemesterById(Long id) {
        Semester sem = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", id));
        return SemesterResponse.fromEntity(sem);
    }

    @Transactional
    public SemesterResponse createSemester(SemesterRequest request, HttpServletRequest httpRequest) {
        if (semesterRepository.existsBySemesterNumber(request.getSemesterNumber())) {
            throw new IllegalArgumentException("Semester number " + request.getSemesterNumber() + " already exists.");
        }

        Semester semester = new Semester(
                request.getSemesterNumber(),
                request.getSemesterName().trim(),
                request.getTerm().trim().toUpperCase()
        );
        if (request.getIsActive() != null) {
            semester.setIsActive(request.getIsActive());
        }

        Semester saved = semesterRepository.save(semester);
        logAudit("CREATE_SEMESTER", "Created semester " + saved.getSemesterNumber(), httpRequest);
        return SemesterResponse.fromEntity(saved);
    }

    @Transactional
    public SemesterResponse updateSemester(Long id, SemesterRequest request, HttpServletRequest httpRequest) {
        Semester sem = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", id));

        sem.setSemesterName(request.getSemesterName().trim());
        sem.setTerm(request.getTerm().trim().toUpperCase());
        if (request.getIsActive() != null) {
            sem.setIsActive(request.getIsActive());
        }

        Semester saved = semesterRepository.save(sem);
        logAudit("UPDATE_SEMESTER", "Updated semester " + saved.getSemesterNumber(), httpRequest);
        return SemesterResponse.fromEntity(saved);
    }

    @Transactional
    public SemesterResponse updateSemesterStatus(Long id, boolean isActive, HttpServletRequest httpRequest) {
        Semester sem = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", id));

        sem.setIsActive(isActive);
        Semester saved = semesterRepository.save(sem);
        logAudit("STATUS_SEMESTER", "Changed semester " + saved.getSemesterNumber() + " status to " + isActive, httpRequest);
        return SemesterResponse.fromEntity(saved);
    }

    // =========================================================================
    // 3. SUBJECTS
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<SubjectResponse> getSubjects(Long departmentId, Integer semester, String search, Boolean activeOnly, Pageable pageable) {
        Specification<Subject> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (departmentId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("department").get("departmentId"), departmentId));
            }
            if (semester != null) {
                predicates = cb.and(predicates, cb.equal(root.get("semester"), semester));
            }
            if (Boolean.TRUE.equals(activeOnly)) {
                predicates = cb.and(predicates, cb.isTrue(root.get("isActive")));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                var searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("subjectName")), pattern),
                        cb.like(cb.lower(root.get("subjectCode")), pattern)
                );
                predicates = cb.and(predicates, searchPredicate);
            }
            return predicates;
        };

        return subjectRepository.findAll(spec, pageable).map(s -> SubjectResponse.fromEntity(s, false));
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long id, boolean includeUnits) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
        return SubjectResponse.fromEntity(subject, includeUnits);
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request, HttpServletRequest httpRequest) {
        String code = request.getSubjectCode().trim().toUpperCase();
        if (subjectRepository.existsBySubjectCode(code)) {
            throw new IllegalArgumentException("A subject with code '" + code + "' already exists.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", request.getDepartmentId()));

        Subject subject = new Subject(
                code,
                request.getSubjectName().trim(),
                request.getSemester(),
                department,
                request.getDescription()
        );
        if (request.getIsActive() != null) {
            subject.setIsActive(request.getIsActive());
        }

        Subject saved = subjectRepository.save(subject);
        logAudit("CREATE_SUBJECT", "Created subject " + saved.getSubjectCode(), httpRequest);
        return SubjectResponse.fromEntity(saved, false);
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request, HttpServletRequest httpRequest) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));

        String code = request.getSubjectCode().trim().toUpperCase();
        if (subjectRepository.existsBySubjectCodeAndSubjectIdNot(code, id)) {
            throw new IllegalArgumentException("Another subject with code '" + code + "' already exists.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", request.getDepartmentId()));

        subject.setSubjectCode(code);
        subject.setSubjectName(request.getSubjectName().trim());
        subject.setSemester(request.getSemester());
        subject.setDepartment(department);
        subject.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            subject.setIsActive(request.getIsActive());
        }

        Subject updated = subjectRepository.save(subject);
        logAudit("UPDATE_SUBJECT", "Updated subject " + updated.getSubjectCode(), httpRequest);
        return SubjectResponse.fromEntity(updated, false);
    }

    @Transactional
    public SubjectResponse updateSubjectStatus(Long id, boolean isActive, HttpServletRequest httpRequest) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));

        subject.setIsActive(isActive);
        Subject saved = subjectRepository.save(subject);
        logAudit("STATUS_SUBJECT", "Changed subject " + saved.getSubjectCode() + " status to " + isActive, httpRequest);
        return SubjectResponse.fromEntity(saved, false);
    }

    // =========================================================================
    // 4. UNITS (Course Syllabus Units 1 to 10)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<UnitResponse> getUnitsForSubject(Long subjectId, Boolean activeOnly) {
        // Verify subject exists
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject", subjectId);
        }

        List<Unit> units = Boolean.TRUE.equals(activeOnly)
                ? unitRepository.findBySubject_SubjectIdAndIsActiveTrueOrderByUnitNumberAsc(subjectId)
                : unitRepository.findBySubject_SubjectIdOrderByUnitNumberAsc(subjectId);

        return units.stream().map(UnitResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public UnitResponse getUnitById(Long unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", unitId));
        return UnitResponse.fromEntity(unit);
    }

    @Transactional
    public UnitResponse createUnit(Long subjectId, UnitRequest request, HttpServletRequest httpRequest) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));

        if (unitRepository.existsBySubject_SubjectIdAndUnitNumber(subjectId, request.getUnitNumber())) {
            throw new IllegalArgumentException("Unit " + request.getUnitNumber() + " already exists for subject " + subject.getSubjectCode());
        }

        Unit unit = new Unit(
                subject,
                request.getUnitNumber(),
                request.getUnitTitle().trim(),
                request.getDescription()
        );
        if (request.getIsActive() != null) {
            unit.setIsActive(request.getIsActive());
        }

        Unit saved = unitRepository.save(unit);
        logAudit("CREATE_UNIT", "Created Unit " + saved.getUnitNumber() + " for subject " + subject.getSubjectCode(), httpRequest);
        return UnitResponse.fromEntity(saved);
    }

    @Transactional
    public UnitResponse updateUnit(Long unitId, UnitRequest request, HttpServletRequest httpRequest) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", unitId));

        Long subjectId = unit.getSubject().getSubjectId();
        if (unitRepository.existsBySubject_SubjectIdAndUnitNumberAndUnitIdNot(subjectId, request.getUnitNumber(), unitId)) {
            throw new IllegalArgumentException("Unit " + request.getUnitNumber() + " already exists for this subject.");
        }

        unit.setUnitNumber(request.getUnitNumber());
        unit.setUnitTitle(request.getUnitTitle().trim());
        unit.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            unit.setIsActive(request.getIsActive());
        }

        Unit updated = unitRepository.save(unit);
        logAudit("UPDATE_UNIT", "Updated Unit " + updated.getUnitNumber() + " for subject " + unit.getSubject().getSubjectCode(), httpRequest);
        return UnitResponse.fromEntity(updated);
    }

    @Transactional
    public UnitResponse updateUnitStatus(Long unitId, boolean isActive, HttpServletRequest httpRequest) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", unitId));

        unit.setIsActive(isActive);
        Unit saved = unitRepository.save(unit);
        logAudit("STATUS_UNIT", "Changed Unit " + saved.getUnitNumber() + " status to " + isActive, httpRequest);
        return UnitResponse.fromEntity(saved);
    }

    // =========================================================================
    // 5. CATEGORIES
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategories(String search, Boolean activeOnly, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return categoryRepository.findByCategoryNameContainingIgnoreCase(search.trim(), pageable)
                    .map(CategoryResponse::fromEntity);
        }

        if (Boolean.TRUE.equals(activeOnly)) {
            return categoryRepository.findAll((root, query, cb) -> cb.isTrue(root.get("isActive")), pageable)
                    .map(CategoryResponse::fromEntity);
        }

        return categoryRepository.findAll(pageable).map(CategoryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategoriesList() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return CategoryResponse.fromEntity(cat);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, HttpServletRequest httpRequest) {
        String name = request.getCategoryName().trim();
        if (categoryRepository.existsByCategoryName(name)) {
            throw new IllegalArgumentException("A category with name '" + name + "' already exists.");
        }

        Category category = new Category(name, request.getDescription());
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        Category saved = categoryRepository.save(category);
        logAudit("CREATE_CATEGORY", "Created category " + saved.getCategoryName(), httpRequest);
        return CategoryResponse.fromEntity(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, HttpServletRequest httpRequest) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        String name = request.getCategoryName().trim();
        if (categoryRepository.existsByCategoryNameAndCategoryIdNot(name, id)) {
            throw new IllegalArgumentException("Another category with name '" + name + "' already exists.");
        }

        category.setCategoryName(name);
        category.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        Category updated = categoryRepository.save(category);
        logAudit("UPDATE_CATEGORY", "Updated category " + updated.getCategoryName(), httpRequest);
        return CategoryResponse.fromEntity(updated);
    }

    @Transactional
    public CategoryResponse updateCategoryStatus(Long id, boolean isActive, HttpServletRequest httpRequest) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        category.setIsActive(isActive);
        Category saved = categoryRepository.save(category);
        logAudit("STATUS_CATEGORY", "Changed category " + saved.getCategoryName() + " status to " + isActive, httpRequest);
        return CategoryResponse.fromEntity(saved);
    }

    // =========================================================================
    // AUDIT LOGGING HELPER
    // =========================================================================

    private void logAudit(String action, String details, HttpServletRequest request) {
        try {
            User user = null;
            try {
                user = authService.getCurrentAuthenticatedUser();
            } catch (Exception ignored) {}

            AuditLog logEntry = new AuditLog();
            logEntry.setUser(user);
            logEntry.setAction(action);
            logEntry.setStatus("SUCCESS");
            logEntry.setResourceType("MASTER_DATA");
            logEntry.setResourceIdentifier(details);
            logEntry.setIpAddress(request != null ? SecurityUtils.getClientIp(request) : "LOCAL");
            logEntry.setDetails(details);
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to record audit log for master data action: {}", ex.getMessage());
        }
    }
}
