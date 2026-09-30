package com.example.document_management.dto.response;

import com.example.document_management.enums.UserRoleEnum;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private UserRoleEnum role;
    private boolean isActive;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private java.time.Instant createdAt;
}
