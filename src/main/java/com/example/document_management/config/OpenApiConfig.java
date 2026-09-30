package com.example.document_management.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hệ thống Quản lý Tài liệu & Tích hợp AI (Document Management System API)")
                        .version("v1.0.0")
                        .description("Tài liệu hướng dẫn sử dụng REST API cho hệ thống Quản lý Tài liệu, phân quyền bảo mật JWT, lưu trữ đám mây MinIO và tích hợp AI.")
                        .contact(new Contact()
                                .name("Document Management Team")
                                .email("contact@example.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Các API xác thực tài khoản (Đăng ký, Đăng nhập, Làm mới Token, Phiên làm việc)"),
                        new Tag().name("User Profile").description("Quản lý thông tin tài khoản cá nhân, đổi mật khẩu và quản lý phiên đăng nhập"),
                        new Tag().name("Project Management").description("Quản lý dự án làm việc, cấu hình cài đặt và số liệu thống kê Dashboard"),
                        new Tag().name("Project Members").description("Quản lý danh sách thành viên dự án, gửi lời mời và phân quyền vai trò"),
                        new Tag().name("User Invitations").description("Quản lý và phản hồi lời mời tham gia dự án của người dùng hiện tại"),
                        new Tag().name("Document Management").description("Quản lý và thao tác tệp tài liệu MinIO (Upload, Download URL, Preview URL, Bulk Delete)"),
                        new Tag().name("AI Assistant").description("Trợ lý AI hỏi đáp thông minh và phân tích tài liệu theo ngữ cảnh (RAG Chat)"),
                        new Tag().name("Admin Management").description("Quản trị hệ thống, quản lý người dùng và giám sát toàn bộ dự án (Dành riêng cho Quản trị viên)")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập Access Token JWT (dạng: Bearer <token>)")));
    }
}
