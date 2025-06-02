package org.nmcpye.datarun.importprocessor.datastagedefinition;

import org.nmcpye.datarun.importprocessor.ImportContext;
import org.nmcpye.datarun.importprocessor.ImportMessage;
import org.nmcpye.datarun.importprocessor.ImportStage;
import org.nmcpye.datarun.jpa.datastage.dto.DataStageDefinitionDto;
import org.nmcpye.datarun.jpa.datastage.repository.DataStageDefinitionRepository;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada (02-06-2025), <7amza.it@gmail.com>
 */
@Component
public class DataStageDefinitionDiffStage implements ImportStage<DataStageDefinitionDto> {
    private final DataStageDefinitionRepository repository;

    public DataStageDefinitionDiffStage(DataStageDefinitionRepository repository) {
        this.repository = repository;
    }

    public void process(ImportContext<DataStageDefinitionDto> context) {
        for (DataStageDefinitionDto dto : context.getProcessed()) {
            boolean exists = repository.existsByUid(dto.getUid());
            context.addMessage(new ImportMessage(
                exists ? "Would update stage: " + dto.getUid() : "Would create stage: " + dto.getUid(),
                ImportMessage.Severity.INFO));
        }
    }
}

