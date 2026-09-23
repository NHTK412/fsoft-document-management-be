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
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> projectMembers = new ArrayList<>();

    public void addMember(User user, ProjectMemberRoleEnum role) {
        ProjectMember member = ProjectMember.builder()
                .project(this)
                .user(user)
                .role(role)
                .build();
        this.projectMembers.add(member);
    }
}
