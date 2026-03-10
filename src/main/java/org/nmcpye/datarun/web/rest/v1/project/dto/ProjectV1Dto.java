package org.nmcpye.datarun.web.rest.v1.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.translation.Translation;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Frozen DTO representing the v1 Project structure as consumed by the mobile
 * app.
 * This is a read-only contract — changes to the JPA entity do not affect this
 * shape.
 */
@Getter
@Setter
public class ProjectV1Dto {

    private String id;

    private String uid;

    private String code;

    private String name;

    private Boolean disabled;

    @JsonProperty("properties")
    private Map<String, Object> properties;

    private Set<Translation> translations;

    private String createdBy;

    private Instant createdDate;

    private String lastModifiedBy;

    private Instant lastModifiedDate;
}
