package com.example.document_management.repository;

import com.example.document_management.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentMetadata, Long> {
    List<DocumentMetadata> findByProjectId(Long projectId);
    List<DocumentMetadata> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<DocumentMetadata> findByProjectIdAndFileNameContainingIgnoreCaseOrderByCreatedAtDesc(Long projectId, String fileName);
    List<DocumentMetadata> findByProjectIdAndContentTypeContainingIgnoreCaseOrderByCreatedAtDesc(Long projectId, String contentType);
    List<DocumentMetadata> findByProjectIdAndFileNameContainingIgnoreCaseAndContentTypeContainingIgnoreCaseOrderByCreatedAtDesc(Long projectId, String fileName, String contentType);

    long countByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}
