package com.example.document_management.controller;

import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.DocumentMetadataResponse;
import com.example.document_management.service.DocumentService;
import com.example.document_management.service.DocumentService.DocumentFileView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "5. Document Management", description = "API quản lý và thao tác tài liệu (Upload, Download, Preview, Filter, Delete)")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/projects/{projectId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải tệp lên dự án", description = "Thành viên dự án upload tệp lên MinIO và lưu metadata vào database")
    public ResponseEntity<ApiResponse<DocumentMetadataResponse>> uploadDocument(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        DocumentMetadataResponse response = documentService.uploadDocument(projectId, file, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Tải tệp lên thành công!"));
    }

    @GetMapping("/projects/{projectId}/documents")
    @Operation(summary = "Lấy danh sách tệp của dự án", description = "Hỗ trợ lọc theo loại (vd: image, video, pdf) và tìm kiếm theo tên tệp")
    public ResponseEntity<ApiResponse<List<DocumentMetadataResponse>>> getDocumentsByProject(
            @PathVariable Long projectId,
            @Parameter(description = "Từ khóa tìm kiếm theo tên tệp") @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Lọc theo loại tệp (ví dụ: image, video, pdf, doc)") @RequestParam(name = "type", required = false) String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<DocumentMetadataResponse> responses = documentService.getDocumentsByProject(projectId, search, type, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, responses, "Lấy danh sách tệp thành công!"));
    }

    @GetMapping("/documents/{documentId}")
    @Operation(summary = "Lấy thông tin chi tiết của một tệp", description = "Lấy metadata chi tiết (tên, kích thước, mimetype, ngày tạo)")
    public ResponseEntity<ApiResponse<DocumentMetadataResponse>> getDocumentById(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        DocumentMetadataResponse response = documentService.getDocumentById(documentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy thông tin tệp thành công!"));
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "Tải tệp về máy", description = "Mặc định trả về binary stream trực tiếp. Nếu presigned=true sẽ trả về Pre-signed URL từ MinIO")
    public ResponseEntity<?> downloadDocument(
            @PathVariable Long documentId,
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
    @Operation(summary = "Stream xem trực tiếp tài liệu", description = "Stream file trực tiếp cho PDF viewer, Image preview, Video player")
    public ResponseEntity<Resource> previewDocument(
            @PathVariable Long documentId,
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
    @Operation(summary = "Xóa tệp", description = "Chỉ người upload, chủ dự án hoặc Admin mới có quyền xóa tài liệu")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        documentService.deleteDocument(documentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Xóa tệp thành công!"));
    }
}
