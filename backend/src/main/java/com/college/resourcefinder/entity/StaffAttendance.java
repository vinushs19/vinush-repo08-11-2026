package com.college.resourcefinder.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing a daily or period-level attendance record for a staff/faculty member.
 * Mapped to the {@code staff_attendance} table.
 *
 * <p>Composite uniqueness is enforced at the database level:
 * {@code (staff_identifier, attendance_date, session_period)}.</p>
 *
 * <p>Each record maintains its origin via {@code data_source}
 * (e.g. {@code MANUAL_ENTRY}, {@code CSV_IMPORT}, {@code BIOMETRIC_SYNC}).</p>
 */
@Entity
@Table(name = "staff_attendance", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_staff_attendance_session",
        columnNames = {"staff_identifier", "attendance_date", "session_period"}
    )
})
public class StaffAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_attendance_id")
    private Long staffAttendanceId;

    @Column(name = "staff_identifier", nullable = false, length = 50)
    private String staffIdentifier;

    @Column(name = "staff_name", nullable = false, length = 150)
    private String staffName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "session_period", nullable = false)
    private Integer sessionPeriod = 1;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PRESENT";

    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource = "MANUAL_ENTRY";

    @Column(name = "remarks", length = 255)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PRESENT";
        }
        if (this.dataSource == null) {
            this.dataSource = "MANUAL_ENTRY";
        }
        if (this.sessionPeriod == null) {
            this.sessionPeriod = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- Constructors ----

    public StaffAttendance() {}

    public StaffAttendance(String staffIdentifier, String staffName, Department department,
                           User user, LocalDate attendanceDate, Integer sessionPeriod,
                           String status, String dataSource, String remarks, User recordedBy) {
        this.staffIdentifier = staffIdentifier;
        this.staffName = staffName;
        this.department = department;
        this.user = user;
        this.attendanceDate = attendanceDate;
        this.sessionPeriod = sessionPeriod != null ? sessionPeriod : 1;
        this.status = status != null ? status : "PRESENT";
        this.dataSource = dataSource != null ? dataSource : "MANUAL_ENTRY";
        this.remarks = remarks;
        this.recordedBy = recordedBy;
    }

    // ---- Getters and Setters ----

    public Long getStaffAttendanceId() { return staffAttendanceId; }
    public void setStaffAttendanceId(Long staffAttendanceId) { this.staffAttendanceId = staffAttendanceId; }

    public String getStaffIdentifier() { return staffIdentifier; }
    public void setStaffIdentifier(String staffIdentifier) { this.staffIdentifier = staffIdentifier; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public Integer getSessionPeriod() { return sessionPeriod; }
    public void setSessionPeriod(Integer sessionPeriod) { this.sessionPeriod = sessionPeriod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public User getRecordedBy() { return recordedBy; }
    public void setRecordedBy(User recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ---- equals / hashCode ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaffAttendance that = (StaffAttendance) o;
        return Objects.equals(staffAttendanceId, that.staffAttendanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(staffAttendanceId);
    }

    @Override
    public String toString() {
        return "StaffAttendance{id=" + staffAttendanceId + ", staff='" + staffIdentifier +
               "', date=" + attendanceDate + ", period=" + sessionPeriod + ", status='" + status + "'}";
    }
}
