package com.example.document_management.service;

import com.example.document_management.dto.response.ChatCitationDto;
import com.example.document_management.dto.response.CitationDto;
import com.example.document_management.dto.response.PredictResponse;
import com.example.document_management.entity.DocumentMetadata;
import com.example.document_management.entity.Project;
import com.example.document_management.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntegrationService {

    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    public PredictResponse generateRagResponse(Project project, String userQuery, List<Long> selectedDocumentIds) {

        var body = new HashMap<>();
        body.put("query", userQuery);
        body.put("project_id", project.getId().toString());

        try {
            var response = restClient.post()
                    .uri("/api/v1/chat/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PredictResponse.class);

            return response != null ? response : PredictResponse.builder()
                    .answer("Không nhận được phản hồi từ dịch vụ AI.")
                    .citations(List.of())
                    .build();
        } catch (Exception e) {
            log.error("Lỗi khi kết nối dịch vụ Python AI RAG: {}", e.getMessage());
            return PredictResponse.builder()
                    .answer("Hệ thống AI hiện đang xử lý hoặc chưa thể kết nối đến dịch vụ tri thức: " + e.getMessage())
                    .citations(List.of())
                    .build();
        }
    }
}
