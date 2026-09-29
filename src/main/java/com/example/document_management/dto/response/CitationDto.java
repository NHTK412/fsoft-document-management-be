package com.example.document_management.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitationDto {
    @JsonProperty("source_file")
    @JsonAlias({"source_file", "sourceFile", "source"})
    private String sourceFile;

    @JsonProperty("location")
    @JsonAlias({"location", "locaiton"})
    private String location;

    @JsonProperty("quote")
    private String quote;
}
