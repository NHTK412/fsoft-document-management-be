package com.example.document_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCitationDto {
    private String fileName;
    private Long documentId;
    private Integer page;
    private String confidence;
    private String snippet;
}
