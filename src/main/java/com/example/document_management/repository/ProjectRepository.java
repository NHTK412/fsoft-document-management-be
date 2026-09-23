package com.example.document_management.repository;

import com.example.document_management.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerId(Long ownerId);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.owner " +
            "LEFT JOIN FETCH p.projectMembers pm " +
            "LEFT JOIN FETCH pm.user " +
            "WHERE p.id IN (SELECT m.project.id FROM ProjectMember m WHERE m.user.email = :email)")
    List<Project> findAllByMemberEmail(@Param("email") String email);

}
