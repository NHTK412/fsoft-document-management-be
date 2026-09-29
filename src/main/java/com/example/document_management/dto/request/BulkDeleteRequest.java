package com.example.document_management.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDeleteRequest {
    @NotEmpty(message = "Danh sách ID tệp tin cần xóa không được rỗng!")
    private List<Long> ids;
}
