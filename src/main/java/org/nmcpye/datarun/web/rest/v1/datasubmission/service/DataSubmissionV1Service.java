package org.nmcpye.datarun.web.rest.v1.datasubmission.service;

import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionV1Dto;

import java.util.List;

public interface DataSubmissionV1Service {
    EntitySaveSummaryVM upsertAll(List<DataSubmissionV1Dto> dtoList);
}
