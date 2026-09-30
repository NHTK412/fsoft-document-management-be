package com.example.document_management.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.example.document_management.dto.request.DeleteProjectConfirmRequest;
import com.example.document_management.dto.request.ProjectCreateRequest;
import com.example.document_management.dto.request.ProjectUpdateRequest;
import com.example.document_management.dto.request.TransferOwnershipRequest;
import com.example.document_management.dto.request.UpdateProjectSettingsRequest;
import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.ProjectResponse;
import com.example.document_management.dto.response.ProjectSettingsResponse;
import com.example.document_management.dto.response.UpdateProjectSettingsResponse;
import com.example.document_management.dto.response.ProjectActivityResponse;
import com.example.document_management.dto.response.ProjectDashboardStatsResponse;
import com.example.document_management.dto.response.RecentlyViewedDocResponse;
import com.example.document_management.service.ProjectDashboardService;
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
@Tag(name = "Project Management", description = "Quản lý dự án làm việc, cấu hình cài đặt và số liệu thống kê Dashboard")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectDashboardService projectDashboardService;

    @PostMapping
    @Operation(summary = "Tạo dự án mới", description = "Tạo dự án làm việc mới. Người tạo mặc định sẽ là Project Owner (Chủ dự án)")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProjectCreateRequest projectCreateRequest) {
        ProjectResponse response = projectService.createProject(userDetails.getUsername(), projectCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response, "Dự án đã được tạo thành công!"));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách dự án tham gia", description = "Lấy danh sách tất cả các dự án mà người dùng hiện tại đang tham gia, hỗ trợ tìm kiếm và lọc theo vai trò, trạng thái")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjectByUser(
            @Parameter(description = "Từ khóa tìm kiếm tên hoặc mô tả dự án") @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Lọc theo vai trò: all | owner | admin | member") @RequestParam(name = "role", required = false) String role,
            @Parameter(description = "Lọc theo trạng thái: active | archived") @RequestParam(name = "status", required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectResponse> responses = projectService.getAllProjects(userDetails.getUsername(), search, role, status);
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

    @GetMapping("/{projectId}/settings")
    @Operation(summary = "Lấy cấu hình chi tiết dự án", description = "Lấy thông tin cấu hình dự án, bao gồm giới hạn lưu trữ, định dạng tệp và AI Persona")
    public ResponseEntity<ApiResponse<ProjectSettingsResponse>> getProjectSettings(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectSettingsResponse response = projectService.getProjectSettings(projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy cấu hình dự án thành công!"));
    }

    @PutMapping("/{projectId}/settings")
    @Operation(summary = "Cập nhật cấu hình dự án", description = "Cập nhật cài đặt dự án (tên, mô tả, dung lượng tệp, định dạng cho phép, cấu hình AI Persona)")
    public ResponseEntity<ApiResponse<UpdateProjectSettingsResponse>> updateProjectSettings(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProjectSettingsRequest request) {
        UpdateProjectSettingsResponse response = projectService.updateProjectSettings(projectId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, response, "Cài đặt dự án đã được lưu thành công!"));
    }

    @PostMapping("/{projectId}/transfer-ownership")
    @Operation(summary = "Chuyển nhượng quyền chủ sở hữu dự án", description = "Chuyển giao quyền Project Owner sang cho email thành viên khác. Chỉ Owner hiện tại mới có quyền gọi.")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferOwnershipRequest request) {
        projectService.transferOwnership(projectId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, null, "Đã chuyển nhượng quyền Project Owner thành công"));
    }

    @PostMapping("/{projectId}/archive")
    @Operation(summary = "Lưu trữ dự án", description = "Chuyển trạng thái dự án sang lưu trữ (Read-only)")
    public ResponseEntity<ApiResponse<Void>> archiveProject(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectService.archiveProject(projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Dự án đã được chuyển sang chế độ lưu trữ (Read-only)"));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Xóa vĩnh viễn dự án", description = "Xóa vĩnh viễn dự án và toàn bộ dữ liệu MinIO, DB sau khi xác nhận an toàn tên dự án")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @Parameter(description = "ID của dự án cần xóa") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) DeleteProjectConfirmRequest request) {
        projectService.deleteProject(projectId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, null, "Dự án và toàn bộ dữ liệu MinIO, vector đã bị xóa vĩnh viễn"));
    }

    @GetMapping("/{projectId}/dashboard/stats")
    @Operation(summary = "Lấy số liệu thống kê chỉ số dự án", description = "Lấy các chỉ số tổng quan của dự án (tổng số tệp, dung lượng MinIO, hỏi đáp AI trong tuần, thành viên hoạt động) và phân bố định dạng tài liệu")
    public ResponseEntity<ApiResponse<ProjectDashboardStatsResponse>> getDashboardStats(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectDashboardStatsResponse response = projectDashboardService.getDashboardStats(projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy thống kê dự án thành công!"));
    }

    @GetMapping("/{projectId}/dashboard/recently-viewed")
    @Operation(summary = "Lấy danh sách tài liệu truy cập gần đây", description = "Lấy danh sách tệp tài liệu được tải lên hoặc truy cập gần nhất trong dự án")
    public ResponseEntity<ApiResponse<List<RecentlyViewedDocResponse>>> getRecentlyViewed(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "Số lượng bản ghi tối đa (mặc định 5)") @RequestParam(name = "limit", required = false, defaultValue = "5") Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<RecentlyViewedDocResponse> response = projectDashboardService.getRecentlyViewed(projectId, limit, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy danh sách tài liệu gần đây thành công!"));
    }

    @GetMapping("/{projectId}/dashboard/activities")
    @Operation(summary = "Dòng thời gian hoạt động của dự án", description = "Lấy timeline lịch sử hoạt động gần đây của dự án")
    public ResponseEntity<ApiResponse<List<ProjectActivityResponse>>> getActivities(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "Số lượng bản ghi tối đa (mặc định 10)") @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectActivityResponse> response = projectDashboardService.getActivities(projectId, limit, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy dòng thời gian hoạt động thành công!"));
    }
}
