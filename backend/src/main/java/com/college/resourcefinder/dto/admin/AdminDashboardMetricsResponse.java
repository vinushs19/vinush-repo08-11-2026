package com.college.resourcefinder.dto.admin;

/**
 * Data transfer object encapsulating system-wide aggregate metrics for the administrator dashboard.
 * All values are computed strictly from real database queries (no fabricated statistics).
 */
public class AdminDashboardMetricsResponse {

    private long totalUsers;
    private long activeUsers;
    private long studentCount;
    private long facultyCount;
    private long adminCount;

    private long totalResources;
    private long activeResources;
    private long storageUsedBytes;
    private String storageUsedFormatted;

    private long totalSearchQueries;
    private long totalAttendanceRecords;
    private long totalFileOperations;

    public AdminDashboardMetricsResponse() {}

    public AdminDashboardMetricsResponse(long totalUsers, long activeUsers, long studentCount,
                                         long facultyCount, long adminCount, long totalResources,
                                         long activeResources, long storageUsedBytes,
                                         String storageUsedFormatted, long totalSearchQueries,
                                         long totalAttendanceRecords, long totalFileOperations) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.studentCount = studentCount;
        this.facultyCount = facultyCount;
        this.adminCount = adminCount;
        this.totalResources = totalResources;
        this.activeResources = activeResources;
        this.storageUsedBytes = storageUsedBytes;
        this.storageUsedFormatted = storageUsedFormatted;
        this.totalSearchQueries = totalSearchQueries;
        this.totalAttendanceRecords = totalAttendanceRecords;
        this.totalFileOperations = totalFileOperations;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getStudentCount() { return studentCount; }
    public void setStudentCount(long studentCount) { this.studentCount = studentCount; }

    public long getFacultyCount() { return facultyCount; }
    public void setFacultyCount(long facultyCount) { this.facultyCount = facultyCount; }

    public long getAdminCount() { return adminCount; }
    public void setAdminCount(long adminCount) { this.adminCount = adminCount; }

    public long getTotalResources() { return totalResources; }
    public void setTotalResources(long totalResources) { this.totalResources = totalResources; }

    public long getActiveResources() { return activeResources; }
    public void setActiveResources(long activeResources) { this.activeResources = activeResources; }

    public long getStorageUsedBytes() { return storageUsedBytes; }
    public void setStorageUsedBytes(long storageUsedBytes) { this.storageUsedBytes = storageUsedBytes; }

    public String getStorageUsedFormatted() { return storageUsedFormatted; }
    public void setStorageUsedFormatted(String storageUsedFormatted) { this.storageUsedFormatted = storageUsedFormatted; }

    public long getTotalSearchQueries() { return totalSearchQueries; }
    public void setTotalSearchQueries(long totalSearchQueries) { this.totalSearchQueries = totalSearchQueries; }

    public long getTotalAttendanceRecords() { return totalAttendanceRecords; }
    public void setTotalAttendanceRecords(long totalAttendanceRecords) { this.totalAttendanceRecords = totalAttendanceRecords; }

    public long getTotalFileOperations() { return totalFileOperations; }
    public void setTotalFileOperations(long totalFileOperations) { this.totalFileOperations = totalFileOperations; }
}
