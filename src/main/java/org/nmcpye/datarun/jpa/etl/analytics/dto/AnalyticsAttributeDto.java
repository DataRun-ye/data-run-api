package org.nmcpye.datarun.jpa.etl.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.nmcpye.datarun.analytics.dto.Aggregation;
import org.nmcpye.datarun.jpa.etl.analytics.domain.AnalyticsAttribute;
import org.nmcpye.datarun.jpa.etl.analytics.domain.enums.AttributeType;
import org.nmcpye.datarun.jpa.etl.analytics.domain.enums.DataType;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for {@link AnalyticsAttribute}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class AnalyticsAttributeDto implements Serializable {
    private String uid;
    private String templateUid;
    private String templateVersionUid;
    private String sourceColumnMapping;
    private AttributeType attributeType;
    private Map<String, String> displayName;
    private DataType dataType;
    private Aggregation aggregationType;
    private String entityRefType;
    private String sourceElementUid;
    private String sourceSemanticPath;
    private String elementTemplateConfigUid;
    private String dataElementUid;
    private String optionSetUid;
}
