package org.nmcpye.datarun.web.rest.v1.orgunit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.translation.Translation;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Frozen DTO representing the v1 OrgUnit structure as consumed by the mobile
 * app.
 * Read-only contract — changes to the JPA entity do not affect this shape.
 */
@Getter
@Setter
public class OrgUnitV1Dto {

    private String id;

    private String uid;

    private String code;

    private String name;

    private String path;

    @JsonProperty("parent")
    private OrgUnitRefDto parent;

    @JsonProperty("properties")
    private Map<String, Object> properties;

    private Set<Translation> translations;

    private String createdBy;

    private Instant createdDate;

    private String lastModifiedBy;

    private Instant lastModifiedDate;

    /**
     * Minimal reference for parent/child to avoid recursive serialization.
     */
    @Getter
    @Setter
    public static class OrgUnitRefDto {
        private String id;
        private String uid;
        private String code;
        private String name;
        private String path;
    }
}
