package com.example.document_management.repository;

import com.example.document_management.entity.ProjectActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectActivityRepository extends JpaRepository<ProjectActivity, Long> {
    List<ProjectActivity> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);
    void deleteByProjectId(Long projectId);
}
