package org.nmcpye.datarun.mongo.datastagesubmission.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataFormSubmissionHistoryMapper {
    @Mappings({
        @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())"),
        @Mapping(target = "id", ignore = true),
    })
    DataFormSubmissionHistory fromSubmission(DataFormSubmission submission);
}
