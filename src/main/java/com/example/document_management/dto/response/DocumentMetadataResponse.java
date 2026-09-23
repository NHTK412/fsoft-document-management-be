package com.example.document_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadataResponse {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Long projectId;
    private Long uploaderId;
    private Instant createdAt;
}
