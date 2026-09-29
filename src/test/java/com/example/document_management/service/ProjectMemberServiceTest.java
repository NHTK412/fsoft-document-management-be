package com.example.document_management.service;

import com.example.document_management.dto.request.ProjectInviteRequest;
import com.example.document_management.dto.request.UpdateMemberRoleRequest;
import com.example.document_management.dto.response.*;
import com.example.document_management.entity.*;
import com.example.document_management.enums.ProjectInviteStatusEnum;
import com.example.document_management.enums.ProjectMemberRoleEnum;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInviteRepository projectInviteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProjectActivityRepository projectActivityRepository;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private User owner;
    private User memberUser;
    private Project project;
    private ProjectMember member;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("owner@example.com")
                .fullName("Owner Test")
                .role(UserRoleEnum.ROLE_USER)
                .build();

        memberUser = User.builder()
                .id(2L)
                .email("member@example.com")
                .fullName("Member Test")
                .role(UserRoleEnum.ROLE_USER)
                .build();

        project = Project.builder()
                .id(10L)
                .name("Member Management Project")
                .owner(owner)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        member = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(memberUser)
                .role(ProjectMemberRoleEnum.ROLE_MEMBER)
                .joinedAt(Instant.now())
                .build();
    }

    @Test
    void testGetMembers_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.searchMembers(eq(10L), isNull())).thenReturn(List.of(member));
        when(documentRepository.countByProjectIdAndUploaderId(10L, 2L)).thenReturn(5L);

        List<ProjectMemberResponse> responses = projectMemberService.getMembers(10L, null, "owner@example.com");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Member Test", responses.get(0).getName());
        assertEquals("member@example.com", responses.get(0).getEmail());
        assertEquals("Member", responses.get(0).getRole());
        assertEquals("5 tệp tải lên", responses.get(0).getContributions());
    }

    @Test
    void testGetPendingInvites_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        ProjectInvite invite = ProjectInvite.builder()
                .id(50L)
                .project(project)
                .email("invitee@example.com")
                .role(ProjectMemberRoleEnum.ROLE_ADMIN)
                .sentDate(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3 * 24 * 3600))
                .status(ProjectInviteStatusEnum.PENDING)
                .build();

        when(projectInviteRepository.findByProjectIdAndStatus(10L, ProjectInviteStatusEnum.PENDING))
                .thenReturn(List.of(invite));

        List<ProjectPendingInviteResponse> responses = projectMemberService.getPendingInvites(10L, "owner@example.com");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("invitee@example.com", responses.get(0).getEmail());
        assertEquals("Admin", responses.get(0).getRole());
    }

    @Test
    void testInviteMember_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.existsByProjectIdAndUserEmail(10L, "new@example.com")).thenReturn(false);
        when(projectInviteRepository.findByProjectIdAndEmail(10L, "new@example.com")).thenReturn(Optional.empty());

        when(projectInviteRepository.save(any(ProjectInvite.class))).thenAnswer(inv -> {
            ProjectInvite pi = inv.getArgument(0);
            pi.setId(77L);
            return pi;
        });

        ProjectInviteRequest request = ProjectInviteRequest.builder()
                .email("new@example.com")
                .role("Member")
                .build();

        ProjectInviteResponse response = projectMemberService.inviteMember(10L, request, "owner@example.com");

        assertNotNull(response);
        assertEquals(77L, response.getInviteId());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("Member", response.getRole());
    }

    @Test
    void testUpdateMemberRole_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.findById(100L)).thenReturn(Optional.of(member));

        UpdateMemberRoleRequest request = UpdateMemberRoleRequest.builder()
                .role("Admin")
                .build();

        UpdateMemberRoleResponse response = projectMemberService.updateMemberRole(10L, 100L, request, "owner@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getMemberId());
        assertEquals("Admin", response.getRole());
        assertEquals(ProjectMemberRoleEnum.ROLE_ADMIN, member.getRole());
    }

    @Test
    void testRemoveMember_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.findById(100L)).thenReturn(Optional.of(member));

        projectMemberService.removeMember(10L, 100L, "owner@example.com");

        verify(projectMemberRepository, times(1)).delete(member);
    }
}
