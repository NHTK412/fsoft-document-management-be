package com.example.document_management.repository;

import com.example.document_management.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentMetadata, Long> {
    List<DocumentMetadata> findByProjectId(Long projectId);
    long countByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}
