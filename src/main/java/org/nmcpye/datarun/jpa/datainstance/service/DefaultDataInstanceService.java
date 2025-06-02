package org.nmcpye.datarun.jpa.datainstance.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignmenttype.repository.AssignmentTypeRepository;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.datainstance.DataInstance;
import org.nmcpye.datarun.jpa.datainstance.repository.DataInstanceRepository;
import org.nmcpye.datarun.jpa.datastage.repository.DataStageDefinitionRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.entityinstance.repository.EntityInstanceRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
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
    extends DefaultJpaSoftDeleteService<DataInstance>
    implements DataInstanceService {

    private final TeamRepository teamRepository;
    private final AssignmentRepository assignmentRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final AssignmentTypeRepository assignmentTypeRepository;
    private final DataStageDefinitionRepository stageDefinitionRepository;
    private final EntityInstanceRepository entityInstanceRepository;
    private final DataTemplateRepository dataTemplateRepository;

    public DefaultDataInstanceService(DataInstanceRepository repository, CacheManager cacheManager, UserAccessService userAccessService, TeamRepository teamRepository, AssignmentRepository assignmentRepository, OrgUnitRepository orgUnitRepository, AssignmentTypeRepository assignmentTypeRepository, DataStageDefinitionRepository stageDefinitionRepository, EntityInstanceRepository entityInstanceRepository, DataTemplateRepository dataTemplateRepository) {
        super(repository, cacheManager, userAccessService);
        this.teamRepository = teamRepository;
        this.assignmentRepository = assignmentRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.assignmentTypeRepository = assignmentTypeRepository;
        this.stageDefinitionRepository = stageDefinitionRepository;
        this.entityInstanceRepository = entityInstanceRepository;
        this.dataTemplateRepository = dataTemplateRepository;
    }

    @Override
    public DataInstance saveWithRelations(DataInstance object) {

        Assignment assignment = null;

        if (object.getAssignment() != null) {
            assignment = findAssignment(object.getAssignment());
        }

        if (object.getOrgUnitUid() != null && !orgUnitRepository.existsByUid(object.getOrgUnitUid())) {
            throw new PropertyNotFoundException("OrgUnit not found: " + object.getOrgUnitUid());
        }


        object.setAssignment(assignment);

        return save(object);
    }

    private Assignment findAssignment(Assignment assignment) {
        return Optional.ofNullable(assignment.getId()).flatMap(assignmentRepository::findById)
            .or(() -> Optional.ofNullable(assignment.getUid()).flatMap(assignmentRepository::findByUid))
            .orElseThrow(() -> new PropertyNotFoundException("Team not found: " + assignment));
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
