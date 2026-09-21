package com.example.document_management.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String currentPassword;
    private String newPassword;
}
