package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.StaffAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long>, JpaSpecificationExecutor<StaffAttendance> {

    boolean existsByStaffIdentifierAndAttendanceDateAndSessionPeriod(
            String staffIdentifier, LocalDate attendanceDate, Integer sessionPeriod);

    Optional<StaffAttendance> findByStaffIdentifierAndAttendanceDateAndSessionPeriod(
            String staffIdentifier, LocalDate attendanceDate, Integer sessionPeriod);

    List<StaffAttendance> findByStaffIdentifier(String staffIdentifier);

    List<StaffAttendance> findByStaffIdentifierAndAttendanceDateBetween(
            String staffIdentifier, LocalDate startDate, LocalDate endDate);

    long countByStaffIdentifier(String staffIdentifier);

    @Query("SELECT COUNT(sa) FROM StaffAttendance sa WHERE sa.staffIdentifier = :staffId " +
           "AND (:startDate IS NULL OR sa.attendanceDate >= :startDate) " +
           "AND (:endDate IS NULL OR sa.attendanceDate <= :endDate)")
    long countApplicableSessions(@Param("staffId") String staffId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(sa) FROM StaffAttendance sa WHERE sa.staffIdentifier = :staffId " +
           "AND sa.status IN ('PRESENT', 'ON_DUTY') " +
           "AND (:startDate IS NULL OR sa.attendanceDate >= :startDate) " +
           "AND (:endDate IS NULL OR sa.attendanceDate <= :endDate)")
    long countAttendedSessions(@Param("staffId") String staffId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(sa) FROM StaffAttendance sa WHERE sa.staffIdentifier = :staffId " +
           "AND sa.status = :status " +
           "AND (:startDate IS NULL OR sa.attendanceDate >= :startDate) " +
           "AND (:endDate IS NULL OR sa.attendanceDate <= :endDate)")
    long countByStatus(@Param("staffId") String staffId,
                       @Param("status") String status,
                       @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);
}
