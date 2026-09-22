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
    private UserRoleEnum role;
    private boolean isActive;
}
