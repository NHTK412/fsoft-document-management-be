package com.example.document_management.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.UserResponse;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "3. Admin User Controller", description = "Quản lý tài khoản hệ thống")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService userService;

    @GetMapping()
    @Operation(summary = "Lấy danh sách tất cả user trong hệ thống", description = "Admin")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> usersResponse = userService.getAllUsers();

        return ResponseEntity.ok(ApiResponse.success(200, usersResponse, "Lấy danh sách user thành công"));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Xem thông tin chi tiết của một người dùng", description = "Admin")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        UserResponse userResponse = userService.getUserById(userId);

        return ResponseEntity
                .ok(ApiResponse.success(200, userResponse, "Xem thông tin chi tiết người dùng thành công"));
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "Đổi quyền hệ thống của user (Admin, Owner, User)", description = "Admin")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(@PathVariable Long userId,
            @RequestParam UserRoleEnum role) {
        UserResponse user = userService.changeRole(userId, role);
        return ResponseEntity.ok(ApiResponse.success(200, user, "Đổi quyền thành công!"));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Khóa hoặc kích hoạt lại tài khoản user", description = "Admin")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(@PathVariable Long userId) {
        UserResponse user = userService.changeStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(200, user, "Thay đổi trạng thái thành công!"));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Xóa tài khoản người dùng khỏi hệ thống", description = "Admin")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(200, null, "Xóa tài khoản thành công!"));
    }

}
