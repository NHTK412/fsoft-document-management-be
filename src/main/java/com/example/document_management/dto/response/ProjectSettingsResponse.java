package com.example.document_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSettingsResponse {
    private String projectName;
    private String projectDesc;
    private String logoUrl;
    private String minioBucket;
    private Long storageUsedBytes;
    private Long storageLimitBytes;
    private String maxFileSize;
    private List<String> allowedFormats;
    private AiPersonaDto aiPersona;
}
