package com.example.document_management.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cập nhật thông tin dự án")
public class ProjectUpdateRequest {

    @NotBlank(message = "Tên dự án không được để trống")
    @JsonAlias({"title"})
    @Schema(description = "Tên dự án mới", example = "AI Knowledge Core", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Mô tả chi tiết dự án", example = "Không gian quản lý tài liệu và hỏi đáp AI thông minh")
    private String description;

    @Schema(description = "Trạng thái dự án: active | archived", example = "active")
    private String status;

    @Schema(description = "Kích thước tệp tải lên tối đa", example = "50 MB")
    private String maxFileSize;

    @Schema(description = "Danh sách định dạng tệp cho phép", example = "[\"pdf\", \"docx\", \"md\", \"txt\"]")
    private List<String> allowedFormats;

    @JsonIgnore
    public String getEffectiveName() {
        return name != null ? name.trim() : "";
    }
}
