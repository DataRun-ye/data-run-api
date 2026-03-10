package org.nmcpye.datarun.web.rest.v1.datasubmission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.CompositeSubmissionValidator;
import org.nmcpye.datarun.jpa.datasubmission.validation.SubmissionAccessValidator;
import org.nmcpye.datarun.jpa.datasubmissionbatching.job.MigrationRepeatIdGenerator;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateElementService;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionV1Mapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataSubmissionV1ServiceImpl implements DataSubmissionV1Service {

    private final DataSubmissionService submissionService;
    private final DataSubmissionV1Mapper v1Mapper;
    private final ObjectMapper objectMapper;
    private final CompositeSubmissionValidator compositeValidator;
    private final SubmissionAccessValidator submissionAccessValidator;
    private final TemplateElementService templateElementService;

    @Override
    public EntitySaveSummaryVM upsertAll(List<DataSubmissionV1Dto> dtoList) {
        List<DataSubmission> entities = v1Mapper.toEntity(dtoList);
        List<DataSubmission> processedEntities = preProcess(entities);

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        submissionService.upsertAll(processedEntities, SecurityUtils.getCurrentUserDetailsOrThrow(), summary);
        return summary;
    }

    private List<DataSubmission> preProcess(List<DataSubmission> payLoadEntities) {
        return payLoadEntities.stream()
                .peek(payLoadEntity -> {
                    ObjectNode root = (ObjectNode) (payLoadEntity.getFormData() == null
                            ? objectMapper.createObjectNode()
                            : payLoadEntity.getFormData().deepCopy());
                    final var migrationRepeatIdGenerator = new MigrationRepeatIdGenerator(
                            templateElementService.getTemplateElementMap(payLoadEntity.getForm(),
                                    payLoadEntity.getFormVersion()));
                    int generated = migrationRepeatIdGenerator
                            .generateMissingIdsForMigration(root, payLoadEntity.getUid());
                    if (generated > 0) {
                        payLoadEntity.setFormData(root);
                    }

                    compositeValidator.validateAndEnrich(submissionAccessValidator.validateAccess(payLoadEntity,
                            SecurityUtils.getCurrentUserDetailsOrThrow()));
                }).collect(Collectors.toList());
    }
}
