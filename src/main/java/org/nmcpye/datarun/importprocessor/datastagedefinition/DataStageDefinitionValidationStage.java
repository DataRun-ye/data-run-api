package org.nmcpye.datarun.importprocessor.datastagedefinition;

import org.nmcpye.datarun.importprocessor.ImportContext;
import org.nmcpye.datarun.importprocessor.ImportMessage;
import org.nmcpye.datarun.importprocessor.ImportStage;
import org.nmcpye.datarun.jpa.datastage.dto.DataStageDefinitionDto;
import org.springframework.stereotype.Component;

/**
 * Validation Stage
 *
 * @author Hamza Assada (02-06-2025), <7amza.it@gmail.com>
 */
@Component
public class DataStageDefinitionValidationStage implements ImportStage<DataStageDefinitionDto> {
    public void process(ImportContext<DataStageDefinitionDto> context) {
        for (DataStageDefinitionDto dto : context.getProcessed()) {
            if (dto.getAssignmentTypeUid() == null || dto.getAssignmentTypeUid().isBlank()) {
                context.addMessage(new ImportMessage("Missing assignmentType", ImportMessage.Severity.ERROR));
            }

            if (dto.getDataTemplateUid() == null || dto.getDataTemplateUid().isBlank()) {
                context.addMessage(new ImportMessage("Missing DataTemplateUid", ImportMessage.Severity.ERROR));
            }

            if (dto.getName() == null || dto.getName().isBlank()) {
                context.addMessage(new ImportMessage("Missing name", ImportMessage.Severity.ERROR));
            }
        }
    }
}
