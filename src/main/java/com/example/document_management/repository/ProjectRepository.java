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

    @Query(value = "SELECT p FROM Project p LEFT JOIN FETCH p.owner " +
            "WHERE p.id IN (SELECT m.project.id FROM ProjectMember m WHERE m.user.email = :email)",
           countQuery = "SELECT COUNT(DISTINCT m.project.id) FROM ProjectMember m WHERE m.user.email = :email")
    Page<Project> findAllByMemberEmail(@Param("email") String email, Pageable pageable);
}
