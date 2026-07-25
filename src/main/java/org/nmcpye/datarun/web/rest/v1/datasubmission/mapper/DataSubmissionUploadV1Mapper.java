package org.nmcpye.datarun.web.rest.v1.datasubmission.mapper;

import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.springframework.stereotype.Component;

@Component
public class DataSubmissionUploadV1Mapper {

    public DataSubmission toEntity(DataSubmissionUploadV1Dto source) {
        DataSubmission target = new DataSubmission();
        target.setUid(source.getUid());
        target.setFormData(source.getFormData());
        target.setStatus(source.getStatus());
        target.setForm(source.getForm());
        target.setFormVersion(source.getFormVersion());
        target.setVersion(source.getVersion());
        target.setTeam(source.getTeam());
        target.setTeamCode(source.getTeamCode());
        target.setOrgUnit(source.getOrgUnit());
        target.setOrgUnitCode(source.getOrgUnitCode());
        target.setOrgUnitName(source.getOrgUnitName());
        target.setActivity(source.getActivity());
        target.setAssignment(source.getAssignment());
        target.setStartEntryTime(source.getStartEntryTime());
        target.setFinishedEntryTime(source.getFinishedEntryTime());
        if (source.getDeleted() != null) {
            target.setDeleted(source.getDeleted());
        }
        target.setDeletedAt(source.getDeletedAt());
        return target;
    }
}
