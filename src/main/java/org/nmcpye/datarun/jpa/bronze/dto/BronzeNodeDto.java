package org.nmcpye.datarun.jpa.bronze.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class BronzeNodeDto {
    private Long bronzeId;
    private UUID ingestionId;
    private String submissionId;
    private String templateId;
    private String templateVersion;
    private String nodePath;
    private String nodeKind;
    private String valueType;
    private String roleSuggestion;
    private String cardinality;
    private String repeatInstanceId;
    private Integer sequenceIndex;
    private String valueString;
    private BigDecimal valueNum;
    private Boolean valueBool;
    private OffsetDateTime valueTs;
    private JsonNode valueArray;
    private JsonNode classifiers;
    private JsonNode profiling;
    private String registryVersion;
    private String parserVersion;
    private OffsetDateTime createdAt;
}
