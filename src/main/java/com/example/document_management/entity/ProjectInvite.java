package com.example.document_management.entity;

import com.example.document_management.enums.ProjectInviteStatusEnum;
import com.example.document_management.enums.ProjectMemberRoleEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "project_invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectMemberRoleEnum role;

    private String token;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProjectInviteStatusEnum status = ProjectInviteStatusEnum.PENDING;

    @Builder.Default
    private Instant sentDate = Instant.now();

    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        if (sentDate == null) {
            sentDate = Instant.now();
        }
        if (expiresAt == null) {
            expiresAt = sentDate.plusSeconds(7 * 24 * 3600); // Mặc định hết hạn sau 7 ngày
        }
    }
}
