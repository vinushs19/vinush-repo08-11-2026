package com.college.resourcefinder.repository;

import com.college.resourcefinder.entity.FileOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileOperationRepository extends JpaRepository<FileOperation, Long>, JpaSpecificationExecutor<FileOperation> {

    List<FileOperation> findByResource_ResourceId(Long resourceId);

    List<FileOperation> findByUser_UserId(Long userId);

    Page<FileOperation> findByUser_UserId(Long userId, Pageable pageable);

    long countByUser_UserId(Long userId);

    long countByDestinationCategory(String destinationCategory);
}
