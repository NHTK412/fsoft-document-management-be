package com.example.document_management.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "6. AI Assistant", description = "Tương tác và hỏi đáp thông minh với tài liệu bằng AI")
public class AiChatController {
}
