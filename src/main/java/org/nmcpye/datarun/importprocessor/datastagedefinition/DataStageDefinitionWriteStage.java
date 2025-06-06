package org.nmcpye.datarun.importprocessor.datastagedefinition;

import org.nmcpye.datarun.importprocessor.ImportContext;
import org.nmcpye.datarun.importprocessor.ImportStage;
import org.nmcpye.datarun.jpa.assignmenttype.repository.AssignmentTypeRepository;
import org.nmcpye.datarun.jpa.common.IdentifiableObjectManager;
import org.nmcpye.datarun.jpa.datastage.dto.DataStageDefinitionDto;
import org.nmcpye.datarun.jpa.datastage.repository.DataStageDefinitionRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.springframework.stereotype.Component;

/**
 * Write Stage (conditionally skipped)
 *
 * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
 */
@Component
public class DataStageDefinitionWriteStage implements ImportStage<DataStageDefinitionDto> {
    private final DataStageDefinitionRepository repository;
    private final AssignmentTypeRepository assignmentTypeRepository;
    private final DataTemplateRepository dataTemplateRepository;
    private final IdentifiableObjectManager identifiableObjectManager;

    public DataStageDefinitionWriteStage(DataStageDefinitionRepository repository,
                                         AssignmentTypeRepository assignmentTypeRepository,
                                         DataTemplateRepository dataTemplateRepository, IdentifiableObjectManager identifiableObjectManager) {
        this.repository = repository;
        this.assignmentTypeRepository = assignmentTypeRepository;
        this.dataTemplateRepository = dataTemplateRepository;
        this.identifiableObjectManager = identifiableObjectManager;
    }

    public void process(ImportContext<DataStageDefinitionDto> context) {
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
