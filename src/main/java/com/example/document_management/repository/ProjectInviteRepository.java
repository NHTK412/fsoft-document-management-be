package com.example.document_management.repository;

import com.example.document_management.entity.ProjectInvite;
import com.example.document_management.enums.ProjectInviteStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, Long> {
    List<ProjectInvite> findByProjectIdAndStatus(Long projectId, ProjectInviteStatusEnum status);
    Optional<ProjectInvite> findByProjectIdAndEmail(Long projectId, String email);
    Optional<ProjectInvite> findByToken(String token);
    void deleteByProjectId(Long projectId);
}
