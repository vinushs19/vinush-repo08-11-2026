package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.ChatInteraction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatInteractionRepository extends JpaRepository<ChatInteraction, Long> {

    List<ChatInteraction> findByUser_UserIdOrderByTimestampDesc(Long userId);

    Page<ChatInteraction> findByUser_UserId(Long userId, Pageable pageable);

    long countByUser_UserId(Long userId);
}
