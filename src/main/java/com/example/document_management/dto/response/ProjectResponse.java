package com.example.document_management.dto.response;

import com.example.document_management.enums.ProjectMemberRoleEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;

    private String name;

    private String logoUrl;

    @JsonProperty("title")
    public String getTitle() {
        return name;
    }

    private String description;

    @JsonProperty("desc")
    public String getDesc() {
        return description;
    }

    private Long ownerId;
    private String ownerName;
    private String ownerEmail;

    private ProjectMemberRoleEnum currentUserRole;

    @JsonProperty("role")
    public String getRole() {
        if (currentUserRole == null) return "Member";
        return switch (currentUserRole) {
            case ROLE_OWNER -> "Owner";
            case ROLE_ADMIN -> "Admin";
            case ROLE_VIEWER -> "Viewer";
            default -> "Member";
        };
    }

    @Builder.Default
    private String status = "active";

    private Long totalFiles;

    @JsonProperty("docsCount")
    public String getDocsCount() {
        return (totalFiles != null ? totalFiles : 0) + " tệp";
    }

    private Long totalMembers;

    @JsonProperty("activeMembers")
    public Long getActiveMembers() {
        return totalMembers != null ? totalMembers : 0L;
    }

    @JsonProperty("membersCount")
    public String getMembersCount() {
        return (totalMembers != null ? totalMembers : 0) + " thành viên";
    }

    private String storageUsed;

    private Long storageUsedBytes;

    private String storageLimit;

    private Long storageLimitBytes;

    private String iconBg;

    private String iconColor;

    private List<String> avatars;

    private Integer extraMembers;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;
}
