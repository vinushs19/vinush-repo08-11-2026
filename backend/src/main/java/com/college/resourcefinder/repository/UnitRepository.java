package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findBySubject_SubjectIdOrderByUnitNumberAsc(Long subjectId);

    List<Unit> findBySubject_SubjectIdAndIsActiveTrueOrderByUnitNumberAsc(Long subjectId);

    Optional<Unit> findBySubject_SubjectIdAndUnitNumber(Long subjectId, Integer unitNumber);

    boolean existsBySubject_SubjectIdAndUnitNumber(Long subjectId, Integer unitNumber);

    boolean existsBySubject_SubjectIdAndUnitNumberAndUnitIdNot(Long subjectId, Integer unitNumber, Long unitId);
}
