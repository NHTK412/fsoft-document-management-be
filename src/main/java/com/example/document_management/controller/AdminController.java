package com.example.document_management.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.example.document_management.dto.response.AdminStatsResponse;
import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.ProjectResponse;
import com.example.document_management.dto.response.UserResponse;
import com.example.document_management.entity.User;
import com.example.document_management.repository.ProjectRepository;
import com.example.document_management.repository.UserRepository;
import com.example.document_management.service.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin System Management", description = "Quản trị hệ thống dành riêng cho Quản trị viên")
public class AdminController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    @GetMapping("/stats")
    @Operation(summary = "Thống kê tổng quan hệ thống", description = "Dành riêng cho Admin. Lấy số lượng dự án đang hoạt động, tổng số người dùng và danh sách người dùng gần đây")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        long activeProjectsCount = projectRepository.countByStatus("active");
        long totalUsersCount = userRepository.count();
        List<User> recentUserEntities = userRepository.findTop10ByOrderByCreatedAtDesc();
        List<UserResponse> recentUsers = recentUserEntities.stream()
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .avatarUrl(u.getAvatarUrl())
                        .role(u.getRole())
                        .isActive(u.isActive())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();

        AdminStatsResponse stats = AdminStatsResponse.builder()
                .activeProjectsCount(activeProjectsCount)
                .totalUsersCount(totalUsersCount)
                .recentUsers(recentUsers)
                .build();

        return ResponseEntity.ok(ApiResponse.success(200, stats, "Lấy thống kê hệ thống thành công!"));
    }

    @GetMapping("/projects")
    @Operation(summary = "Lấy danh sách tất cả các dự án trong hệ thống", description = "Dành riêng cho Admin. Xem toàn bộ dự án của tất cả người dùng trong hệ thống")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjects() {
        List<ProjectResponse> projects = projectService.getAllProjectsForAdmin();
        return ResponseEntity.ok(ApiResponse.success(200, projects, "Lấy danh sách dự án thành công!"));
    }

    @DeleteMapping("/projects/{projectId}")
    @Operation(summary = "Xóa dự án khỏi hệ thống", description = "Dành riêng cho Admin. Xóa toàn bộ dự án và dữ liệu liên quan")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @Parameter(description = "ID của dự án cần xóa") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails adminDetails) {
        projectService.deleteProject(projectId, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Xóa dự án thành công!"));
    }
}
