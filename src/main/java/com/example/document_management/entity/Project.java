package com.example.document_management.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.document_management.enums.ProjectMemberRoleEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne()
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Builder.Default
    private String status = "active";

    @Builder.Default
    private String maxFileSize = "50 MB";

    @Builder.Default
    private String allowedFormats = "pdf,docx,xlsx,pptx,md,txt,images";

    @Builder.Default
    private Double aiTemperature = 0.2;

    @Column(columnDefinition = "TEXT")
    private String aiSystemPrompt;

    private String logoUrl;

    @Builder.Default
    private Long storageLimitBytes = 10737418240L;

    @Builder.Default
    private java.time.Instant createdAt = java.time.Instant.now();

    @Builder.Default
    private java.time.Instant updatedAt = java.time.Instant.now();

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> projectMembers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.Instant.now();
    }

    public void addMember(User user, ProjectMemberRoleEnum role) {
        ProjectMember member = ProjectMember.builder()
                .project(this)
                .user(user)
                .role(role)
                .build();
        this.projectMembers.add(member);
    }
}
