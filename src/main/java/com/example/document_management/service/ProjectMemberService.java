package com.example.document_management.service;

import com.example.document_management.dto.request.ProjectInviteRequest;
import com.example.document_management.dto.request.UpdateMemberRoleRequest;
import com.example.document_management.dto.response.*;
import com.example.document_management.entity.Project;
import com.example.document_management.entity.ProjectActivity;
import com.example.document_management.entity.ProjectInvite;
import com.example.document_management.entity.ProjectMember;
import com.example.document_management.entity.User;
import com.example.document_management.enums.ProjectInviteStatusEnum;
import com.example.document_management.enums.ProjectMemberRoleEnum;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInviteRepository projectInviteRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ProjectActivityRepository projectActivityRepository;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);

    private static final String[] AVATAR_COLORS = {
            "#4F46E5", "#059669", "#D97706", "#DB2777", "#0284C7", "#9333EA", "#DC2626", "#2563EB"
    };

    /**
     * Lấy danh sách thành viên hiện tại của dự án
     */
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(Long projectId, String search, String currentUserEmail) {
        Project project = getProjectAndValidateMemberAccess(projectId, currentUserEmail);

        String searchKeyword = (search != null && !search.isBlank()) ? search.trim() : null;
        List<ProjectMember> members = projectMemberRepository.searchMembers(projectId, searchKeyword);

        List<ProjectMemberResponse> responseList = new ArrayList<>();
        for (ProjectMember member : members) {
            User u = member.getUser();
            int colorIdx = u != null && u.getId() != null ? (int) (Math.abs(u.getId()) % AVATAR_COLORS.length) : 0;
            String avatarBg = AVATAR_COLORS[colorIdx];

            String name = u != null && u.getFullName() != null && !u.getFullName().isBlank()
                    ? u.getFullName()
                    : (u != null ? u.getEmail() : "Thành viên");
            String initial = extractInitials(name);

            Instant joined = member.getJoinedAt() != null ? member.getJoinedAt() : project.getCreatedAt();
            String joinedDateStr = DATE_FORMATTER.format(joined);

            long uploadCount = (u != null) ? documentRepository.countByProjectIdAndUploaderId(projectId, u.getId()) : 0L;
            String contributions = uploadCount + " tệp tải lên";

            responseList.add(ProjectMemberResponse.builder()
                    .id(member.getId())
                    .name(name)
                    .email(u != null ? u.getEmail() : "")
                    .role(formatRole(member.getRole()))
                    .avatarBg(avatarBg)
                    .initial(initial)
                    .joinedDate(joinedDateStr)
                    .joinedAt(joined)
                    .contributions(contributions)
                    .build());
        }

        return responseList;
    }

    /**
     * Lấy danh sách lời mời đang chờ (Pending Invites)
     */
    @Transactional(readOnly = true)
    public List<ProjectPendingInviteResponse> getPendingInvites(Long projectId, String currentUserEmail) {
        getProjectAndValidateMemberAccess(projectId, currentUserEmail);

        List<ProjectInvite> invites = projectInviteRepository.findByProjectIdAndStatus(projectId, ProjectInviteStatusEnum.PENDING);

        List<ProjectPendingInviteResponse> responseList = new ArrayList<>();
        Instant now = Instant.now();

        for (ProjectInvite inv : invites) {
            String sentDateStr = inv.getSentDate() != null ? DATE_FORMATTER.format(inv.getSentDate()) : DATE_FORMATTER.format(now);
            String expiresInStr;
            if (inv.getExpiresAt() != null) {
                if (now.isAfter(inv.getExpiresAt())) {
                    expiresInStr = "Hết hạn";
                } else {
                    long days = Duration.between(now, inv.getExpiresAt()).toDays();
                    expiresInStr = (days + 1) + " ngày";
                }
            } else {
                expiresInStr = "7 ngày";
            }

            responseList.add(ProjectPendingInviteResponse.builder()
                    .id(inv.getId())
                    .email(inv.getEmail())
                    .role(formatRole(inv.getRole()))
                    .sentDate(sentDateStr)
                    .expiresIn(expiresInStr)
                    .expiresAt(inv.getExpiresAt())
                    .build());
        }

        return responseList;
    }

    /**
     * Gửi lời mời thành viên mới vào dự án
     */
    @Transactional
    public ProjectInviteResponse inviteMember(Long projectId, ProjectInviteRequest request, String currentUserEmail) {
        Project project = getProjectAndValidateManagerAccess(projectId, currentUserEmail);
        User currentUser = getAuthenticatedUser(currentUserEmail);

        String email = request.getEmail().trim().toLowerCase();
        if (email.equalsIgnoreCase(currentUserEmail)) {
            throw new IllegalArgumentException("Bạn không thể tự gửi lời mời cho chính mình!");
        }

        if (projectMemberRepository.existsByProjectIdAndUserEmail(projectId, email)) {
            throw new IllegalArgumentException("Người dùng với email '" + email + "' đã là thành viên trong dự án này!");
        }

        ProjectMemberRoleEnum role = parseRole(request.getRole());
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(7 * 24 * 3600); // 7 ngày

        Optional<ProjectInvite> existingInvite = projectInviteRepository.findByProjectIdAndEmail(projectId, email);
        ProjectInvite invite;
        if (existingInvite.isPresent()) {
            invite = existingInvite.get();
            invite.setRole(role);
            invite.setStatus(ProjectInviteStatusEnum.PENDING);
            invite.setToken(UUID.randomUUID().toString());
            invite.setSentDate(now);
            invite.setExpiresAt(expiresAt);
        } else {
            invite = ProjectInvite.builder()
                    .project(project)
                    .email(email)
                    .role(role)
                    .token(UUID.randomUUID().toString())
                    .status(ProjectInviteStatusEnum.PENDING)
                    .sentDate(now)
                    .expiresAt(expiresAt)
                    .build();
        }

        ProjectInvite saved = projectInviteRepository.save(invite);

        // Ghi nhận hoạt động
        try {
            projectActivityRepository.save(ProjectActivity.builder()
                    .project(project)
                    .user(currentUser)
                    .userAction((currentUser.getFullName() != null ? currentUser.getFullName() : "Quản trị viên")
                            + " đã gửi lời mời tham gia dự án")
                    .target(email)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể lưu hoạt động gửi lời mời: {}", e.getMessage());
        }

        return ProjectInviteResponse.builder()
                .inviteId(saved.getId())
                .email(saved.getEmail())
                .role(formatRole(saved.getRole()))
                .expiresAt(saved.getExpiresAt())
                .build();
    }

    /**
     * Cập nhật vai trò thành viên trong dự án
     */
    @Transactional
    public UpdateMemberRoleResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request, String currentUserEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        User currentUser = getAuthenticatedUser(currentUserEmail);
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên với ID: " + memberId));

        if (!member.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Thành viên không thuộc dự án có ID: " + projectId);
        }

        // Không cho phép thay đổi vai trò của Project Owner
        if (member.getRole() == ProjectMemberRoleEnum.ROLE_OWNER
                || (project.getOwner() != null && project.getOwner().getId().equals(member.getUser().getId()))) {
            throw new IllegalArgumentException("Không thể thay đổi vai trò của Chủ dự án (Project Owner)!");
        }

        boolean isSysAdmin = currentUser.getRole() == UserRoleEnum.ROLE_ADMIN;
        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(currentUser.getId());
        ProjectMemberRoleEnum callerRole = projectMemberRepository.findByProjectIdAndUserEmail(projectId, currentUserEmail)
                .map(ProjectMember::getRole)
                .orElse(null);

        boolean isProjectAdmin = callerRole == ProjectMemberRoleEnum.ROLE_ADMIN;

        if (!isSysAdmin && !isOwner && !isProjectAdmin) {
            throw new AccessDeniedException("Chỉ Chủ dự án hoặc Quản trị viên mới có quyền cập nhật vai trò thành viên!");
        }

        ProjectMemberRoleEnum newRole = parseRole(request.getRole());

        // Quản trị viên thông thường không được phép thăng cấp người khác lên OWNER hoặc tự gán quyền vượt cấp
        if (newRole == ProjectMemberRoleEnum.ROLE_OWNER && !isOwner && !isSysAdmin) {
            throw new AccessDeniedException("Chỉ Chủ dự án mới có quyền chuyển giao quyền sở hữu dự án!");
        }

        member.setRole(newRole);
        projectMemberRepository.save(member);

        // Ghi nhận hoạt động
        try {
            projectActivityRepository.save(ProjectActivity.builder()
                    .project(project)
                    .user(currentUser)
                    .userAction((currentUser.getFullName() != null ? currentUser.getFullName() : "Quản trị viên")
                            + " đã cập nhật vai trò của " + member.getUser().getFullName())
                    .target(formatRole(newRole))
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể lưu hoạt động cập nhật vai trò: {}", e.getMessage());
        }

        return UpdateMemberRoleResponse.builder()
                .memberId(member.getId())
                .role(formatRole(newRole))
                .build();
    }

    /**
     * Gửi lại email lời mời tham gia
     */
    @Transactional
    public void resendInvite(Long projectId, Long inviteId, String currentUserEmail) {
        Project project = getProjectAndValidateManagerAccess(projectId, currentUserEmail);
        User currentUser = getAuthenticatedUser(currentUserEmail);

        ProjectInvite invite = projectInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời với ID: " + inviteId));

        if (!invite.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Lời mời không thuộc dự án này!");
        }

        invite.setSentDate(Instant.now());
        invite.setExpiresAt(Instant.now().plusSeconds(7 * 24 * 3600));
        invite.setToken(UUID.randomUUID().toString());
        invite.setStatus(ProjectInviteStatusEnum.PENDING);
        projectInviteRepository.save(invite);

        try {
            projectActivityRepository.save(ProjectActivity.builder()
                    .project(project)
                    .user(currentUser)
                    .userAction((currentUser.getFullName() != null ? currentUser.getFullName() : "Quản trị viên")
                            + " đã gửi lại lời mời tham gia dự án")
                    .target(invite.getEmail())
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể lưu hoạt động gửi lại lời mời: {}", e.getMessage());
        }
    }

    /**
     * Hủy lời mời đang chờ xử lý
     */
    @Transactional
    public void cancelInvite(Long projectId, Long inviteId, String currentUserEmail) {
        Project project = getProjectAndValidateManagerAccess(projectId, currentUserEmail);
        User currentUser = getAuthenticatedUser(currentUserEmail);

        ProjectInvite invite = projectInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời với ID: " + inviteId));

        if (!invite.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Lời mời không thuộc dự án này!");
        }

        String inviteEmail = invite.getEmail();
        projectInviteRepository.delete(invite);

        try {
            projectActivityRepository.save(ProjectActivity.builder()
                    .project(project)
                    .user(currentUser)
                    .userAction((currentUser.getFullName() != null ? currentUser.getFullName() : "Quản trị viên")
                            + " đã hủy lời mời tham gia dự án")
                    .target(inviteEmail)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể lưu hoạt động hủy lời mời: {}", e.getMessage());
        }
    }

    /**
     * Xóa thành viên khỏi dự án
     */
    @Transactional
    public void removeMember(Long projectId, Long memberId, String currentUserEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        User currentUser = getAuthenticatedUser(currentUserEmail);
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên với ID: " + memberId));

        if (!member.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Thành viên không thuộc dự án có ID: " + projectId);
        }

        // Không cho phép xóa Project Owner
        if (member.getRole() == ProjectMemberRoleEnum.ROLE_OWNER
                || (project.getOwner() != null && project.getOwner().getId().equals(member.getUser().getId()))) {
            throw new IllegalArgumentException("Không thể xóa Chủ dự án (Project Owner) khỏi dự án!");
        }

        boolean isSysAdmin = currentUser.getRole() == UserRoleEnum.ROLE_ADMIN;
        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(currentUser.getId());
        ProjectMemberRoleEnum callerRole = projectMemberRepository.findByProjectIdAndUserEmail(projectId, currentUserEmail)
                .map(ProjectMember::getRole)
                .orElse(null);

        boolean isProjectAdmin = callerRole == ProjectMemberRoleEnum.ROLE_ADMIN;

        if (!isSysAdmin && !isOwner && !isProjectAdmin) {
            throw new AccessDeniedException("Chỉ Chủ dự án hoặc Quản trị viên mới có quyền xóa thành viên!");
        }

        // Admin dự án không được xóa Admin khác
        if (isProjectAdmin && !isOwner && !isSysAdmin && member.getRole() == ProjectMemberRoleEnum.ROLE_ADMIN) {
            throw new AccessDeniedException("Chỉ Chủ dự án mới có quyền xóa thành viên có vai trò Admin!");
        }

        String removedUserName = member.getUser() != null ? member.getUser().getFullName() : "Thành viên";
        projectMemberRepository.delete(member);

        try {
            projectActivityRepository.save(ProjectActivity.builder()
                    .project(project)
                    .user(currentUser)
                    .userAction((currentUser.getFullName() != null ? currentUser.getFullName() : "Quản trị viên")
                            + " đã xóa thành viên khỏi dự án")
                    .target(removedUserName)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể lưu hoạt động xóa thành viên: {}", e.getMessage());
        }
    }

    private Project getProjectAndValidateMemberAccess(Long projectId, String currentUserEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        User user = getAuthenticatedUser(currentUserEmail);
        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());
        boolean isSysAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserEmail(projectId, currentUserEmail);

        if (!isOwner && !isSysAdmin && !isMember) {
            throw new AccessDeniedException("Bạn không có quyền truy cập vào thông tin thành viên dự án này!");
        }

        return project;
    }

    private Project getProjectAndValidateManagerAccess(Long projectId, String currentUserEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        User user = getAuthenticatedUser(currentUserEmail);
        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());
        boolean isSysAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;
        ProjectMemberRoleEnum callerRole = projectMemberRepository.findByProjectIdAndUserEmail(projectId, currentUserEmail)
                .map(ProjectMember::getRole)
                .orElse(null);

        boolean isProjectAdmin = callerRole == ProjectMemberRoleEnum.ROLE_ADMIN;

        if (!isOwner && !isSysAdmin && !isProjectAdmin) {
            throw new AccessDeniedException("Chỉ Chủ dự án hoặc Quản trị viên mới có quyền thực hiện thao tác này!");
        }

        return project;
    }

    private User getAuthenticatedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng!"));
    }

    private String formatRole(ProjectMemberRoleEnum role) {
        if (role == null) return "Member";
        if (role == ProjectMemberRoleEnum.ROLE_OWNER) return "Project Owner";
        if (role == ProjectMemberRoleEnum.ROLE_ADMIN) return "Admin";
        if (role == ProjectMemberRoleEnum.ROLE_VIEWER) return "Viewer";
        return "Member";
    }

    private ProjectMemberRoleEnum parseRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return ProjectMemberRoleEnum.ROLE_MEMBER;
        }
        String cleaned = roleStr.trim().toUpperCase();
        if (cleaned.contains("OWNER")) {
            return ProjectMemberRoleEnum.ROLE_OWNER;
        } else if (cleaned.contains("ADMIN")) {
            return ProjectMemberRoleEnum.ROLE_ADMIN;
        } else if (cleaned.contains("VIEWER")) {
            return ProjectMemberRoleEnum.ROLE_VIEWER;
        } else {
            return ProjectMemberRoleEnum.ROLE_MEMBER;
        }
    }

    private String extractInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "U" : last.substring(0, 1).toUpperCase();
    }
}
