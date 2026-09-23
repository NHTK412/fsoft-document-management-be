package com.example.document_management.controller;

import java.util.List;

import jakarta.validation.Valid;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "4. Project Management", description = "API quản lý dự án (Project)")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Tạo dự án mới", description = "Người tạo mặc định sẽ là Project Owner")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProjectCreateRequest projectCreateRequest) {
        ProjectResponse response = projectService.createProject(userDetails.getUsername(), projectCreateRequest);
        return ResponseEntity.ok(ApiResponse.success(200, response, "Tạo dự án mới thành công!"));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách dự án mà user hiện tại đang tham gia")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjectByUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectResponse> responses = projectService.getAllProjectByUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, responses, "Lấy danh sách dự án thành công!"));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Lấy thông tin chi tiết một dự án", description = "Dành cho thành viên dự án hoặc Admin")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectResponse response = projectService.getProjectById(projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy thông tin dự án thành công!"));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Cập nhật thông tin dự án", description = "Chỉ dành cho Project Owner hoặc Admin")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProjectUpdateRequest request) {
        ProjectResponse response = projectService.updateProject(projectId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, response, "Cập nhật thông tin dự án thành công!"));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Xóa dự án", description = "Chỉ dành cho Project Owner hoặc Admin")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectService.deleteProject(projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Xóa dự án thành công!"));
    }
}
