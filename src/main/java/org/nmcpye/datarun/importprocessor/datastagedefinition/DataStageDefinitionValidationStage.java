package org.nmcpye.datarun.importprocessor.datastagedefinition;

import org.nmcpye.datarun.importprocessor.ImportContext;
import org.nmcpye.datarun.importprocessor.ImportMessage;
import org.nmcpye.datarun.importprocessor.ImportStage;
import org.nmcpye.datarun.jpa.stagedefinition.dto.StepTypeDto;
import org.springframework.stereotype.Component;

/**
 * Validation Stage
 *
 * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
 */
@Component
public class DataStageDefinitionValidationStage implements ImportStage<StepTypeDto> {
    public void process(ImportContext<StepTypeDto> context) {
        for (StepTypeDto dto : context.processed()) {
            if (dto.getFlowTypeId() == null || dto.getFlowTypeId().isBlank()) {
                context.addMessage(new ImportMessage("Missing assignmentType", ImportMessage.Severity.ERROR));
            }

            if (dto.getDataTemplateId() == null || dto.getDataTemplateId().isBlank()) {
                context.addMessage(new ImportMessage("Missing DataTemplateUid", ImportMessage.Severity.ERROR));
            }

            if (dto.getName() == null || dto.getName().isBlank()) {
                context.addMessage(new ImportMessage("Missing name", ImportMessage.Severity.ERROR));
            }
        }
    }
}
