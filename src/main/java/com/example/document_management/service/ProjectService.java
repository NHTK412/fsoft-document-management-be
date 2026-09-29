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

import com.example.document_management.dto.request.DeleteProjectConfirmRequest;
import com.example.document_management.dto.request.ProjectCreateRequest;
import com.example.document_management.dto.request.ProjectUpdateRequest;
import com.example.document_management.dto.request.TransferOwnershipRequest;
import com.example.document_management.dto.request.UpdateProjectSettingsRequest;
import com.example.document_management.dto.response.AiPersonaDto;
import com.example.document_management.dto.response.ProjectResponse;
import com.example.document_management.dto.response.ProjectSettingsResponse;
import com.example.document_management.dto.response.UpdateProjectSettingsResponse;
import com.example.document_management.entity.DocumentMetadata;
import com.example.document_management.entity.Project;
import java.time.format.DateTimeFormatter;
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
    private final StorageService storageService;
    private final com.example.document_management.repository.ChatSessionRepository chatSessionRepository;

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

    // -------------------------------------------------------------
    // PROJECT SETTINGS ENDPOINTS (Screen 3.7: ProjectSettings.jsx)
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public ProjectSettingsResponse getProjectSettings(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserEmail(projectId, email);
        boolean isMember = memberOpt.isPresent();
        boolean isSystemAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;

        if (!isMember && !isSystemAdmin) {
            throw new AccessDeniedException("Bạn không có quyền truy cập vào cài đặt của dự án này!");
        }

        Long storageUsedBytes = documentRepository.sumFileSizeByProjectId(projectId);
        List<String> allowedFormats = new ArrayList<>();
        if (project.getAllowedFormats() != null && !project.getAllowedFormats().isBlank()) {
            for (String f : project.getAllowedFormats().split(",")) {
                if (!f.trim().isBlank()) {
                    allowedFormats.add(f.trim());
                }
            }
        }

        AiPersonaDto aiPersona = AiPersonaDto.builder()
                .temperature(project.getAiTemperature() != null ? project.getAiTemperature() : 0.2)
                .systemPrompt(project.getAiSystemPrompt())
                .build();

        return ProjectSettingsResponse.builder()
                .projectName(project.getName())
                .projectDesc(project.getDescription())
                .logoUrl(project.getLogoUrl())
                .minioBucket(storageService.getBucketName())
                .storageUsedBytes(storageUsedBytes != null ? storageUsedBytes : 0L)
                .storageLimitBytes(project.getStorageLimitBytes() != null ? project.getStorageLimitBytes() : 10737418240L)
                .maxFileSize(project.getMaxFileSize() != null ? project.getMaxFileSize() : "50 MB")
                .allowedFormats(allowedFormats)
                .aiPersona(aiPersona)
                .build();
    }

    @Transactional
    public UpdateProjectSettingsResponse updateProjectSettings(Long projectId, String email, UpdateProjectSettingsRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserEmail(projectId, email);
        boolean isOwnerOrAdmin = memberOpt.isPresent() &&
                (memberOpt.get().getRole() == ProjectMemberRoleEnum.ROLE_OWNER || memberOpt.get().getRole() == ProjectMemberRoleEnum.ROLE_ADMIN);
        boolean isSystemAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;

        if (!isOwnerOrAdmin && !isSystemAdmin) {
            throw new AccessDeniedException("Chỉ chủ dự án (Project Owner) hoặc Quản trị viên (Admin) mới có quyền cập nhật cấu hình dự án!");
        }

        if (request.getProjectName() != null && !request.getProjectName().isBlank()) {
            project.setName(request.getProjectName().trim());
        }
        if (request.getProjectDesc() != null) {
            project.setDescription(request.getProjectDesc().trim());
        }
        if (request.getMaxFileSize() != null && !request.getMaxFileSize().isBlank()) {
            project.setMaxFileSize(request.getMaxFileSize().trim());
        }
        if (request.getAllowedFormats() != null) {
            project.setAllowedFormats(String.join(",", request.getAllowedFormats()));
        }
        if (request.getAiPersona() != null) {
            if (request.getAiPersona().getTemperature() != null) {
                project.setAiTemperature(request.getAiPersona().getTemperature());
            }
            if (request.getAiPersona().getSystemPrompt() != null) {
                project.setAiSystemPrompt(request.getAiPersona().getSystemPrompt());
            }
        }

        Instant now = Instant.now();
        project.setUpdatedAt(now);
        projectRepository.save(project);

        projectActivityRepository.save(ProjectActivity.builder()
                .project(project)
                .user(user)
                .userAction("Cập nhật cấu hình dự án")
                .target(project.getName())
                .createdAt(now)
                .build());

        return UpdateProjectSettingsResponse.builder()
                .updatedAt(DateTimeFormatter.ISO_INSTANT.format(now))
                .build();
    }

    @Transactional
    public void transferOwnership(Long projectId, String email, TransferOwnershipRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Optional<ProjectMember> currentMemberOpt = projectMemberRepository.findByProjectIdAndUserEmail(projectId, email);
        boolean isOwner = currentMemberOpt.isPresent() && currentMemberOpt.get().getRole() == ProjectMemberRoleEnum.ROLE_OWNER;
        boolean isSystemAdmin = currentUser.getRole() == UserRoleEnum.ROLE_ADMIN;

        if (!isOwner && !isSystemAdmin) {
            throw new AccessDeniedException("Chỉ chủ sở hữu dự án (Project Owner) mới có quyền chuyển nhượng dự án!");
        }

        User newOwner = userRepository.findByEmail(request.getNewOwnerEmail().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + request.getNewOwnerEmail()));

        if (newOwner.getId().equals(project.getOwner().getId())) {
            throw new IllegalArgumentException("Người dùng này đã là chủ sở hữu của dự án!");
        }

        // Cập nhật vai trò của owner cũ xuống ROLE_ADMIN
        if (currentMemberOpt.isPresent()) {
            ProjectMember oldOwnerMember = currentMemberOpt.get();
            oldOwnerMember.setRole(ProjectMemberRoleEnum.ROLE_ADMIN);
            projectMemberRepository.save(oldOwnerMember);
        }

        // Cập nhật hoặc thêm vai trò ROLE_OWNER cho owner mới
        Optional<ProjectMember> newOwnerMemberOpt = projectMemberRepository.findByProjectIdAndUserId(projectId, newOwner.getId());
        if (newOwnerMemberOpt.isPresent()) {
            ProjectMember newOwnerMember = newOwnerMemberOpt.get();
            newOwnerMember.setRole(ProjectMemberRoleEnum.ROLE_OWNER);
            projectMemberRepository.save(newOwnerMember);
        } else {
            ProjectMember newMember = ProjectMember.builder()
                    .project(project)
                    .user(newOwner)
                    .role(ProjectMemberRoleEnum.ROLE_OWNER)
                    .joinedAt(Instant.now())
                    .build();
            projectMemberRepository.save(newMember);
        }

        project.setOwner(newOwner);
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);

        projectActivityRepository.save(ProjectActivity.builder()
                .project(project)
                .user(currentUser)
                .userAction("Chuyển nhượng quyền chủ dự án")
                .target(newOwner.getFullName() != null ? newOwner.getFullName() : newOwner.getEmail())
                .createdAt(Instant.now())
                .build());
    }

    @Transactional
    public void archiveProject(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với id: " + projectId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserEmail(projectId, email);
        boolean isOwnerOrAdmin = memberOpt.isPresent() &&
                (memberOpt.get().getRole() == ProjectMemberRoleEnum.ROLE_OWNER || memberOpt.get().getRole() == ProjectMemberRoleEnum.ROLE_ADMIN);
        boolean isSystemAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;

        if (!isOwnerOrAdmin && !isSystemAdmin) {
            throw new AccessDeniedException("Chỉ chủ dự án (Project Owner) hoặc Quản trị viên (Admin) mới có quyền lưu trữ dự án!");
        }

        project.setStatus("archived");
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);

        projectActivityRepository.save(ProjectActivity.builder()
                .project(project)
                .user(user)
                .userAction("Lưu trữ dự án (Read-only)")
                .target(project.getName())
                .createdAt(Instant.now())
                .build());
    }

    @Transactional
    public void deleteProject(Long projectId, String email, DeleteProjectConfirmRequest request) {
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

        if (request != null && request.getConfirmationProjectName() != null && !request.getConfirmationProjectName().isBlank()) {
            if (!request.getConfirmationProjectName().trim().equals(project.getName().trim())) {
                throw new IllegalArgumentException("Tên dự án xác nhận không trùng khớp!");
            }
        }

        // Xóa các file trên MinIO
        List<DocumentMetadata> docs = documentRepository.findByProjectId(projectId);
        for (DocumentMetadata doc : docs) {
            if (doc.getS3Key() != null && !doc.getS3Key().isBlank()) {
                try {
                    storageService.deleteFile(doc.getS3Key());
                } catch (Exception e) {
                    log.warn("Không thể xóa file MinIO '{}': {}", doc.getS3Key(), e.getMessage());
                }
            }
        }

        documentRepository.deleteByProjectId(projectId);
        projectInviteRepository.deleteByProjectId(projectId);
        projectActivityRepository.deleteByProjectId(projectId);
        chatSessionRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }

    @Transactional
    public void deleteProject(Long projectId, String email) {
        deleteProject(projectId, email, null);
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
