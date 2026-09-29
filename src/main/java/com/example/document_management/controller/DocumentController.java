package com.example.document_management.controller;

import com.example.document_management.dto.request.BulkDeleteRequest;
import com.example.document_management.dto.response.*;
import com.example.document_management.service.DocumentService;
import com.example.document_management.service.DocumentService.DocumentFileView;
import com.example.document_management.service.DocumentService.ProjectDocumentsDataAndMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "5. Document Management", description = "Quản lý và thao tác tệp tài liệu (Upload MinIO, Download, Preview trực tiếp, Tìm kiếm, Xóa)")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/projects/{projectId}/documents")
    @Operation(summary = "Lấy danh sách tệp tài liệu của dự án", description = "Dành cho thành viên dự án hoặc Admin. Hỗ trợ phân trang, lọc theo danh mục (docs, sheets, media, images, code), tìm kiếm và sắp xếp")
    public ResponseEntity<ApiResponse<ProjectDocumentsResponse>> getProjectDocuments(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "Từ khóa tìm kiếm theo tên tệp") @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Lọc theo danh mục: all | docs | sheets | media | images | code") @RequestParam(name = "category", required = false, defaultValue = "all") String category,
            @Parameter(description = "Trang hiện tại (1-based)") @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @Parameter(description = "Số tệp mỗi trang") @RequestParam(name = "limit", required = false, defaultValue = "20") int limit,
            @Parameter(description = "Trường sắp xếp: name | size | updatedAt") @RequestParam(name = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @Parameter(description = "Thứ tự sắp xếp: asc | desc") @RequestParam(name = "sortOrder", required = false, defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProjectDocumentsDataAndMeta result = documentService.getDocumentsExplorer(
                projectId, search, category, page, limit, sortBy, sortOrder, userDetails.getUsername()
        );

        return ResponseEntity.ok(ApiResponse.success(200, result.getData(), "Lấy danh sách tài liệu thành công!", result.getMeta()));
    }

    @PostMapping(value = "/projects/{projectId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải tệp lên dự án", description = "Dành cho thành viên dự án hoặc Admin. Tải tệp lên MinIO (multipart/form-data), validate kích thước/định dạng và lưu metadata")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
            @Parameter(description = "ID của dự án tải tệp lên") @PathVariable Long projectId,
            @Parameter(description = "Tệp đính kèm cần upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Danh mục tệp: docs, sheets, media, images, code (tùy chọn)") @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal UserDetails userDetails) {

        DocumentUploadResponse response = documentService.uploadDocumentExplorer(projectId, file, category, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response, "Tải lên tệp thành công. Đã đưa vào hàng đợi đánh chỉ mục vector."));
    }

    @GetMapping("/projects/{projectId}/documents/{documentId}/preview-url")
    @Operation(summary = "Lấy URL xem trước tài liệu trực tiếp từ MinIO", description = "Sinh Presigned URL xem trước file trực tiếp trên trình duyệt")
    public ResponseEntity<ApiResponse<DocumentPreviewUrlResponse>> getPreviewUrl(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "ID của tệp tài liệu") @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        DocumentPreviewUrlResponse response = documentService.getPreviewUrl(projectId, documentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy liên kết xem trước thành công!"));
    }

    @GetMapping("/projects/{projectId}/documents/{documentId}/download-url")
    @Operation(summary = "Lấy URL tải xuống tệp tin từ MinIO", description = "Sinh Presigned URL đính kèm content-disposition attachment để tải tệp trực tiếp")
    public ResponseEntity<ApiResponse<DocumentDownloadUrlResponse>> getDownloadUrl(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "ID của tệp tài liệu") @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        DocumentDownloadUrlResponse response = documentService.getDownloadUrl(projectId, documentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy liên kết tải xuống thành công!"));
    }

    @DeleteMapping("/projects/{projectId}/documents/{documentId}")
    @Operation(summary = "Xóa đơn tệp tin trong dự án", description = "Dành cho chủ dự án, Admin hoặc người tải tệp lên. Xóa vĩnh viễn tệp trên MinIO và metadata")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentInProject(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "ID của tệp tài liệu") @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        documentService.deleteDocumentInProject(projectId, documentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Tệp tin và vector liên kết đã được xóa vĩnh viễn"));
    }

    @PostMapping("/projects/{projectId}/documents/bulk-delete")
    @Operation(summary = "Xóa hàng loạt tệp tin trong dự án", description = "Dành cho chủ dự án, Admin hoặc người tải tệp lên. Xóa nhiều tệp cùng lúc trên MinIO và metadata")
    public ResponseEntity<ApiResponse<BulkDeleteResponse>> bulkDeleteDocuments(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Valid @RequestBody BulkDeleteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        BulkDeleteResponse response = documentService.bulkDeleteDocuments(projectId, request.getIds(), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Đã xóa thành công " + response.getDeletedCount() + " tệp tin đã chọn"));
    }

    // --- Legacy endpoints retained for backward compatibility ---

    @GetMapping("/documents/{documentId}")
    @Operation(summary = "Xem thông tin chi tiết một tệp", description = "Dành cho thành viên dự án hoặc Admin. Lấy thông tin metadata chi tiết")
    public ResponseEntity<ApiResponse<DocumentMetadataResponse>> getDocumentById(
            @Parameter(description = "ID của tệp tài liệu") @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        DocumentMetadataResponse response = documentService.getDocumentById(documentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy thông tin tệp thành công!"));
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "Tải tệp về máy (Binary hoặc Presigned)", description = "Mặc định tải file binary stream trực tiếp. Nếu presigned=true sẽ trả về Pre-signed URL")
    public ResponseEntity<?> downloadDocument(
            @Parameter(description = "ID của tệp tài liệu") @PathVariable Long documentId,
            @Parameter(description = "Nếu true, trả về pre-signed URL thay vì tải binary trực tiếp")
            @RequestParam(name = "presigned", defaultValue = "false") boolean presigned,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (presigned) {
            String presignedUrl = documentService.getPresignedUrl(documentId, userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success(200, presignedUrl, "Sinh pre-signed URL tải tệp thành công!"));
        }

        DocumentFileView fileView = documentService.getDocumentFileForDownload(documentId, userDetails.getUsername());

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileView.getMetadata().getContentType() != null && !fileView.getMetadata().getContentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(fileView.getMetadata().getContentType());
            } catch (Exception ignored) {
            }
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileView.getMetadata().getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .contentLength(fileView.getMetadata().getFileSize() != null ? fileView.getMetadata().getFileSize() : -1)
                .body(new InputStreamResource(fileView.getInputStream()));
    }

    @GetMapping("/documents/{documentId}/preview")
    @Operation(summary = "Stream xem trước tài liệu trực tiếp", description = "Stream tệp trực tiếp trong trình duyệt")
    public ResponseEntity<Resource> previewDocument(
            @Parameter(description = "ID của tệp tài liệu") @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        DocumentFileView fileView = documentService.getDocumentFileForDownload(documentId, userDetails.getUsername());

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileView.getMetadata().getContentType() != null && !fileView.getMetadata().getContentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(fileView.getMetadata().getContentType());
            } catch (Exception ignored) {
            }
        }

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(fileView.getMetadata().getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .contentLength(fileView.getMetadata().getFileSize() != null ? fileView.getMetadata().getFileSize() : -1)
                .body(new InputStreamResource(fileView.getInputStream()));
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Xóa tệp (Legacy path)", description = "Xóa tệp theo documentId")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @Parameter(description = "ID của tệp tài liệu cần xóa") @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        documentService.deleteDocument(documentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Xóa tệp thành công!"));
    }
}
