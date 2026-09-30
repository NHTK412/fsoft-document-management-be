package com.example.document_management.controller;

import com.example.document_management.dto.request.*;
import com.example.document_management.dto.response.*;
import com.example.document_management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Các API xác thực tài khoản (Đăng ký, Đăng nhập, Làm mới Token, Phiên làm việc)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới", description = "Tạo tài khoản người dùng mới trong hệ thống (vai trò mặc định: ROLE_USER)")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response, "Đăng ký tài khoản thành công!"));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống", description = "Xác thực tài khoản bằng Email và Password, trả về Access Token và Refresh Token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(200, response, "Đăng nhập thành công!"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Cấp lại Access Token", description = "Lấy Access Token mới khi token cũ đã hết hạn bằng cách sử dụng Refresh Token hợp lệ")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(200, response, "Làm mới token thành công!"));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin phiên hiện tại", description = "Lấy thông tin chi tiết của người dùng hiện tại đang đăng nhập")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, response, "Lấy thông tin phiên hiện tại thành công!"));
    }

    
}
