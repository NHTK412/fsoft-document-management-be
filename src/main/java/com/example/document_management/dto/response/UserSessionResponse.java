package com.example.document_management.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionResponse {
    private Long id;
    private String deviceName;
    private String location;
    private String ip;

    @JsonProperty("isCurrent")
    private Boolean isCurrent;

    private String deviceType;
    private String lastActive;
}
