package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    Optional<Resource> findByResourceIdAndIsActiveTrue(Long resourceId);

    List<Resource> findBySubject_SubjectIdAndIsActiveTrue(Long subjectId);

    List<Resource> findBySubject_SubjectIdAndUnit_UnitIdAndIsActiveTrue(Long subjectId, Long unitId);

    List<Resource> findByUploadedBy_UserIdAndIsActiveTrue(Long userId);

    long countByIsActiveTrue();

    long countBySubject_Department_DepartmentId(Long departmentId);

    long countBySubject_SubjectId(Long subjectId);

    long countByCategory_CategoryId(Long categoryId);

    @Query("SELECT COALESCE(SUM(r.fileSize), 0) FROM Resource r WHERE r.isActive = true")
    Long sumActiveFileSizeBytes();

    @Query("SELECT r.subject.department.departmentCode, r.subject.department.departmentName, COUNT(r), COALESCE(SUM(r.fileSize), 0) " +
           "FROM Resource r WHERE r.isActive = true GROUP BY r.subject.department.departmentCode, r.subject.department.departmentName ORDER BY COUNT(r) DESC")
    List<Object[]> findResourceDistributionByDepartment();

    @Query("SELECT r.subject.semester, COUNT(r) " +
           "FROM Resource r WHERE r.isActive = true GROUP BY r.subject.semester ORDER BY r.subject.semester ASC")
    List<Object[]> findResourceDistributionBySemester();

    @Query("SELECT r.category.categoryName, COUNT(r) " +
           "FROM Resource r WHERE r.isActive = true GROUP BY r.category.categoryName ORDER BY COUNT(r) DESC")
    List<Object[]> findResourceDistributionByCategory();
}
