package com.example.document_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDocumentsResponse {

    private DocumentSummaryDto summary;
    private List<ProjectDocumentItemResponse> files;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSummaryDto {
        private Long totalFiles;
        private String totalSize;
        private Map<String, Long> counts;
    }
}
