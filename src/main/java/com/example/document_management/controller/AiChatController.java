package com.example.document_management.controller;

import com.example.document_management.dto.request.CreateChatSessionRequest;
import com.example.document_management.dto.request.SendMessageRequest;
import com.example.document_management.dto.response.ApiResponse;
import com.example.document_management.dto.response.ChatMessageResponse;
import com.example.document_management.dto.response.ChatSessionResponse;
import com.example.document_management.dto.response.CreateChatSessionResponse;
import com.example.document_management.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "6. AI Assistant", description = "Tương tác và hỏi đáp thông minh với tài liệu bằng AI")
public class AiChatController {

    private final AiChatService aiChatService;

    @GetMapping("/{projectId}/chat/sessions")
    @Operation(summary = "Lấy danh sách phiên trò chuyện", description = "Lấy lịch sử các phiên chat trong dự án của người dùng hiện tại")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getChatSessions(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ChatSessionResponse> responses = aiChatService.getChatSessions(projectId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, responses, "Lấy danh sách phiên chat thành công!"));
    }

    @PostMapping("/{projectId}/chat/sessions")
    @Operation(summary = "Tạo phiên chat mới", description = "Khởi tạo phiên chat mới trong dự án với câu hỏi ban đầu")
    public ResponseEntity<ApiResponse<CreateChatSessionResponse>> createChatSession(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateChatSessionRequest request) {
        CreateChatSessionResponse response = aiChatService.createChatSession(projectId, userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response, "Tạo phiên chat mới thành công!"));
    }

    @GetMapping("/{projectId}/chat/sessions/{sessionId}/messages")
    @Operation(summary = "Lấy lịch sử tin nhắn trong phiên", description = "Lấy toàn bộ tin nhắn đã trao đổi (user và AI kèm citations) trong một phiên chat")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getSessionMessages(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "ID của phiên chat") @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ChatMessageResponse> responses = aiChatService.getSessionMessages(projectId, sessionId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(200, responses, "Lấy lịch sử tin nhắn thành công!"));
    }

    @PostMapping("/{projectId}/chat/sessions/{sessionId}/messages")
    @Operation(summary = "Gửi câu hỏi và nhận phản hồi AI", description = "Gửi tin nhắn thắc mắc vào phiên chat và nhận câu trả lời phân tích từ trợ lý AI kèm nguồn trích dẫn")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Parameter(description = "ID của dự án") @PathVariable Long projectId,
            @Parameter(description = "ID của phiên chat") @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SendMessageRequest request) {
        ChatMessageResponse response = aiChatService.sendMessage(projectId, sessionId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(200, response, "Nhận phản hồi từ trợ lý AI thành công!"));
    }
}
