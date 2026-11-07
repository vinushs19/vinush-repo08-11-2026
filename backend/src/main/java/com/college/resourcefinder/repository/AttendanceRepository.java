package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    // Custom queries will be added in Module 09 (Attendance Management)
}
