package com.example.document_management.repository;

import com.example.document_management.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    Optional<ProjectMember> findByProjectIdAndUserEmail(Long projectId, String email);
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
    boolean existsByProjectIdAndUserEmail(Long projectId, String email);
    long countByProjectId(Long projectId);
    List<ProjectMember> findByProjectId(Long projectId);

    @org.springframework.data.jpa.repository.Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId " +
            "AND (:search IS NULL OR LOWER(pm.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(pm.user.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<ProjectMember> searchMembers(@org.springframework.data.repository.query.Param("projectId") Long projectId, @org.springframework.data.repository.query.Param("search") String search);

    void deleteByProjectId(Long projectId);
}
