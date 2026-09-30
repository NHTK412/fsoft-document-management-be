package com.example.document_management.controller;

import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.UserPendingInviteResponse;
import com.example.document_management.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invites")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Invitations", description = "Quản lý và phản hồi lời mời tham gia dự án của người dùng hiện tại")
public class UserInviteController {

    private final ProjectMemberService projectMemberService;

    @GetMapping("/my-invites")
    @Operation(summary = "Lấy danh sách lời mời của tôi", description = "Lấy tất cả các lời mời tham gia dự án đang chờ xử lý của người dùng hiện tại")
    public ResponseEntity<ApiResponse<List<UserPendingInviteResponse>>> getMyPendingInvites(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<UserPendingInviteResponse> response = projectMemberService.getMyPendingInvites(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy danh sách lời mời thành công!"));
    }

    @PostMapping("/{inviteId}/accept")
    @Operation(summary = "Chấp nhận lời mời tham gia dự án", description = "Người dùng chấp nhận lời mời và được tự động thêm vào danh sách thành viên dự án")
    public ResponseEntity<ApiResponse<Void>> acceptInvite(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectMemberService.acceptInvite(inviteId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Đã tham gia dự án thành công!"));
    }

    @PostMapping("/{inviteId}/decline")
    @Operation(summary = "Từ chối lời mời tham gia dự án", description = "Người dùng từ chối lời mời tham gia dự án")
    public ResponseEntity<ApiResponse<Void>> declineInvite(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectMemberService.declineInvite(inviteId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Đã từ chối lời mời tham gia"));
    }
}
