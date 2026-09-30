package com.example.document_management.repository;

import com.example.document_management.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerId(Long ownerId);

    long countByStatus(String status);

    @Query(value = "SELECT p FROM Project p LEFT JOIN FETCH p.owner " +
            "WHERE p.id IN (SELECT m.project.id FROM ProjectMember m WHERE m.user.email = :email)",
           countQuery = "SELECT COUNT(DISTINCT m.project.id) FROM ProjectMember m WHERE m.user.email = :email")
    Page<Project> findAllByMemberEmail(@Param("email") String email, Pageable pageable);

    @Query(value = "SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.owner " +
            "JOIN p.projectMembers m " +
            "WHERE m.user.email = :email " +
            "AND (CAST(:search AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:role IS NULL OR :role = 'all' OR " +
            "     (:role = 'owner' AND m.role = 'ROLE_OWNER') OR " +
            "     (:role = 'admin' AND m.role = 'ROLE_ADMIN') OR " +
            "     (:role = 'member' AND m.role IN ('ROLE_MEMBER', 'ROLE_VIEWER')))")
    List<Project> searchProjectsByUser(
            @Param("email") String email,
            @Param("search") String search,
            @Param("role") String role,
            @Param("status") String status
    );
}
