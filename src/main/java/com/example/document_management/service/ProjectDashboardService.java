package com.example.document_management.service;

import com.example.document_management.dto.response.ProjectActivityResponse;
import com.example.document_management.dto.response.ProjectDashboardStatsResponse;
import com.example.document_management.dto.response.ProjectDashboardStatsResponse.*;
import com.example.document_management.dto.response.RecentlyViewedDocResponse;
import com.example.document_management.entity.DocumentMetadata;
import com.example.document_management.entity.Project;
import com.example.document_management.entity.ProjectActivity;
import com.example.document_management.entity.User;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectDashboardService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final DocumentRepository documentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProjectActivityRepository projectActivityRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProjectDashboardStatsResponse getDashboardStats(Long projectId, String email) {
        Project project = getProjectAndValidateAccess(projectId, email);

        // 1. Total files
        long totalFiles = documentRepository.countByProjectId(projectId);

        // 2. Storage used & limit
        Long usedBytes = documentRepository.sumFileSizeByProjectId(projectId);
        if (usedBytes == null) {
            usedBytes = 0L;
        }
        long limitBytes = 10L * 1024 * 1024 * 1024; // Default 10 GB limit
        double storagePercent = limitBytes > 0 ? (usedBytes * 100.0) / limitBytes : 0.0;
        String storageUsedStr = formatBytes(usedBytes);
        String storageLimitStr = "10 GB";
        String storageUnit = String.format(Locale.US, "trên %s (%.1f%%)", storageLimitStr, storagePercent);

        // 3. AI queries in the last 7 days
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        long aiQueries = chatMessageRepository.countByProjectIdAndCreatedAtAfter(projectId, sevenDaysAgo);

        // 4. Active members
        long activeMembers = projectMemberRepository.countByProjectId(projectId);

        List<DashboardMetricDto> metrics = List.of(
                DashboardMetricDto.builder()
                        .id("total_files")
                        .title("TỔNG SỐ TỆP TIN")
                        .value(String.format(Locale.US, "%,d", totalFiles))
                        .unit("tệp tài liệu")
                        .build(),
                DashboardMetricDto.builder()
                        .id("storage_used")
                        .title("DUNG LƯỢNG MINIO")
                        .value(storageUsedStr)
                        .unit(storageUnit)
                        .build(),
                DashboardMetricDto.builder()
                        .id("ai_queries")
                        .title("HỎI ĐÁP AI TRONG TUẦN")
                        .value(String.format(Locale.US, "%,d", aiQueries))
                        .unit("lượt giải đáp")
                        .build(),
                DashboardMetricDto.builder()
                        .id("active_members")
                        .title("THÀNH VIÊN HOẠT ĐỘNG")
                        .value(String.valueOf(activeMembers))
                        .unit("thành viên dự án")
                        .build()
        );

        // 5. Format distribution
        List<DocumentMetadata> allDocs = documentRepository.findByProjectId(projectId);
        long pdfCount = 0;
        long officeCount = 0;
        long mdCount = 0;
        long videoCount = 0;
        long otherCount = 0;

        for (DocumentMetadata doc : allDocs) {
            String name = doc.getFileName() != null ? doc.getFileName().toLowerCase() : "";
            String contentType = doc.getContentType() != null ? doc.getContentType().toLowerCase() : "";

            if (name.endsWith(".pdf") || contentType.contains("pdf")) {
                pdfCount++;
            } else if (name.endsWith(".docx") || name.endsWith(".doc")
                    || name.endsWith(".xlsx") || name.endsWith(".xls")
                    || name.endsWith(".pptx") || name.endsWith(".ppt")
                    || contentType.contains("word") || contentType.contains("sheet")
                    || contentType.contains("presentation") || contentType.contains("officedocument")) {
                officeCount++;
            } else if (name.endsWith(".md") || name.endsWith(".markdown")
                    || name.endsWith(".txt") || name.endsWith(".json")
                    || name.endsWith(".yaml") || name.endsWith(".yml")
                    || name.endsWith(".csv") || contentType.contains("text")
                    || contentType.contains("markdown") || contentType.contains("json")) {
                mdCount++;
            } else if (name.endsWith(".mp4") || name.endsWith(".mov")
                    || name.endsWith(".avi") || name.endsWith(".mkv")
                    || contentType.startsWith("video/")) {
                videoCount++;
            } else {
                otherCount++;
            }
        }

        int pdfPercent = totalFiles > 0 ? (int) Math.round((pdfCount * 100.0) / totalFiles) : 0;
        int officePercent = totalFiles > 0 ? (int) Math.round((officeCount * 100.0) / totalFiles) : 0;
        int mdPercent = totalFiles > 0 ? (int) Math.round((mdCount * 100.0) / totalFiles) : 0;
        int videoPercent = totalFiles > 0 ? (int) Math.round((videoCount * 100.0) / totalFiles) : 0;
        int otherPercent = totalFiles > 0 ? Math.max(0, 100 - (pdfPercent + officePercent + mdPercent + videoPercent)) : 0;
        if (otherCount == 0 && otherPercent > 0) {
            otherPercent = 0;
        }

        List<FormatDistributionItemDto> formats = List.of(
                FormatDistributionItemDto.builder()
                        .label("PDF Documents")
                        .percent(pdfPercent)
                        .count(pdfCount)
                        .color("bg-[#EF4444]")
                        .build(),
                FormatDistributionItemDto.builder()
                        .label("Office (DOCX, XLSX, PPTX)")
                        .percent(officePercent)
                        .count(officeCount)
                        .color("bg-[#3B82F6]")
                        .build(),
                FormatDistributionItemDto.builder()
                        .label("Markdown & Text")
                        .percent(mdPercent)
                        .count(mdCount)
                        .color("bg-[#8B5CF6]")
                        .build(),
                FormatDistributionItemDto.builder()
                        .label("Video (MP4, MOV)")
                        .percent(videoPercent)
                        .count(videoCount)
                        .color("bg-[#F59E0B]")
                        .build(),
                FormatDistributionItemDto.builder()
                        .label("Hình ảnh & Khác")
                        .percent(otherPercent)
                        .count(otherCount)
                        .color("bg-[#10B981]")
                        .build()
        );

        FormatDistributionDto formatDistribution = FormatDistributionDto.builder()
                .totalFiles(String.format(Locale.US, "%,d", totalFiles))
                .formats(formats)
                .build();

        return ProjectDashboardStatsResponse.builder()
                .metrics(metrics)
                .formatDistribution(formatDistribution)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecentlyViewedDocResponse> getRecentlyViewed(Long projectId, Integer limit, String email) {
        getProjectAndValidateAccess(projectId, email);

        int queryLimit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 50);
        List<DocumentMetadata> docs = documentRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, queryLimit));

        List<RecentlyViewedDocResponse> result = new ArrayList<>();
        for (DocumentMetadata doc : docs) {
            String type = extractExtension(doc.getFileName());
            double sizeInMb = Math.round((doc.getFileSize() / (1024.0 * 1024.0)) * 100.0) / 100.0;
            if (sizeInMb <= 0.0) {
                sizeInMb = 0.01;
            }

            result.add(RecentlyViewedDocResponse.builder()
                    .id(doc.getId())
                    .name(doc.getFileName())
                    .type(type)
                    .size(sizeInMb)
                    .sizeFormatted(formatBytes(doc.getFileSize()))
                    .createdAt(doc.getCreatedAt())
                    .updatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt() : doc.getCreatedAt())
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<ProjectActivityResponse> getActivities(Long projectId, Integer limit, String email) {
        Project project = getProjectAndValidateAccess(projectId, email);

        int queryLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        List<ProjectActivity> activities = projectActivityRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, queryLimit));

        List<ProjectActivityResponse> result = new ArrayList<>();
        if (!activities.isEmpty()) {
            for (ProjectActivity act : activities) {
                result.add(ProjectActivityResponse.builder()
                        .id(act.getId())
                        .userAction(act.getUserAction())
                        .target(act.getTarget())
                        .createdAt(act.getCreatedAt())
                        .userName(act.getUser() != null ? act.getUser().getFullName() : null)
                        .userAvatar(act.getUser() != null ? act.getUser().getAvatarUrl() : null)
                        .build());
            }
        } else {
            // Fallback: derive activities from recent documents and project creation so dashboard timeline is not empty
            List<DocumentMetadata> recentDocs = documentRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, queryLimit));
            long syntheticId = 1L;
            for (DocumentMetadata doc : recentDocs) {
                result.add(ProjectActivityResponse.builder()
                        .id(syntheticId++)
                        .userAction("Đã tải lên tệp mới")
                        .target(doc.getFileName())
                        .createdAt(doc.getCreatedAt())
                        .userName(project.getOwner() != null ? project.getOwner().getFullName() : "Thành viên")
                        .userAvatar(project.getOwner() != null ? project.getOwner().getAvatarUrl() : null)
                        .build());
            }

            if (result.size() < queryLimit) {
                result.add(ProjectActivityResponse.builder()
                        .id(syntheticId)
                        .userAction("Đã khởi tạo dự án")
                        .target(project.getName())
                        .createdAt(project.getCreatedAt())
                        .userName(project.getOwner() != null ? project.getOwner().getFullName() : "Chủ dự án")
                        .userAvatar(project.getOwner() != null ? project.getOwner().getAvatarUrl() : null)
                        .build());
            }
        }

        return result;
    }

    private Project getProjectAndValidateAccess(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng!"));

        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRoleEnum.ROLE_ADMIN;
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserEmail(projectId, email);

        if (!isOwner && !isAdmin && !isMember) {
            throw new AccessDeniedException("Bạn không có quyền truy cập vào thông tin dự án này!");
        }

        return project;
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "file";
        }
        int lastDot = fileName.lastIndexOf('.');
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
