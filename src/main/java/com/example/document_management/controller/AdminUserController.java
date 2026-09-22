package com.example.document_management.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.document_management.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "3. Admin User Controller", description = "Quản lý tài khoản hệ thống")
@SecurityRequirement(name= "bearerAuth")
public class AdminUserController {

    @GetMapping()
    @Operation(summary = "Lấy danh sách tất cả user trong hệ thống", description = "Admin")
    public ApiResponse<?> getAllUsers() {
        throw new RuntimeException("Not Implemented!");
    }

    @GetMapping("{userId}")
    @Operation(summary = "Xem thông tin chi tiết của một người dùng", description = "Admin")
    public ApiResponse<?> getUserById(@PathVariable Long userId) {
        throw new RuntimeException("Not Implemented!");
    }

    @PatchMapping("{userId}/role")
    @Operation(summary = "Đổi quyền hệ thống của user (Admin, Owner, User)", description = "Admin")
    public ApiResponse<?> changeRole(@PathVariable Long userId) {
        throw new RuntimeException("Not Implemented!");

    }

    @PatchMapping("{userId}/status")
    @Operation(summary = "Khóa hoặc kích hoạt lại tài khoản user", description = "Admin")
    public ApiResponse<?> changeStatus(@PathVariable Long userId) {
        throw new RuntimeException("Not Implemented!");

    }

    @DeleteMapping("{userId}")
    @Operation(summary = "Xóa tài khoản người dùng khỏi hệ thống", description = "Admin")
    public ApiResponse<?> deleteUser(@PathVariable Long userId) {
        throw new RuntimeException("Not Implemented!");

    }

}
// GET /api/v1/admin/users Admin Lấy danh sách tất cả user trong hệ thống (hỗ
// trợ phân trang, lọc theo role).
// GET /api/v1/admin/users/{userId} Admin Xem thông tin chi tiết của một người
// dùng.
// PATCH /api/v1/admin/users/{userId}/role Admin Đổi quyền hệ thống của user
// (Admin, Owner, User).
// PATCH /api/v1/admin/users/{userId}/status Admin Khóa hoặc kích hoạt lại tài
// khoản user.
// DELETE /api/v1/admin/users/{userId} Admin Xóa tài khoản người dùng khỏi hệ
// thống.