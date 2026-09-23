package com.example.document_management.entity;

import java.util.List;

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
}
