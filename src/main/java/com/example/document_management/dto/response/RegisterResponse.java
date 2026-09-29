package com.example.document_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private Long id;
    private String email;
    private Boolean requiresEmailVerification;

    @com.fasterxml.jackson.annotation.JsonProperty("userId")
    public Long getUserId() {
        return id;
    }
}
