package org.nmcpye.datarun.jpa.datasubmission.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada
 * @since 25/09/2025
 */
@Component
public class ExtractionManifestMapper {
    private final ObjectMapper objectMapper;

    public ExtractionManifestMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ExtractionManifestDto fromJson(String json) {
        try {
            return objectMapper.readValue(json, ExtractionManifestDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ExtractionManifest JSON", e);
        }
    }

    public String toJson(ExtractionManifestDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ExtractionManifest DTO", e);
        }
    }
}
