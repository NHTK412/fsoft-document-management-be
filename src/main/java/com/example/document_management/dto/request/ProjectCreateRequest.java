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
@Schema(description = "Yêu cầu tạo dự án mới")
public class ProjectCreateRequest {

    @NotBlank(message = "Tên dự án không được để trống")
    @JsonAlias({"title"})
    @Schema(description = "Tên của dự án", example = "AI Knowledge Hub", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Mô tả chi tiết về dự án", example = "Không gian lưu trữ và hỏi đáp tài liệu tri thức")
    private String description;

    @Builder.Default
    @Schema(description = "Kích thước tệp tải lên tối đa", example = "50 MB")
    private String maxFileSize = "50 MB";

    @Schema(description = "Danh sách các định dạng tệp được phép tải lên", example = "[\"pdf\", \"docx\", \"md\", \"txt\"]")
    private List<String> allowedFormats;

    @Schema(description = "Danh sách email mời tham gia ban đầu (cách nhau bởi dấu phẩy)", example = "dev1@fsoft.com, dev2@fsoft.com")
    private String inviteEmails;

    @JsonIgnore
    public String getEffectiveName() {
        return name != null ? name.trim() : "";
    }
}
