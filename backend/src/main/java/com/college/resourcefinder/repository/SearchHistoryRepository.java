package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findByUser_UserIdOrderBySearchDateDesc(Long userId, Pageable pageable);

    Page<SearchHistory> findByUser_UserId(Long userId, Pageable pageable);

    Optional<SearchHistory> findBySearchIdAndUser_UserId(Long searchId, Long userId);

    void deleteByUser_UserId(Long userId);

    long countByUser_UserId(Long userId);

    @Query("SELECT s.searchQuery, COUNT(s) FROM SearchHistory s GROUP BY s.searchQuery ORDER BY COUNT(s) DESC")
    List<Object[]> findTopSearchQueries(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT LOWER(s.searchQuery)) FROM SearchHistory s")
    long countDistinctQueries();
}
