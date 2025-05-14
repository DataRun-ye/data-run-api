package org.nmcpye.datarun.mapper.dto;

import jakarta.validation.constraints.Size;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataform.FormTemplate}
 */
@Value
public class FormTemplateDto implements Serializable {
    @Size(max = 11)
    String uid;

    String code;
    String name;
    @Size(max = 2000)
    String description;
    Boolean disabled;
    Boolean deleted;
    Integer latestVersion;
    String defaultLocale;
    Map<String, String> label;
    Instant createdDate;
    Instant lastModifiedDate;
}
