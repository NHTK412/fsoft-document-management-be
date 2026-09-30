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
public class ChatMessageResponse {
    private Long id;
    private String sender;
    private String text;
    private String intro;
    private List<String> steps;
    private ChatCitationDto citation;
    private List<ChatCitationDto> citations;
    private String createdAt;
}
