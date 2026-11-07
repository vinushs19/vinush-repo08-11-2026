package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    Optional<Department> findByDepartmentCode(String departmentCode);

    boolean existsByDepartmentCode(String departmentCode);

    boolean existsByDepartmentCodeAndDepartmentIdNot(String departmentCode, Long departmentId);

    List<Department> findByIsActiveTrue();

    Page<Department> findByDepartmentNameContainingIgnoreCaseOrDepartmentCodeContainingIgnoreCase(
            String name, String code, Pageable pageable);

    Page<Department> findByIsActiveTrueAndDepartmentNameContainingIgnoreCaseOrIsActiveTrueAndDepartmentCodeContainingIgnoreCase(
            String name, String code, Pageable pageable);
}
