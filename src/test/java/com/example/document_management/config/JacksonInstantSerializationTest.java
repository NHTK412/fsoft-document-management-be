package com.example.document_management.config;

import com.example.document_management.dto.response.ProjectResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonInstantSerializationTest {

    @Test
    void testInstantSerializationFormat() throws Exception {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();

        Instant testTime = Instant.parse("2026-09-29T06:05:18.342Z");
        ProjectResponse response = ProjectResponse.builder()
                .id(1L)
                .name("Test Project")
                .updatedAt(testTime)
                .createdAt(testTime)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"updatedAt\":\"2026-09-29T06:05:18.342Z\""),
                "Expected JSON to contain formatted updatedAt, but was: " + json);
        assertTrue(json.contains("\"createdAt\":\"2026-09-29T06:05:18.342Z\""),
                "Expected JSON to contain formatted createdAt, but was: " + json);
    }
}
