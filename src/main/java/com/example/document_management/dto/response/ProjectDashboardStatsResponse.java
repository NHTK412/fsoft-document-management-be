package com.example.document_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDashboardStatsResponse {

    private List<DashboardMetricDto> metrics;
    private FormatDistributionDto formatDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardMetricDto {
        private String id;
        private String title;
        private String value;
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormatDistributionDto {
        private String totalFiles;
        private List<FormatDistributionItemDto> formats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormatDistributionItemDto {
        private String label;
        private Integer percent;
        private Long count;
        private String color;
    }
}
