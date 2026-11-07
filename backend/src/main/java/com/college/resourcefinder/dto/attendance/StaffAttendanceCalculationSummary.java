package com.college.resourcefinder.dto.attendance;

import java.time.LocalDate;

public class StaffAttendanceCalculationSummary {

    private String staffIdentifier;
    private String staffName;
    private String departmentCode;
    private String departmentName;
    private LocalDate startDate;
    private LocalDate endDate;

    private long totalApplicableSessions;
    private long sessionsAttended;
    private long sessionsPresent;
    private long sessionsOnDuty;
    private long sessionsAbsent;
    private long sessionsOnLeave;

    private double attendancePercentage;
    private String attendancePercentageFormatted;
    private boolean meetsRequirement; // Typically >= 75.0%

    public StaffAttendanceCalculationSummary() {}

    public StaffAttendanceCalculationSummary(String staffIdentifier, String staffName,
                                            String departmentCode, String departmentName,
                                            LocalDate startDate, LocalDate endDate,
                                            long totalApplicableSessions, long sessionsAttended,
                                            long sessionsPresent, long sessionsOnDuty,
                                            long sessionsAbsent, long sessionsOnLeave,
                                            double attendancePercentage) {
        this.staffIdentifier = staffIdentifier;
        this.staffName = staffName;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalApplicableSessions = totalApplicableSessions;
        this.sessionsAttended = sessionsAttended;
        this.sessionsPresent = sessionsPresent;
        this.sessionsOnDuty = sessionsOnDuty;
        this.sessionsAbsent = sessionsAbsent;
        this.sessionsOnLeave = sessionsOnLeave;
        this.attendancePercentage = attendancePercentage;
        this.attendancePercentageFormatted = String.format(java.util.Locale.US, "%.2f%%", attendancePercentage);
        this.meetsRequirement = attendancePercentage >= 75.0;
    }

    // Getters and Setters
    public String getStaffIdentifier() { return staffIdentifier; }
    public void setStaffIdentifier(String staffIdentifier) { this.staffIdentifier = staffIdentifier; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public long getTotalApplicableSessions() { return totalApplicableSessions; }
    public void setTotalApplicableSessions(long totalApplicableSessions) { this.totalApplicableSessions = totalApplicableSessions; }

    public long getSessionsAttended() { return sessionsAttended; }
    public void setSessionsAttended(long sessionsAttended) { this.sessionsAttended = sessionsAttended; }

    public long getSessionsPresent() { return sessionsPresent; }
    public void setSessionsPresent(long sessionsPresent) { this.sessionsPresent = sessionsPresent; }

    public long getSessionsOnDuty() { return sessionsOnDuty; }
    public void setSessionsOnDuty(long sessionsOnDuty) { this.sessionsOnDuty = sessionsOnDuty; }

    public long getSessionsAbsent() { return sessionsAbsent; }
    public void setSessionsAbsent(long sessionsAbsent) { this.sessionsAbsent = sessionsAbsent; }

    public long getSessionsOnLeave() { return sessionsOnLeave; }
    public void setSessionsOnLeave(long sessionsOnLeave) { this.sessionsOnLeave = sessionsOnLeave; }

    public double getAttendancePercentage() { return attendancePercentage; }
    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
        this.attendancePercentageFormatted = String.format(java.util.Locale.US, "%.2f%%", attendancePercentage);
        this.meetsRequirement = attendancePercentage >= 75.0;
    }

    public String getAttendancePercentageFormatted() { return attendancePercentageFormatted; }
    public void setAttendancePercentageFormatted(String attendancePercentageFormatted) { this.attendancePercentageFormatted = attendancePercentageFormatted; }

    public boolean isMeetsRequirement() { return meetsRequirement; }
    public void setMeetsRequirement(boolean meetsRequirement) { this.meetsRequirement = meetsRequirement; }
}
