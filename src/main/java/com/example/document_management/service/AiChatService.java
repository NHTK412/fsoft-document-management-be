package com.example.document_management.service;

import com.example.document_management.dto.request.CreateChatSessionRequest;
import com.example.document_management.dto.request.SendMessageRequest;
import com.example.document_management.dto.response.ChatCitationDto;
import com.example.document_management.dto.response.ChatMessageResponse;
import com.example.document_management.dto.response.ChatSessionResponse;
import com.example.document_management.dto.response.CreateChatSessionResponse;
import com.example.document_management.entity.ChatMessage;
import com.example.document_management.entity.ChatSession;
import com.example.document_management.entity.Project;
import com.example.document_management.entity.User;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.ChatMessageRepository;
import com.example.document_management.repository.ChatSessionRepository;
import com.example.document_management.repository.ProjectMemberRepository;
import com.example.document_management.repository.ProjectRepository;
import com.example.document_management.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final AiIntegrationService aiIntegrationService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getChatSessions(Long projectId, String email) {
        Project project = getProject(projectId);
        User user = getUser(email);
        verifyProjectAccess(project, user);

        List<ChatSession> sessions = chatSessionRepository.findByProjectIdAndUserIdOrderByUpdatedAtDesc(projectId, user.getId());

        return sessions.stream().map(session -> {
            long count = chatMessageRepository.countByChatSessionId(session.getId());
            Instant updatedAt = session.getUpdatedAt() != null ? session.getUpdatedAt() : session.getCreatedAt();
            return ChatSessionResponse.builder()
                    .id(session.getId())
                    .title(session.getTitle())
                    .updatedAt(DateTimeFormatter.ISO_INSTANT.format(updatedAt))
                    .messageCount(count)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public CreateChatSessionResponse createChatSession(Long projectId, String email, CreateChatSessionRequest request) {
        Project project = getProject(projectId);
        User user = getUser(email);
        verifyProjectAccess(project, user);

        String title = generateTitleFromQuery(request.getInitialQuery());
        Instant now = Instant.now();

        ChatSession session = ChatSession.builder()
                .project(project)
                .user(user)
                .title(title)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ChatSession saved = chatSessionRepository.save(session);

        return CreateChatSessionResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .createdAt(DateTimeFormatter.ISO_INSTANT.format(saved.getCreatedAt()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getSessionMessages(Long projectId, Long sessionId, String email) {
        Project project = getProject(projectId);
        User user = getUser(email);
        verifyProjectAccess(project, user);

        ChatSession session = getChatSession(sessionId, projectId);

        List<ChatMessage> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(sessionId);

        return messages.stream().map(this::mapToChatMessageResponse).collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long projectId, Long sessionId, String email, SendMessageRequest request) {
        Project project = getProject(projectId);
        User user = getUser(email);
        verifyProjectAccess(project, user);

        ChatSession session = getChatSession(sessionId, projectId);

        Instant now = Instant.now();

        // 1. Lưu tin nhắn của người dùng
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .sender("user")
                .content(request.getMessage().trim())
                .createdAt(now)
                .build();
        chatMessageRepository.save(userMessage);

        // 2. Sinh phản hồi AI kèm trích dẫn RAG
        AiIntegrationService.GeneratedAiMessage aiGen = aiIntegrationService.generateRagResponse(
                project,
                request.getMessage(),
                request.getSelectedDocumentIds()
        );

        Instant aiTime = Instant.now();

        // 3. Lưu câu trả lời của AI
        ChatMessage aiMessage = ChatMessage.builder()
                .chatSession(session)
                .sender("ai")
                .intro(aiGen.intro())
                .content(aiGen.text())
                .stepsJson(aiGen.stepsJson())
                .citationJson(aiGen.citationJson())
                .createdAt(aiTime)
                .build();

        ChatMessage savedAi = chatMessageRepository.save(aiMessage);

        // 4. Cập nhật thời điểm phiên chat
        session.setUpdatedAt(aiTime);
        chatSessionRepository.save(session);

        return ChatMessageResponse.builder()
                .id(savedAi.getId())
                .sender("ai")
                .intro(aiGen.intro())
                .text(aiGen.text())
                .steps(aiGen.steps())
                .citation(aiGen.citation())
                .createdAt(DateTimeFormatter.ISO_INSTANT.format(aiTime))
                .build();
    }

    // -------------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------------

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    private ChatSession getChatSession(Long sessionId, Long projectId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên chat với id: " + sessionId));
        if (!session.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Phiên chat không thuộc dự án được chỉ định!");
        }
        return session;
    }

    private void verifyProjectAccess(Project project, User user) {
        if (user.getRole() == UserRoleEnum.ROLE_ADMIN) {
            return;
        }
        boolean isMember = projectMemberRepository.findByProjectIdAndUserEmail(project.getId(), user.getEmail()).isPresent();
        if (!isMember) {
            throw new AccessDeniedException("Bạn không phải thành viên của dự án này!");
        }
    }

    private String generateTitleFromQuery(String query) {
        if (query == null || query.isBlank()) {
            return "Phiên hỏi đáp mới";
        }
        String cleaned = query.trim().replaceAll("[\\?\\!\\.\\,\\;]+$", "");
        if (cleaned.length() > 60) {
            return cleaned.substring(0, 57) + "...";
        }
        return cleaned;
    }

    private ChatMessageResponse mapToChatMessageResponse(ChatMessage msg) {
        List<String> steps = new ArrayList<>();
        if (msg.getStepsJson() != null && !msg.getStepsJson().isBlank()) {
            try {
                steps = objectMapper.readValue(msg.getStepsJson(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Không thể parse stepsJson: {}", e.getMessage());
            }
        }

        ChatCitationDto citation = null;
        if (msg.getCitationJson() != null && !msg.getCitationJson().isBlank()) {
            try {
                citation = objectMapper.readValue(msg.getCitationJson(), ChatCitationDto.class);
            } catch (Exception e) {
                log.warn("Không thể parse citationJson: {}", e.getMessage());
            }
        }

        return ChatMessageResponse.builder()
                .id(msg.getId())
                .sender(msg.getSender())
                .text(msg.getContent())
                .intro(msg.getIntro())
                .steps(steps)
                .citation(citation)
                .createdAt(DateTimeFormatter.ISO_INSTANT.format(msg.getCreatedAt()))
                .build();
    }
}
