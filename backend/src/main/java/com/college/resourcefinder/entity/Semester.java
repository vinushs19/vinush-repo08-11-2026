package com.college.resourcefinder.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing academic semesters (1 to 8) and terms (ODD/EVEN).
 * Mapped to the {@code semesters} table.
 */
@Entity
@Table(name = "semesters")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "semester_number", nullable = false, unique = true)
    private Integer semesterNumber;

    @Column(name = "semester_name", nullable = false, length = 100)
    private String semesterName;

    @Column(name = "term", nullable = false, length = 20)
    private String term = "ODD";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    // ---- Constructors ----

    public Semester() {}

    public Semester(Integer semesterNumber, String semesterName, String term) {
        this.semesterNumber = semesterNumber;
        this.semesterName = semesterName;
        this.term = term;
        this.isActive = true;
    }

    // ---- Getters and Setters ----

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public Integer getSemesterNumber() { return semesterNumber; }
    public void setSemesterNumber(Integer semesterNumber) { this.semesterNumber = semesterNumber; }

    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ---- equals / hashCode ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Semester semester = (Semester) o;
        return Objects.equals(semesterId, semester.semesterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semesterId);
    }

    @Override
    public String toString() {
        return "Semester{id=" + semesterId + ", number=" + semesterNumber + ", term='" + term + "'}";
    }
}
