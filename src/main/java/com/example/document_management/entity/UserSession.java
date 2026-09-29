package com.example.document_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String refreshTokenHash;

    private String deviceName;

    private String ipAddress;

    private String location;

    @Builder.Default
    private String deviceType = "desktop"; // "desktop", "mobile", "tablet"

    @Builder.Default
    private boolean isCurrent = false;

    @Builder.Default
    private Instant lastActive = Instant.now();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastActive == null) {
            lastActive = createdAt;
        }
    }
}
