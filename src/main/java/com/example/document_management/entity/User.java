package com.example.document_management.entity;


import org.hibernate.annotations.SoftDelete;

import com.example.document_management.enums.UserRoleEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SoftDelete
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;

    @Builder.Default
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    private UserRoleEnum role;

    private String title;

    private String phone;

    private String avatarUrl;

    @Builder.Default
    private java.time.Instant createdAt = java.time.Instant.now();

    @Builder.Default
    private java.time.Instant updatedAt = java.time.Instant.now();

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
}
