package com.example.document_management.service;

import com.example.document_management.dto.response.*;
import com.example.document_management.service.DocumentService.ProjectDocumentsDataAndMeta;
import com.example.document_management.entity.DocumentMetadata;
import com.example.document_management.entity.Project;
import com.example.document_management.entity.User;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectActivityRepository projectActivityRepository;

    @InjectMocks
    private DocumentService documentService;

    private User user;
    private Project project;
    private DocumentMetadata doc;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRoleEnum.ROLE_USER)
                .build();

        project = Project.builder()
                .id(10L)
                .name("Demo Project")
                .owner(user)
                .maxFileSize("50 MB")
                .allowedFormats("pdf, docx, xlsx")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        doc = DocumentMetadata.builder()
                .id(100L)
                .fileName("spec.pdf")
                .s3Key("projects/10/spec.pdf")
                .fileSize(1024L * 1024L)
                .contentType("application/pdf")
                .projectId(10L)
                .uploaderId(1L)
                .category("docs")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testGetDocumentsExplorer_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserEmail(10L, "test@example.com")).thenReturn(true);
        when(documentRepository.findByProjectId(10L)).thenReturn(List.of(doc));
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ProjectDocumentsDataAndMeta result = documentService.getDocumentsExplorer(
                10L, null, "all", 1, 20, "updatedAt", "desc", "test@example.com"
        );

        assertNotNull(result);
        assertEquals(1L, result.getData().getSummary().getTotalFiles());
        assertEquals(1, result.getData().getFiles().size());
        assertEquals("spec.pdf", result.getData().getFiles().get(0).getName());
        assertEquals("docs", result.getData().getFiles().get(0).getCategory());
        assertEquals(1, result.getMeta().get("page"));
        assertEquals(1L, result.getMeta().get("totalItems"));
    }

    @Test
    void testUploadDocumentExplorer_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserEmail(10L, "test@example.com")).thenReturn(true);
        when(storageService.getDirectFileUrl(anyString())).thenReturn("http://localhost:9000/bucket/projects/10/spec.pdf");
        when(documentRepository.save(any(DocumentMetadata.class))).thenAnswer(inv -> {
            DocumentMetadata d = inv.getArgument(0);
            d.setId(101L);
            return d;
        });

        MockMultipartFile file = new MockMultipartFile("file", "upload.pdf", "application/pdf", new byte[1024]);
        DocumentUploadResponse response = documentService.uploadDocumentExplorer(10L, file, "docs", "test@example.com");

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("upload.pdf", response.getName());
        assertEquals("PDF", response.getFormat());
        verify(storageService, times(1)).uploadFile(eq(file), anyString());
    }

    @Test
    void testGetPreviewUrl_Success() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserEmail(10L, "test@example.com")).thenReturn(true);
        when(storageService.getPreSignedUrl(eq("projects/10/spec.pdf"), eq(3600), eq(TimeUnit.SECONDS), anyMap()))
                .thenReturn("http://presigned.preview.url");

        DocumentPreviewUrlResponse response = documentService.getPreviewUrl(10L, 100L, "test@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getDocumentId());
        assertEquals("http://presigned.preview.url", response.getPreviewUrl());
        assertEquals(3600, response.getExpiresIn());
    }

    @Test
    void testGetDownloadUrl_Success() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserEmail(10L, "test@example.com")).thenReturn(true);
        when(storageService.getPreSignedUrl(eq("projects/10/spec.pdf"), eq(600), eq(TimeUnit.SECONDS), anyMap()))
                .thenReturn("http://presigned.download.url");

        DocumentDownloadUrlResponse response = documentService.getDownloadUrl(10L, 100L, "test@example.com");

        assertNotNull(response);
        assertEquals("http://presigned.download.url", response.getDownloadUrl());
        assertEquals("spec.pdf", response.getFileName());
        assertEquals(600, response.getExpiresIn());
    }

    @Test
    void testBulkDeleteDocuments_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserEmail(10L, "test@example.com")).thenReturn(true);
        when(documentRepository.findByIdInAndProjectId(List.of(100L), 10L)).thenReturn(List.of(doc));

        BulkDeleteResponse response = documentService.bulkDeleteDocuments(10L, List.of(100L), "test@example.com");

        assertNotNull(response);
        assertEquals(1, response.getDeletedCount());
        verify(storageService, times(1)).deleteFile(doc.getS3Key());
        verify(documentRepository, times(1)).delete(doc);
    }
}
