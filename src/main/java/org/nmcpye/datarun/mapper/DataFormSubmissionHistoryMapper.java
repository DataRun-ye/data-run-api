package org.nmcpye.datarun.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataFormSubmissionHistoryMapper {
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    DataFormSubmissionHistory fromSubmission(DataFormSubmission submission);
}
