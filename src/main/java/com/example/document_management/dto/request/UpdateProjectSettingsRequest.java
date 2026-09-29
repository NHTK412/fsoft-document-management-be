package com.example.document_management.dto.request;

import com.example.document_management.dto.response.AiPersonaDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectSettingsRequest {
    private String projectName;
    private String projectDesc;
    private String maxFileSize;
    private List<String> allowedFormats;
    private AiPersonaDto aiPersona;
}
