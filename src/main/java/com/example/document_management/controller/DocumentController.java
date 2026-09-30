package com.example.document_management.controller;

import com.example.document_management.dto.request.BulkDeleteRequest;
import com.example.document_management.dto.response.*;
import com.example.document_management.service.DocumentService;
import com.example.document_management.service.DocumentService.ProjectDocumentsDataAndMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Document Management", description = "Quản lý và thao tác tệp tài liệu MinIO (Upload, Download URL, Preview URL, Bulk Delete)")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/projects/{projectId}/documents")
    @Operation(summary = "Lấy danh sách tệp tài liệu của dự án", description = "Dành cho thành viên dự án hoặc Admin. Hỗ trợ phân trang, lọc theo danh mục (docs, sheets, media, images, code), tìm kiếm, lọc theo trạng thái lập chỉ mục AI và sắp xếp")
    public ResponseEntity<ApiResponse<ProjectDocumentsResponse>> getProjectDocuments(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "Từ khóa tìm kiếm theo tên tệp") @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Lọc theo danh mục: all | docs | sheets | media | images | code") @RequestParam(name = "category", required = false, defaultValue = "all") String category,
            @Parameter(description = "Lọc theo tài liệu đã lập chỉ mục AI (RAG)") @RequestParam(name = "isAiIndexed", required = false) Boolean isAiIndexed,
            @Parameter(description = "Trang hiện tại (1-based)") @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @Parameter(description = "Số tệp mỗi trang") @RequestParam(name = "limit", required = false, defaultValue = "20") int limit,
            @Parameter(description = "Trường sắp xếp: name | size | updatedAt") @RequestParam(name = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @Parameter(description = "Thứ tự sắp xếp: asc | desc") @RequestParam(name = "sortOrder", required = false, defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProjectDocumentsDataAndMeta result = documentService.getDocumentsExplorer(
                projectId, search, category, isAiIndexed, page, limit, sortBy, sortOrder, userDetails.getUsername()
        );

        return ResponseEntity.ok(ApiResponse.success(200, result.getData(), "Lấy danh sách tài liệu thành công!", result.getMeta()));
    }

    @PostMapping(value = "/projects/{projectId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải tệp lên dự án", description = "Dành cho thành viên dự án hoặc Admin. Tải tệp lên MinIO (multipart/form-data), validate kích thước/định dạng và lưu metadata. Với tệp PDF, Word, Markdown, Text có thể tùy chọn nạp vector RAG.")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
            @Parameter(description = "ID của dự án tải tệp lên") @PathVariable Long projectId,
            @Parameter(description = "Tệp đính kèm cần upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Danh mục tệp: docs, sheets, media, images, code (tùy chọn)") @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "enableRag", required = false, defaultValue = "true") Boolean enableRag,
            @AuthenticationPrincipal UserDetails userDetails) {

        DocumentUploadResponse response = documentService.uploadDocumentExplorer(projectId, file, category, enableRag, userDetails.getUsername());
        String msg = Boolean.TRUE.equals(response.getIsAiIndexed())
                ? "Tải lên tệp thành công và đã hoàn tất lập chỉ mục Vector hỏi đáp AI."
                : "Tải lên tệp thành công và lưu trữ trên hệ thống.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response, msg));
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
}
