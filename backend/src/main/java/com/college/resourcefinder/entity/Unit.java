package com.college.resourcefinder.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing a course curriculum syllabus unit (1 to 10) within a subject.
 * Mapped to the {@code units} table.
 */
@Entity
@Table(name = "units", uniqueConstraints = {
    @UniqueConstraint(name = "uk_units_subject_number", columnNames = {"subject_id", "unit_number"})
})
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id")
    private Long unitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "unit_number", nullable = false)
    private Integer unitNumber;

    @Column(name = "unit_title", nullable = false, length = 200)
    private String unitTitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- Constructors ----

    public Unit() {}

    public Unit(Subject subject, Integer unitNumber, String unitTitle, String description) {
        this.subject = subject;
        this.unitNumber = unitNumber;
        this.unitTitle = unitTitle;
        this.description = description;
        this.isActive = true;
    }

    // ---- Getters and Setters ----

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Integer getUnitNumber() { return unitNumber; }
    public void setUnitNumber(Integer unitNumber) { this.unitNumber = unitNumber; }

    public String getUnitTitle() { return unitTitle; }
    public void setUnitTitle(String unitTitle) { this.unitTitle = unitTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ---- equals / hashCode ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Unit unit = (Unit) o;
        return Objects.equals(unitId, unit.unitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unitId);
    }

    @Override
    public String toString() {
        return "Unit{id=" + unitId + ", number=" + unitNumber + ", title='" + unitTitle + "'}";
    }
}
