package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    long countByAction(String action);
}
