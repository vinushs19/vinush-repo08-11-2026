package com.college.resourcefinder.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity for tracking user search queries.
 * Mapped to the {@code search_history} table.
 */
@Entity
@Table(name = "search_history")
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_id")
    private Long searchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "search_query", nullable = false, length = 500)
    private String searchQuery;

    @Column(name = "result_count", nullable = false)
    private Integer resultCount = 0;

    @Column(name = "search_date", nullable = false, updatable = false)
    private LocalDateTime searchDate;

    @PrePersist
    protected void onCreate() {
        this.searchDate = LocalDateTime.now();
    }

    // ---- Constructors ----

    public SearchHistory() {}

    // ---- Getters and Setters ----

    public Long getSearchId() { return searchId; }
    public void setSearchId(Long searchId) { this.searchId = searchId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }

    public LocalDateTime getSearchDate() { return searchDate; }
    public void setSearchDate(LocalDateTime searchDate) { this.searchDate = searchDate; }

    // ---- equals / hashCode ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchHistory that = (SearchHistory) o;
        return Objects.equals(searchId, that.searchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchId);
    }

    @Override
    public String toString() {
        return "SearchHistory{id=" + searchId + ", query='" + searchQuery + "'}";
    }
}
