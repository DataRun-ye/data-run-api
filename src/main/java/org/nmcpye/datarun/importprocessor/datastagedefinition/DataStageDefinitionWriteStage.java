package org.nmcpye.datarun.importprocessor.datastagedefinition;

import org.nmcpye.datarun.importprocessor.ImportContext;
import org.nmcpye.datarun.importprocessor.ImportStage;
import org.nmcpye.datarun.jpa.common.IdentifiableObjectManager;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
import org.nmcpye.datarun.jpa.stagedefinition.dto.StepTypeDto;
import org.nmcpye.datarun.jpa.stagedefinition.repository.StageDefinitionRepository;
import org.springframework.stereotype.Component;

/**
 * Write Stage (conditionally skipped)
 *
 * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
 */
@Component
public class DataStageDefinitionWriteStage implements ImportStage<StepTypeDto> {
    private final StageDefinitionRepository repository;
    private final FlowTypeRepository flowTypeRepository;
    private final DataTemplateRepository dataTemplateRepository;
    private final IdentifiableObjectManager identifiableObjectManager;

    public DataStageDefinitionWriteStage(StageDefinitionRepository repository,
                                         FlowTypeRepository flowTypeRepository,
                                         DataTemplateRepository dataTemplateRepository, IdentifiableObjectManager identifiableObjectManager) {
        this.repository = repository;
        this.flowTypeRepository = flowTypeRepository;
        this.dataTemplateRepository = dataTemplateRepository;
        this.identifiableObjectManager = identifiableObjectManager;
    }

    public void process(ImportContext<StepTypeDto> context) {
//        if (!context.isDryRun()) {
//            List<DataStageDefinition> users = context.getProcessed().stream()
//                .map(dto -> {
//                    final var assignmentType = assignmentTypeRepository.findByUid(dto.getAssignmentTypeUid());
//                    final var dataTemplate = dataTemplateRepository.findByUid(dto.getDataTemplateUid());
//                    final var dataStage = new DataStageDefinition(dto.getEmail(), dto.getName());
//
//                    return dataStage;
//                })
//                .toList();
//            repository.saveAll(users);
//        }
    }
}
