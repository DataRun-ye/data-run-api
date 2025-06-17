package org.nmcpye.datarun.importprocessor.datastagedefinition;

import org.nmcpye.datarun.importprocessor.ImportContext;
import org.nmcpye.datarun.importprocessor.ImportMessage;
import org.nmcpye.datarun.importprocessor.ImportStage;
import org.nmcpye.datarun.jpa.stagedefinition.dto.StepTypeDto;
import org.nmcpye.datarun.jpa.stagedefinition.repository.StageDefinitionRepository;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
 */
@Component
public class DataStageDefinitionDiffStage implements ImportStage<StepTypeDto> {
    private final StageDefinitionRepository repository;

    public DataStageDefinitionDiffStage(StageDefinitionRepository repository) {
        this.repository = repository;
    }

    public void process(ImportContext<StepTypeDto> context) {
        for (StepTypeDto dto : context.processed()) {
            boolean exists = repository.existsById(dto.getId());
            context.addMessage(new ImportMessage(
                exists ? "Would update stage: " + dto.getId() : "Would create stage: " + dto.getId(),
                ImportMessage.Severity.INFO));
        }
    }
}

