package com.example.document_management.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserResponse user;
}
