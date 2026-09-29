package com.example.document_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPreviewUrlResponse {
    private Long documentId;
    private String previewUrl;
    private String mimeType;
    private Integer expiresIn;
}
