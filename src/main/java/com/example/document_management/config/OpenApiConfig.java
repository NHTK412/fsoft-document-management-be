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
                        new Tag().name("1. Authentication").description("Các API xác thực tài khoản (Đăng ký, Đăng nhập, Làm mới Token)"),
                        new Tag().name("2. User Profile").description("Quản lý thông tin tài khoản cá nhân của người dùng"),
                        new Tag().name("3. Admin User Management").description("Quản trị tài khoản và phân quyền người dùng trong hệ thống (Dành riêng cho Admin)"),
                        new Tag().name("4. Project Management").description("Quản lý dự án làm việc, thông tin thành viên và quyền sở hữu dự án"),
                        new Tag().name("5. Document Management").description("Quản lý và thao tác tệp tài liệu (Upload MinIO, Download, Preview trực tiếp, Tìm kiếm, Xóa)"),
                        new Tag().name("6. AI Assistant").description("Tương tác và hỏi đáp thông minh với tài liệu bằng AI")
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
