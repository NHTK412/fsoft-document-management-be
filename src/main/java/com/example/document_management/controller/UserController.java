package com.example.document_management.controller;

import com.example.document_management.dto.request.UpdateProfileRequest;
import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.UserResponse;
import com.example.document_management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "2. Profile", description = "Quản lý thông tin tài khoản cá nhân")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin cá nhân của người dùng đang đăng nhập")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse user = authService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user, "Lấy thông tin thành công!"));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin cá nhân (họ tên, đổi mật khẩu)")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        UserResponse user = authService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(user, "Cập nhật thông tin thành công!"));
    }
}
