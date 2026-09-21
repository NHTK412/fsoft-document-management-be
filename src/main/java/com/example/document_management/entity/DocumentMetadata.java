package com.example.document_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    private String s3Key;

    private Long fileSize;

    private String contentType;

    private Long projectId;

    private Long uploaderId;
}
