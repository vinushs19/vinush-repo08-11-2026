package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long>, JpaSpecificationExecutor<Subject> {

    Optional<Subject> findBySubjectCode(String subjectCode);

    boolean existsBySubjectCode(String subjectCode);

    boolean existsBySubjectCodeAndSubjectIdNot(String subjectCode, Long subjectId);

    List<Subject> findByDepartment_DepartmentId(Long departmentId);

    List<Subject> findByDepartment_DepartmentIdAndSemester(Long departmentId, Integer semester);

    List<Subject> findByDepartment_DepartmentIdAndSemesterAndIsActiveTrue(Long departmentId, Integer semester);

    Page<Subject> findBySubjectNameContainingIgnoreCaseOrSubjectCodeContainingIgnoreCase(
            String name, String code, Pageable pageable);
}
