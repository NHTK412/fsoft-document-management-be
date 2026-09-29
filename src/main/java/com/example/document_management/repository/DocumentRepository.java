package com.example.document_management.repository;

import com.example.document_management.entity.DocumentMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentMetadata, Long>, JpaSpecificationExecutor<DocumentMetadata> {
    List<DocumentMetadata> findByProjectId(Long projectId);
    List<DocumentMetadata> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    Page<DocumentMetadata> findByProjectId(Long projectId, Pageable pageable);
    Page<DocumentMetadata> findByProjectIdAndFileNameContainingIgnoreCase(Long projectId, String fileName, Pageable pageable);
    Page<DocumentMetadata> findByProjectIdAndContentTypeContainingIgnoreCase(Long projectId, String contentType, Pageable pageable);
    Page<DocumentMetadata> findByProjectIdAndFileNameContainingIgnoreCaseAndContentTypeContainingIgnoreCase(Long projectId, String fileName, String contentType, Pageable pageable);

    long countByProjectId(Long projectId);
    long countByProjectIdAndUploaderId(Long projectId, Long uploaderId);

    List<DocumentMetadata> findByIdInAndProjectId(List<Long> ids, Long projectId);

    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM DocumentMetadata d WHERE d.projectId = :projectId")
    Long sumFileSizeByProjectId(@Param("projectId") Long projectId);

    void deleteByProjectId(Long projectId);
}
