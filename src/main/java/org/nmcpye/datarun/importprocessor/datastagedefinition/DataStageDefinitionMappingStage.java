package org.nmcpye.datarun.importprocessor.datastagedefinition;

import org.nmcpye.datarun.importprocessor.ImportContext;
import org.nmcpye.datarun.importprocessor.ImportStage;
import org.nmcpye.datarun.jpa.datastage.dto.DataStageDefinitionDto;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <02-06-2025>
 */
@Component
public class DataStageDefinitionMappingStage implements ImportStage<DataStageDefinitionDto> {
    public void process(ImportContext<DataStageDefinitionDto> context) {
        for (DataStageDefinitionDto dto : context.getRawData()) {
            // here you could do transformation if needed
            context.addProcessed(dto);
        }
    }
}
