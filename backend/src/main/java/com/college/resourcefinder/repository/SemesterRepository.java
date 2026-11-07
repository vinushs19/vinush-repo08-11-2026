package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findBySemesterNumber(Integer semesterNumber);

    boolean existsBySemesterNumber(Integer semesterNumber);

    List<Semester> findByIsActiveTrueOrderBySemesterNumberAsc();

    List<Semester> findAllByOrderBySemesterNumberAsc();
}
