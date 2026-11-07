package com.college.resourcefinder.service;

import com.college.resourcefinder.dto.admin.*;
import com.college.resourcefinder.entity.AuditLog;
import com.college.resourcefinder.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service providing factual administrative reports and metrics.
 * Strictly calculates data from actual database queries (no fabricated or simulated statistics).
 */
@Service
public class AdminReportService {

    private static final Logger log = LoggerFactory.getLogger(AdminReportService.class);

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final StaffAttendanceRepository staffAttendanceRepository;
    private final FileOperationRepository fileOperationRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminReportService(UserRepository userRepository,
                              ResourceRepository resourceRepository,
                              SearchHistoryRepository searchHistoryRepository,
                              StaffAttendanceRepository staffAttendanceRepository,
                              FileOperationRepository fileOperationRepository,
                              AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.staffAttendanceRepository = staffAttendanceRepository;
        this.fileOperationRepository = fileOperationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Aggregates key system metrics for the administrative overview.
     */
    @Transactional(readOnly = true)
    public AdminDashboardMetricsResponse getDashboardMetrics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActive(true);
        long studentCount = userRepository.countByRole("STUDENT");
        long facultyCount = userRepository.countByRole("FACULTY");
        long adminCount = userRepository.countByRole("ADMIN");

        long totalResources = resourceRepository.count();
        long activeResources = resourceRepository.countByIsActiveTrue();
        Long storageBytes = resourceRepository.sumActiveFileSizeBytes();
        long storageUsedBytes = storageBytes != null ? storageBytes : 0L;
        String storageUsedFormatted = formatBytes(storageUsedBytes);

        long totalSearchQueries = searchHistoryRepository.count();
        long totalAttendanceRecords = staffAttendanceRepository.count();
        long totalFileOperations = fileOperationRepository.count();

        return new AdminDashboardMetricsResponse(
                totalUsers, activeUsers, studentCount, facultyCount, adminCount,
                totalResources, activeResources, storageUsedBytes, storageUsedFormatted,
                totalSearchQueries, totalAttendanceRecords, totalFileOperations
        );
    }

    /**
     * Reports resource distribution and storage consumption grouped by academic department.
     */
    @Transactional(readOnly = true)
    public List<DepartmentResourceDistributionDto> getDepartmentDistribution() {
        List<Object[]> rawList = resourceRepository.findResourceDistributionByDepartment();
        List<DepartmentResourceDistributionDto> dtos = new ArrayList<>();

        for (Object[] row : rawList) {
            String deptCode = (String) row[0];
            String deptName = (String) row[1];
            long count = row[2] instanceof Number ? ((Number) row[2]).longValue() : 0L;
            long storage = row[3] instanceof Number ? ((Number) row[3]).longValue() : 0L;

            dtos.add(new DepartmentResourceDistributionDto(
                    deptCode, deptName, count, storage, formatBytes(storage)
            ));
        }
        return dtos;
    }

    /**
     * Reports resource distribution grouped by semester.
     */
    @Transactional(readOnly = true)
    public List<SemesterResourceDistributionDto> getSemesterDistribution() {
        List<Object[]> rawList = resourceRepository.findResourceDistributionBySemester();
        List<SemesterResourceDistributionDto> dtos = new ArrayList<>();

        for (Object[] row : rawList) {
            Integer semester = (Integer) row[0];
            long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            dtos.add(new SemesterResourceDistributionDto(semester, count));
        }
        return dtos;
    }

    /**
     * Reports resource distribution grouped by category.
     */
    @Transactional(readOnly = true)
    public List<CategoryResourceDistributionDto> getCategoryDistribution() {
        List<Object[]> rawList = resourceRepository.findResourceDistributionByCategory();
        List<CategoryResourceDistributionDto> dtos = new ArrayList<>();

        for (Object[] row : rawList) {
            String categoryName = (String) row[0];
            long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            dtos.add(new CategoryResourceDistributionDto(categoryName, count));
        }
        return dtos;
    }

    /**
     * Reports search activity summaries derived strictly from actual search history records.
     */
    @Transactional(readOnly = true)
    public SearchActivitySummaryDto getSearchActivitySummary(int limit) {
        long totalSearches = searchHistoryRepository.count();
        long uniqueQueries = searchHistoryRepository.countDistinctQueries();

        int topLimit = Math.max(1, Math.min(limit, 50));
        List<Object[]> rawTop = searchHistoryRepository.findTopSearchQueries(PageRequest.of(0, topLimit));
        List<QueryFrequencyDto> topQueries = new ArrayList<>();

        for (Object[] row : rawTop) {
            String query = (String) row[0];
            long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            topQueries.add(new QueryFrequencyDto(query, count));
        }

        return new SearchActivitySummaryDto(totalSearches, uniqueQueries, topQueries);
    }

    /**
     * Retrieves paginated and filtered audit logs for administrative security audits.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(String action,
                                               String status,
                                               LocalDateTime fromDate,
                                               LocalDateTime toDate,
                                               Pageable pageable) {
        Specification<AuditLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null && !action.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("action")),
                        "%" + action.trim().toLowerCase() + "%"
                ));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("status")),
                        status.trim().toUpperCase()
                ));
            }

            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), fromDate));
            }

            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), toDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::fromEntity);
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
