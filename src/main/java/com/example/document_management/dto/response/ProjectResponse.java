package com.example.document_management.dto.response;

import com.example.document_management.enums.ProjectMemberRoleEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;

    private String name;

    private String description;

    private Long ownerId;

    private ProjectMemberRoleEnum currentUserRole;

    private Long totalFiles;

    private Long totalMembers;
}
