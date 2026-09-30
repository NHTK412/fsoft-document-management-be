package com.example.document_management.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.example.document_management.dto.request.ChangePasswordRequest;
import com.example.document_management.dto.request.UpdateUserProfileRequest;
import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.UserProfileResponse;
import com.example.document_management.dto.response.UserResponse;
import com.example.document_management.dto.response.UserSessionResponse;
import com.example.document_management.service.AuthService;
import com.example.document_management.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Profile", description = "Quản lý thông tin tài khoản cá nhân, đổi mật khẩu và quản lý phiên đăng nhập")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    // -------------------------------------------------------------
    // USER PROFILE ENDPOINTS (Screen 3.8: UserProfile.jsx)
    // -------------------------------------------------------------

    @GetMapping("/me/profile")
    @Operation(summary = "Lấy chi tiết hồ sơ cá nhân", description = "Lấy thông tin hồ sơ mở rộng của người dùng hiện tại (họ tên, chức danh, số điện thoại, vai trò, avatar, initials)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, profile, "Lấy thông tin hồ sơ thành công!"));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Cập nhật hồ sơ cá nhân", description = "Cập nhật thông tin chi tiết cá nhân (họ tên, chức danh, số điện thoại)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserProfileResponse updatedProfile = userService.updateUserProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, updatedProfile, "Hồ sơ cá nhân đã được cập nhật thành công"));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Đổi mật khẩu tài khoản", description = "Thay đổi mật khẩu đăng nhập của người dùng hiện tại")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, null, "Mật khẩu đã được thay đổi thành công"));
    }

    @GetMapping("/me/sessions")
    @Operation(summary = "Lấy danh sách phiên đăng nhập thiết bị", description = "Lấy danh sách các thiết bị và phiên đăng nhập hiện tại của người dùng")
    public ResponseEntity<ApiResponse<List<UserSessionResponse>>> getUserSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        List<UserSessionResponse> sessions = userService.getUserSessions(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, sessions, "Lấy danh sách phiên đăng nhập thành công!"));
    }

    @DeleteMapping("/me/sessions/{sessionId}")
    @Operation(summary = "Thu hồi một phiên đăng nhập chỉ định", description = "Đăng xuất và thu hồi quyền của một thiết bị cụ thể")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long sessionId) {
        userService.revokeSession(userDetails.getUsername(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(200, null, "Đã thu hồi phiên đăng nhập thành công"));
    }

    @DeleteMapping("/me/sessions")
    @Operation(summary = "Đăng xuất khỏi tất cả các thiết bị khác", description = "Thu hồi tất cả các phiên đăng nhập khác, chỉ giữ lại phiên hiện tại")
    public ResponseEntity<ApiResponse<Void>> revokeAllOtherSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        userService.revokeAllOtherSessions(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, null, "Đã đăng xuất khỏi tất cả các thiết bị khác thành công"));
    }
}
