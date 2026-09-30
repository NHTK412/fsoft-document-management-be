package com.example.document_management.controller;

import com.example.document_management.dto.request.ProjectInviteRequest;
import com.example.document_management.dto.request.UpdateMemberRoleRequest;
import com.example.document_management.dto.response.*;
import com.example.document_management.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Project Members", description = "Quản lý danh sách thành viên dự án, gửi lời mời và phân quyền vai trò")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @GetMapping("/members")
    @Operation(summary = "Lấy danh sách thành viên hiện tại", description = "Dành cho thành viên dự án hoặc Admin. Hỗ trợ tìm kiếm theo tên hoặc email")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getMembers(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "Từ khóa tìm kiếm theo tên hoặc email") @RequestParam(name = "search", required = false) String search,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectMemberResponse> response = projectMemberService.getMembers(projectId, search, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy danh sách thành viên thành công!"));
    }

    @GetMapping("/invites")
    @Operation(summary = "Lấy danh sách lời mời đang chờ", description = "Dành cho thành viên dự án hoặc Admin. Xem danh sách các lời mời Pending")
    public ResponseEntity<ApiResponse<List<ProjectPendingInviteResponse>>> getPendingInvites(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectPendingInviteResponse> response = projectMemberService.getPendingInvites(projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy danh sách lời mời đang chờ thành công!"));
    }

    @PostMapping("/invites")
    @Operation(summary = "Gửi lời mời thành viên mới", description = "Chỉ dành cho Chủ dự án hoặc Admin dự án. Gửi lời mời tham gia qua email kèm vai trò chỉ định")
    public ResponseEntity<ApiResponse<ProjectInviteResponse>> inviteMember(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Valid @RequestBody ProjectInviteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectInviteResponse response = projectMemberService.inviteMember(projectId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response, "Lời mời tham gia dự án đã được gửi tới email thành công"));
    }

    @PatchMapping("/members/{memberId}/role")
    @Operation(summary = "Đổi vai trò thành viên", description = "Chỉ dành cho Chủ dự án hoặc Admin dự án. Cập nhật quyền hạn vai trò (Admin, Member, Viewer)")
    public ResponseEntity<ApiResponse<UpdateMemberRoleResponse>> updateMemberRole(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "ID của thành viên trong dự án") @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UpdateMemberRoleResponse response = projectMemberService.updateMemberRole(projectId, memberId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Cập nhật vai trò thành công"));
    }

    @PostMapping("/invites/{inviteId}/resend")
    @Operation(summary = "Gửi lại lời mời tham gia", description = "Chỉ dành cho Chủ dự án hoặc Admin dự án. Đặt lại hạn sử dụng và gửi lại email lời mời")
    public ResponseEntity<ApiResponse<Void>> resendInvite(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "ID của bản ghi lời mời") @PathVariable Long inviteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectMemberService.resendInvite(projectId, inviteId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Đã gửi lại email lời mời thành công"));
    }

    @DeleteMapping("/invites/{inviteId}")
    @Operation(summary = "Hủy lời mời tham gia", description = "Chỉ dành cho Chủ dự án hoặc Admin dự án. Xóa bỏ lời mời đang chờ xử lý")
    public ResponseEntity<ApiResponse<Void>> cancelInvite(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "ID của bản ghi lời mời") @PathVariable Long inviteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectMemberService.cancelInvite(projectId, inviteId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Đã hủy lời mời thành công"));
    }

    @DeleteMapping("/members/{memberId}")
    @Operation(summary = "Xóa thành viên khỏi dự án", description = "Chỉ dành cho Chủ dự án hoặc Admin dự án. Thu hồi quyền truy cập và xóa khỏi danh sách thành viên")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "ID của thành viên cần xóa") @PathVariable Long memberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectMemberService.removeMember(projectId, memberId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Đã xóa thành viên khỏi dự án thành công"));
    }
}
