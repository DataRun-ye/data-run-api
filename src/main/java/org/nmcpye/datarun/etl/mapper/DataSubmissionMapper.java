package org.nmcpye.datarun.etl.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.etl.model.SubmissionContext;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface DataSubmissionMapper {
    @Mapping(source = "startTime", target = "startEntryTime")
    @Mapping(source = "submissionCreationTime", target = "createdDate")
    @Mapping(source = "assignmentUid", target = "assignment")
    @Mapping(source = "activityUid", target = "activity")
    @Mapping(source = "orgUnitUid", target = "orgUnit")
    @Mapping(source = "teamUid", target = "team")
    @Mapping(source = "templateVersionUid", target = "formVersion")
    @Mapping(source = "templateUid", target = "form")
    @Mapping(source = "submissionSerial", target = "serialNumber")
    @Mapping(source = "submissionUid", target = "uid")
    @Mapping(source = "submissionUid", target = "uid")
    @Mapping(source = "submissionId", target = "id")
    @Mapping(source = "version", target = "lockVersion")
    DataSubmission toEntity(SubmissionContext submissionContext);

    @InheritInverseConfiguration(name = "toEntity")
    SubmissionContext toDto(DataSubmission dataSubmission);

    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DataSubmission partialUpdate(SubmissionContext submissionContext, @MappingTarget DataSubmission dataSubmission);
}
