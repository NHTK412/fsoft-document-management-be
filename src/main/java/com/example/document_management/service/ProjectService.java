package com.example.document_management.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.document_management.dto.request.ProjectCreateRequest;
import com.example.document_management.dto.request.ProjectUpdateRequest;
import com.example.document_management.dto.response.ProjectResponse;
import com.example.document_management.entity.Project;
import com.example.document_management.entity.ProjectInvite;
import com.example.document_management.entity.ProjectMember;
import com.example.document_management.entity.User;
import com.example.document_management.enums.ProjectInviteStatusEnum;
import com.example.document_management.enums.ProjectMemberRoleEnum;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.DocumentRepository;
import com.example.document_management.repository.ProjectInviteRepository;
import com.example.document_management.repository.ProjectMemberRepository;
import com.example.document_management.repository.ProjectRepository;
import com.example.document_management.entity.ProjectActivity;
import com.example.document_management.repository.ProjectActivityRepository;
import com.example.document_management.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final DocumentRepository documentRepository;
    private final ProjectInviteRepository projectInviteRepository;
    private final ProjectActivityRepository projectActivityRepository;

    private static final String[][] PALETTES = {
        {"#EEF2FF", "#4F46E5"}, // Indigo
        {"#E0F2FE", "#0284C7"}, // Sky
        {"#F3E8FF", "#9333EA"}, // Purple
        {"#FEF3C7", "#D97706"}, // Amber
        {"#FCE7F3", "#DB2777"}, // Pink
        {"#ECFDF5", "#059669"}  // Emerald
    };

    private static final String[] AVATAR_COLORS = {
        "#4F46E5", "#059669", "#D97706", "#7C3AED", "#0284C7", "#DC2626", "#DB2777"
    };

    @Transactional
    public ProjectResponse createProject(String email, ProjectCreateRequest projectCreateRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        String allowedFormatsStr = (projectCreateRequest.getAllowedFormats() != null && !projectCreateRequest.getAllowedFormats().isEmpty())
                ? String.join(",", projectCreateRequest.getAllowedFormats())
                : "pdf,docx,xlsx,pptx,md,txt,images";

        Project newProject = Project.builder()
                .name(projectCreateRequest.getEffectiveName())
                .description(projectCreateRequest.getDescription())
                .maxFileSize(projectCreateRequest.getMaxFileSize() != null ? projectCreateRequest.getMaxFileSize() : "50 MB")
                .allowedFormats(allowedFormatsStr)
                .status("active")
                .owner(user)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        newProject.addMember(user, ProjectMemberRoleEnum.ROLE_OWNER);
        Project savedProject = projectRepository.save(newProject);

        // Xử lý gửi lời mời thành viên ban đầu nếu có nhập danh sách email
        if (projectCreateRequest.getInviteEmails() != null && !projectCreateRequest.getInviteEmails().isBlank()) {
            String[] emails = projectCreateRequest.getInviteEmails().split(",");
            for (String invEmail : emails) {
                String cleanEmail = invEmail.trim();
                if (!cleanEmail.isBlank() && !cleanEmail.equalsIgnoreCase(email)) {
                    ProjectInvite invite = ProjectInvite.builder()
                            .project(savedProject)
                            .email(cleanEmail)
                            .role(ProjectMemberRoleEnum.ROLE_MEMBER)
                            .token(UUID.randomUUID().toString())
                            .status(ProjectInviteStatusEnum.PENDING)
                            .sentDate(Instant.now())
                            .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                            .build();
                    projectInviteRepository.save(invite);
                }
            }
        }

        try {
            projectActivityRepository.save(ProjectActivity.builder()
                    .project(savedProject)
                    .user(user)
                    .userAction((user.getFullName() != null ? user.getFullName() : "Người dùng") + " đã khởi tạo dự án")
                    .target(savedProject.getName())
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể lưu hoạt động tạo dự án: {}", e.getMessage());
        }

        return mapToProjectResponse(savedProject, ProjectMemberRoleEnum.ROLE_OWNER, 0L, 1L, 0L);
    }

    public List<ProjectResponse> getAllProjects(String email, String search, String role, String status) {
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String roleParam = (role != null && !role.trim().isEmpty()) ? role.trim().toLowerCase() : null;
        String statusParam = (status != null && !status.trim().isEmpty()) ? status.trim().toLowerCase() : null;

        List<Project> projects = projectRepository.searchProjectsByUser(email, searchParam, roleParam, statusParam);

        return projects.stream().map(project -> {
            ProjectMemberRoleEnum currentUserRole = projectMemberRepository
                    .findByProjectIdAndUserEmail(project.getId(), email)
                    .map(ProjectMember::getRole)
                    .orElse(null);

            long totalFiles = documentRepository.countByProjectId(project.getId());
            long totalMembers = projectMemberRepository.countByProjectId(project.getId());
            long storageUsedBytes = documentRepository.sumFileSizeByProjectId(project.getId());

            return mapToProjectResponse(project, currentUserRole, totalFiles, totalMembers, storageUsedBytes);
        }).toList();
    }

    public Page<ProjectResponse> getAllProjectByUser(String email, Pageable pageable) {
        Page<Project> projects = projectRepository.findAllByMemberEmail(email, pageable);

        return projects.map(project -> {
            ProjectMemberRoleEnum currentUserRole = projectMemberRepository
                    .findByProjectIdAndUserEmail(project.getId(), email)
                    .map(ProjectMember::getRole)
                    .orElse(null);

            long totalFiles = documentRepository.countByProjectId(project.getId());
            long totalMembers = projectMemberRepository.countByProjectId(project.getId());
            long storageUsedBytes = documentRepository.sumFileSizeByProjectId(project.getId());

            return mapToProjectResponse(project, currentUserRole, totalFiles, totalMembers, storageUsedBytes);
        });
    }

    public ProjectResponse getProjectById(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserEmail(projectId, email);
        boolean isSystemAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;

        if (memberOpt.isEmpty() && !isSystemAdmin) {
            throw new AccessDeniedException("Bạn không có quyền truy cập vào dự án này!");
        }

        ProjectMemberRoleEnum currentUserRole = memberOpt.map(ProjectMember::getRole).orElse(null);
        long totalFiles = documentRepository.countByProjectId(projectId);
        long totalMembers = projectMemberRepository.countByProjectId(projectId);
        long storageUsedBytes = documentRepository.sumFileSizeByProjectId(projectId);

        return mapToProjectResponse(project, currentUserRole, totalFiles, totalMembers, storageUsedBytes);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, String email, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserEmail(projectId, email);
        boolean isOwner = memberOpt.isPresent() && memberOpt.get().getRole() == ProjectMemberRoleEnum.ROLE_OWNER;
        boolean isSystemAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;

        if (!isOwner && !isSystemAdmin) {
            throw new AccessDeniedException("Chỉ chủ dự án (Project Owner) mới có quyền cập nhật thông tin dự án!");
        }

        project.setName(request.getEffectiveName());
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            project.setStatus(request.getStatus().trim());
        }
        if (request.getMaxFileSize() != null && !request.getMaxFileSize().isBlank()) {
            project.setMaxFileSize(request.getMaxFileSize().trim());
        }
        if (request.getAllowedFormats() != null && !request.getAllowedFormats().isEmpty()) {
            project.setAllowedFormats(String.join(",", request.getAllowedFormats()));
        }
        project.setUpdatedAt(Instant.now());

        Project updated = projectRepository.save(project);

        long totalFiles = documentRepository.countByProjectId(projectId);
        long totalMembers = projectMemberRepository.countByProjectId(projectId);
        long storageUsedBytes = documentRepository.sumFileSizeByProjectId(projectId);
        ProjectMemberRoleEnum currentUserRole = memberOpt.map(ProjectMember::getRole).orElse(null);

        return mapToProjectResponse(updated, currentUserRole, totalFiles, totalMembers, storageUsedBytes);
    }

    @Transactional
    public void deleteProject(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserEmail(projectId, email);
        boolean isOwner = memberOpt.isPresent() && memberOpt.get().getRole() == ProjectMemberRoleEnum.ROLE_OWNER;
        boolean isSystemAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;

        if (!isOwner && !isSystemAdmin) {
            throw new AccessDeniedException("Chỉ chủ dự án (Project Owner) hoặc Admin hệ thống mới có quyền xóa dự án!");
        }

        documentRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }

    private ProjectResponse mapToProjectResponse(
            Project project,
            ProjectMemberRoleEnum currentUserRole,
            Long totalFiles,
            Long totalMembers,
            Long storageUsedBytes) {

        int paletteIndex = (int) (Math.abs(project.getId() != null ? project.getId() : 0) % PALETTES.length);
        String iconBg = PALETTES[paletteIndex][0];
        String iconColor = PALETTES[paletteIndex][1];

        // Tạo danh sách màu avatar mẫu cho thành viên
        int count = (int) Math.min(totalMembers != null ? totalMembers : 1, 4);
        List<String> avatars = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            avatars.add(AVATAR_COLORS[(paletteIndex + i) % AVATAR_COLORS.length]);
        }
        int extraMembers = (totalMembers != null && totalMembers > avatars.size())
                ? (int) (totalMembers - avatars.size())
                : 0;

        Instant updateTime = project.getUpdatedAt() != null ? project.getUpdatedAt() : project.getCreatedAt();
        String storageUsedFormatted = formatBytes(storageUsedBytes != null ? storageUsedBytes : 0L);
        String storageLimitFormatted = formatBytes(project.getStorageLimitBytes() != null ? project.getStorageLimitBytes() : 10737418240L);

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwner() != null ? project.getOwner().getId() : null)
                .currentUserRole(currentUserRole)
                .status(project.getStatus() != null ? project.getStatus() : "active")
                .totalFiles(totalFiles)
                .totalMembers(totalMembers)
                .storageUsed(storageUsedFormatted)
                .storageUsedBytes(storageUsedBytes)
                .storageLimit(storageLimitFormatted)
                .storageLimitBytes(project.getStorageLimitBytes())
                .iconBg(iconBg)
                .iconColor(iconColor)
                .avatars(avatars)
                .extraMembers(extraMembers)
                .createdAt(project.getCreatedAt())
                .updatedAt(updateTime)
                .build();
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
