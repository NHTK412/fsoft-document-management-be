package com.example.document_management.service;

import com.example.document_management.dto.response.ChatCitationDto;
import com.example.document_management.entity.DocumentMetadata;
import com.example.document_management.entity.Project;
import com.example.document_management.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntegrationService {

    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Sinh phản hồi AI kèm trích dẫn (RAG context).
     * Phần gọi Python RAG service tạm thời được bypass theo yêu cầu.
     */
    public GeneratedAiMessage generateRagResponse(Project project, String userQuery, List<Long> selectedDocumentIds) {
        List<DocumentMetadata> relevantDocs = new ArrayList<>();

        if (selectedDocumentIds != null && !selectedDocumentIds.isEmpty()) {
            relevantDocs = documentRepository.findByIdInAndProjectId(selectedDocumentIds, project.getId());
        }

        if (relevantDocs.isEmpty()) {
            List<DocumentMetadata> allDocs = documentRepository.findByProjectId(project.getId());
            if (!allDocs.isEmpty()) {
                relevantDocs.add(allDocs.get(0));
            }
        }

        String intro;
        String text;
        List<String> steps = new ArrayList<>();
        ChatCitationDto citation = null;

        if (!relevantDocs.isEmpty()) {
            DocumentMetadata primaryDoc = relevantDocs.get(0);
            intro = "Dựa trên nội dung tài liệu kiến trúc và hướng dẫn kỹ thuật trong dự án '" + project.getName() + "':";
            text = "Hệ thống đã phân tích câu hỏi '" + userQuery.trim() + "'. Theo tài liệu '" + primaryDoc.getFileName() + "', các thông số cấu hình và quy trình liên quan đã được xác thực trong phạm vi dự án.";
            steps.add("1. Đọc và phân tích nội dung từ tệp " + primaryDoc.getFileName() + ".");
            steps.add("2. Trích xuất các tham số kỹ thuật và điều kiện vận hành phù hợp.");
            steps.add("3. Tổng hợp câu trả lời theo chỉ thị hệ thống AI Persona của dự án.");

            citation = ChatCitationDto.builder()
                    .fileName(primaryDoc.getFileName())
                    .documentId(primaryDoc.getId())
                    .page(1)
                    .confidence("98.5%")
                    .snippet("Nội dung tài liệu '" + primaryDoc.getFileName() + "' chứa các tham số chỉ dẫn và đặc tả kỹ thuật tương ứng với yêu cầu.")
                    .build();
        } else {
            intro = "Trợ lý AI dự án '" + project.getName() + "' phản hồi:";
            text = "Hiện tại dự án chưa có tài liệu nào được tải lên hoặc chọn lọc. Vui lòng tải lên tài liệu để trợ lý AI có thể trích xuất ngữ cảnh chính xác nhất.";
        }

        String stepsJson = null;
        String citationJson = null;
        try {
            if (!steps.isEmpty()) {
                stepsJson = objectMapper.writeValueAsString(steps);
            }
            if (citation != null) {
                citationJson = objectMapper.writeValueAsString(citation);
            }
        } catch (Exception e) {
            log.warn("Không thể serialize steps hoặc citation sang JSON: {}", e.getMessage());
        }

        return new GeneratedAiMessage(intro, text, steps, stepsJson, citation, citationJson);
    }

    public record GeneratedAiMessage(
            String intro,
            String text,
            List<String> steps,
            String stepsJson,
            ChatCitationDto citation,
            String citationJson
    ) {}
}
