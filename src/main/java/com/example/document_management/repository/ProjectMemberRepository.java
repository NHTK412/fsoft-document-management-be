package com.example.document_management.repository;

import com.example.document_management.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    Optional<ProjectMember> findByProjectIdAndUserEmail(Long projectId, String email);
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
    boolean existsByProjectIdAndUserEmail(Long projectId, String email);
    long countByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}
