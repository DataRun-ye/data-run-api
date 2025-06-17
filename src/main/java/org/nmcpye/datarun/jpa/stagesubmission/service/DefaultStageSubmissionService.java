package org.nmcpye.datarun.jpa.stagesubmission.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.entityinstance.repository.EntityInstanceRepository;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.repository.FlowInstanceRepository;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.stagedefinition.repository.StageDefinitionRepository;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;
import org.nmcpye.datarun.jpa.stagesubmission.repository.StageSubmissionRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Primary
@Transactional
public class DefaultStageSubmissionService
    extends DefaultJpaSoftDeleteService<StageInstance>
    implements StageSubmissionService {

    private final TeamRepository teamRepository;
    private final FlowInstanceRepository flowInstanceRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final FlowTypeRepository flowTypeRepository;
    private final StageDefinitionRepository stageDefinitionRepository;
    private final EntityInstanceRepository entityInstanceRepository;
    private final DataTemplateRepository dataTemplateRepository;

    public DefaultStageSubmissionService(StageSubmissionRepository repository, CacheManager cacheManager, UserAccessService userAccessService, TeamRepository teamRepository, FlowInstanceRepository flowInstanceRepository, OrgUnitRepository orgUnitRepository, FlowTypeRepository flowTypeRepository, StageDefinitionRepository stageDefinitionRepository, EntityInstanceRepository entityInstanceRepository, DataTemplateRepository dataTemplateRepository) {
        super(repository, cacheManager, userAccessService);
        this.teamRepository = teamRepository;
        this.flowInstanceRepository = flowInstanceRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.flowTypeRepository = flowTypeRepository;
        this.stageDefinitionRepository = stageDefinitionRepository;
        this.entityInstanceRepository = entityInstanceRepository;
        this.dataTemplateRepository = dataTemplateRepository;
    }

    @Override
    public StageInstance saveWithRelations(StageInstance object) {

        FlowInstance flowInstance = null;

        if (object.getFlowInstance() != null) {
            flowInstance = findAssignment(object.getFlowInstance());
        }

//        if (object.getStepType() != null && !orgUnitRepository.existsByUid(object.getOrgUnitUid())) {
//            throw new PropertyNotFoundException("OrgUnit not found: " + object.getOrgUnitUid());
//        }


        object.setFlowInstance(flowInstance);

        return save(object);
    }

    private FlowInstance findAssignment(FlowInstance flowInstance) {
        return Optional.ofNullable(flowInstance.getId()).flatMap(flowInstanceRepository::findById)
            .or(() -> Optional.ofNullable(flowInstance.getUid()).flatMap(flowInstanceRepository::findByUid))
            .orElseThrow(() -> new PropertyNotFoundException("Team not found: " + flowInstance));
    }

    private OrgUnit findOrgUnit(OrgUnit orgUnit) {
        return Optional.ofNullable(orgUnit.getId())
            .flatMap(orgUnitRepository::findById)
            .or(() -> Optional.ofNullable(orgUnit.getUid())
                .flatMap(orgUnitRepository::findByUid))
            .or(() -> Optional.ofNullable(orgUnit.getCode())
                .flatMap(orgUnitRepository::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("OrgUniy not found: " + orgUnit));
    }

    public StageInstance submitOneOff(String flowInstanceId, Map<String, Object> formData) {
        FlowInstance instance = flowInstanceRepository.findById(flowInstanceId)
            .orElseThrow(() -> new IllegalArgumentException("FlowInstance not found"));

        if (instance.getStatus() == FlowStatus.DONE) {
            throw new IllegalStateException("Flow already completed");
        }

        DataTemplate template = dataTemplateRepository.findById(instance.getFlowType().getStages().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("FormTemplate missing"))
                .getDataTemplateId())
            .orElseThrow(() -> new IllegalStateException("FormTemplate missing"));

        // (Optional) validate formData against template.getDataElements()
        // validationService.validate(formData, template.getDataElements());

        StageInstance submission = new StageInstance();
        submission.setFlowInstance(instance);
        submission.setStatus(StageInstance.SubmissionStatus.SUBMITTED);
        submission.setInstanceData(formData);

        repository.save(submission);

        instance.setStatus(FlowStatus.DONE);
//        instance.setCompletedAt(Instant.now());
        flowInstanceRepository.save(instance);

        return submission;
    }
}
