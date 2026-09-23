package com.example.document_management.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.document_management.dto.request.ProjectCreateRequest;
import com.example.document_management.dto.request.ProjectUpdateRequest;
import com.example.document_management.dto.response.ProjectResponse;
import com.example.document_management.entity.Project;
import com.example.document_management.entity.ProjectMember;
import com.example.document_management.entity.User;
import com.example.document_management.enums.ProjectMemberRoleEnum;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.DocumentRepository;
import com.example.document_management.repository.ProjectMemberRepository;
import com.example.document_management.repository.ProjectRepository;
import com.example.document_management.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public ProjectResponse createProject(String email, ProjectCreateRequest projectCreateRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Project newProject = Project.builder()
                .name(projectCreateRequest.getName())
                .description(projectCreateRequest.getDescription())
                .owner(user)
                .build();

        newProject.addMember(user, ProjectMemberRoleEnum.ROLE_OWNER);

        Project savedProject = projectRepository.save(newProject);
        return mapToProjectResponse(savedProject, ProjectMemberRoleEnum.ROLE_OWNER, 0L);
    }

    public org.springframework.data.domain.Page<ProjectResponse> getAllProjectByUser(String email, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Project> projects = projectRepository.findAllByMemberEmail(email, pageable);

        return projects.map(project -> {
            ProjectMemberRoleEnum currentUserRole = projectMemberRepository
                    .findByProjectIdAndUserEmail(project.getId(), email)
                    .map(ProjectMember::getRole)
                    .orElse(null);

            long totalFiles = documentRepository.countByProjectId(project.getId());
            long totalMembers = projectMemberRepository.countByProjectId(project.getId());
            return mapToProjectResponse(project, currentUserRole, totalFiles, totalMembers);
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

        return mapToProjectResponse(project, currentUserRole, totalFiles);
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

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        Project updated = projectRepository.save(project);

        long totalFiles = documentRepository.countByProjectId(projectId);
        ProjectMemberRoleEnum currentUserRole = memberOpt.map(ProjectMember::getRole).orElse(null);

        return mapToProjectResponse(updated, currentUserRole, totalFiles);
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

    private ProjectResponse mapToProjectResponse(Project project, ProjectMemberRoleEnum currentUserRole, Long totalFiles, Long totalMembers) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwner() != null ? project.getOwner().getId() : null)
                .currentUserRole(currentUserRole)
                .totalFiles(totalFiles)
                .totalMembers(totalMembers)
                .build();
    }

    private ProjectResponse mapToProjectResponse(Project project, ProjectMemberRoleEnum currentUserRole, Long totalFiles) {
        long totalMembers = projectMemberRepository.countByProjectId(project.getId());
        return mapToProjectResponse(project, currentUserRole, totalFiles, totalMembers);
    }
}
