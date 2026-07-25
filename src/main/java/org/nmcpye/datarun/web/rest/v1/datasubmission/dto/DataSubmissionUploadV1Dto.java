package org.nmcpye.datarun.web.rest.v1.datasubmission.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.FlowStatus;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSubmissionUploadV1Dto {
    private String uid;
    private JsonNode formData;
    private FlowStatus status;
    private String form;
    private String formVersion;
    private Integer version;
    private String team;
    private String teamCode;
    private String orgUnit;
    private String orgUnitCode;
    private String orgUnitName;
    private String activity;
    private String assignment;
    private Instant startEntryTime;
    private Instant finishedEntryTime;
    private Boolean deleted;
    private Instant deletedAt;
    private List<ReferenceDefinitionV1Dto> referenceDefinitions;
}
