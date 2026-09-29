package com.example.document_management.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreateRequest {

    @JsonAlias({"name", "title"})
    private String name;

    private String title;

    private String description;

    @Builder.Default
    private String maxFileSize = "50 MB";

    private List<String> allowedFormats;

    private String inviteEmails;

    @AssertTrue(message = "Tên dự án không được để trống")
    public boolean isNameOrTitlePresent() {
        return (name != null && !name.isBlank()) || (title != null && !title.isBlank());
    }

    public String getEffectiveName() {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return "";
    }
}
