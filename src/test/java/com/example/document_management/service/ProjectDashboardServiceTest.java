package com.example.document_management.service;

import com.example.document_management.dto.response.ProjectActivityResponse;
import com.example.document_management.dto.response.ProjectDashboardStatsResponse;
import com.example.document_management.dto.response.RecentlyViewedDocResponse;
import com.example.document_management.entity.DocumentMetadata;
import com.example.document_management.entity.Project;
import com.example.document_management.entity.ProjectActivity;
import com.example.document_management.entity.User;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectDashboardServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ProjectActivityRepository projectActivityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectDashboardService projectDashboardService;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("owner@example.com")
                .fullName("Owner Test")
                .role(UserRoleEnum.ROLE_USER)
                .build();

        project = Project.builder()
                .id(100L)
                .name("Test Project")
                .owner(owner)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testGetDashboardStats_Success() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(documentRepository.countByProjectId(100L)).thenReturn(2L);
        when(documentRepository.sumFileSizeByProjectId(100L)).thenReturn(5242880L); // 5 MB
        when(chatMessageRepository.countByProjectIdAndCreatedAtAfter(eq(100L), any(Instant.class))).thenReturn(15L);
        when(projectMemberRepository.countByProjectId(100L)).thenReturn(4L);

        DocumentMetadata doc1 = DocumentMetadata.builder()
                .id(1L)
                .fileName("report.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .build();
        DocumentMetadata doc2 = DocumentMetadata.builder()
                .id(2L)
                .fileName("sheet.xlsx")
                .contentType("application/vnd.openxmlformats")
                .fileSize(2048L)
                .build();

        when(documentRepository.findByProjectId(100L)).thenReturn(List.of(doc1, doc2));

        ProjectDashboardStatsResponse response = projectDashboardService.getDashboardStats(100L, "owner@example.com");

        assertNotNull(response);
        assertEquals(4, response.getMetrics().size());
        assertEquals("2", response.getMetrics().get(0).getValue());
        assertEquals("15", response.getMetrics().get(2).getValue());
        assertEquals("4", response.getMetrics().get(3).getValue());
        assertNotNull(response.getFormatDistribution());
        assertEquals("2", response.getFormatDistribution().getTotalFiles());
        assertEquals(5, response.getFormatDistribution().getFormats().size());
    }

    @Test
    void testGetRecentlyViewed_Success() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        DocumentMetadata doc = DocumentMetadata.builder()
                .id(10L)
                .fileName("architecture.pdf")
                .fileSize(4404019L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(documentRepository.findByProjectIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(List.of(doc));

        List<RecentlyViewedDocResponse> response = projectDashboardService.getRecentlyViewed(100L, 5, "owner@example.com");

        assertEquals(1, response.size());
        assertEquals("architecture.pdf", response.get(0).getName());
        assertEquals("pdf", response.get(0).getType());
        assertEquals(4.2, response.get(0).getSize());
    }

    @Test
    void testGetActivities_Success() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        ProjectActivity act = ProjectActivity.builder()
                .id(1L)
                .user(owner)
                .userAction("Owner Test đã tải lên tệp mới")
                .target("architecture.pdf")
                .createdAt(Instant.now())
                .build();

        when(projectActivityRepository.findByProjectIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(List.of(act));

        List<ProjectActivityResponse> response = projectDashboardService.getActivities(100L, 10, "owner@example.com");

        assertEquals(1, response.size());
        assertEquals("Owner Test đã tải lên tệp mới", response.get(0).getUserAction());
        assertEquals("architecture.pdf", response.get(0).getTarget());
    }
}
