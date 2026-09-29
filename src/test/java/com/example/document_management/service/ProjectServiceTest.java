package com.example.document_management.service;

import com.example.document_management.dto.request.DeleteProjectConfirmRequest;
import com.example.document_management.dto.request.TransferOwnershipRequest;
import com.example.document_management.dto.request.UpdateProjectSettingsRequest;
import com.example.document_management.dto.response.AiPersonaDto;
import com.example.document_management.dto.response.ProjectSettingsResponse;
import com.example.document_management.dto.response.UpdateProjectSettingsResponse;
import com.example.document_management.entity.*;
import com.example.document_management.enums.ProjectMemberRoleEnum;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProjectInviteRepository projectInviteRepository;

    @Mock
    private ProjectActivityRepository projectActivityRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ProjectService projectService;

    private User owner;
    private User otherUser;
    private Project project;
    private ProjectMember ownerMember;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("owner@example.com")
                .fullName("Owner Test")
                .role(UserRoleEnum.ROLE_USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .fullName("Other Test")
                .role(UserRoleEnum.ROLE_USER)
                .build();

        project = Project.builder()
                .id(100L)
                .name("AI Knowledge Core")
                .description("Sample description")
                .owner(owner)
                .status("active")
                .maxFileSize("50 MB")
                .allowedFormats("pdf,docx,xlsx")
                .aiTemperature(0.2)
                .aiSystemPrompt("You are an AI assistant.")
                .storageLimitBytes(10737418240L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ownerMember = ProjectMember.builder()
                .id(10L)
                .project(project)
                .user(owner)
                .role(ProjectMemberRoleEnum.ROLE_OWNER)
                .build();
    }

    @Test
    void testGetProjectSettings_Success() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.findByProjectIdAndUserEmail(100L, "owner@example.com"))
                .thenReturn(Optional.of(ownerMember));
        when(documentRepository.sumFileSizeByProjectId(100L)).thenReturn(1048576L);
        when(storageService.getBucketName()).thenReturn("document-management");

        ProjectSettingsResponse response = projectService.getProjectSettings(100L, "owner@example.com");

        assertNotNull(response);
        assertEquals("AI Knowledge Core", response.getProjectName());
        assertEquals("Sample description", response.getProjectDesc());
        assertEquals("document-management", response.getMinioBucket());
        assertEquals(1048576L, response.getStorageUsedBytes());
        assertEquals(10737418240L, response.getStorageLimitBytes());
        assertEquals("50 MB", response.getMaxFileSize());
        assertEquals(List.of("pdf", "docx", "xlsx"), response.getAllowedFormats());
        assertNotNull(response.getAiPersona());
        assertEquals(0.2, response.getAiPersona().getTemperature());
        assertEquals("You are an AI assistant.", response.getAiPersona().getSystemPrompt());
    }

    @Test
    void testUpdateProjectSettings_Success() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.findByProjectIdAndUserEmail(100L, "owner@example.com"))
                .thenReturn(Optional.of(ownerMember));

        UpdateProjectSettingsRequest request = UpdateProjectSettingsRequest.builder()
                .projectName("AI Knowledge Core v2")
                .projectDesc("Updated description")
                .maxFileSize("100 MB")
                .allowedFormats(List.of("pdf", "docx"))
                .aiPersona(AiPersonaDto.builder().temperature(0.5).systemPrompt("Updated prompt").build())
                .build();

        UpdateProjectSettingsResponse response = projectService.updateProjectSettings(100L, "owner@example.com", request);

        assertNotNull(response);
        assertNotNull(response.getUpdatedAt());
        assertEquals("AI Knowledge Core v2", project.getName());
        assertEquals("Updated description", project.getDescription());
        assertEquals("100 MB", project.getMaxFileSize());
        assertEquals("pdf,docx", project.getAllowedFormats());
        assertEquals(0.5, project.getAiTemperature());
        assertEquals("Updated prompt", project.getAiSystemPrompt());

        verify(projectRepository, times(1)).save(project);
        verify(projectActivityRepository, times(1)).save(any(ProjectActivity.class));
    }

    @Test
    void testTransferOwnership_Success() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(projectMemberRepository.findByProjectIdAndUserEmail(100L, "owner@example.com"))
                .thenReturn(Optional.of(ownerMember));
        when(projectMemberRepository.findByProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        TransferOwnershipRequest request = TransferOwnershipRequest.builder()
                .newOwnerEmail("other@example.com")
                .build();

        assertDoesNotThrow(() -> projectService.transferOwnership(100L, "owner@example.com", request));

        assertEquals(otherUser, project.getOwner());
        assertEquals(ProjectMemberRoleEnum.ROLE_ADMIN, ownerMember.getRole());
        verify(projectMemberRepository, times(2)).save(any(ProjectMember.class));
        verify(projectRepository, times(1)).save(project);
    }

    @Test
    void testTransferOwnership_NotOwnerThrowsAccessDenied() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        ProjectMember regularMember = ProjectMember.builder()
                .project(project)
                .user(otherUser)
                .role(ProjectMemberRoleEnum.ROLE_MEMBER)
                .build();
        when(projectMemberRepository.findByProjectIdAndUserEmail(100L, "other@example.com"))
                .thenReturn(Optional.of(regularMember));

        TransferOwnershipRequest request = TransferOwnershipRequest.builder()
                .newOwnerEmail("owner@example.com")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                projectService.transferOwnership(100L, "other@example.com", request));
    }

    @Test
    void testArchiveProject_Success() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.findByProjectIdAndUserEmail(100L, "owner@example.com"))
                .thenReturn(Optional.of(ownerMember));

        assertDoesNotThrow(() -> projectService.archiveProject(100L, "owner@example.com"));

        assertEquals("archived", project.getStatus());
        verify(projectRepository, times(1)).save(project);
        verify(projectActivityRepository, times(1)).save(any(ProjectActivity.class));
    }

    @Test
    void testDeleteProject_WithConfirmationSuccess() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.findByProjectIdAndUserEmail(100L, "owner@example.com"))
                .thenReturn(Optional.of(ownerMember));

        DocumentMetadata doc = DocumentMetadata.builder()
                .id(1L)
                .projectId(100L)
                .s3Key("projects/100/docs/file.pdf")
                .build();
        when(documentRepository.findByProjectId(100L)).thenReturn(List.of(doc));

        DeleteProjectConfirmRequest request = DeleteProjectConfirmRequest.builder()
                .confirmationProjectName("AI Knowledge Core")
                .build();

        assertDoesNotThrow(() -> projectService.deleteProject(100L, "owner@example.com", request));

        verify(storageService, times(1)).deleteFile("projects/100/docs/file.pdf");
        verify(documentRepository, times(1)).deleteByProjectId(100L);
        verify(projectInviteRepository, times(1)).deleteByProjectId(100L);
        verify(projectActivityRepository, times(1)).deleteByProjectId(100L);
        verify(projectRepository, times(1)).delete(project);
    }

    @Test
    void testDeleteProject_WithMismatchedNameThrowsException() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.findByProjectIdAndUserEmail(100L, "owner@example.com"))
                .thenReturn(Optional.of(ownerMember));

        DeleteProjectConfirmRequest request = DeleteProjectConfirmRequest.builder()
                .confirmationProjectName("Wrong Name")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                projectService.deleteProject(100L, "owner@example.com", request));

        assertEquals("Tên dự án xác nhận không trùng khớp!", ex.getMessage());
        verify(projectRepository, never()).delete(any(Project.class));
    }
}
