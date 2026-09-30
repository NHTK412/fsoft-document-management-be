package com.example.document_management.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.UserResponse;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Management", description = "Quản trị hệ thống, quản lý người dùng và giám sát toàn bộ dự án (Dành riêng cho Quản trị viên)")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả người dùng", description = "Dành riêng cho Admin. Xem danh sách phân trang người dùng trong hệ thống kèm vai trò và trạng thái kích hoạt")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<UserResponse> usersResponse = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(200, usersResponse, "Lấy danh sách người dùng thành công!"));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Xem thông tin chi tiết một người dùng", description = "Dành riêng cho Admin. Xem thông tin chi tiết của người dùng theo ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "ID của người dùng cần xem") @PathVariable Long userId) {
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(200, userResponse, "Xem thông tin người dùng thành công!"));
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "Thay đổi quyền hệ thống của người dùng", description = "Dành riêng cho Admin. Cập nhật vai trò hệ thống của người dùng (ROLE_ADMIN, ROLE_USER)")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @Parameter(description = "ID của người dùng cần đổi quyền") @PathVariable Long userId,
            @Parameter(description = "Vai trò mới cần cấp") @RequestParam UserRoleEnum role) {
        UserResponse user = userService.changeRole(userId, role);
        return ResponseEntity.ok(ApiResponse.success(200, user, "Đổi quyền thành công!"));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Khóa hoặc mở khóa tài khoản người dùng", description = "Dành riêng cho Admin. Thay đổi trạng thái hoạt động (kích hoạt / vô hiệu hóa) của tài khoản")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @Parameter(description = "ID của người dùng cần đổi trạng thái") @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails adminDetails) {
        UserResponse user = userService.changeStatus(userId, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, user, "Thay đổi trạng thái tài khoản thành công!"));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Xóa tài khoản người dùng", description = "Dành riêng cho Admin. Xóa mềm (soft delete) tài khoản người dùng khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "ID của người dùng cần xóa") @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails adminDetails) {
        userService.deleteUser(userId, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, null, "Xóa tài khoản thành công!"));
    }
}
