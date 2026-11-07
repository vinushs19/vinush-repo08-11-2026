package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRegisterNumber(String registerNumber);

    boolean existsByRegisterNumber(String registerNumber);
}
