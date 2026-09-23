package com.example.document_management.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.example.document_management.dto.request.ProjectCreateRequest;
import com.example.document_management.dto.request.ProjectUpdateRequest;
import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.ProjectResponse;
import com.example.document_management.service.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "4. Project Management", description = "Quản lý dự án làm việc, thông tin thành viên và quyền sở hữu dự án")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Tạo dự án mới", description = "Tạo dự án làm việc mới. Người tạo mặc định sẽ là Project Owner (Chủ dự án)")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProjectCreateRequest projectCreateRequest) {
        ProjectResponse response = projectService.createProject(userDetails.getUsername(), projectCreateRequest);
        return ResponseEntity.ok(ApiResponse.success(200, response, "Tạo dự án mới thành công!"));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách dự án tham gia", description = "Lấy danh sách phân trang tất cả các dự án mà người dùng hiện tại đang tham gia hoặc làm chủ sở hữu")
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getAllProjectByUser(
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Page<ProjectResponse> responses = projectService.getAllProjectByUser(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(200, responses, "Lấy danh sách dự án thành công!"));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Lấy thông tin chi tiết một dự án", description = "Dành cho thành viên dự án hoặc Admin. Xem thông tin chi tiết dự án kèm số lượng tài liệu và thành viên")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @Parameter(description = "ID của dự án cần xem") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectResponse response = projectService.getProjectById(projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy thông tin dự án thành công!"));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Cập nhật thông tin dự án", description = "Chỉ dành cho Project Owner hoặc Admin. Cập nhật tên và mô tả của dự án")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @Parameter(description = "ID của dự án cần cập nhật") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProjectUpdateRequest request) {
        ProjectResponse response = projectService.updateProject(projectId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, response, "Cập nhật thông tin dự án thành công!"));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Xóa dự án", description = "Chỉ dành cho Project Owner hoặc Admin. Xóa vĩnh viễn dự án cùng toàn bộ thành viên và tài liệu liên kết")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @Parameter(description = "ID của dự án cần xóa") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectService.deleteProject(projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Xóa dự án thành công!"));
    }
}
