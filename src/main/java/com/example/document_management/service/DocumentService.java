package com.example.document_management.service;

import com.example.document_management.dto.response.*;
import com.example.document_management.dto.response.ProjectDocumentsResponse.DocumentSummaryDto;
import com.example.document_management.entity.DocumentMetadata;
import com.example.document_management.entity.Project;
import com.example.document_management.entity.ProjectActivity;
import com.example.document_management.entity.User;
import com.example.document_management.enums.ProjectMemberRoleEnum;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.DocumentRepository;
import com.example.document_management.repository.ProjectActivityRepository;
import com.example.document_management.repository.ProjectMemberRepository;
import com.example.document_management.repository.ProjectRepository;
import com.example.document_management.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectActivityRepository projectActivityRepository;
    private final RestClient restClient;

    private static final String[] AUTHOR_COLORS = {
            "#4F46E5", "#0284C7", "#9333EA", "#D97706", "#DB2777", "#059669", "#DC2626", "#2563EB"
    };

    @Getter
    @AllArgsConstructor
    public static class DocumentFileView {
        private final DocumentMetadata metadata;
        private final InputStream inputStream;
    }

    @Getter
    @AllArgsConstructor
    public static class ProjectDocumentsDataAndMeta {
        private final ProjectDocumentsResponse data;
        private final Map<String, Object> meta;
    }

    /**
     * Lấy danh sách tài liệu phục vụ màn hình Project Documents Explorer
     */
    @Transactional(readOnly = true)
    public ProjectDocumentsDataAndMeta getDocumentsExplorer(
            Long projectId, String search, String category, int page, int limit,
            String sortBy, String sortOrder, String email) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        User user = getAuthenticatedUser(email);
        checkProjectAccess(projectId, user);

        // 1. Tính toán summary toàn bộ dự án
        List<DocumentMetadata> allDocs = documentRepository.findByProjectId(projectId);
        long totalFiles = allDocs.size();
        long totalSizeBytes = 0L;
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", totalFiles);
        counts.put("docs", 0L);
        counts.put("sheets", 0L);
        counts.put("media", 0L);
        counts.put("images", 0L);
        counts.put("code", 0L);

        for (DocumentMetadata doc : allDocs) {
            if (doc.getFileSize() != null) {
                totalSizeBytes += doc.getFileSize();
            }
            String cat = doc.getCategory();
            if (cat == null || cat.isBlank()) {
                cat = inferCategory(doc.getFileName(), doc.getContentType());
            }
            cat = cat.toLowerCase();
            if (counts.containsKey(cat)) {
                counts.put(cat, counts.get(cat) + 1);
            } else {
                counts.put("docs", counts.get("docs") + 1);
            }
        }

        DocumentSummaryDto summary = DocumentSummaryDto.builder()
                .totalFiles(totalFiles)
                .totalSize(formatBytes(totalSizeBytes))
                .counts(counts)
                .build();

        // 2. Lọc và phân trang theo search, category, sort
        Specification<DocumentMetadata> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("fileName")), searchPattern));
            }

            if (category != null && !category.trim().isEmpty() && !category.trim().equalsIgnoreCase("all")) {
                String catParam = category.trim().toLowerCase();
                predicates.add(cb.equal(cb.lower(root.get("category")), catParam));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        String sortField = "createdAt";
        if ("name".equalsIgnoreCase(sortBy)) {
            sortField = "fileName";
        } else if ("size".equalsIgnoreCase(sortBy)) {
            sortField = "fileSize";
        } else if ("updatedAt".equalsIgnoreCase(sortBy)) {
            sortField = "updatedAt";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, Math.min(limit, 100));
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by(direction, sortField));

        Page<DocumentMetadata> pagedDocs = documentRepository.findAll(spec, pageRequest);

        // 3. Map danh sách tệp
        List<ProjectDocumentItemResponse> fileItems = new ArrayList<>();
        Map<Long, User> userCache = new HashMap<>();

        for (DocumentMetadata doc : pagedDocs.getContent()) {
            User author = null;
            if (doc.getUploaderId() != null) {
                author = userCache.computeIfAbsent(doc.getUploaderId(), id -> userRepository.findById(id).orElse(null));
            }

            String authorName = (author != null && author.getFullName() != null && !author.getFullName().isBlank())
                    ? author.getFullName()
                    : (author != null ? author.getEmail() : "Thành viên");
            String initials = extractInitials(authorName);
            int colorIdx = author != null && author.getId() != null
                    ? (int) (Math.abs(author.getId()) % AUTHOR_COLORS.length)
                    : 0;
            String authorColor = AUTHOR_COLORS[colorIdx];

            String ext = extractExtension(doc.getFileName());
            String cat = doc.getCategory() != null && !doc.getCategory().isBlank()
                    ? doc.getCategory().toLowerCase()
                    : inferCategory(doc.getFileName(), doc.getContentType());

            fileItems.add(ProjectDocumentItemResponse.builder()
                    .id(doc.getId())
                    .name(doc.getFileName())
                    .type(ext)
                    .format(ext.toUpperCase())
                    .size(formatBytes(doc.getFileSize() != null ? doc.getFileSize() : 0L))
                    .sizeBytes(doc.getFileSize() != null ? doc.getFileSize() : 0L)
                    .category(cat)
                    .author(authorName)
                    .authorInitials(initials)
                    .authorColor(authorColor)
                    .updatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt() : doc.getCreatedAt())
                    .createdAt(doc.getCreatedAt())
                    .minioKey(doc.getS3Key())
                    .build());
        }

        ProjectDocumentsResponse data = ProjectDocumentsResponse.builder()
                .summary(summary)
                .files(fileItems)
                .build();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", page);
        meta.put("limit", pageSize);
        meta.put("totalItems", pagedDocs.getTotalElements());
        meta.put("totalPages", pagedDocs.getTotalPages());

        return new ProjectDocumentsDataAndMeta(data, meta);
    }

    /**
     * Tải lên tài liệu mới vào dự án (hỗ trợ category, validate size và format của
     * dự án)
     */
    @Transactional
    public DocumentUploadResponse uploadDocumentExplorer(Long projectId, MultipartFile file, String category,
            String email) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp để tải lên!");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        User user = getAuthenticatedUser(email);
        checkProjectAccess(projectId, user);

        // Validate maxFileSize của dự án nếu có
        if (project.getMaxFileSize() != null && !project.getMaxFileSize().isBlank()) {
            long maxAllowedBytes = parseSizeLimit(project.getMaxFileSize());
            if (file.getSize() > maxAllowedBytes) {
                throw new IllegalArgumentException("Dung lượng tệp (" + formatBytes(file.getSize())
                        + ") vượt quá giới hạn tối đa của dự án (" + project.getMaxFileSize() + ")!");
            }
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed_file";
        }
        originalFilename = Paths.get(originalFilename).getFileName().toString();
        String ext = extractExtension(originalFilename);

        // Validate allowedFormats của dự án nếu có
        if (project.getAllowedFormats() != null && !project.getAllowedFormats().isBlank()) {
            String allowed = project.getAllowedFormats().toLowerCase();
            if (!allowed.contains(ext.toLowerCase())) {
                throw new IllegalArgumentException(
                        "Định dạng tệp '." + ext + "' không được hỗ trợ trong dự án này (chỉ cho phép: "
                                + project.getAllowedFormats() + ")!");
            }
        }

        // Xác định category
        String resolvedCategory = (category != null && !category.isBlank())
                ? category.trim().toLowerCase()
                : inferCategory(originalFilename, file.getContentType());

        String s3Key = "projects/" + projectId + "/" + UUID.randomUUID() + "_" + originalFilename;
        storageService.uploadFile(file, s3Key);

        DocumentMetadata metadata = DocumentMetadata.builder()
                .fileName(originalFilename)
                .s3Key(s3Key)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .projectId(projectId)
                .uploaderId(user.getId())
                .category(resolvedCategory)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        DocumentMetadata saved = documentRepository.save(metadata);

        // Lưu hoạt động
        try {
            projectActivityRepository.save(ProjectActivity.builder()
                    .project(project)
                    .user(user)
                    .userAction(
                            (user.getFullName() != null ? user.getFullName() : "Người dùng") + " đã tải lên tệp mới")
                    .target(originalFilename)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể lưu hoạt động tải lên tài liệu: {}", e.getMessage());
        }

        // GỬI LÊN PYTHON SERVICE

        Map<String, Object> request = new HashMap<>();

        request.put("project_id", projectId.toString());
        request.put("object_name", s3Key);
        request.put("bucket_name", "document-management");

        Map<String, Object> response = restClient.post()
                .uri("/api/v1/documents/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);

        if (!response.get("status").equals("success")) {
            throw new RuntimeException("Lỗi khi gửi yêu cầu sang python service");
        }

        return DocumentUploadResponse.builder()
                .id(saved.getId())
                .name(saved.getFileName())
                .size(formatBytes(saved.getFileSize()))
                .format(ext.toUpperCase())
                .minioUrl(storageService.getDirectFileUrl(s3Key))
                .build();
    }

    /**
     * Tạo liên kết xem trước tài liệu trực tiếp từ MinIO (Presigned URL)
     */
    @Transactional(readOnly = true)
    public DocumentPreviewUrlResponse getPreviewUrl(Long projectId, Long documentId, String email) {
        DocumentMetadata doc = getDocumentAndValidateProject(projectId, documentId, email);

        int expiresInSeconds = 3600;
        String previewUrl = storageService.getPreSignedUrl(
                doc.getS3Key(),
                expiresInSeconds,
                TimeUnit.SECONDS,
                Map.of("response-content-disposition", "inline"));

        return DocumentPreviewUrlResponse.builder()
                .documentId(doc.getId())
                .previewUrl(previewUrl)
                .mimeType(doc.getContentType() != null ? doc.getContentType() : "application/octet-stream")
                .expiresIn(expiresInSeconds)
                .build();
    }

    /**
     * Tạo liên kết tải xuống tệp kèm disposition attachment (Presigned URL)
     */
    @Transactional(readOnly = true)
    public DocumentDownloadUrlResponse getDownloadUrl(Long projectId, Long documentId, String email) {
        DocumentMetadata doc = getDocumentAndValidateProject(projectId, documentId, email);

        int expiresInSeconds = 600;
        String downloadUrl = storageService.getPreSignedUrl(
                doc.getS3Key(),
                expiresInSeconds,
                TimeUnit.SECONDS,
                Map.of("response-content-disposition", "attachment; filename=\"" + doc.getFileName() + "\""));

        return DocumentDownloadUrlResponse.builder()
                .downloadUrl(downloadUrl)
                .fileName(doc.getFileName())
                .expiresIn(expiresInSeconds)
                .build();
    }

    /**
     * Xóa đơn tệp tin trong dự án
     */
    @Transactional
    public void deleteDocumentInProject(Long projectId, Long documentId, String email) {
        DocumentMetadata doc = getDocumentAndValidateProject(projectId, documentId, email);
        User user = getAuthenticatedUser(email);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án"));

        boolean isAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;
        boolean isUploader = user.getId().equals(doc.getUploaderId());
        boolean isOwner = (project.getOwner() != null && project.getOwner().getId().equals(user.getId()))
                || projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
                        .map(m -> m.getRole() == ProjectMemberRoleEnum.ROLE_OWNER)
                        .orElse(false);

        if (!isAdmin && !isOwner && !isUploader) {
            throw new AccessDeniedException("Chỉ người upload, chủ dự án hoặc Admin mới có quyền xóa tài liệu!");
        }

        storageService.deleteFile(doc.getS3Key());
        documentRepository.delete(doc);
        deleteVectorEmbeddingsFromPython(projectId, doc.getS3Key(), doc.getFileName());

        try {
            projectActivityRepository.save(ProjectActivity.builder()
                    .project(project)
                    .user(user)
                    .userAction((user.getFullName() != null ? user.getFullName() : "Người dùng") + " đã xóa tệp tin")
                    .target(doc.getFileName())
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể lưu hoạt động xóa tài liệu: {}", e.getMessage());
        }
    }

    /**
     * Xóa hàng loạt tệp tin trong dự án
     */
    @Transactional
    public BulkDeleteResponse bulkDeleteDocuments(Long projectId, List<Long> documentIds, String email) {
        if (documentIds == null || documentIds.isEmpty()) {
            return BulkDeleteResponse.builder().deletedCount(0).build();
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        User user = getAuthenticatedUser(email);
        checkProjectAccess(projectId, user);

        boolean isAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;
        boolean isOwner = (project.getOwner() != null && project.getOwner().getId().equals(user.getId()))
                || projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
                        .map(m -> m.getRole() == ProjectMemberRoleEnum.ROLE_OWNER)
                        .orElse(false);

        List<DocumentMetadata> docs = documentRepository.findByIdInAndProjectId(documentIds, projectId);
        int deletedCount = 0;

        for (DocumentMetadata doc : docs) {
            boolean isUploader = user.getId().equals(doc.getUploaderId());
            if (isAdmin || isOwner || isUploader) {
                try {
                    storageService.deleteFile(doc.getS3Key());
                    documentRepository.delete(doc);
                    deleteVectorEmbeddingsFromPython(projectId, doc.getS3Key(), doc.getFileName());
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Lỗi khi xóa tệp ID {}: {}", doc.getId(), e.getMessage());
                }
            }
        }

        if (deletedCount > 0) {
            try {
                projectActivityRepository.save(ProjectActivity.builder()
                        .project(project)
                        .user(user)
                        .userAction((user.getFullName() != null ? user.getFullName() : "Người dùng")
                                + " đã xóa " + deletedCount + " tệp tin")
                        .target(deletedCount + " tệp tài liệu")
                        .createdAt(Instant.now())
                        .build());
            } catch (Exception e) {
                log.warn("Không thể lưu hoạt động xóa tệp hàng loạt: {}", e.getMessage());
            }
        }

        return BulkDeleteResponse.builder().deletedCount(deletedCount).build();
    }

    // --- Legacy methods retained for backward compatibility ---

    @Transactional
    public DocumentMetadataResponse uploadDocument(Long projectId, MultipartFile file, String email) {
        DocumentUploadResponse explorerResponse = uploadDocumentExplorer(projectId, file, null, email);
        return DocumentMetadataResponse.builder()
                .id(explorerResponse.getId())
                .fileName(explorerResponse.getName())
                .projectId(projectId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public Page<DocumentMetadataResponse> getDocumentsByProject(Long projectId, String search, String type,
            String email, Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId);
        }

        User user = getAuthenticatedUser(email);
        checkProjectAccess(projectId, user);

        Page<DocumentMetadata> docs;
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasType = type != null && !type.isBlank();

        if (hasSearch && hasType) {
            docs = documentRepository.findByProjectIdAndFileNameContainingIgnoreCaseAndContentTypeContainingIgnoreCase(
                    projectId, search, type, pageable);
        } else if (hasSearch) {
            docs = documentRepository.findByProjectIdAndFileNameContainingIgnoreCase(projectId, search, pageable);
        } else if (hasType) {
            docs = documentRepository.findByProjectIdAndContentTypeContainingIgnoreCase(projectId, type, pageable);
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
        deleteDocumentInProject(doc.getProjectId(), documentId, email);
    }

    private DocumentMetadata getDocumentAndValidateProject(Long projectId, Long documentId, String email) {
        DocumentMetadata doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu với ID: " + documentId));

        if (!doc.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("Tài liệu không thuộc về dự án có ID: " + projectId);
        }

        User user = getAuthenticatedUser(email);
        checkProjectAccess(projectId, user);
        return doc;
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

    public String inferCategory(String fileName, String contentType) {
        String ext = extractExtension(fileName);
        String mime = contentType != null ? contentType.toLowerCase() : "";

        if (ext.equals("pdf") || mime.contains("pdf")) {
            return "docs";
        }
        if (ext.equals("docx") || ext.equals("doc") || ext.equals("txt") || ext.equals("rtf")
                || mime.contains("word") || mime.contains("document") || mime.contains("text/plain")) {
            return "docs";
        }
        if (ext.equals("xlsx") || ext.equals("xls") || ext.equals("csv")
                || mime.contains("sheet") || mime.contains("excel") || mime.contains("csv")) {
            return "sheets";
        }
        if (ext.equals("mp4") || ext.equals("mov") || ext.equals("avi") || ext.equals("mkv") || ext.equals("webm")
                || ext.equals("mp3") || ext.equals("wav") || mime.startsWith("video/") || mime.startsWith("audio/")) {
            return "media";
        }
        if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif")
                || ext.equals("webp") || ext.equals("svg") || mime.startsWith("image/")) {
            return "images";
        }
        if (ext.equals("js") || ext.equals("jsx") || ext.equals("ts") || ext.equals("tsx")
                || ext.equals("py") || ext.equals("java") || ext.equals("html") || ext.equals("css")
                || ext.equals("json") || ext.equals("yaml") || ext.equals("yml") || ext.equals("sql")
                || ext.equals("sh") || ext.equals("cpp") || ext.equals("c")) {
            return "code";
        }
        return "docs";
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "file";
        }
        int lastDot = fileName.lastIndexOf('.');
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    private String extractInitials(String fullName) {
        if (fullName == null || fullName.isBlank())
            return "U";
        String[] parts = fullName.trim().split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "U" : last.substring(0, 1).toUpperCase();
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0)
            return "0 B";
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private long parseSizeLimit(String sizeStr) {
        if (sizeStr == null || sizeStr.isBlank())
            return 100L * 1024 * 1024;
        String cleaned = sizeStr.trim().toUpperCase();
        try {
            if (cleaned.endsWith("GB")) {
                double val = Double.parseDouble(cleaned.replace("GB", "").trim());
                return (long) (val * 1024 * 1024 * 1024);
            } else if (cleaned.endsWith("MB")) {
                double val = Double.parseDouble(cleaned.replace("MB", "").trim());
                return (long) (val * 1024 * 1024);
            } else if (cleaned.endsWith("KB")) {
                double val = Double.parseDouble(cleaned.replace("KB", "").trim());
                return (long) (val * 1024);
            } else {
                return Long.parseLong(cleaned);
            }
        } catch (Exception e) {
            return 100L * 1024 * 1024;
        }
    }

    private void deleteVectorEmbeddingsFromPython(Long projectId, String s3Key, String fileName) {
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("project_id", projectId.toString());
            req.put("object_name", s3Key);

            restClient.method(HttpMethod.DELETE)
                    .uri("/api/v1/documents/delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Đã đồng bộ xóa vector embedding trên Python service cho tệp: {}", fileName);
        } catch (Exception e) {
            log.warn("Không thể xóa vector embedding trên Python service cho tệp {}: {}", fileName, e.getMessage());
        }
    }
}
