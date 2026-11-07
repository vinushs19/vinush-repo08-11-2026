package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.master.*;
import com.college.resourcefinder.dto.response.ApiResponse;
import com.college.resourcefinder.service.MasterDataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Academic Master Data REST API controller.
 * Base path: {@code /api/v1/master}
 *
 * <p>Read endpoints are accessible to authenticated students, faculty, and administrators.
 * Write and status modification endpoints are strictly restricted to administrators.</p>
 */
@RestController
@RequestMapping("/api/v1/master")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    // =========================================================================
    // DEPARTMENTS
    // =========================================================================

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<Page<DepartmentResponse>>> listDepartments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 20, sort = "departmentName", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<DepartmentResponse> departments = masterDataService.getDepartments(search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(departments));
    }

    @GetMapping("/departments/active")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> listActiveDepartments() {
        List<DepartmentResponse> departments = masterDataService.getActiveDepartmentsList();
        return ResponseEntity.ok(ApiResponse.success(departments));
    }

    @GetMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartment(@PathVariable Long id) {
        DepartmentResponse dept = masterDataService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success(dept));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentRequest request,
            HttpServletRequest httpRequest) {
        DepartmentResponse created = masterDataService.createDepartment(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Department created successfully.", created));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request,
            HttpServletRequest httpRequest) {
        DepartmentResponse updated = masterDataService.updateDepartment(id, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Department updated successfully.", updated));
    }

    @PatchMapping("/departments/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        DepartmentResponse updated = masterDataService.updateDepartmentStatus(id, request.getIsActive(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Department status updated successfully.", updated));
    }

    // =========================================================================
    // SEMESTERS
    // =========================================================================

    @GetMapping("/semesters")
    public ResponseEntity<ApiResponse<List<SemesterResponse>>> listSemesters(
            @RequestParam(required = false) Boolean activeOnly) {
        List<SemesterResponse> semesters = masterDataService.getSemesters(activeOnly);
        return ResponseEntity.ok(ApiResponse.success(semesters));
    }

    @GetMapping("/semesters/{id}")
    public ResponseEntity<ApiResponse<SemesterResponse>> getSemester(@PathVariable Long id) {
        SemesterResponse sem = masterDataService.getSemesterById(id);
        return ResponseEntity.ok(ApiResponse.success(sem));
    }

    @PostMapping("/semesters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> createSemester(
            @Valid @RequestBody SemesterRequest request,
            HttpServletRequest httpRequest) {
        SemesterResponse created = masterDataService.createSemester(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Semester created successfully.", created));
    }

    @PutMapping("/semesters/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> updateSemester(
            @PathVariable Long id,
            @Valid @RequestBody SemesterRequest request,
            HttpServletRequest httpRequest) {
        SemesterResponse updated = masterDataService.updateSemester(id, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Semester updated successfully.", updated));
    }

    @PatchMapping("/semesters/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> updateSemesterStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        SemesterResponse updated = masterDataService.updateSemesterStatus(id, request.getIsActive(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Semester status updated successfully.", updated));
    }

    // =========================================================================
    // SUBJECTS
    // =========================================================================

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<Page<SubjectResponse>>> listSubjects(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 20, sort = "subjectName", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<SubjectResponse> subjects = masterDataService.getSubjects(departmentId, semester, search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> getSubject(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeUnits) {
        SubjectResponse subject = masterDataService.getSubjectById(id, includeUnits);
        return ResponseEntity.ok(ApiResponse.success(subject));
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            @Valid @RequestBody SubjectRequest request,
            HttpServletRequest httpRequest) {
        SubjectResponse created = masterDataService.createSubject(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subject created successfully.", created));
    }

    @PutMapping("/subjects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request,
            HttpServletRequest httpRequest) {
        SubjectResponse updated = masterDataService.updateSubject(id, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Subject updated successfully.", updated));
    }

    @PatchMapping("/subjects/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubjectStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        SubjectResponse updated = masterDataService.updateSubjectStatus(id, request.getIsActive(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Subject status updated successfully.", updated));
    }

    // =========================================================================
    // UNITS
    // =========================================================================

    @GetMapping("/subjects/{subjectId}/units")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> listUnits(
            @PathVariable Long subjectId,
            @RequestParam(required = false) Boolean activeOnly) {
        List<UnitResponse> units = masterDataService.getUnitsForSubject(subjectId, activeOnly);
        return ResponseEntity.ok(ApiResponse.success(units));
    }

    @GetMapping("/units/{unitId}")
    public ResponseEntity<ApiResponse<UnitResponse>> getUnit(@PathVariable Long unitId) {
        UnitResponse unit = masterDataService.getUnitById(unitId);
        return ResponseEntity.ok(ApiResponse.success(unit));
    }

    @PostMapping("/subjects/{subjectId}/units")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(
            @PathVariable Long subjectId,
            @Valid @RequestBody UnitRequest request,
            HttpServletRequest httpRequest) {
        UnitResponse created = masterDataService.createUnit(subjectId, request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Unit created successfully.", created));
    }

    @PutMapping("/units/{unitId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnit(
            @PathVariable Long unitId,
            @Valid @RequestBody UnitRequest request,
            HttpServletRequest httpRequest) {
        UnitResponse updated = masterDataService.updateUnit(unitId, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Unit updated successfully.", updated));
    }

    @PatchMapping("/units/{unitId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnitStatus(
            @PathVariable Long unitId,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        UnitResponse updated = masterDataService.updateUnitStatus(unitId, request.getIsActive(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Unit status updated successfully.", updated));
    }

    // =========================================================================
    // CATEGORIES
    // =========================================================================

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> listCategories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 20, sort = "categoryName", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<CategoryResponse> categories = masterDataService.getCategories(search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/categories/active")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listActiveCategories() {
        List<CategoryResponse> categories = masterDataService.getActiveCategoriesList();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable Long id) {
        CategoryResponse category = masterDataService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request,
            HttpServletRequest httpRequest) {
        CategoryResponse created = masterDataService.createCategory(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully.", created));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            HttpServletRequest httpRequest) {
        CategoryResponse updated = masterDataService.updateCategory(id, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully.", updated));
    }

    @PatchMapping("/categories/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        CategoryResponse updated = masterDataService.updateCategoryStatus(id, request.getIsActive(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Category status updated successfully.", updated));
    }
}
