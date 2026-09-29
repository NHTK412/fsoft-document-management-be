package com.example.document_management.repository;

import com.example.document_management.entity.DocumentMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentMetadata, Long> {
    List<DocumentMetadata> findByProjectId(Long projectId);
    List<DocumentMetadata> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    Page<DocumentMetadata> findByProjectId(Long projectId, Pageable pageable);
    Page<DocumentMetadata> findByProjectIdAndFileNameContainingIgnoreCase(Long projectId, String fileName, Pageable pageable);
    Page<DocumentMetadata> findByProjectIdAndContentTypeContainingIgnoreCase(Long projectId, String contentType, Pageable pageable);
    Page<DocumentMetadata> findByProjectIdAndFileNameContainingIgnoreCaseAndContentTypeContainingIgnoreCase(Long projectId, String fileName, String contentType, Pageable pageable);

    long countByProjectId(Long projectId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM DocumentMetadata d WHERE d.projectId = :projectId")
    Long sumFileSizeByProjectId(@org.springframework.data.repository.query.Param("projectId") Long projectId);

    void deleteByProjectId(Long projectId);
}
