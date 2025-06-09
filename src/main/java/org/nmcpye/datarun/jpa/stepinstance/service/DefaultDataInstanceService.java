package org.nmcpye.datarun.jpa.stepinstance.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.entityinstance.repository.EntityInstanceRepository;
import org.nmcpye.datarun.jpa.flowrun.FlowRun;
import org.nmcpye.datarun.jpa.flowrun.repository.FlowRunRepository;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.stepinstance.StepInstance;
import org.nmcpye.datarun.jpa.stepinstance.repository.DataInstanceRepository;
import org.nmcpye.datarun.jpa.steptype.repository.DataStageDefinitionRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class DefaultDataInstanceService
    extends DefaultJpaSoftDeleteService<StepInstance>
    implements DataInstanceService {

    private final TeamRepository teamRepository;
    private final FlowRunRepository flowRunRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final FlowTypeRepository flowTypeRepository;
    private final DataStageDefinitionRepository stageDefinitionRepository;
    private final EntityInstanceRepository entityInstanceRepository;
    private final DataTemplateRepository dataTemplateRepository;

    public DefaultDataInstanceService(DataInstanceRepository repository, CacheManager cacheManager, UserAccessService userAccessService, TeamRepository teamRepository, FlowRunRepository flowRunRepository, OrgUnitRepository orgUnitRepository, FlowTypeRepository flowTypeRepository, DataStageDefinitionRepository stageDefinitionRepository, EntityInstanceRepository entityInstanceRepository, DataTemplateRepository dataTemplateRepository) {
        super(repository, cacheManager, userAccessService);
        this.teamRepository = teamRepository;
        this.flowRunRepository = flowRunRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.flowTypeRepository = flowTypeRepository;
        this.stageDefinitionRepository = stageDefinitionRepository;
        this.entityInstanceRepository = entityInstanceRepository;
        this.dataTemplateRepository = dataTemplateRepository;
    }

    @Override
    public StepInstance saveWithRelations(StepInstance object) {

        FlowRun flowRun = null;

        if (object.getFlowRun() != null) {
            flowRun = findAssignment(object.getFlowRun());
        }

        if (object.getOrgUnitUid() != null && !orgUnitRepository.existsByUid(object.getOrgUnitUid())) {
            throw new PropertyNotFoundException("OrgUnit not found: " + object.getOrgUnitUid());
        }


        object.setFlowRun(flowRun);

        return save(object);
    }

    private FlowRun findAssignment(FlowRun flowRun) {
        return Optional.ofNullable(flowRun.getId()).flatMap(flowRunRepository::findById)
            .or(() -> Optional.ofNullable(flowRun.getUid()).flatMap(flowRunRepository::findByUid))
            .orElseThrow(() -> new PropertyNotFoundException("Team not found: " + flowRun));
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
}
