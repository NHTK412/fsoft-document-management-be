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
import com.example.document_management.entity.ProjectMember;
import com.example.document_management.entity.User;
import com.example.document_management.enums.ProjectMemberRoleEnum;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.repository.ChatMessageRepository;
import com.example.document_management.repository.ChatSessionRepository;
import com.example.document_management.repository.ProjectMemberRepository;
import com.example.document_management.repository.ProjectRepository;
import com.example.document_management.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiIntegrationService aiIntegrationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiChatService aiChatService;

    private User user;
    private Project project;
    private ChatSession chatSession;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRoleEnum.ROLE_USER)
                .build();

        project = Project.builder()
                .id(10L)
                .name("KBase Alpha")
                .build();

        chatSession = ChatSession.builder()
                .id(100L)
                .project(project)
                .user(user)
                .title("Kiến trúc Kubernetes")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testGetChatSessions_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByProjectIdAndUserEmail(10L, "test@example.com"))
                .thenReturn(Optional.of(ProjectMember.builder().role(ProjectMemberRoleEnum.ROLE_MEMBER).build()));
        when(chatSessionRepository.findByProjectIdAndUserIdOrderByUpdatedAtDesc(10L, 1L))
                .thenReturn(List.of(chatSession));
        when(chatMessageRepository.countByChatSessionId(100L)).thenReturn(4L);

        List<ChatSessionResponse> responses = aiChatService.getChatSessions(10L, "test@example.com");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(100L, responses.get(0).getId());
        assertEquals("Kiến trúc Kubernetes", responses.get(0).getTitle());
        assertEquals(4L, responses.get(0).getMessageCount());
    }

    @Test
    void testCreateChatSession_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByProjectIdAndUserEmail(10L, "test@example.com"))
                .thenReturn(Optional.of(ProjectMember.builder().role(ProjectMemberRoleEnum.ROLE_MEMBER).build()));

        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession s = invocation.getArgument(0);
            s.setId(200L);
            return s;
        });

        CreateChatSessionRequest request = CreateChatSessionRequest.builder()
                .initialQuery("Quy trình triển khai microservices lên Kubernetes?")
                .build();

        CreateChatSessionResponse response = aiChatService.createChatSession(10L, "test@example.com", request);

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals("Quy trình triển khai microservices lên Kubernetes", response.getTitle());
        assertNotNull(response.getCreatedAt());
        verify(chatSessionRepository, times(1)).save(any(ChatSession.class));
    }

    @Test
    void testGetSessionMessages_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByProjectIdAndUserEmail(10L, "test@example.com"))
                .thenReturn(Optional.of(ProjectMember.builder().role(ProjectMemberRoleEnum.ROLE_MEMBER).build()));
        when(chatSessionRepository.findById(100L)).thenReturn(Optional.of(chatSession));

        ChatMessage msg1 = ChatMessage.builder()
                .id(1L)
                .chatSession(chatSession)
                .sender("user")
                .content("Câu hỏi 1")
                .createdAt(Instant.now())
                .build();

        ChatMessage msg2 = ChatMessage.builder()
                .id(2L)
                .chatSession(chatSession)
                .sender("ai")
                .intro("Giới thiệu")
                .content("Nội dung AI")
                .stepsJson("[\"bước 1\",\"bước 2\"]")
                .citationJson("{\"fileName\":\"test.pdf\",\"documentId\":5,\"page\":1,\"confidence\":\"99%\",\"snippet\":\"trích dẫn\"}")
                .createdAt(Instant.now())
                .build();

        when(chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(msg1, msg2));

        List<ChatMessageResponse> messages = aiChatService.getSessionMessages(10L, 100L, "test@example.com");

        assertNotNull(messages);
        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).getSender());
        assertEquals("Câu hỏi 1", messages.get(0).getText());

        assertEquals("ai", messages.get(1).getSender());
        assertEquals("Nội dung AI", messages.get(1).getText());
        assertEquals("Giới thiệu", messages.get(1).getIntro());
        assertEquals(2, messages.get(1).getSteps().size());
        assertNotNull(messages.get(1).getCitation());
        assertEquals("test.pdf", messages.get(1).getCitation().getFileName());
    }

    @Test
    void testSendMessage_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByProjectIdAndUserEmail(10L, "test@example.com"))
                .thenReturn(Optional.of(ProjectMember.builder().role(ProjectMemberRoleEnum.ROLE_MEMBER).build()));
        when(chatSessionRepository.findById(100L)).thenReturn(Optional.of(chatSession));

        ChatCitationDto citation = ChatCitationDto.builder()
                .fileName("Architecture.pdf")
                .documentId(5L)
                .page(2)
                .confidence("99%")
                .snippet("Trích dẫn tài liệu")
                .build();

        AiIntegrationService.GeneratedAiMessage generated = new AiIntegrationService.GeneratedAiMessage(
                "Intro text",
                "Answer text",
                List.of("Step 1", "Step 2"),
                "[\"Step 1\",\"Step 2\"]",
                citation,
                "{\"fileName\":\"Architecture.pdf\"}"
        );

        when(aiIntegrationService.generateRagResponse(eq(project), anyString(), any()))
                .thenReturn(generated);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage m = invocation.getArgument(0);
            if ("ai".equals(m.getSender())) {
                m.setId(99L);
            } else {
                m.setId(98L);
            }
            return m;
        });

        SendMessageRequest request = SendMessageRequest.builder()
                .message("Các cổng MinIO cần mở?")
                .selectedDocumentIds(List.of(5L))
                .build();

        ChatMessageResponse response = aiChatService.sendMessage(10L, 100L, "test@example.com", request);

        assertNotNull(response);
        assertEquals(99L, response.getId());
        assertEquals("ai", response.getSender());
        assertEquals("Intro text", response.getIntro());
        assertEquals("Answer text", response.getText());
        assertEquals(2, response.getSteps().size());
        assertNotNull(response.getCitation());
        assertEquals("Architecture.pdf", response.getCitation().getFileName());

        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
        verify(chatSessionRepository, times(1)).save(chatSession);
    }

    @Test
    void testAccessDeniedWhenNotMember() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByProjectIdAndUserEmail(10L, "test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                aiChatService.getChatSessions(10L, "test@example.com"));
    }
}
