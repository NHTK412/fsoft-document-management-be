package com.example.document_management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferOwnershipRequest {

    @NotBlank(message = "Email của chủ sở hữu mới không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String newOwnerEmail;
}
