package com.example.document_management.service;

import com.example.document_management.dto.response.DocumentMetadataResponse;
import com.example.document_management.entity.DocumentMetadata;
import com.example.document_management.entity.Project;
import com.example.document_management.entity.User;
import com.example.document_management.enums.ProjectMemberRoleEnum;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.DocumentRepository;
import com.example.document_management.repository.ProjectMemberRepository;
import com.example.document_management.repository.ProjectRepository;
import com.example.document_management.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    @Getter
    @AllArgsConstructor
    public static class DocumentFileView {
        private final DocumentMetadata metadata;
        private final InputStream inputStream;
    }

    @Transactional
    public DocumentMetadataResponse uploadDocument(Long projectId, MultipartFile file, String email) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp để tải lên!");
        }

        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId);
        }

        User user = getAuthenticatedUser(email);
        checkProjectAccess(projectId, user);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed_file";
        }
        originalFilename = Paths.get(originalFilename).getFileName().toString();

        String s3Key = "projects/" + projectId + "/" + UUID.randomUUID() + "_" + originalFilename;

        storageService.uploadFile(file, s3Key);

        DocumentMetadata metadata = DocumentMetadata.builder()
                .fileName(originalFilename)
                .s3Key(s3Key)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .projectId(projectId)
                .uploaderId(user.getId())
                .createdAt(Instant.now())
                .build();

        DocumentMetadata saved = documentRepository.save(metadata);
        return mapToResponse(saved);
    }

    public org.springframework.data.domain.Page<DocumentMetadataResponse> getDocumentsByProject(Long projectId, String search, String type, String email, org.springframework.data.domain.Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId);
        }

        User user = getAuthenticatedUser(email);
        checkProjectAccess(projectId, user);

        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String typeParam = (type != null && !type.trim().isEmpty()) ? type.trim() : null;

        org.springframework.data.domain.Page<DocumentMetadata> docs;
        if (searchParam != null && typeParam != null) {
            docs = documentRepository.findByProjectIdAndFileNameContainingIgnoreCaseAndContentTypeContainingIgnoreCase(projectId, searchParam, typeParam, pageable);
        } else if (searchParam != null) {
            docs = documentRepository.findByProjectIdAndFileNameContainingIgnoreCase(projectId, searchParam, pageable);
        } else if (typeParam != null) {
            docs = documentRepository.findByProjectIdAndContentTypeContainingIgnoreCase(projectId, typeParam, pageable);
        } else {
            docs = documentRepository.findByProjectId(projectId, pageable);
        }

        return docs.map(this::mapToResponse);
    }

    public DocumentMetadataResponse getDocumentById(Long documentId, String email) {
        DocumentMetadata doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu với id: " + documentId));

        User user = getAuthenticatedUser(email);
        checkProjectAccess(doc.getProjectId(), user);

        return mapToResponse(doc);
    }

    public DocumentFileView getDocumentFileForDownload(Long documentId, String email) {
        DocumentMetadata doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu với id: " + documentId));

        User user = getAuthenticatedUser(email);
        checkProjectAccess(doc.getProjectId(), user);

        InputStream inputStream = storageService.getFile(doc.getS3Key());
        return new DocumentFileView(doc, inputStream);
    }

    public String getPresignedUrl(Long documentId, String email) {
        DocumentMetadata doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu với id: " + documentId));

        User user = getAuthenticatedUser(email);
        checkProjectAccess(doc.getProjectId(), user);

        return storageService.getPreSignedUrl(doc.getS3Key(), 60);
    }

    @Transactional
    public void deleteDocument(Long documentId, String email) {
        DocumentMetadata doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu với id: " + documentId));

        User user = getAuthenticatedUser(email);
        Project project = projectRepository.findById(doc.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án liên kết với tài liệu"));

        boolean isAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;
        boolean isUploader = user.getId().equals(doc.getUploaderId());
        boolean isOwner = (project.getOwner() != null && project.getOwner().getId().equals(user.getId()))
                || projectMemberRepository.findByProjectIdAndUserId(doc.getProjectId(), user.getId())
                .map(m -> m.getRole() == ProjectMemberRoleEnum.ROLE_OWNER)
                .orElse(false);

        if (!isAdmin && !isOwner && !isUploader) {
            throw new AccessDeniedException("Chỉ người upload, chủ dự án hoặc Admin mới có quyền xóa tài liệu!");
        }

        storageService.deleteFile(doc.getS3Key());
        documentRepository.delete(doc);
    }

    private User getAuthenticatedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng"));
    }

    private void checkProjectAccess(Long projectId, User user) {
        if (user.getRole() == UserRoleEnum.ROLE_ADMIN) {
            return;
        }

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserEmail(projectId, user.getEmail());
        if (!isMember) {
            throw new AccessDeniedException("Bạn không có quyền truy cập tài liệu trong dự án này!");
        }
    }

    private DocumentMetadataResponse mapToResponse(DocumentMetadata doc) {
        return DocumentMetadataResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .contentType(doc.getContentType())
                .projectId(doc.getProjectId())
                .uploaderId(doc.getUploaderId())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt() : doc.getCreatedAt())
                .build();
    }
}
